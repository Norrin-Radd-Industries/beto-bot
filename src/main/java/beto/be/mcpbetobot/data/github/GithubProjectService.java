package beto.be.mcpbetobot.data.github;

import beto.be.mcpbetobot.domain.entities.GithubTask;
import beto.be.mcpbetobot.domain.entities.SourceFile;
import beto.be.mcpbetobot.domain.usecases.gateways.GithubProjectGateway;
import beto.be.mcpbetobot.data.mappers.GithubParser;
import beto.be.mcpbetobot.data.util.QueryLoader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GithubProjectService implements GithubProjectGateway {

    private final Logger logger = LoggerFactory.getLogger(GithubProjectService.class);

    private final RestClient restClient;
    private final String orgOwner;
    private final int projectNumber;

    private String projectId;
    private String statusFieldId;
    private final Map<String, String> statusOptions = new HashMap<>();
    private final GithubAppAuthService authService;

    private final JsonMapper mapper = new JsonMapper();

    public GithubProjectService(GithubAppAuthService authService,
                                 @Value("${GITHUB_ORG_OWNER}") String orgOwner,
                                 @Value("${GITHUB_PROJECT_NUMBER:1}") int projectNumber) {
        this.orgOwner = orgOwner;
        this.projectNumber = projectNumber;
        this.restClient = RestClient.create();
        this.authService = authService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initProjectId() {
        String query = QueryLoader.loadQuery("get-project-metadata.graphql");
        JsonNode response = executeQuery(query, Map.of("org", orgOwner, "number", projectNumber));

        JsonNode projectNode = response.at("/data/organization/projectV2");
        this.projectId = projectNode.get("id").asText();
        this.statusFieldId = projectNode.at("/field/id").asText();

        // Map Status names to their IDs for easy moving later
        projectNode.at("/field/options").forEach(opt ->
                statusOptions.put(opt.get("name").asText(), opt.get("id").asText())
        );

        logger.info("Project initialized. Status options: {}", statusOptions.keySet());
    }

    @Override
    public List<GithubTask> getAvailableTasks() {
        String query = QueryLoader.loadQuery("list-project-tasks.graphql");
        JsonNode response = executeQuery(query, Map.of("org", orgOwner, "number", projectNumber));
        return GithubParser.parseTasksFromGraphQL(response);
    }

    @Override
    @Tool(description = "Move a GitHub project issue to a new status")
    public String moveTask(@ToolParam(description = "The project item ID") String itemId,
                           @ToolParam(description = "Target status") String statusName) {
        String optionId = statusOptions.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(statusName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (optionId == null) return "Invalid status: " + statusName;

        String mutation = QueryLoader.loadQuery("update-task-status.graphql");
        executeQuery(mutation, Map.of(
                "projectId", projectId,
                "itemId", itemId,
                "fieldId", statusFieldId,
                "optionId", optionId
        ));
        return "Successfully moved task to " + statusName;
    }

    private JsonNode executeQuery(String query, Map<String, Object> variables) {
        String response = restClient.post()
                .uri("https://api.github.com/graphql")
                .header("Authorization", "Bearer " + authService.getInstallationToken())
                .body(Map.of("query", query, "variables", variables))
                .retrieve()
                .body(String.class);

        return stringToJsonNode(response);
    }

    private JsonNode stringToJsonNode(String response) {
        try {
            return mapper.readTree(response);
        } catch (Exception e) {
            logger.error("Failed to parse GitHub GraphQL response: {}", response);
        }
        return null;
    }

    @Override
    public List<String> fetchLinkedRepositoryNames() {
        String query = QueryLoader.loadQuery("get-linked-repositories.graphql");
        JsonNode response = executeQuery(query, Map.of("org", orgOwner, "number", projectNumber));
        List<String> repoNames = new ArrayList<>();
        response.at("/data/organization/projectV2/repositories/nodes")
                .forEach(node -> repoNames.add(node.get("nameWithOwner").asText()));
        return repoNames;
    }

    @Override
    public List<SourceFile> fetchEntireRepository(String repoWithOwner) {
        try {
            String repoMetaJson = restClient.get()
                    .uri("https://api.github.com/repos/" + repoWithOwner)
                    .header("Authorization", "Bearer " + authService.getInstallationToken())
                    .header("User-Agent", "Norrin-Radd-Industries-Beto-Bot")
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(String.class);

            JsonNode repoNode = mapper.readTree(repoMetaJson);
            String defaultBranch = repoNode.path("default_branch").asText();

            String treeResponse = restClient.get()
                    .uri("https://api.github.com/repos/" + repoWithOwner + "/git/trees/" + defaultBranch + "?recursive=1")
                    .header("Authorization", "Bearer " + authService.getInstallationToken())
                    .header("User-Agent", "Norrin-Radd-Industries-Beto-Bot")
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(String.class);

            JsonNode response = stringToJsonNode(treeResponse);

            if (response == null) {
                return List.of();
            }

            List<String> paths = new ArrayList<>();
            response.path("tree").forEach(node -> {
                String path = node.get("path").asText();
                if (node.get("type").asText().equals("blob") &&
                        (path.endsWith(".java"))) {
                    paths.add(path);
                }
            });

            String[] parts = repoWithOwner.split("/");
            String owner = parts[0];
            String repo = parts[1];

            List<SourceFile> allFiles = new ArrayList<>();
            for (int i = 0; i < paths.size(); i += 20) {
                List<String> batch = paths.subList(i, Math.min(i + 20, paths.size()));
                String batchQuery = buildBatchQuery(owner, repo, batch, defaultBranch);
                JsonNode contentResponse = executeQuery(batchQuery, Map.of());

                batch.forEach(path -> {
                    String alias = "file_" + Math.abs(path.hashCode());
                    String content = contentResponse.at("/data/repository/" + alias + "/text").asText();
                    allFiles.add(new SourceFile(content, repoWithOwner, path, defaultBranch));
                });
            }
            return allFiles;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to fetch repository codebase from Github", e);
        }
    }

    private String buildBatchQuery(String owner, String repo, List<String> paths, String defaultBranch) {
        StringBuilder sb = new StringBuilder("query { repository(owner: \"" + owner + "\", name: \"" + repo + "\") {");
        for (String path : paths) {
            String alias = "file_" + Math.abs(path.hashCode());
            sb.append(String.format("%s: object(expression: \"%s:%s\") { ... on Blob { text } } ", alias, defaultBranch, path));
        }
        sb.append("} }");
        return sb.toString();
    }
}
