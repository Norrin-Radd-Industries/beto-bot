package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.domain.GithubTask;
import beto.be.mcpbetobot.events.GitHubTaskEvent;
import beto.be.mcpbetobot.github.GithubProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetoBotTaskFetcherTest {

    @Mock
    private GithubProjectService githubProjectService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private CodebaseSyncService codebaseSyncService;

    @InjectMocks
    private BetoBotTaskFetcher fetcher;

    @Test
    void loadInitialCodebaseShouldSyncAllRepositories() {
        fetcher.loadInitialCodebase();
        verify(codebaseSyncService, times(1)).syncAllRepositories();
    }

    @Test
    void checkForAvailableWorkShouldDoNothingWhenNoTasks() {
        when(githubProjectService.getAvailableTasks()).thenReturn(Collections.emptyList());

        fetcher.checkForAvailableWork();

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void checkForAvailableWorkShouldDoNothingWhenAllTasksBlockedOrClosed() {
        // Blocked task
        GithubTask blocker = new GithubTask("b1", "issB", 2, "Blocker", "Body", "OPEN", "repo", "owner", "CODER", List.of());
        GithubTask task = new GithubTask("t1", "issT", 1, "Blocked Task", "Body", "OPEN", "repo", "owner", "CODER", List.of(blocker));

        when(githubProjectService.getAvailableTasks()).thenReturn(List.of(task));

        fetcher.checkForAvailableWork();

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void checkForAvailableWorkShouldPublishEventsOnlyForRunnableTasks() {
        GithubTask runnableTask = new GithubTask("t1", "issT", 1, "Runnable Task", "Body", "OPEN", "repo", "owner", "CODER", List.of());
        GithubTask blockedTask = new GithubTask("t2", "issT2", 2, "Blocked Task", "Body", "OPEN", "repo", "owner", "ANALYSIS", 
                List.of(new GithubTask("b1", "issB", 3, "Blocker", "Body", "OPEN", "repo", "owner", "CODER", List.of())));

        when(githubProjectService.getAvailableTasks()).thenReturn(List.of(runnableTask, blockedTask));

        fetcher.checkForAvailableWork();

        ArgumentCaptor<GitHubTaskEvent> eventCaptor = ArgumentCaptor.forClass(GitHubTaskEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());

        List<GitHubTaskEvent> publishedEvents = eventCaptor.getAllValues();
        assertEquals(1, publishedEvents.size());

        GitHubTaskEvent firstEvent = publishedEvents.get(0);
        assertEquals(runnableTask, firstEvent.getGithubTask());
        assertEquals("CODER", firstEvent.getType());
    }
}
