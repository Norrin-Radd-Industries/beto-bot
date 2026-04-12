package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.agentic.Agent;
import beto.be.mcpbetobot.agentic.AnalystAgent;
import beto.be.mcpbetobot.agentic.CodingAgent;
import beto.be.mcpbetobot.domain.GithubTask;
import beto.be.mcpbetobot.events.GitHubTaskEvent;
import com.google.genai.types.Tool;
import io.modelcontextprotocol.client.McpAsyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

import static beto.be.mcpbetobot.util.GithubParser.getAllTools;

@Service
public class BetoBotOrchestrator {

    private final Logger logger = LoggerFactory.getLogger(BetoBotOrchestrator.class);

    private final McpAsyncClient githubMcpClientImpl;
    private final CodingAgent codingAgent;
    private final AnalystAgent analystAgent;

    public BetoBotOrchestrator(List<McpAsyncClient> customMcpAsyncClientList,
                               CodingAgent codingAgent,
                               AnalystAgent analystAgent) {
        this.githubMcpClientImpl = customMcpAsyncClientList.getFirst();
        this.codingAgent = codingAgent;
        this.analystAgent = analystAgent;
    }

    @EventListener
    public void processEvent(GitHubTaskEvent taskEvent){
        GithubTask task = taskEvent.getGithubTask();
        githubMcpClientImpl.listTools() // hands agent tools from external mcp-server
                .timeout(Duration.ofSeconds(60)) // give the agent some time to think
                .doOnSuccess(toolsList -> {
                    List<Tool> allTools = getAllTools(toolsList);
                    if (task.type().equals("ANALYSIS")) {
                        runAgent(task, allTools, analystAgent);
                    } else if (task.type().equals("CODER")) {
                        runAgent(task, allTools, codingAgent);
                    }
                })
                .doOnError(error -> logger.error("Assignment failed: {}", error.getMessage()))
                .subscribe();
    }

    private void runAgent(GithubTask task,List<Tool> tools, Agent agent){
        // start virtual thread to have agents be non-blocking for platform threads
        logger.info(">>> Assigning {} agent for task: {} <<<", task.type(), task.number());
        Thread.ofVirtual().start(() -> {
            try {
                agent.start(task, tools);
            } catch (Exception e) {
                logger.error("Virtual Thread with agent failed: {}", e.getMessage());
            }
        });
    }
}
