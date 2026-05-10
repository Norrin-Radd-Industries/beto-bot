package beto.be.mcpbetobot.config;

import beto.be.mcpbetobot.github.GithubProjectService;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.StaticToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration
public class AgenticConfig {

    @Value("${GITHUB_MCP_SERVER_ADDRESS}")
    private String githubMcpServerAddress;

    @Bean
    @Primary
    public List<McpSyncClient> httpMcpSyncClients(@Value("${GITHUB_PERSONAL_ACCESS_TOKEN}") String mcpKey) {
        var transport = HttpClientStreamableHttpTransport.builder(githubMcpServerAddress)
                .customizeRequest(requestBuilder ->
                    requestBuilder
                        .setHeader("Authorization", "Bearer " + mcpKey)
                        .setHeader("X-MCP-Toolsets", "all")
                        .setHeader("X-MCP-Insiders", "true"))
                    .build();

        var client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .build();

        client.initialize();
        return List.of(client);
    }


    // ToolCallbackProvider find all tools automagically :-0
    @Bean
    public ChatClient coderChatClient(ChatClient.Builder builder,
                                      @Value("${AGENT_MODEL_CODER}") String model,
                                      @Qualifier("sanityCheckToolCallbackProvider") ToolCallbackProvider mcpToolProvider,
                                      GithubProjectService githubProjectService) {
        return builder
                .defaultOptions(ChatOptions.builder()
                        .model(model)
                        .build())
                .defaultAdvisors(
                        SimpleLoggerAdvisor.builder().build())
                .defaultTools(githubProjectService)
                .defaultToolCallbacks(mcpToolProvider)
                .build();
    }

    @Bean
    public ChatClient analystChatClient(ChatClient.Builder builder,
                                        @Value("${AGENT_MODEL_ANALYST}") String model,
                                        @Qualifier("sanityCheckToolCallbackProvider") ToolCallbackProvider mcpToolProvider,
                                        GithubProjectService githubProjectService) {
        return builder
                .defaultOptions(ChatOptions.builder()
                        .model(model)
                        .build())
                .defaultAdvisors(
                        SimpleLoggerAdvisor.builder().build())
                .defaultTools(githubProjectService)
                .defaultToolCallbacks(mcpToolProvider)
                .build();
    }


    // using this bean because there is an issue with mcp returning a simple string error instead of json
    @Bean
    @Primary
    public ToolCallbackProvider sanityCheckToolCallbackProvider(List<McpSyncClient> mcpSyncClients) {
        ToolCallback[] originalCallbacks = SyncMcpToolCallbackProvider.builder()
                .mcpClients(mcpSyncClients)
                .build()
                .getToolCallbacks();

        List<ToolCallback> wrappedCallbacks = Arrays.stream(originalCallbacks)
                .map(delegate -> (ToolCallback) new ToolCallback() {
                    @Override
                    public @NonNull ToolDefinition getToolDefinition() { return delegate.getToolDefinition(); }

                    @Override
                    public @NonNull String call(@NonNull String arguments) {
                        try {
                            String result = delegate.call(arguments);
                            if (!result.trim().startsWith("{") && !result.trim().startsWith("[")) {
                                return "{\"result\": \"" + result.replace("\"", "\\\"") + "\"}";
                            }
                            return result;
                        } catch (Exception e) {
                            return "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
                        }
                    }
                })
                .toList();

        return new StaticToolCallbackProvider(wrappedCallbacks);
    }
}
