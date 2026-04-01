package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.events.GithubIssueEvent;
import beto.be.mcpbetobot.facilitator.GeminiAgent;
import beto.be.mcpbetobot.messages.response.GithubIssue;
import beto.be.mcpbetobot.process.github.McpClient;
import beto.be.mcpbetobot.util.Parser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.genai.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BetoBotOrchestrator implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(BetoBotOrchestrator.class);

    private final McpClient githubClient;
    private final GeminiAgent geminiAgent;
    private List<Tool> cachedGeminiTools = new ArrayList<>();

    public BetoBotOrchestrator(McpClient mcpClient, GeminiAgent geminiAgent) {
        this.githubClient = mcpClient;
        this.geminiAgent = geminiAgent;
    }

    /**
     * Initial handshake and tool fetch + cache
     */
    @Override
    public void run(String... args) {
        githubClient.connect()
                .thenCompose(v -> githubClient.listTools())
                .thenAccept(tools -> {
                    try {
                       cachedGeminiTools = Parser.parseGithubMcpToolsToGeminiTools(tools);
                    } catch (JsonProcessingException e) {
                        logger.error("Error parsing Tools");
                        throw new RuntimeException(e);
                    }
                });
    }

    private void startAgent(String prompt, List<Tool> tools) {
        List<Content> history = new ArrayList<>();
        history.add(Content.builder()
                .role("user")
                .parts(List.of(Part.builder()
                        .text(prompt)
                        .build()))
                .build());

        boolean finished = false;
        while (!finished) {
            GenerateContentConfig config = GenerateContentConfig.builder().tools(tools).build();
            GenerateContentResponse response = geminiAgent.askWithTools(history, config);

            // add the llm's response to the history too
            Content modelResponse = response.candidates()
                    .flatMap(list -> list.stream().findFirst())
                    .flatMap(Candidate::content)
                    .orElse(Content.builder().build());

            history.add(modelResponse);

            List<Part> parts = response.candidates()
                    .flatMap(list -> list.stream().findFirst())
                    .flatMap(Candidate::content)
                    .flatMap(Content::parts)
                    .orElse(Collections.emptyList());

            if (parts.isEmpty()) {
                logger.error("Empty response or gated response by LLM");
                finished = true;
                continue;
            }

            Optional<FunctionCall> toolCall = parts.stream()
                    .map(Part::functionCall)
                    .flatMap(Optional::stream)
                    .findFirst();

            if (toolCall.isPresent()) {
                FunctionCall call = toolCall.get();
                logger.info("---Using {}", call.name());
                if (call.args().isPresent() && call.name().isPresent()) {
                    String result = githubClient.callTool(call.name().get(), call.args().get()).join();
                    history.add(Content.builder().role("function")
                            .parts(List.of(Part.builder()
                                    .functionResponse(FunctionResponse.builder()
                                            .name(call.name().get())
                                            .response(Map.of("result", result))
                                            .build())
                                    .build()))
                            .build());
                }
            } else {
                String answer = parts.stream()
                        .map(Part::text)
                        .flatMap(Optional::stream)
                        .findFirst()
                        .orElse("No answer given");

                logger.info("---Answer: {}", answer);
                finished = true;
            }
        }
    }

    @EventListener
    public void processTicket(GithubIssueEvent issueEvent){
        GithubIssue issue = issueEvent.getGithubIssue();
        logger.info(">>> Agent starting on issue: {} <<<", issue.number());

        String prompt = String.format("""
                System context:
                Repository owner: SilverSurferState
                Repository name: beto-bot
                you must always provide these owner and repo values when calling tools
                
                Task:
                You are senior Java Developer. You need to fix or implement the following issue:
                
                Title: %s
                Description: %s
                
                Todo:
                1. Use 'get_file_contents' to understand the project
                2. Once you understand the project, implement or fix the issue
                3. Create a new branch named 'feature/issue-%d'
                4. Use 'push_files' to commit your changes and to that branch you just created
                5. Finish by summarizing what you changed
                """, issue.title(), issue.body(), issue.number());

        if (!this.cachedGeminiTools.isEmpty()){
            startAgent(prompt, this.cachedGeminiTools);
        } else {
            logger.error("no tools found, check handshake logic");
        }
    }
}
