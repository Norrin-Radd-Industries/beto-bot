package beto.be.mcpbetobot.config;

import beto.be.mcpbetobot.github.GithubProjectService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgenticConfig {

    @Bean
    public ChatClient coderChatClient(ChatClient.Builder builder,
                                      @Value("${AGENT_MODEL_CODER}") String model,
                                      SyncMcpToolCallbackProvider toolCallbackProvider,
                                      GithubProjectService githubProjectService) {
        return builder
                .defaultOptions(ChatOptions.builder()
                        .model(model)
                        .temperature(0.0))
                .defaultTools(githubProjectService)
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    @Bean
    public ChatClient analystChatClient(ChatClient.Builder builder,
                                        @Value("${AGENT_MODEL_ANALYST}") String model,
                                        SyncMcpToolCallbackProvider toolCallbackProvider,
                                        GithubProjectService githubProjectService) {
        return builder
                .defaultOptions(ChatOptions.builder()
                        .model(model)
                        .temperature(0.3))
                .defaultTools(githubProjectService)
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }
}