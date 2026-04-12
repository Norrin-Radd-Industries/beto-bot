package beto.be.mcpbetobot.util;

import beto.be.mcpbetobot.domain.GithubTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Tool;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static beto.be.mcpbetobot.util.CustomMcpParser.mapCustomProjectToolsForAgent;

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

    /**
    *  Maps GitHub Schema to Gemini Schema
    */
    public static Schema githubToGeminiSchema(McpSchema.JsonSchema githubSchema) {
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            String schemaJson = mapper.writeValueAsString(githubSchema);
            return Schema.fromJson(schemaJson);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing githubSchema <-> geminiSchema");
        }
        return null;
    }

    /**
     *  Maps GitHub tools to Gemini Tools
     */
    public static List<Tool> mapToolsForAgent(List<McpSchema.Tool> tools){
        List<FunctionDeclaration> declarations = tools.stream()
                .map(githubTool -> FunctionDeclaration.builder()
                        .name(githubTool.name())
                        .description(githubTool.description())
                        .parameters(githubToGeminiSchema(githubTool.inputSchema())
                        ).build())
                .toList();

        return List.of(Tool.builder().functionDeclarations(declarations).build());
    }


    /**
     *  concat all Tools from sources ( github + custom )
     */
    public static List<Tool> getAllTools(McpSchema.ListToolsResult toolsList) {
        List<Tool> githubTools = toolsList != null ? mapToolsForAgent(toolsList.tools()) : Collections.emptyList();
        List<Tool> customTools = mapCustomProjectToolsForAgent();

        List<Tool> allTools = new ArrayList<>();
        allTools.addAll(githubTools);
        allTools.addAll(customTools);
        return allTools;
    }
}
