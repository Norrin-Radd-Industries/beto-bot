package beto.be.mcpbetobot.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class ProjectService {

    Logger logger = LoggerFactory.getLogger(ProjectService.class);

    @Value("${GITHUB_PERSONAL_ACCESS_TOKEN}")
    private String apiKey;

    @Value("${GITHUB_PROJECT_ID}")
    private String projectId;

    // github id's for the statuses in the project (represents a column) only using 2 atm
    private static final String STATUS_FIELD_ID = "PVTSSF_lAHOBGwfss4BUU8fzhBd48A";
    private static final String STATUS_BACKLOG = "f75ad846";
    private static final String STATUS_ANALYSED = "cf8dd98e";
    private static final String STATUS_TODO = "61e4505c";
    private static final String STATUS_IN_PROGRESS = "47fc9ee4";
    private static final String STATUS_IN_REVIEW = "df73e18b";
    private static final String STATUS_DONE = "98236657";

    private final RestClient restClient;

    public ProjectService() {
        this.restClient = RestClient.create();
    }

    @McpTool(description = "Move a GitHub project issue to the Analysed column once analysis is complete")
    public String moveTaskToAnalysed(@McpToolParam(description = "The item ID") String itemId) {
        String result = updateItemStatus(itemId, STATUS_ANALYSED);
        logger.info("move result: {}", result);
        return result;
    }

    @McpTool(description = "Move a GitHub project issue to In Progress when the coder is done working on it")
    public String moveTaskToInProgress(@ToolParam(description = "The item ID") String itemId) {
        return updateItemStatus(itemId, STATUS_IN_PROGRESS);
    }

    private String updateItemStatus(String itemId, String statusOptionId) {
        Map<String, String> body = Map.of(
                "query",
                """
                mutation {
                  updateProjectV2ItemFieldValue(
                    input: {
                      projectId: "%s"
                      itemId: "%s"
                      fieldId: "%s"
                      value: {
                        singleSelectOptionId: "%s"
                      }
                    }
                  ) {
                    projectV2Item {
                      id
                    }
                  }
                }
                """.formatted(projectId, itemId, STATUS_FIELD_ID, statusOptionId)
        );

        return restClient.post()
                .uri("https://api.github.com/graphql")
                .header("Authorization", "bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);
    }
}
