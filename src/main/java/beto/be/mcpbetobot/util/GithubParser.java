package beto.be.mcpbetobot.util;

import beto.be.mcpbetobot.domain.GithubTask;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GithubParser {

    private static final Logger logger = LoggerFactory.getLogger(GithubParser.class);
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * individual task mapper
     */
    public static GithubTask parseTaskFromJsonNode(JsonNode task, String type) {
        try {
            String itemId = task.get("id").asText();
            JsonNode taskNode = task.get("content");
            if (taskNode != null) {
                return new GithubTask(itemId,
                        taskNode.get("id").asText(),
                        taskNode.get("number").asInt(),
                        taskNode.get("title").asText(),
                        taskNode.get("body").asText(),
                        taskNode.get("state").asText(),
                        taskNode.get("repository").get("name").asText(),
                        taskNode.get("repository").get("owner").get("login").asText(),
                        type
                        );
            }
        } catch (Exception e) {
            logger.error("error parsing GitHubTask: {}", e.getMessage());
        }
        return null;
    }


    /**
     * Parses the jsonResponse into GithubTasks
     */
    public static List<GithubTask> parseTasksFromProject(String jsonResponse) {
        try {
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode tasks = root.get("data").get("node").get("items").get("nodes");
            List<GithubTask> parsedTasks = new ArrayList<>();
            for (JsonNode task : tasks) {
                JsonNode fieldValueNode = task.get("fieldValues").get("nodes");
                if (fieldValueNode != null && fieldValueNode.isArray()) {
                    for (JsonNode node : fieldValueNode) {
                        if (!node.isEmpty()){
                            switch(node.get("name").asText()) {
                                case "Backlog" :
                                    parsedTasks.add(parseTaskFromJsonNode(task, "ANALYSIS"));
                                    break;
                                case "Todo" :
                                    parsedTasks.add(parseTaskFromJsonNode(task, "CODER"));
                                    break;
                            }
                        }
                        }
                    }
                }
            return parsedTasks;
        } catch (Exception e) {
            logger.error("error parsing tasks from project: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
}
