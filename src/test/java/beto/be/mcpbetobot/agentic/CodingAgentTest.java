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
class CodingAgentTest {

    @Mock
    private ChatClient chatClient;

    @InjectMocks
    private CodingAgent codingAgent;

    @Test
    void buildPrompt_ContainsExpectedFields() {
        GithubTask task = new GithubTask("item789", "issue012", 345, "Fix Bug", "Bug Description", "OPEN", "my-repo", "my-owner", "CODER");

        String prompt = codingAgent.buildPrompt(task);

        assertTrue(prompt.contains("my-owner"));
        assertTrue(prompt.contains("my-repo"));
        assertTrue(prompt.contains("Fix Bug"));
        assertTrue(prompt.contains("Bug Description"));
        assertTrue(prompt.contains("345"));
        assertTrue(prompt.contains("item789"));
        assertTrue(prompt.contains("senior Java Developer"));
    }
}
