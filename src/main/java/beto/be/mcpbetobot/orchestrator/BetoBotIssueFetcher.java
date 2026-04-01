package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.events.GithubIssueEvent;
import beto.be.mcpbetobot.messages.response.GithubIssue;
import beto.be.mcpbetobot.process.github.McpClient;
import beto.be.mcpbetobot.util.Parser;
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
    private final McpClient githubClient;
    private final ApplicationEventPublisher applicationEventPublisher;

    public BetoBotIssueFetcher(McpClient githubClient, ApplicationEventPublisher applicationEventPublisher) {
        this.githubClient = githubClient;
        this.applicationEventPublisher = applicationEventPublisher;
    }
    //TODO make it more generic instead of limiting it one repo
    @Scheduled(fixedRate = 18000000)
    public void checkForAvailableWork() {
        logger.info(" --Checking for available work-- ");
        githubClient.callTool("list_issues", Map.of("owner", "SilverSurferState",
                        "repo", "beto-bot",
                        "state", "open"))
                .thenAccept(issues -> {
                    List<GithubIssue> newIssues = Parser.parseIssues(issues);
                // send an event to the coder tool when new issues are found
                    newIssues.forEach(issue -> {
                        applicationEventPublisher.publishEvent(new GithubIssueEvent("Fetcher", issue));
                    });
                });
    }
}
