package beto.be.mcpbetobot.util;

import beto.be.mcpbetobot.domain.GithubTask;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class GithubParser {

    private static final Logger logger = LoggerFactory.getLogger(GithubParser.class);
    private static final JsonMapper mapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();


    /**
     * Parses the jsonResponse into GithubTasks
     */
    public static List<GithubTask> parseTasksFromProject(String jsonResponse) {
        try {
            JsonNode root = mapper.readTree(jsonResponse);

            if (!root.isArray()) {
                return Collections.emptyList();
            }

            List<GithubTask> parsedTasks = new ArrayList<>();
            for (JsonNode item : root) {
                String status = item.path("status").asText();

                switch(status) {
                    case "Ready" :
                        parsedTasks.add(parseTaskFromJsonNode(item, "ANALYSIS"));
                        break;
                    case "In progress" :
                        parsedTasks.add(parseTaskFromJsonNode(item, "CODER"));
                        break;
                }
            }
            return parsedTasks;
        } catch (Exception e) {
            logger.error("error parsing tasks from project: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

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
     * Parse repo names from get_projects
     */
    public static List<String> parseRepoNames(String jsonResponse) {
        Set<String> repoNames = new HashSet<>();
        try {
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode items = root.path("items");
            if (items.isArray()) {
                for (JsonNode item: items) {
                    JsonNode repo = item.at("/content/repository/full_name");
                    repoNames.add(repo.asText());
                }
            }
        } catch (Exception e) {
            logger.error("error parsing repository names", e);
        }
        return repoNames.stream().toList();
    }

    /**
     * Parses the JSON array returned by the MCP list_pull_requests tool
     */
    public static List<Document> parseMergedPRsToDocuments(String jsonResponse, String repoName) {
        List<Document> documents = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(jsonResponse);
            if (root.isArray()) {
                for (JsonNode pr : root) {
                    // only absorb successful merges
                    if (!pr.path("merged_at").isNull() && !pr.path("merged_at").isMissingNode()) {
                        String content = String.format("PR Title: %s\nDescription: %s",
                                pr.path("title").asText("No Title"),
                                pr.path("body").asText("No Description"));

                        // make Spring AI doc with data, this will help decide which agent we need
                        Document doc = new Document(content, Map.of(
                                "source", "merged_pr",
                                "repository", repoName,
                                "merged_at", pr.path("merged_at").asText(),
                                "pr_url", pr.path("html_url").asText("")
                        ));
                        documents.add(doc);
                    }
                }
            }
        }catch (Exception e) {
            logger.error("Error parsing merged PRs", e);
        }
        return documents;
    }

    /**
     * Parses the JSON returned by the MCP get_repository_tree tool
     */
    public static List<String> parseMcpTreeToPaths(String jsonResponse) {
        List<String> filePaths = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode tree = root.isObject()? root.path("tree") : root;

            if (tree.isArray()) {
                for (JsonNode node : tree) {
                    // we only want actual files ("blob"), not directories ("tree")
                    if ("blob".equals(node.path("type").asText())) {
                        filePaths.add(node.path("path").asText());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing MCP tree to paths", e);
        }
        return filePaths;
    }

    public static Document createDocument(String codeContent, String repoName, String filePath) {
        String uniqueId = repoName + ":" + filePath;
        UUID deterministicId = UUID.nameUUIDFromBytes(uniqueId.getBytes(StandardCharsets.UTF_8));
        return Document.builder()
                .id(deterministicId.toString())
                .text(codeContent)
                .metadata("type", "code_file")
                .metadata("filePath", filePath)
                .metadata("repository", repoName)
                .build();
    }

    public static String extractText(McpSchema.CallToolResult result) {
        if (result == null || result.content() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        result.content().forEach(content -> {
            if (content instanceof McpSchema.TextContent textContent) {
                sb.append(textContent.text());
            }
        });
        return sb.toString();
    }

}
