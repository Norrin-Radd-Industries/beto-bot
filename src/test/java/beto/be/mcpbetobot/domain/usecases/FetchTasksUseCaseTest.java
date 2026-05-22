package beto.be.mcpbetobot.domain.usecases;

import beto.be.mcpbetobot.domain.entities.GithubTask;
import beto.be.mcpbetobot.domain.usecases.gateways.GithubProjectGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FetchTasksUseCaseTest {

    @Mock
    private GithubProjectGateway githubProjectGateway;

    @Mock
    private SyncCodebaseUseCase syncCodebaseUseCase;

    @InjectMocks
    private FetchTasksUseCase fetchTasksUseCase;

    @Test
    void loadInitialCodebaseShouldSyncAllRepositories() {
        fetchTasksUseCase.loadInitialCodebase();
        verify(syncCodebaseUseCase, times(1)).syncAllRepositories();
    }

    @Test
    void checkForAvailableWorkShouldDoNothingWhenNoTasks() {
        when(githubProjectGateway.getAvailableTasks()).thenReturn(Collections.emptyList());

        AtomicInteger publishCount = new AtomicInteger();
        fetchTasksUseCase.checkForAvailableWork((task, type) -> publishCount.incrementAndGet());

        assertEquals(0, publishCount.get());
    }

    @Test
    void checkForAvailableWorkShouldDoNothingWhenAllTasksBlockedOrClosed() {
        GithubTask blocker = new GithubTask("b1", "issB", 2, "Blocker", "Body", "OPEN", "repo", "owner", "CODER", List.of());
        GithubTask task = new GithubTask("t1", "issT", 1, "Blocked Task", "Body", "OPEN", "repo", "owner", "CODER", List.of(blocker));

        when(githubProjectGateway.getAvailableTasks()).thenReturn(List.of(task));

        AtomicInteger publishCount = new AtomicInteger();
        fetchTasksUseCase.checkForAvailableWork((t, type) -> publishCount.incrementAndGet());

        assertEquals(0, publishCount.get());
    }

    @Test
    void checkForAvailableWorkShouldPublishEventsOnlyForRunnableTasks() {
        GithubTask runnableTask = new GithubTask("t1", "issT", 1, "Runnable Task", "Body", "OPEN", "repo", "owner", "CODER", List.of());
        GithubTask blockedTask = new GithubTask("t2", "issT2", 2, "Blocked Task", "Body", "OPEN", "repo", "owner", "ANALYSIS",
                List.of(new GithubTask("b1", "issB", 3, "Blocker", "Body", "OPEN", "repo", "owner", "CODER", List.of())));

        when(githubProjectGateway.getAvailableTasks()).thenReturn(List.of(runnableTask, blockedTask));

        List<GithubTask> published = new ArrayList<>();
        fetchTasksUseCase.checkForAvailableWork((t, type) -> published.add(t));

        assertEquals(1, published.size());
        assertTrue(published.contains(runnableTask));
        assertFalse(published.contains(blockedTask));
    }
}
