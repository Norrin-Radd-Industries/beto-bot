package beto.be.mcpbetobot.config;

import beto.be.mcpbetobot.process.github.McpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class McpConfig {

    @Value("${GITHUB_PERSONAL_ACCESS_TOKEN}")
    private String githubApiKey;


    @Bean
    public McpClient githubClient() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c",
                "npx -y @modelcontextprotocol/server-github@latest mcp-server-github");
        processBuilder.environment().put("GITHUB_PERSONAL_ACCESS_TOKEN", githubApiKey);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        return new McpClient(processBuilder);
    }
}

