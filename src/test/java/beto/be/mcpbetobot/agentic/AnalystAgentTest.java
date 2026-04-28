package beto.be.mcpbetobot.agentic;

import beto.be.mcpbetobot.domain.GithubTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AnalystAgentTest {

    @Mock
    private ChatClient chatClient;

    @InjectMocks
    private AnalystAgent analystAgent;

    @Test
    void buildPrompt_ContainsExpectedFields() {
        GithubTask task = new GithubTask("item123", "issue456", 789, "Test Title", "Test Body", "OPEN", "test-repo", "test-owner", "ANALYSIS");

        String prompt = analystAgent.buildPrompt(task);

        assertTrue(prompt.contains("test-owner"));
        assertTrue(prompt.contains("test-repo"));
        assertTrue(prompt.contains("789"));
        assertTrue(prompt.contains("Test Title"));
        assertTrue(prompt.contains("Test Body"));
        assertTrue(prompt.contains("item123"));
        assertTrue(prompt.contains("functional analyst"));
    }
}
