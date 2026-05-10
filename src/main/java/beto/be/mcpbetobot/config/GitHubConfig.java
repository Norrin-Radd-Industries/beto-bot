package beto.be.mcpbetobot.config;

import beto.be.mcpbetobot.github.GithubAppAuthService;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class GitHubConfig {

    @Bean
    public GitHub gitHubClient(GithubAppAuthService authService) throws IOException {
        return new GitHubBuilder()
                .withAuthorizationProvider(authService::getInstallationToken)
                .build();
    }
}
