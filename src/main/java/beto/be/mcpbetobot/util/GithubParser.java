package beto.be.mcpbetobot.util;

import beto.be.mcpbetobot.domain.GithubTask;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.document.Document;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GithubParser {

    public static Document createDocument(String codeContent, String repoName, String filePath, String branch) {
        String uniqueId = repoName + ":" + filePath;
        UUID deterministicId = UUID.nameUUIDFromBytes(uniqueId.getBytes(StandardCharsets.UTF_8));
        return Document.builder()
                .id(deterministicId.toString())
                .text(codeContent)
                .metadata("type", "code_file")
                .metadata("filePath", filePath)
                .metadata("repository", repoName)
                .metadata("branch", branch)
                .build();
    }

    public static List<GithubTask> parseTasksFromGraphQL(JsonNode response) {
        List<GithubTask> tasks = new ArrayList<>();
        JsonNode items = response.at("/data/organization/projectV2/items/nodes");

        if (items.isArray()) {
            for (JsonNode item : items) {
                String status = item.at("/status/name").asText();
                JsonNode issue = item.path("content");

                if (!isOpenIssue(issue)){
                    continue;
                }

                if (!issue.isMissingNode()) {
                    List<GithubTask> blockers = new ArrayList<>();
                    issue.at("/blockedBy/nodes").forEach(node -> {
                        if (isOpenIssue(node)) {
                            blockers.add(createBlockingGithubTask(node));
                        }
                    });
                    switch(status.toLowerCase()) {
                        case "to analyze" -> tasks.add(createGithubTask(item, issue, "ANALYSIS", blockers));
                        case "to develop" -> tasks.add(createGithubTask(item, issue, "CODER", blockers));
                    }
                }
            }
        }
        return tasks;
    }

    private static GithubTask createBlockingGithubTask(JsonNode node) {
        return new GithubTask(null, null,
                node.get("number").asInt(),
                null, null,
                node.get("state").asText(),
                null, null, null,
                List.of());
    }

    private static GithubTask createGithubTask(JsonNode item,JsonNode issue, String type, List<GithubTask> blockedBy){
        return new GithubTask(
                item.get("id").asText(),
                issue.get("id").asText(),
                issue.get("number").asInt(),
                issue.get("title").asText(),
                issue.get("body").asText(),
                issue.get("state").asText(),
                issue.at("/repository/name").asText(),
                issue.at("/repository/owner/login").asText(),
                type,
                blockedBy
        );
    }

    private static boolean isOpenIssue(JsonNode issue){
        return "OPEN".equalsIgnoreCase(issue.path("state").asText());
    }

}
