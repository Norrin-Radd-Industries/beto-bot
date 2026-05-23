package beto.be.mcpbetobot.presentation.controllers;

import beto.be.mcpbetobot.domain.entities.GithubTask;
import beto.be.mcpbetobot.domain.usecases.SyncCodebaseUseCase;
import beto.be.mcpbetobot.infrastructure.agentic.AnalystAgent;
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
    private final AnalystAgent analystAgent;
    private final WebhookSignatureValidator signatureValidator;
    private final JsonMapper objectMapper;

    public GithubWebhookController(SyncCodebaseUseCase syncCodebaseUseCase,
                                   AnalystAgent analystAgent,
                                   WebhookSignatureValidator signatureValidator) {
        this.syncCodebaseUseCase = syncCodebaseUseCase;
        this.analystAgent = analystAgent;
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
        } else if ("project_v2".equalsIgnoreCase(eventType)) {
            try {
                JsonNode root = objectMapper.readTree(payload);
                String action = root.path("action").asText();
                if ("edited".equals(action)) {
                    JsonNode projectItem = root.path("project_item");
                    String status = projectItem.path("status").asText();
                    
                    // Check if the status is "To analyze" or "To develop"
                    if ("To analyze".equalsIgnoreCase(status) || "To develop".equalsIgnoreCase(status)) {
                        logger.info("Project item status changed to {}", status);
                        
                        // Extract task information from the project item
                        String itemId = projectItem.path("id").asText();
                        JsonNode content = projectItem.path("content");
                        
                        if (!content.isMissingNode()) {
                            int issueNumber = content.path("number").asInt();
                            String title = content.path("title").asText();
                            String body = content.path("body").asText();
                            String state = content.path("state").asText();
                            
                            // Get repository info
                            JsonNode repository = content.path("repository");
                            String repoName = repository.path("name").asText();
                            String owner = repository.path("owner").path("login").asText();
                            
                            // Create a GithubTask object and trigger analysis
                            GithubTask task = new GithubTask(
                                itemId,
                                content.path("id").asText(),
                                issueNumber,
                                title,
                                body,
                                state,
                                repoName,
                                owner,
                                "ANALYSIS", // Set type to ANALYSIS for this case
                                java.util.List.of()
                            );
                            
                            logger.info("Triggering analysis for issue {}", issueNumber);
                            analystAgent.execute(task);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error processing project_v2 webhook payload", e);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error processing payload");
            }
        } else {
            logger.info("Received GitHub event: {}. No action taken.", eventType);
        }

        return ResponseEntity.ok("Webhook processed");
    }
}
