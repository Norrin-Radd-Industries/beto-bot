package beto.be.mcpbetobot.infrastructure.agentic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CavemanRAGAdvisorTest {

    @Mock
    private ChatClientRequest request;

    @Mock
    private ChatClientRequest.Builder requestBuilder;

    @Mock
    private CallAdvisorChain chain;

    @Mock
    private ChatClientResponse response;

    @Test
    void shouldPreprocessRAGContextInsideSystemMessage() {
        CavemanRAGAdvisor advisor = new CavemanRAGAdvisor();
        String originalSystemText = """
                You are an agent.
                
                RELEVANT CODE & HISTORY FROM KNOWLEDGE DATABASE:
                --- FILE: src/main/java/beto/be/mcpbetobot/data/rag/RagService.java | BRANCH: main ---
                task: retrieval_document: package beto.be.mcpbetobot.data.rag;
                // This is a test comment
                public class RagService {
                    /* block comment */
                    int value = 42;
                }
                
                Issue: 12 - Fix issue
                """;

        String expectedProcessedSystemText = """
                You are an agent.
                
                RELEVANT CODE & HISTORY FROM KNOWLEDGE DATABASE:
                --- FILE: src/main/java/beto/be/mcpbetobot/data/rag/RagService.java | BRANCH: main ---
                task: retrieval_document: package beto.be.mcpbetobot.data.rag;
                public class RagService {
                    int value = 42;
                }
                
                Issue: 12 - Fix issue
                """;

        assertEquals(expectedProcessedSystemText.trim(), advisor.preprocessRAGContext(originalSystemText).trim());
    }

    @Test
    void shouldAdviseCallAndMutateRequestWhenSystemMessageContainsRAG() {
        CavemanRAGAdvisor advisor = new CavemanRAGAdvisor();

        String originalSystemText = """
                System prompt
                --- FILE: file.java | BRANCH: master ---
                task: retrieval_document: // comment
                public class Foo {}
                """;

        SystemMessage originalSystemMessage = new SystemMessage(originalSystemText);
        UserMessage userMessage = new UserMessage("Hello");
        Prompt prompt = new Prompt(List.of(originalSystemMessage, userMessage));

        when(request.prompt()).thenReturn(prompt);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.prompt(any(Prompt.class))).thenReturn(requestBuilder);
        when(requestBuilder.build()).thenReturn(request);
        when(chain.nextCall(request)).thenReturn(response);

        ChatClientResponse result = advisor.adviseCall(request, chain);

        assertEquals(response, result);

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(requestBuilder).prompt(promptCaptor.capture());
        Prompt capturedPrompt = promptCaptor.getValue();

        List<Message> capturedMessages = capturedPrompt.getInstructions();
        assertEquals(2, capturedMessages.size());

        Message updatedSystemMessage = capturedMessages.get(0);
        assertEquals(MessageType.SYSTEM, updatedSystemMessage.getMessageType());
        
        String expectedText = """
                System prompt
                --- FILE: file.java | BRANCH: master ---
                task: retrieval_document: public class Foo {}
                """;
        assertEquals(expectedText.trim(), updatedSystemMessage.getText().trim());

        Message unchangedUserMessage = capturedMessages.get(1);
        assertEquals(MessageType.USER, unchangedUserMessage.getMessageType());
        assertEquals("Hello", unchangedUserMessage.getText());
    }

    @Test
    void shouldNotMutateRequestWhenNoRAGContextPresent() {
        CavemanRAGAdvisor advisor = new CavemanRAGAdvisor();

        SystemMessage systemMessage = new SystemMessage("System prompt without RAG");
        UserMessage userMessage = new UserMessage("Hello");
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        when(request.prompt()).thenReturn(prompt);
        when(chain.nextCall(request)).thenReturn(response);

        ChatClientResponse result = advisor.adviseCall(request, chain);

        assertEquals(response, result);
        verify(request, never()).mutate();
    }
}
