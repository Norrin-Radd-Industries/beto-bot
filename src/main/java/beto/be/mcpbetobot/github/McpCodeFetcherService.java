package beto.be.mcpbetobot.github;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static beto.be.mcpbetobot.util.GithubParser.*;

@Service
public class McpCodeFetcherService {

    private final Logger logger = LoggerFactory.getLogger(McpCodeFetcherService.class);
    private final McpSyncClient githubMcpClient;

    public McpCodeFetcherService(List<McpSyncClient> mcpClients) {
        if (mcpClients == null || mcpClients.isEmpty()) {
            throw new IllegalStateException("No MCP clients found!");
        }
        this.githubMcpClient = mcpClients.getFirst();
    }

    public List<Document> fetchEntireRepository(String repoWithOwner) {
        List<Document> allRepoDocuments = new ArrayList<>();
        String[] parts = repoWithOwner.split("/");
        String owner = parts[0];
        String repo = parts[1];

        try {
            McpSchema.CallToolResult result = githubMcpClient.callTool(
                    new McpSchema.CallToolRequest("get_repository_tree", Map.of("repo", repo, "owner", owner))
            );
            String treeText = extractText(result);
            List<String> filePaths = parseMcpTreeToPaths(treeText);

            for (String path : filePaths) {
                if (path.endsWith(".java") || path.endsWith(".md") || path.endsWith(".xml")) {
                    McpSchema.CallToolResult fileResult = githubMcpClient.callTool(
                            new McpSchema.CallToolRequest("get_file_contents", Map.of(
                                    "owner", owner,
                                    "repo", repo,
                                    "path", path))
                    );
                    String fileContent = extractText(fileResult);
                    allRepoDocuments.add(createDocument(fileContent, repoWithOwner, path));
                }
            }
        } catch (Exception e) {
            logger.error("Failed to fetch repository via MCP for {}", repoWithOwner, e);
        }
        return allRepoDocuments;
    }

    public List<Document> fetchRecentPrActivity(String repoWithOwner) {
        String[] parts = repoWithOwner.split("/");
        if (parts.length < 2) {
            return List.of();
        }

        try {
            McpSchema.CallToolResult prResult = githubMcpClient.callTool(
                    new McpSchema.CallToolRequest("list_pull_requests", Map.of(
                            "owner", parts[0],
                            "repo", parts[1],
                            "state", "all"
                    ))
            );

            String prContent = extractText(prResult);
            return parseMergedPRsToDocuments(prContent, repoWithOwner);
        } catch (Exception e) {
            logger.error("Failed to fetch recent PRs via MCP for {}", repoWithOwner, e);
            return List.of();
        }
    }
}
