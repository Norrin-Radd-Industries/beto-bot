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
    void processEvent_AnalysisTask_CallsAnalystAgent() {
        GithubTask task = new GithubTask("item1", "issue1", 1, "Title", "Body", "OPEN", "repo", "owner", "ANALYSIS");
        GitHubTaskEvent event = new GitHubTaskEvent(this, task, "ANALYSIS");

        orchestrator.processEvent(event);

        verify(analystAgent, timeout(1000)).start(task);
        verifyNoInteractions(codingAgent);
    }

    @Test
    void processEvent_CoderTask_CallsCodingAgent() {
        GithubTask task = new GithubTask("item2", "issue2", 2, "Title", "Body", "OPEN", "repo", "owner", "CODER");
        GitHubTaskEvent event = new GitHubTaskEvent(this, task, "CODER");

        orchestrator.processEvent(event);

        verify(codingAgent, timeout(1000)).start(task);
        verifyNoInteractions(analystAgent);
    }

    @Test
    void processEvent_UnknownTask_DoesNothing() {
        GithubTask task = new GithubTask("item3", "issue3", 3, "Title", "Body", "OPEN", "repo", "owner", "UNKNOWN");
        GitHubTaskEvent event = new GitHubTaskEvent(this, task, "UNKNOWN");

        orchestrator.processEvent(event);

        verifyNoInteractions(analystAgent, codingAgent);
    }
}
