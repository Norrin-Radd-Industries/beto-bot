package beto.be.mcpbetobot.github;

import beto.be.mcpbetobot.domain.GithubTask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static beto.be.mcpbetobot.util.GithubParser.*;

@Service
public class GithubProjectService {

    Logger logger = LoggerFactory.getLogger(GithubProjectService.class);

    private final McpSyncClient mcpClient;
    private final JsonMapper mapper = new JsonMapper();

    private final String orgOwner;
    private final int projectNumber;
    private String projectId;
    private final List<String> repos = new ArrayList<>();

    public GithubProjectService(List<McpSyncClient> mcpClients,
                                @Value("${GITHUB_ORG_OWNER}") String orgOwner,
                                @Value("${GITHUB_PROJECT_NUMBER:1}") int projectNumber) {
        this.mcpClient = mcpClients.getFirst();
        this.orgOwner = orgOwner;
        this.projectNumber = projectNumber;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initProjectId() {
        try {
            logger.info("Resolving project ID for project #{} in org: {}", projectNumber, orgOwner);
            var result = mcpClient.callTool(new McpSchema.CallToolRequest(
                    "projects_list",
                    Map.of(
                            "owner", orgOwner,
                            "method", "list_projects",
                    "project_number", projectNumber)
            ));

            String jsonResponse = extractText(result);
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode projects = root.path("projects");
            if (projects.isArray()) {
                for (JsonNode project : projects) {
                    if (project.path("number").asInt() == projectNumber) {
                        this.projectId = project.path("node_id").asText();
                        return;
                    }
                }
            }
            logger.error("Could not find project #{} in the org list!", projectNumber);
        } catch (Exception e) {
            logger.error("Failed to resolve ID after startup!", e);
        }

    }

    @SuppressWarnings("unused")
    @Tool(description = "Move a GitHub project issue by updating its status field")
    public String moveTask(@ToolParam(description = "The item ID") String itemId,
                                     @ToolParam(description = "The status it should move to") String statusName) {
        // use the mcpClient to call update the project's item state, effectively moving it
        mcpClient.callTool(new McpSchema.CallToolRequest(
                "update_project_item",
                Map.of(
                        "itemId", itemId,
                        "field", "Status",
                        "value", statusName
                )
        ));
        return "Project issue moved to: " + statusName;
    }

    // wrapper around getAvailableTasks
    public List<GithubTask> fetchAvailableTasks() {
        if (projectId == null || projectId.isBlank()) {
            logger.warn("No default project ID set. Cannot fetch available tasks.");
            return List.of();
        }
        return getAvailableTasks();
    }

    // get repo's connected to this project so we can index them for RAG
    public List<String> fetchLinkedRepositoryNames() {
        if (projectId == null || projectId.isBlank()) {
            logger.warn("No default project ID set. Cannot fetch available repos.");
            return List.of();
        }
        try {
            var result = mcpClient.callTool(new McpSchema.CallToolRequest(
                    "projects_list",
                    Map.of("projectId", projectId,
                            "method", "list_project_items",
                            "owner", orgOwner,
                            "project_number", projectNumber)
            ));
            List<String> repoNames = parseRepoNames(extractText(result));

            repos.addAll(repoNames);
            return repoNames;
        } catch (Exception e) {
            logger.error("Failed to fetch linked repos through MCP", e);
            return List.of();
        }
    }

    // get task connected to this project
    public List<GithubTask> getAvailableTasks() {
        for (String repo: repos) {
            String[] ownerWithRepo = repo.split("/");
            var result = mcpClient.callTool(new McpSchema.CallToolRequest(
                    "list_issues",
                    Map.of("owner", ownerWithRepo[0],
                            "repo", ownerWithRepo[1],
                            "state", "open")
            ));
            return parseTasksFromProject(extractText(result));
        }
        return List.of();
    }
}
