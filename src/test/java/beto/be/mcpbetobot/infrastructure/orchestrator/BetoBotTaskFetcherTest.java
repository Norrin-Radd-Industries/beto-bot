package beto.be.mcpbetobot.infrastructure.orchestrator;

import beto.be.mcpbetobot.domain.entities.GithubTask;
import beto.be.mcpbetobot.domain.usecases.FetchTasksUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetoBotTaskFetcherTest {

    @Mock
    private FetchTasksUseCase fetchTasksUseCase;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private BetoBotTaskFetcher fetcher;

    @Test
    void loadInitialCodebaseShouldSyncAllRepositories() {
        fetcher.loadInitialCodebase();
        verify(fetchTasksUseCase, times(1)).loadInitialCodebase();
    }

    @Test
    void checkForAvailableWorkShouldInvokeUseCaseAndPublishEvent() {
        GithubTask task = new GithubTask("t1", "issT", 1, "Runnable Task", "Body", "OPEN", "repo", "owner", "CODER", List.of());

        doAnswer(invocation -> {
            BiConsumer<GithubTask, String> publisher = invocation.getArgument(0);
            publisher.accept(task, "CODER");
            return null;
        }).when(fetchTasksUseCase).checkForAvailableWork(any());

        fetcher.checkForAvailableWork();

        ArgumentCaptor<GitHubTaskEvent> eventCaptor = ArgumentCaptor.forClass(GitHubTaskEvent.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());

        GitHubTaskEvent publishedEvent = eventCaptor.getValue();
        assertEquals(task, publishedEvent.getGithubTask());
        assertEquals("CODER", publishedEvent.getType());
    }
}
