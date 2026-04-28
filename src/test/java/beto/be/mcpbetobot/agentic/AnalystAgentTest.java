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
    void buildPrompt() {
        GithubTask task = new GithubTask("item123", "issue456", 1, "Title", "Body", "OPEN", "Repo", "Owner", "ANALYSIS");
        String prompt = analystAgent.buildPrompt(task);

        assertTrue(prompt.contains("functional analyst"));
        assertTrue(prompt.contains("item123"));
        assertTrue(prompt.contains("issue456"));
        assertTrue(prompt.contains("Title"));
        assertTrue(prompt.contains("Repo"));
    }
}
