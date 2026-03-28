package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.messages.response.toolresponse.GithubIssue;
import beto.be.mcpbetobot.messages.response.toolresponse.ListIssuesParams;
import beto.be.mcpbetobot.process.github.McpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BetoBotOrchestrator implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(BetoBotOrchestrator.class);

    private final McpClient githubClient;

    public BetoBotOrchestrator(McpClient mcpClient) {
        this.githubClient = mcpClient;
    }

    @Override
    public void run(String... args) throws Exception {
        ListIssuesParams myRepo = new ListIssuesParams("SilverSurferState", "beto-bot");
        githubClient.connect()
                .thenCompose(v -> githubClient.callTool("list_issues", myRepo)
                .thenAccept(response -> {
                    try {
                        List<GithubIssue> issues = githubClient.parseIssues(response);
                        logger.info(">>>ISSUES<<<");
                        for(GithubIssue issue: issues) {
                            logger.info("-----------Start------------");
                            logger.info("Title: {}", issue.title());
                            logger.info("Issue: {}", issue.body());
                            logger.info("-----------End-----------");
                        }
                    } catch (Exception e) {
                        logger.error("Parse failed");
                    }
                })
                        .exceptionally(ex -> {
                            logger.error("Failed to call tool: ", ex);
                            return null;
                        }));
    }
}
