package beto.be.mcpbetobot.infrastructure.config;

import beto.be.mcpbetobot.domain.usecases.FetchTasksUseCase;
import beto.be.mcpbetobot.domain.usecases.ProcessTaskUseCase;
import beto.be.mcpbetobot.domain.usecases.SyncCodebaseUseCase;
import beto.be.mcpbetobot.domain.usecases.gateways.AgentGateway;
import beto.be.mcpbetobot.domain.usecases.gateways.GithubProjectGateway;
import beto.be.mcpbetobot.domain.usecases.gateways.VectorStoreGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public SyncCodebaseUseCase syncCodebaseUseCase(GithubProjectGateway githubProjectGateway,
                                                   VectorStoreGateway vectorStoreGateway) {
        return new SyncCodebaseUseCase(githubProjectGateway, vectorStoreGateway);
    }

    @Bean
    public ProcessTaskUseCase processTaskUseCase(AgentGateway analystAgent,
                                                 AgentGateway codingAgent) {
        return new ProcessTaskUseCase(analystAgent, codingAgent);
    }

    @Bean
    public FetchTasksUseCase fetchTasksUseCase(GithubProjectGateway githubProjectGateway,
                                               SyncCodebaseUseCase syncCodebaseUseCase) {
        return new FetchTasksUseCase(githubProjectGateway, syncCodebaseUseCase);
    }
}
