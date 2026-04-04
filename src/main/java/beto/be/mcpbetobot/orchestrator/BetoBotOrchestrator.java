package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.events.GithubIssueEvent;
import beto.be.mcpbetobot.facilitator.Agent;
import beto.be.mcpbetobot.domain.GithubIssue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.*;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BetoBotOrchestrator {

    private final Logger logger = LoggerFactory.getLogger(BetoBotOrchestrator.class);

    private final McpAsyncClient githubMcpClientImpl;
    private final Agent agent;

    public BetoBotOrchestrator(List<McpAsyncClient> customMcpAsyncClientList, Agent agent) {
        this.githubMcpClientImpl = customMcpAsyncClientList.getFirst();
        this.agent = agent;
    }

    @EventListener
    public void processTicket(GithubIssueEvent issueEvent){
        GithubIssue issue = issueEvent.getGithubIssue();

        logger.info(">>> Orchestrating agent for issue: {} <<<", issue.number());

        githubMcpClientImpl.listTools() // hands agent tools from mcp
                .timeout(Duration.ofSeconds(60)) // give the agent some time to think
                .doOnSuccess(toolsList -> {
                    List<Tool> geminiTools = toolsList != null ? mapGithubToolsToGemini(toolsList.tools()) : Collections.emptyList();
                    String prompt = buildPrompt(issue);
                    // start thread to have it non-blocking
                    Thread.ofVirtual().start(() -> {
                        try {
                            startAgent(prompt, geminiTools);
                        } catch (Exception e) {
                            logger.error("Virtual Thread with agent failed: {}", e.getMessage());
                        }
                    });
                })
                .doOnError(error -> logger.error("Orchestration failed: {}", error.getMessage()))
                .subscribe();
    }


    private void startAgent(String prompt, List<Tool> tools) {
        List<Content> history = new ArrayList<>();
        //hand it our initial prompt
        history.add(buildMessage(prompt));

        boolean finished = false;
        while (!finished) {
            GenerateContentConfig config = GenerateContentConfig.builder().tools(tools).build();
            GenerateContentResponse response = agent.askWithTools(history, config);

            Content modelResponse = extractModelResponse(response);
            history.add(modelResponse);

            Optional<FunctionCall> toolCall = fetchToolCall(modelResponse);
            if (toolCall.isPresent()) {
                executeToolAndAddToHistory(toolCall.get(), history);
            } else {
                logger.info("---Answer: {}", extractText(modelResponse));
                finished = true;
            }
        }
    }

    // when the agent uses a tool we want to see which one and add it to the conversation history
    private void executeToolAndAddToHistory(FunctionCall call, List<Content> history) {
        if(call.name().isPresent() && call.args().isPresent()){
            String name = call.name().get();
            Map<String, Object> args = call.args().orElse(Collections.emptyMap());

            logger.info(" >>> Using mcp-tool: {}", name);

            try {
                McpSchema.CallToolResult result = githubMcpClientImpl.callTool(
                        new McpSchema.CallToolRequest(name, args)).block();
                if (result != null) {
                    String toolOutput = result.content().stream()
                            .map(content ->
                                    (content instanceof McpSchema.TextContent textContent) ? textContent.text()
                                            : content.toString())
                            .collect(Collectors.joining("\n"));
                    // new method to add feed back the response of a tool output
                    // eg: this will feed an error while calling a tool back into the agent so he can handle it
                    addFunctionResponseToHistory(history, name, toolOutput);
                }
            } catch (Exception e) {
                logger.warn("Tool {} failed with error: {}. Handing result back to agent",  name, e.getMessage());
                String error = "Error occurred: " + e.getMessage() + ", adjust your parameters to mitigate";
                addFunctionResponseToHistory(history, name, error);
            }
        }
    }


    private void addFunctionResponseToHistory(List<Content> history, String name, String output) {
        history.add(Content.builder().role("function")
                .parts(List.of(Part.builder()
                        .functionResponse(FunctionResponse.builder()
                                .name(name)
                                .response(Map.of("result", output))
                                .build())
                        .build()))
                .build());
    }


    /* <<< Helper methods >>> */

    private static @NonNull String buildPrompt(GithubIssue issue) {
        return String.format("""
                System context:
                Repository owner: SilverSurferState
                Repository name: beto-bot
                you must always provide these owner and repo values when calling tools
                
                Task:
                You are senior Java Developer. You need to fix or implement the following issue:
                
                Title: %s
                Description: %s
                
                Todo:
                1. Use 'get_file_contents' with path='.' to list the root directory and to understand the project
                2. Once you understand the project, implement or fix the issue
                3. Create a new branch named 'feature/issue-%d'
                4. Use 'push_files' to commit your changes and to that branch you just created
                5. Add the label 'beto-bot:in-progress' to the issue you've processed.
                6. Finish by using 'create_pull_request' to create a new pull request and
                summarizing what you changed in the 'body' section of the 'create_pull_request' function.
                """, issue.title(), issue.body(), issue.number());
    }

    private Content buildMessage(String text) {
        return Content.builder()
                .role("user")
                .parts(List.of(Part.builder()
                        .text(text)
                        .build()))
                .build();
    }

    // response has candidates -> has content -> has parts ( all optional )
    private Content extractModelResponse(GenerateContentResponse response) {
        return response.candidates()
                .flatMap(list -> list.stream().findFirst())
                .flatMap(Candidate::content)
                .orElseThrow(() -> new RuntimeException("Agent returned and empty response"));
    }

    // checks if the agent requires a tool to proceed
    private Optional<FunctionCall> fetchToolCall(Content content) {
        return content.parts().stream()
                .flatMap(List::stream)
                .map(Part::functionCall)
                .flatMap(Optional::stream)
                .findFirst();
    }

    // gathers the parts to form answer
    private String extractText(Content content) {
        return content.parts().stream()
                .flatMap(List::stream)
                .map(Part::text)
                .flatMap(Optional::stream)
                .collect(Collectors.joining("\n"));
    }

    // map tools from our mcp so agent knows its there
    private List<Tool> mapGithubToolsToGemini(List<McpSchema.Tool> githubTools){
        List<FunctionDeclaration> declarations = githubTools.stream()
                .map(githubTool -> FunctionDeclaration.builder()
                        .name(githubTool.name())
                        .description(githubTool.description())
                        .parameters(githubToGeminiSchema(githubTool.inputSchema())
                        ).build())
                .toList();

        return List.of(Tool.builder().functionDeclarations(declarations).build());
    }

    // method to parse the github jsonSchema to the geminiSchema TODO check if works for other agents
    private Schema githubToGeminiSchema(McpSchema.JsonSchema githubSchema) {
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            String schemaJson = mapper.writeValueAsString(githubSchema);
            return Schema.fromJson(schemaJson);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing githubSchema <-> geminiSchema");
        }
        return null;
    }
}
