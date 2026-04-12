package beto.be.mcpbetobot.agentic;

import beto.be.mcpbetobot.domain.GithubTask;
import beto.be.mcpbetobot.mcp.ProjectService;
import com.google.genai.Client;
import com.google.genai.types.*;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Agent {

    private final Logger logger = LoggerFactory.getLogger(Agent.class);
    private final Client client;
    private final String model;
    private final McpAsyncClient gitHubMcpClient;
    private final ProjectService projectService;

    public Agent(Client client, String model,
                 List<McpAsyncClient> customMcpAsyncClientList,
                 ProjectService projectService){
        this.client = client;
        this.model = model;
        this.gitHubMcpClient = customMcpAsyncClientList.getFirst();
        this.projectService = projectService;
    }

    public GenerateContentResponse askWithTools(List<Content> history, GenerateContentConfig config) {
        return client.models.generateContent(model, history, config);
    }

    public void start(GithubTask task, List<Tool> tools) {
        List<Content> history = new ArrayList<>();
        //hand it our initial prompt
        history.add(buildMessage(buildPrompt(task)));

        boolean finished = false;
        while (!finished) {
            GenerateContentConfig config = GenerateContentConfig.builder().tools(tools).build();
            GenerateContentResponse response = askWithTools(history, config);

            Content modelResponse = extractModelResponse(response);
            history.add(modelResponse);
            // sometimes we get multiple toolcalls in a response
            // we couldnt handle that, the agent would falsely finish
            // this adds the ability to handle multiple toolCalls
            List<FunctionCall> toolCalls = fetchAllToolCalls(modelResponse);
            if (!toolCalls.isEmpty()) {
                toolCalls.forEach(call -> executeToolAndAddToHistory(call, history));
            } else {
                String answer = extractText(modelResponse);
                if (answer != null && !answer.isBlank()){
                    logger.info("---Answer: {}", extractText(modelResponse));
                    finished = true;
                } else {
                    // could be empty response || hanging agent
                    logger.warn("Empty response, forcing agent to continue");
                    history.add(buildMessage("Please continue with the next step or try your last step again."));
                }
            }
        }
    }

    private List<FunctionCall> fetchAllToolCalls(Content modelResponse){
        return modelResponse.parts()
                .stream()
                .flatMap(Collection::stream)
                .map(Part::functionCall)
                .flatMap(Optional::stream)
                .toList();
    }

    abstract String buildPrompt(GithubTask task);

    // when we use a tool we want to see which one and add it to the conversation history
    private void executeToolAndAddToHistory(FunctionCall call, List<Content> history) {
        if(call.name().isPresent() && call.args().isPresent()){
            String name = call.name().get();
            Map<String, Object> args = call.args().orElse(Collections.emptyMap());

            logger.info(" >>> Using mcp-tool: {}", name);
            try {
                String toolOutput = switch (name) {
                    case "moveTaskToAnalysed" -> projectService.moveTaskToAnalysed((String) args.get("itemId"));
                    case "moveTaskToInProgress" -> projectService.moveTaskToInProgress((String) args.get("itemId"));
                    default -> {
                        McpSchema.CallToolResult result = gitHubMcpClient.callTool(
                                new McpSchema.CallToolRequest(name, args)).block();
                        if (result == null || result.content() == null) {
                            yield "No response from tool";
                        }
                        yield result.content().stream()
                                .map(content ->
                                        (content instanceof McpSchema.TextContent textContent) ? textContent.text()
                                                : content.toString())
                                .collect(Collectors.joining("\n"));
                    }
                };
                addFunctionResponseToHistory(history, name, toolOutput);
            // new method to feed back the response of a tool output
            // eg: this will feed an error while calling a tool back into the agent so he can handle it
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

    private Content buildMessage(String text) {
        return Content.builder()
                .role("user")
                .parts(List.of(Part.builder()
                        .text(text)
                        .build()))
                .build();
    }

    private Content extractModelResponse(GenerateContentResponse response) {
        return response.candidates()
                .flatMap(list -> list.stream().findFirst())
                .flatMap(Candidate::content)
                .orElseThrow(() -> new RuntimeException("Agent returned and empty response"));
    }

    // gathers the parts to form answer
    private String extractText(Content content) {
        return content.parts().stream()
                .flatMap(List::stream)
                .map(Part::text)
                .flatMap(Optional::stream)
                .collect(Collectors.joining("\n"));
    }

}
