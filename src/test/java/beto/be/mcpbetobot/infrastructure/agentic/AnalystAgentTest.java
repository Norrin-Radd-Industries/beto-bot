package beto.be.mcpbetobot.infrastructure.agentic;

import beto.be.mcpbetobot.domain.entities.GithubTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AnalystAgentTest {

    @InjectMocks
    private AnalystAgent analystAgent;

    @Test
    void buildPrompt() {
        GithubTask task = new GithubTask("item123", "issue456", 1, "do this", "Body", "OPEN", "Repo", "Jos", "ANALYSIS", List.of());
        String prompt = analystAgent.buildPrompt(task, "context");

        assertTrue(prompt.contains("functional analyst"));
        assertTrue(prompt.contains("item123"));
        assertTrue(prompt.contains("Jos"));
        assertTrue(prompt.contains("do this"));
        assertTrue(prompt.contains("Repo"));
    }
}
