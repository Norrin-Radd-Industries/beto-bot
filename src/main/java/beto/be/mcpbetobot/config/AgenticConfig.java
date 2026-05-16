package beto.be.mcpbetobot.config;

import beto.be.mcpbetobot.github.GithubProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;

@Configuration
public class AgenticConfig {

    private final Logger log = LoggerFactory.getLogger(AgenticConfig.class);

    @Bean
    public ChatClient coderChatClient(ChatClient.Builder builder,
                                      @Value("${AGENT_MODEL_CODER}") String model,
                                      GithubProjectService githubProjectService) {
        return builder
                .defaultOptions(ChatOptions.builder()
                        .model(model)
                        .temperature(0.0))
                .defaultTools(githubProjectService)
                .build();
    }

    @Bean
    public ChatClient analystChatClient(ChatClient.Builder builder,
                                        @Value("${AGENT_MODEL_ANALYST}") String model,
                                        GithubProjectService githubProjectService) {
        return builder
                .defaultOptions(ChatOptions.builder()
                        .model(model)
                        .temperature(0.3))
                .defaultTools(githubProjectService)
                .build();
    }

    @Bean
    @Primary
    public ToolCallingManager mcpAwareToolCallingManager(
            SyncMcpToolCallbackProvider toolCallbackProvider) {

        var tools = toolCallbackProvider.getToolCallbacks();
        log.info("Registering mcpAwareToolCallingManager with {} MCP tools:", tools.length);
        Arrays.stream(tools).forEach(cb ->
                log.info("  - {}", cb.getToolDefinition().name()));

        ToolCallbackResolver mcpResolver = toolName ->
                Arrays.stream(toolCallbackProvider.getToolCallbacks())
                        .filter(toolCallBack -> toolCallBack.getToolDefinition().name().equals(toolName))
                        .findFirst()
                        .orElse(null);

        return DefaultToolCallingManager.builder()
                .toolCallbackResolver(mcpResolver)
                .build();
    }
}