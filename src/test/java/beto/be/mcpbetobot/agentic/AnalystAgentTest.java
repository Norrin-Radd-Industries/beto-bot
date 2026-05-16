package beto.be.mcpbetobot.agentic;

import beto.be.mcpbetobot.domain.GithubTask;
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
        String prompt = analystAgent.buildPrompt(task);

        assertTrue(prompt.contains("functional analyst"));
        assertTrue(prompt.contains("item123"));
        assertTrue(prompt.contains("Jos"));
        assertTrue(prompt.contains("do this"));
        assertTrue(prompt.contains("Repo"));
    }

    @Test
    void buildPrompt_WithSpecialCharactersInTitle() {
        GithubTask task = new GithubTask("item123", "issue456", 1, "do this & that", "Body with special chars: @#$%", "OPEN", "Repo", "Jos", "ANALYSIS", List.of());
        String prompt = analystAgent.buildPrompt(task);

        assertTrue(prompt.contains("functional analyst"));
        assertTrue(prompt.contains("item123"));
        assertTrue(prompt.contains("Jos"));
        assertTrue(prompt.contains("do this & that"));
        assertTrue(prompt.contains("Repo"));
    }

    @Test
    void buildPrompt_WithEmptyDescription() {
        GithubTask task = new GithubTask("item123", "issue456", 1, "Title", "", "OPEN", "Repo", "Jos", "ANALYSIS", List.of());
        String prompt = analystAgent.buildPrompt(task);

        assertTrue(prompt.contains("functional analyst"));
        assertTrue(prompt.contains("item123"));
        assertTrue(prompt.contains("Jos"));
        assertTrue(prompt.contains("Title"));
        assertTrue(prompt.contains("Repo"));
    }
}