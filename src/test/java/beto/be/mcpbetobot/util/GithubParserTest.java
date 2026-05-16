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

    @Test
    void shouldHandleClosedIssues() throws Exception {
        JsonMapper mapper = new JsonMapper();

        String mockGraphQLResponse = """
                {
                  "data": {
                    "organization": {
                      "projectV2": {
                        "items": {
                          "nodes": [
                            {
                              "id": "item_closed",
                              "status": { "name": "To analyze" },
                              "content": {
                                "id": "issue_1",
                                "number": 101,
                                "title": "Closed Task",
                                "body": "This task is closed",
                                "state": "CLOSED",
                                "repository": { "name": "beto-bot", "owner": { "login": "Norrin-Radd-Industries" } },
                                "blockedBy": { "nodes": [] }
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
        List<GithubTask> tasks = GithubParser.parseTasksFromGraphQL(responseNode);

        // Closed tasks should be ignored
        assertTrue(tasks.isEmpty());
    }

    @Test
    void shouldCreateDocumentCorrectly() {
        String codeContent = "public class Test { }";
        String repoName = "test-repo";
        String filePath = "src/main/java/Test.java";
        String branch = "main";

        // Test document creation
        var document = GithubParser.createDocument(codeContent, repoName, filePath, branch);

        assertEquals(codeContent, document.getText());
        assertEquals("code_file", document.getMetadata().get("type"));
        assertEquals(filePath, document.getMetadata().get("filePath"));
        assertEquals(repoName, document.getMetadata().get("repository"));
        assertEquals(branch, document.getMetadata().get("branch"));
    }

    @Test
    void shouldHandleDifferentStatuses() throws Exception {
        JsonMapper mapper = new JsonMapper();

        String mockGraphQLResponse = """
                {
                  "data": {
                    "organization": {
                      "projectV2": {
                        "items": {
                          "nodes": [
                            {
                              "id": "item_analysis",
                              "status": { "name": "To analyze" },
                              "content": {
                                "id": "issue_1",
                                "number": 101,
                                "title": "Analysis Task",
                                "body": "Needs analysis",
                                "state": "OPEN",
                                "repository": { "name": "beto-bot", "owner": { "login": "Norrin-Radd-Industries" } },
                                "blockedBy": { "nodes": [] }
                              }
                            },
                            {
                              "id": "item_develop",
                              "status": { "name": "To develop" },
                              "content": {
                                "id": "issue_2",
                                "number": 102,
                                "title": "Development Task",
                                "body": "Needs development",
                                "state": "OPEN",
                                "repository": { "name": "beto-bot", "owner": { "login": "Norrin-Radd-Industries" } },
                                "blockedBy": { "nodes": [] }
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
        List<GithubTask> tasks = GithubParser.parseTasksFromGraphQL(responseNode);

        assertEquals(2, tasks.size());

        // Check that tasks are correctly assigned to their types
        GithubTask analysisTask = tasks.stream().filter(t -> t.number() == 101).findFirst().orElseThrow();
        assertEquals("ANALYSIS", analysisTask.type());

        GithubTask developTask = tasks.stream().filter(t -> t.number() == 102).findFirst().orElseThrow();
        assertEquals("CODER", developTask.type());
    }
}