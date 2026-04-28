package beto.be.mcpbetobot.agentic;

import beto.be.mcpbetobot.domain.GithubTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec chatClientRequestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private static class TestAgent extends Agent {
        public TestAgent(ChatClient client) {
            super(client);
        }

        @Override
        String buildPrompt(GithubTask task) {
            return "Test Prompt";
        }
    }

    @Test
    void start_CallsChatClientCorrectly() {
        TestAgent agent = new TestAgent(chatClient);
        GithubTask task = new GithubTask("id", "issueId", 1, "T", "B", "S", "R", "O", "TYPE");

        when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.system(any(Consumer.class))).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
        when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Response");

        agent.start(task);

        verify(chatClient).prompt();
        verify(chatClientRequestSpec).system(any(Consumer.class));
        verify(chatClientRequestSpec).user("execute the task and confirm when finished");
        verify(chatClientRequestSpec).call();
        verify(callResponseSpec).content();
    }
}
