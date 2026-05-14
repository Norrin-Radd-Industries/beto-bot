package beto.be.mcpbetobot;

import beto.be.mcpbetobot.github.GithubAppAuthService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(GithubAppAuthService.class)
public class McpBetobotApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpBetobotApplication.class, args);
    }

}
