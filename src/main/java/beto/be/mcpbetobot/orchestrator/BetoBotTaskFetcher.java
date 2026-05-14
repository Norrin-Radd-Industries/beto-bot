package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.domain.GithubTask;
import beto.be.mcpbetobot.events.GitHubTaskEvent;
import beto.be.mcpbetobot.github.CodeBaseVectorService;
import beto.be.mcpbetobot.github.GithubProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * A scheduler to run a fetch every 30 min for issues on a specific GitHub project
 * This doesn't leverage any LLM, so it's cheap in that sense
 */
@Service
public class BetoBotTaskFetcher {

    private final Logger logger = LoggerFactory.getLogger(BetoBotTaskFetcher.class);
    private final GithubProjectService githubProjectService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final CodeBaseVectorService codeBaseVectorService;

    public BetoBotTaskFetcher(ApplicationEventPublisher applicationEventPublisher,
                              GithubProjectService githubProjectService,
                              CodeBaseVectorService codeBaseVectorService) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.githubProjectService = githubProjectService;
        this.codeBaseVectorService = codeBaseVectorService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadInitialCodebase() {
        logger.info("-- Initializing codebase to vector sync --");
        List<String> myRepos = githubProjectService.fetchLinkedRepositoryNames();

        try {
            for (String repo : myRepos) {
                List<Document> repoCodeFiles = githubProjectService.fetchEntireRepository(repo);
                codeBaseVectorService.saveCodeDocuments(repoCodeFiles);
                logger.info("Synced vectorDB with {} files for {}", repoCodeFiles.size(), repo);
            }
        } catch (Exception e) {
            logger.error("Failure during fetch of repo", e);
        }
    }

    @Scheduled(fixedRate = 1800000, initialDelay = 60000) // 30 min, delay 1min
    public void checkForAvailableWork() {
        logger.info(" --Checking for available work-- ");
        // get all available tasks in the project setup
        List<GithubTask> githubTasks = githubProjectService.getAvailableTasks();
        // convert them to githubTasks with types
        if (githubTasks.isEmpty()) {
            logger.info(" --Currently no tasks to be done, will check again in 30 min!-- ");
        }
        // send events
        githubTasks.forEach(task -> publishEvent(task, task.type()));
    }

    private void publishEvent(GithubTask task, String type){
        applicationEventPublisher.publishEvent(new GitHubTaskEvent(this, task, type));
    }
}
