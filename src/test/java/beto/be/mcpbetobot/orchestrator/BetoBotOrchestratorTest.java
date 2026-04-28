package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.agentic.AnalystAgent;
import beto.be.mcpbetobot.agentic.CodingAgent;
import beto.be.mcpbetobot.domain.GithubTask;
import beto.be.mcpbetobot.events.GitHubTaskEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetoBotOrchestratorTest {

    @Mock
    private CodingAgent codingAgent;

    @Mock
    private AnalystAgent analystAgent;

    @InjectMocks
    private BetoBotOrchestrator orchestrator;

    @Test
    void processEvent_Analysis() throws InterruptedException {
        GithubTask task = new GithubTask("i1", "iss1", 1, "T", "B", "OPEN", "R", "O", "ANALYSIS");
        GitHubTaskEvent event = new GitHubTaskEvent(this, task, "ANALYSIS");

        orchestrator.processEvent(event);

        // Since it runs in a virtual thread, we might need a small sleep or a better way to verify
        // For unit testing, we could potentially mock Thread.ofVirtual() if we were using a factory,
        // but here we just wait a bit.
        Thread.sleep(100);
        verify(analystAgent, times(1)).start(task);
        verify(codingAgent, never()).start(any());
    }

    @Test
    void processEvent_Coder() throws InterruptedException {
        GithubTask task = new GithubTask("i1", "iss1", 1, "T", "B", "OPEN", "R", "O", "CODER");
        GitHubTaskEvent event = new GitHubTaskEvent(this, task, "CODER");

        orchestrator.processEvent(event);

        Thread.sleep(100);
        verify(codingAgent, times(1)).start(task);
        verify(analystAgent, never()).start(any());
    }
}
