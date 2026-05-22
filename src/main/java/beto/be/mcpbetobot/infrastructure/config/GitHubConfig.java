package beto.be.mcpbetobot.infrastructure.config;

import beto.be.mcpbetobot.data.github.GithubAppAuthService;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

@Configuration
public class GitHubConfig {

    @Bean
    public GitHub gitHubClient(GithubAppAuthService authService) throws IOException {
        return new GitHubBuilder()
                .withAuthorizationProvider(authService::getInstallationToken)
                .build();
    }

    @Bean
    @Primary
    public McpClientCustomizer<HttpClientStreamableHttpTransport.Builder> mcpClientCustomizer(GithubAppAuthService authService) {
        return (name, clientBuilder) -> {
            if (name.equals("github")) {
                clientBuilder.httpRequestCustomizer((builder, _, _, _, _) ->
                        builder.header("Authorization", "Bearer " + authService.getInstallationToken()));
            }
        };
    }
}
