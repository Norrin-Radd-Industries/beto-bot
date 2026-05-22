package beto.be.mcpbetobot.infrastructure.orchestrator;

import beto.be.mcpbetobot.domain.usecases.FetchTasksUseCase;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class BetoBotTaskFetcher {

    private final FetchTasksUseCase fetchTasksUseCase;
    private final ApplicationEventPublisher applicationEventPublisher;

    public BetoBotTaskFetcher(FetchTasksUseCase fetchTasksUseCase,
                              ApplicationEventPublisher applicationEventPublisher) {
        this.fetchTasksUseCase = fetchTasksUseCase;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadInitialCodebase() {
        fetchTasksUseCase.loadInitialCodebase();
    }

    @Scheduled(fixedRate = 1800000, initialDelay = 30000) // 30 min, delay 30s
    public void checkForAvailableWork() {
        fetchTasksUseCase.checkForAvailableWork((task, type) ->
            applicationEventPublisher.publishEvent(new GitHubTaskEvent(this, task, type))
        );
    }
}
