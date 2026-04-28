package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.domain.GithubTask;
import beto.be.mcpbetobot.events.GitHubTaskEvent;
import beto.be.mcpbetobot.util.GithubParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static beto.be.mcpbetobot.util.GithubParser.getQuery;

/**
 * A scheduler to run a fetch every 30 min for issues on a specific GitHub project
 * This doesn't leverage any LLM, so it's cheap in that sense
 */
@Service
public class BetoBotTaskFetcher {

    @Value("${GITHUB_PERSONAL_ACCESS_TOKEN}")
    private String apiKey;

    @Value("${GITHUB_PROJECT_ID}")
    private String projectId;

    @Value("classpath:graphql/fetch-tasks-from-projects.graphql")
    private Resource fetchTasksFromProjects;

    private final RestClient restClient;
    private final Logger logger = LoggerFactory.getLogger(BetoBotTaskFetcher.class);
    private final ApplicationEventPublisher applicationEventPublisher;

    public BetoBotTaskFetcher(ApplicationEventPublisher applicationEventPublisher) {
        this.restClient = RestClient.create();
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Scheduled(fixedRate = 1800000, initialDelay = 5000) // 30 min, delay 5s
    public void checkForAvailableWork() {
        logger.info(" --Checking for available work-- ");
        // get all available tasks in the project setup
        String availableTasks = getGithubTasks();
        // convert them to githubTasks with types
        List<GithubTask> githubTasks = GithubParser.parseTasksFromProject(availableTasks);
        if (githubTasks.isEmpty()) {
            logger.info(" --Currently no tasks to be done, will check again in 30 min!-- ");
        }
        // send events
        githubTasks.forEach(task -> publishEvent(task, task.type()));
    }

    private void publishEvent(GithubTask task, String type){
        applicationEventPublisher.publishEvent(new GitHubTaskEvent(this, task, type));
    }

    // custom graphql GitHub project task fetcher
    private String getGithubTasks(){
        try {
            Map<String, Object> requestBody = Map.of(
                    "query", getQuery(fetchTasksFromProjects),
                    "variables", Map.of("projectId", projectId));

            return restClient.post()
                    .uri("https://api.github.com/graphql")
                    .header("Authorization", "bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (IOException e) {
            logger.error("Failure trying to fetch tasks", e);
            throw new RuntimeException(e);
        }
    }
}
