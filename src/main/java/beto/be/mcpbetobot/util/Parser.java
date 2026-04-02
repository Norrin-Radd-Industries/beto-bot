package beto.be.mcpbetobot.util;

import beto.be.mcpbetobot.messages.response.GithubIssue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class Parser {

    private static final Logger logger = LoggerFactory.getLogger(Parser.class);

    public static List<Tool> parseGithubMcpToolsToGeminiTools(String tools) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        // explore the tree structure of the toolsJson
        JsonNode root = mapper.readTree(tools);
        JsonNode toolsNode = root.get("result").get("tools");

        // map to hold the gemini functionDeclarations
        List<FunctionDeclaration> declarations = new ArrayList<>();

        for (int i = 0; i < toolsNode.size(); i++) {
            JsonNode tool = toolsNode.get(i);
            String name = tool.get("name").asText();
            String description = tool.get("description").asText();

            JsonNode githubSchema = tool.get("inputSchema");
            String schemaJson = mapper.writeValueAsString(githubSchema);
            Schema geminiSchema = Schema.fromJson(schemaJson); // using the built in parser from google Schema now

            declarations.add(
                    FunctionDeclaration.builder()
                            .name(name)
                            .description(description)
                            .parameters(geminiSchema)
                            .build()
            );
        }
        // call the builder for google's Tool to inject the declarations into
        return List.of(Tool.builder().functionDeclarations(declarations).build());
    }

    public static List<GithubIssue> parseIssues(String issue) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(issue, new TypeReference<List<GithubIssue>>() {});
        } catch (JsonProcessingException e) {
            logger.error("error parsing issue");
            return Collections.emptyList();
        }
    }
}
