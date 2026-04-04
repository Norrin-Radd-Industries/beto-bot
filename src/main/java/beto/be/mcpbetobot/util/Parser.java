package beto.be.mcpbetobot.util;

import beto.be.mcpbetobot.domain.GithubIssue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Parser {

    private static final Logger logger = LoggerFactory.getLogger(Parser.class);
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Updated for Spring AI McpClient
     * Uses the raw text content string from the tool result.
     */
    public static List<GithubIssue> parseIssues(String issues) {
        if (issues == null || issues.isBlank()) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = mapper.readTree(issues);
            JsonNode issuesNode = root.get("issues");

            if (issuesNode != null && issuesNode.isArray()) {
                return mapper.readValue(issuesNode.toString(), new TypeReference<List<GithubIssue>>() {});
            }

            if (root.isArray()) {
                return mapper.readValue(issues, new TypeReference<List<GithubIssue>>() {});
            }

        } catch (Exception e) {
            logger.error("error parsing issues: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
}
