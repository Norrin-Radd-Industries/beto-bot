package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.process.github.McpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
public class BetoBotOrchestrator implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(BetoBotOrchestrator.class);

    private final McpClient githubClient;

    public BetoBotOrchestrator(McpClient mcpClient) {
        this.githubClient = mcpClient;
    }

    @Override
    public void run(String... args) throws Exception {
        githubClient.connect()
                .thenCompose(v -> githubClient.listTools())
                .thenAccept(tools -> {
                    logger.info("Connection is made");
                });
    }
}
