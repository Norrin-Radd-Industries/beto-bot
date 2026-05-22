package beto.be.mcpbetobot.infrastructure.orchestrator;

import beto.be.mcpbetobot.domain.entities.GithubTask;
import beto.be.mcpbetobot.domain.usecases.ProcessTaskUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetoBotOrchestratorTest {

    @Mock
    private ProcessTaskUseCase processTaskUseCase;

    @InjectMocks
    private BetoBotOrchestrator orchestrator;

    @Test
    void processEvent() throws InterruptedException {
        GithubTask task = new GithubTask("i1", "iss1", 1, "T", "B", "OPEN", "R", "O", "ANALYSIS", List.of());
        GitHubTaskEvent event = new GitHubTaskEvent(this, task, "ANALYSIS");

        orchestrator.processEvent(event);

        Thread.sleep(150);
        verify(processTaskUseCase, times(1)).process(task);
    }
}
