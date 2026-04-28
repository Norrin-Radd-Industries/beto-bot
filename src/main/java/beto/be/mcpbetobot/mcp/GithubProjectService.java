package beto.be.mcpbetobot.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static beto.be.mcpbetobot.util.GithubParser.getQuery;

@Service
public class GithubProjectService {

    Logger logger = LoggerFactory.getLogger(GithubProjectService.class);

    @Value("${GITHUB_PERSONAL_ACCESS_TOKEN}")
    private String apiKey;

    @Value("${GITHUB_PROJECT_ID}")
    private String projectId;

    @Value("classpath:graphql/fetch-columns-from-projects.graphql")
    private Resource fetchColumnsFromProjects;

    @Value("classpath:graphql/update-item-move.graphql")
    private Resource updateItemMove;

    private final RestClient restClient;

    private final Map<String, String> columns = new HashMap<>();
    private String statusFieldId;

    @PostConstruct
    private void getColumnFields() {
        fetchAndInjectColumnFields();
    }

    public GithubProjectService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @SuppressWarnings("unused")
    @Tool(description = "Move a GitHub project issue to the Analysed column once analysis is complete")
    public String moveTaskToAnalysed(@ToolParam(description = "The item ID") String itemId) {
        String result = updateItemStatus(itemId, columns.get("Analysed"));
        logger.info("move result: {}", result);
        return result;
    }

    @SuppressWarnings("unused")
    @Tool(description = "Move a GitHub project issue to In Progress when the coder is done working on it")
    public String moveTaskToInProgress(@ToolParam(description = "The item ID") String itemId) {
        return updateItemStatus(itemId, columns.get("Todo"));
    }

    private String updateItemStatus(String itemId, String statusOptionId) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "query", getQuery(updateItemMove),
                    "variables", Map.of(
                            "projectId", projectId,
                            "itemId", itemId,
                            "fieldId", statusFieldId,
                            "optionId", statusOptionId
                    )
            );
            return gitHubGraphqlCall(requestBody);
        } catch (IOException e) {
            logger.error("Failure trying to update item task", e);
            throw new RuntimeException(e);
        }
    }

    // method to fetch the columns in the project and map them
    private void fetchAndInjectColumnFields() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, Object> requestBody = Map.of(
                    "query", getQuery(fetchColumnsFromProjects),
                    "variables", Map.of("projectId", projectId)
            );
            String response = gitHubGraphqlCall(requestBody);
            JsonNode root = mapper.readTree(response);
            JsonNode nodes = root.at("/data/node/fields/nodes");

            for (JsonNode field : nodes) {
                if ("Status".equals(field.path("name").asText())) {
                    this.statusFieldId = field.path("id").asText();
                    field.path("options").forEach(option ->
                            columns.put(option.get("name").asText(), option.get("id").asText())
                    );
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to fetch GitHub Project columns: {}", e.getMessage(), e);
        }
    }

    private String gitHubGraphqlCall(Map<String, Object> requestBody) {
        return restClient.post()
                .uri("https://api.github.com/graphql")
                .header("Authorization", "bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);
    }
}
