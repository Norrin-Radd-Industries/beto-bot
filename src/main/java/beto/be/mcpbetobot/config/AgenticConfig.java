package beto.be.mcpbetobot.config;

import beto.be.mcpbetobot.github.GithubProjectService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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
                .defaultAdvisors(
                        SimpleLoggerAdvisor.builder().build())
                .defaultTools(githubProjectService)
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    @Bean
    public ChatClient analystChatClient(ChatClient.Builder builder,
                                        VectorStore vectorStore,
                                        @Value("${AGENT_MODEL_ANALYST}") String model,
                                        SyncMcpToolCallbackProvider toolCallbackProvider,
                                        GithubProjectService githubProjectService) {
        return builder
                .defaultOptions(ChatOptions.builder()
                        .model(model)
                        .temperature(0.3))
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder()
                                .topK(5)
                                .similarityThreshold(0.7)
                                .build())
                        .build())
                .defaultTools(githubProjectService)
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }
}