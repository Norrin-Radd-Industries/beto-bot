package beto.be.mcpbetobot.util;

import beto.be.mcpbetobot.domain.GithubTask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GithubParserTest {

    @Test
    void shouldParseGraphQLTasksWithDependenciesCorrectly() throws Exception {
        JsonMapper mapper = new JsonMapper();

        // mocking the exact GraphQL response structure from GitHub Projects V2
        String mockGraphQLResponse = """
                {
                  "data": {
                    "organization": {
                      "projectV2": {
                        "items": {
                          "nodes": [
                            {
                              "id": "item_ready",
                              "status": { "name": "To analyze" },
                              "content": {
                                "id": "issue_1",
                                "number": 101,
                                "title": "Unblocked Task",
                                "body": "No blockers here",
                                "state": "OPEN",
                                "repository": { "name": "beto-bot", "owner": { "login": "Norrin-Radd-Industries" } },
                                "blockedBy": { "nodes": [] }
                              }
                            },
                            {
                              "id": "item_blocked",
                              "status": { "name": "To develop" },
                              "content": {
                                "id": "issue_2",
                                "number": 102,
                                "title": "Blocked Task",
                                "body": "Depends on issue 101",
                                "state": "OPEN",
                                "repository": { "name": "beto-bot", "owner": { "login": "Norrin-Radd-Industries" } },
                                "blockedBy": {
                                    "nodes": [
                                        { "number": 101, "state": "OPEN" }
                                    ]
                                }
                              }
                            }
                          ]
                        }
                      }
                    }
                  }
                }
                """;

        JsonNode responseNode = mapper.readTree(mockGraphQLResponse);

        // call the method your GithubProjectService actually uses
        List<GithubTask> tasks = GithubParser.parseTasksFromGraphQL(responseNode);

        // assertions
        assertEquals(2, tasks.size());

        // Check Task 1 (Unblocked Analysis Task)
        GithubTask task1 = tasks.stream().filter(t -> t.number() == 101).findFirst().orElseThrow();
        assertEquals("ANALYSIS", task1.type());
        assertTrue(task1.blockedBy().isEmpty(), "Task 101 should not be blocked");
        assertEquals("Norrin-Radd-Industries/beto-bot", task1.repositoryOwner() + "/" + task1.repository());

        // Check Task 2 (Blocked Coder Task)
        GithubTask task2 = tasks.stream().filter(t -> t.number() == 102).findFirst().orElseThrow();
        assertEquals("CODER", task2.type());
        assertFalse(task2.blockedBy().isEmpty(), "Task 102 should be blocked");
        assertEquals(101, task2.blockedBy().getFirst().number(), "Task 102 should be blocked by issue 101");
    }
}
