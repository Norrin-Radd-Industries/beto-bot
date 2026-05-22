package beto.be.mcpbetobot.presentation.controllers;

import beto.be.mcpbetobot.domain.usecases.SyncCodebaseUseCase;
import beto.be.mcpbetobot.infrastructure.orchestrator.WebhookSignatureValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/github")
public class GithubWebhookController {

    private final Logger logger = LoggerFactory.getLogger(GithubWebhookController.class);
    private final SyncCodebaseUseCase syncCodebaseUseCase;
    private final WebhookSignatureValidator signatureValidator;
    private final JsonMapper objectMapper;

    public GithubWebhookController(SyncCodebaseUseCase syncCodebaseUseCase,
                                   WebhookSignatureValidator signatureValidator) {
        this.syncCodebaseUseCase = syncCodebaseUseCase;
        this.signatureValidator = signatureValidator;
        this.objectMapper = new JsonMapper();
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                @RequestHeader("X-GitHub-Event") String eventType,
                                                @RequestHeader("X-Hub-Signature-256") String signature) {

        if (!signatureValidator.isValid(payload, signature)) {
            logger.warn("Invalid webhook signature");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature");
        }

        if ("push".equalsIgnoreCase(eventType)) {
            try {
                JsonNode root = objectMapper.readTree(payload);
                String ref = root.path("ref").asText();
                String repoName = root.path("repository").path("full_name").asText();

                if ("refs/heads/master".equals(ref)) {
                    logger.info("Push event received for master branch on {}. Triggering sync.", repoName);
                    JsonNode commits = root.path("commits");
                    commits.forEach(commit -> {
                        commit.path("removed").forEach(file -> {
                            String filePath = file.asText();
                            syncCodebaseUseCase.syncRepositoryRemoval(repoName, filePath);
                            logger.info("File removed: {}", filePath);
                        });
                        commit.path("modified").forEach(file -> {
                            syncCodebaseUseCase.syncRepository(repoName);
                        });
                    });
                } else {
                    logger.info("Push event received for branch {}. Ignoring.", ref);
                }
            } catch (Exception e) {
                logger.error("Error parsing webhook payload", e);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error parsing payload");
            }
        } else {
            logger.info("Received GitHub event: {}. No action taken.", eventType);
        }

        return ResponseEntity.ok("Webhook processed");
    }
}
