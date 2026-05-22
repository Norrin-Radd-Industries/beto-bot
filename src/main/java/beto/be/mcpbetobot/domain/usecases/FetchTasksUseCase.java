package beto.be.mcpbetobot.domain.usecases;

import beto.be.mcpbetobot.domain.entities.GithubTask;
import beto.be.mcpbetobot.domain.usecases.gateways.GithubProjectGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BiConsumer;

public class FetchTasksUseCase {

    private final Logger logger = LoggerFactory.getLogger(FetchTasksUseCase.class);
    private final GithubProjectGateway githubProjectGateway;
    private final SyncCodebaseUseCase syncCodebaseUseCase;

    public FetchTasksUseCase(GithubProjectGateway githubProjectGateway, SyncCodebaseUseCase syncCodebaseUseCase) {
        this.githubProjectGateway = githubProjectGateway;
        this.syncCodebaseUseCase = syncCodebaseUseCase;
    }

    public void loadInitialCodebase() {
        logger.info(">>> Initializing codebase to vector sync");
        syncCodebaseUseCase.syncAllRepositories();
    }

    public void checkForAvailableWork(BiConsumer<GithubTask, String> taskPublisher) {
        logger.info(">>> Checking for available work");
        List<GithubTask> githubTasks = githubProjectGateway.getAvailableTasks();

        List<GithubTask> runnableTasks = githubTasks.stream()
                .filter(GithubTask::isRunnable)
                .toList();

        if (runnableTasks.isEmpty()) {
            logger.info(" -- All tasks are currently blocked or completed -- ");
            return;
        }

        githubTasks.forEach(task -> taskPublisher.accept(task, task.type()));
    }
}
