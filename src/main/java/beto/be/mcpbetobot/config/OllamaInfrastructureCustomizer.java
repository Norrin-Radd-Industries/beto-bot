package beto.be.mcpbetobot.config;

import org.springframework.ai.mcp.McpToolNamePrefixGenerator;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(OllamaChatModel.class)
public class OllamaInfrastructureCustomizer {

    @Bean
    public OllamaChatOptions defaultOllamaChatOptions() {
        return OllamaChatOptions.builder()
                .disableThinking()
                .build();
    }

    @Bean
    public McpToolNamePrefixGenerator mcpToolNamePrefixGenerator() {
        return McpToolNamePrefixGenerator.noPrefix();
    }
}