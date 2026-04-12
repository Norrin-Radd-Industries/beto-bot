package beto.be.mcpbetobot.config;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Configuration
public class McpClientConfig {

    private final Logger logger = LoggerFactory.getLogger(McpClientConfig.class);

    @Bean
    @Primary
    public McpAsyncClient githubMcpClient() {
        String mcpUrl = "http://localhost:9090/sse";

        var transport = HttpClientSseClientTransport.builder(mcpUrl)
                .build();

        var client = McpClient.async(transport)
                .requestTimeout(Duration.ofMinutes(5)).build();
        try {
            logger.info("--- Connecting to GitHub MCP Proxy ---");
            client.initialize()
                    .retryWhen(Retry.fixedDelay(10, Duration.ofSeconds(2)))
                    .block(Duration.ofSeconds(30));
            logger.info("--- GITHUB MCP READY ---");
        } catch (Exception e) {
            logger.error("Failed to init with MCP Proxy: {}", e.getMessage());
        }
        return client;
    }

    @Bean
    public List<McpAsyncClient> customMcpAsyncClientList(McpAsyncClient githubMcpClient) {
        return List.of(githubMcpClient);
    }
}
