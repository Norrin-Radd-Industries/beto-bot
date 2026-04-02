package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.process.github.McpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service
public class BetoBotOrchestrator implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(BetoBotOrchestrator.class);

    private final McpClient githubClient;
    private final Map<String, String> botAbilities = new HashMap<>();

    public BetoBotOrchestrator(McpClient mcpClient) {
        this.githubClient = mcpClient;
    }

    @Override
    public void run(String... args) throws Exception {
        githubClient.connect()
                .thenAccept(response -> {
                    logger.info("Connection is made");
                })
                .thenCompose(response -> githubClient.listTools())
                .thenAccept(logger::info)
                .exceptionally(ex -> null);
    }
}
