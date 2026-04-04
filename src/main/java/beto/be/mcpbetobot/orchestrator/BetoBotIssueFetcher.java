package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.events.GithubIssueEvent;
import beto.be.mcpbetobot.util.Parser;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * A scheduler to run a fetch every 30 min for issues on a specific github repo
 * This doesnt leverage any LLM, so its cheap in that sense
 */
@Service
public class BetoBotIssueFetcher {

    private final Logger logger = LoggerFactory.getLogger(BetoBotIssueFetcher.class);
    private final McpAsyncClient githubMcpClientImpl;
    private final ApplicationEventPublisher applicationEventPublisher;

    public BetoBotIssueFetcher(List<McpAsyncClient> customMcpAsyncClientList,
                               ApplicationEventPublisher applicationEventPublisher) {
        this.githubMcpClientImpl = customMcpAsyncClientList.getFirst();
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Scheduled(fixedRate = 18000000, initialDelay = 5000) // 30 min, delay 5s
    public void checkForAvailableWork() {
        logger.info(" --Checking for available work-- ");
        githubMcpClientImpl.callTool(new McpSchema.CallToolRequest("list_issues",
                Map.of("owner", "SilverSurferState",
                        "repo", "beto-bot",
                        "state", "open")))
                .flatMapIterable(result -> {
                    String json = result.content().stream()
                            .filter(content -> content instanceof McpSchema.TextContent)
                            .map(content -> (((McpSchema.TextContent) content).text()))
                            .findFirst()
                            .orElse("");
                    return Parser.parseIssues(json);
                }).doOnNext(issue ->
                        applicationEventPublisher.publishEvent(
                                new GithubIssueEvent(this, issue)))
                .subscribe();
    }
}
