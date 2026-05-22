package beto.be.mcpbetobot.webhook;

import beto.be.mcpbetobot.orchestrator.CodebaseSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubWebhookControllerTest {

    @Mock
    private CodebaseSyncService codebaseSyncService;

    @Mock
    private WebhookSignatureValidator signatureValidator;

    @InjectMocks
    private GithubWebhookController controller;

    private static final String PAYLOAD_MASTER_PUSH = """
            {
              "ref": "refs/heads/master",
              "repository": {
                "full_name": "owner/repo"
              },
              "commits": [
                {
                  "removed": ["src/Old.java"],
                  "modified": ["src/New.java"]
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        // Ensure signature validation succeeds by default for standard tests
        lenient().when(signatureValidator.isValid(anyString(), anyString())).thenReturn(true);
    }

    @Test
    void shouldReturnForbiddenWhenSignatureIsInvalid() {
        when(signatureValidator.isValid(anyString(), anyString())).thenReturn(false);

        ResponseEntity<String> response = controller.handleWebhook("some-payload", "push", "invalid-sig");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Invalid signature", response.getBody());
        verifyNoInteractions(codebaseSyncService);
    }

    @Test
    void shouldReturnOkAndDoNothingForNonPushEvents() {
        ResponseEntity<String> response = controller.handleWebhook("some-payload", "ping", "valid-sig");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Webhook processed", response.getBody());
        verifyNoInteractions(codebaseSyncService);
    }

    @Test
    void shouldReturnOkAndDoNothingForNonMasterBranchPush() {
        String payload = """
                {
                  "ref": "refs/heads/feature-branch",
                  "repository": {
                    "full_name": "owner/repo"
                  }
                }
                """;

        ResponseEntity<String> response = controller.handleWebhook(payload, "push", "valid-sig");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Webhook processed", response.getBody());
        verifyNoInteractions(codebaseSyncService);
    }

    @Test
    void shouldReturnBadRequestWhenPayloadIsMalformed() {
        String malformedPayload = "{ invalid json";

        ResponseEntity<String> response = controller.handleWebhook(malformedPayload, "push", "valid-sig");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error parsing payload", response.getBody());
        verifyNoInteractions(codebaseSyncService);
    }

    @Test
    void shouldSyncRepositoryWhenPushToMasterBranch() {
        ResponseEntity<String> response = controller.handleWebhook(PAYLOAD_MASTER_PUSH, "push", "valid-sig");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Webhook processed", response.getBody());

        // Verify codebase sync removal is triggered
        verify(codebaseSyncService, times(1)).syncRepositoryRemoval("owner/repo", "src/Old.java");

        // Verify codebase sync for modification is triggered
        verify(codebaseSyncService, times(1)).syncRepository("owner/repo");
    }
}
