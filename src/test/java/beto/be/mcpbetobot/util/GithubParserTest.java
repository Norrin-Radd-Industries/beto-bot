package beto.be.mcpbetobot.util;

import beto.be.mcpbetobot.domain.GithubTask;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GithubParserTest {

    @Test
    void shouldParseMcpPrResultToDocuments() {
        String mockMcpJson = """
            [
              {
                "title": "Fix Timeout",
                "body": "Changed timeout to 30s",
                "merged_at": "2026-05-01T10:00:00Z",
                "html_url": "https://github.com/org/repo/pull/1"
              }
            ]
            """;

        List<Document> docs = GithubParser.parseMergedPRsToDocuments(mockMcpJson, "org/repo");

        assertEquals(1, docs.size());
        assertEquals("merged_pr", docs.getFirst().getMetadata().get("source"));
        assertTrue(docs.getFirst().getFormattedContent().contains("Fix Timeout"));
    }

    @Test
    void shouldParseMcpTasksCorrectly() {
        String mockMcpJson = """
        [
          {
            "id": "item_123",
            "status": "Ready",
            "content": {
                "id": "issue_456",
                "number": 42,
                "title": "Fix the bug",
                "body": "This is a bug description",
                "state": "OPEN",
                "repository": {
                    "name": "my-repo",
                    "owner": {
                        "login": "my-org"
                    }
                }
            }
          }
        ]
        """;

        List<GithubTask> tasks = GithubParser.parseTasksFromProject(mockMcpJson);

        assertEquals(1, tasks.size());
        assertNotNull(tasks.getFirst());
        assertEquals("ANALYSIS", tasks.getFirst().type());
        assertEquals(42, tasks.getFirst().number());
    }
}
