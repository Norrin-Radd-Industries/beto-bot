package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.domain.GithubTask;
import beto.be.mcpbetobot.events.GitHubTaskEvent;
import beto.be.mcpbetobot.github.GithubProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetoBotTaskFetcherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private GithubProjectService githubProjectService;

    @Mock
    private CodebaseSyncService codebaseSyncService;

    @InjectMocks
    private BetoBotTaskFetcher taskFetcher;

    @Test
    void checkForAvailableWork_ShouldFilterOutBlockedTasksAndPublishEvents() {
        // Given
        GithubTask runnableTask = new GithubTask("item1", "issue1", 1, "Title", "Body", "OPEN", "repo", "owner", "ANALYSIS", List.of());
        GithubTask blockedTask = new GithubTask("item2", "issue2", 2, "Title", "Body", "OPEN", "repo", "owner", "CODER", List.of(new GithubTask(null, null, 1, null, null, "OPEN", null, null, null, List.of())));
        
        when(githubProjectService.getAvailableTasks()).thenReturn(List.of(runnableTask, blockedTask));

        // When
        taskFetcher.checkForAvailableWork();

        // Then
        verify(githubProjectService, times(1)).getAvailableTasks();
        verify(applicationEventPublisher, times(1)).publishEvent(any(GitHubTaskEvent.class));
        // Only the runnable task should be published
    }

    @Test
    void checkForAvailableWork_ShouldNotPublishEventsWhenNoTasks() {
        // Given
        when(githubProjectService.getAvailableTasks()).thenReturn(List.of());

        // When
        taskFetcher.checkForAvailableWork();

        // Then
        verify(githubProjectService, times(1)).getAvailableTasks();
        verify(applicationEventPublisher, never()).publishEvent(any(GitHubTaskEvent.class));
    }

    @Test
    void checkForAvailableWork_ShouldPublishEventsForRunnableTasks() {
        // Given
        GithubTask task1 = new GithubTask("item1", "issue1", 1, "Title", "Body", "OPEN", "repo", "owner", "ANALYSIS", List.of());
        GithubTask task2 = new GithubTask("item2", "issue2", 2, "Title", "Body", "OPEN", "repo", "owner", "CODER", List.of());
        
        when(githubProjectService.getAvailableTasks()).thenReturn(List.of(task1, task2));

        // When
        taskFetcher.checkForAvailableWork();

        // Then
        verify(githubProjectService, times(1)).getAvailableTasks();
        verify(applicationEventPublisher, times(2)).publishEvent(any(GitHubTaskEvent.class));
    }

    @Test
    void loadInitialCodebase_ShouldCallCodebaseSyncService() {
        // When
        taskFetcher.loadInitialCodebase();

        // Then
        verify(codebaseSyncService, times(1)).syncAllRepositories();
    }
}