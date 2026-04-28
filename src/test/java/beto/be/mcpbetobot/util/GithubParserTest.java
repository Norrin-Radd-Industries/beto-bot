package beto.be.mcpbetobot.util;

import beto.be.mcpbetobot.domain.GithubTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GithubParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parseTaskFromJsonNode() throws JsonProcessingException {
        JsonNode testNode = generateTaskNode();
        GithubTask task = GithubParser.parseTaskFromJsonNode(testNode, "test");

        assertNotNull(task);
        assertEquals("item_id", task.itemId());
        assertEquals("issue_ID_123", task.issueId());
        assertEquals(1, task.number());
        assertEquals("title_123", task.title());
        assertEquals("do something", task.body());
        assertEquals("twinkie", task.repository());
        assertEquals("user-123", task.repositoryOwner());
        assertEquals("test", task.type());
    }

    @Test
    void parseTaskFromJsonNode_MissingContent() throws JsonProcessingException {
        String json = """
            {
              "id": "item_id"
            }
            """;
        JsonNode testNode = mapper.readTree(json);
        GithubTask task = GithubParser.parseTaskFromJsonNode(testNode, "test");

        assertNull(task);
    }

    @Test
    void parseTasksFromProject_HappyPath() {
        String jsonResponse = """
            {
              "data": {
                "node": {
                  "items": {
                    "nodes": [
                      {
                        "id": "item_1",
                        "fieldValues": {
                          "nodes": [ { "name": "Backlog" } ]
                        },
                        "content": {
                          "id": "issue_1",
                          "number": 1,
                          "title": "Task 1",
                          "body": "Body 1",
                          "state": "OPEN",
                          "repository": {
                            "name": "repo1",
                            "owner": { "login": "owner1" }
                          }
                        }
                      },
                      {
                        "id": "item_2",
                        "fieldValues": {
                          "nodes": [ { "name": "Todo" } ]
                        },
                        "content": {
                          "id": "issue_2",
                          "number": 2,
                          "title": "Task 2",
                          "body": "Body 2",
                          "state": "OPEN",
                          "repository": {
                            "name": "repo2",
                            "owner": { "login": "owner2" }
                          }
                        }
                      }
                    ]
                  }
                }
              }
            }
            """;

        List<GithubTask> tasks = GithubParser.parseTasksFromProject(jsonResponse);

        assertEquals(2, tasks.size());
        assertEquals("ANALYSIS", tasks.get(0).type());
        assertEquals("CODER", tasks.get(1).type());
    }

    @Test
    void parseTasksFromProject_EmptyNodes() {
        String jsonResponse = """
            {
              "data": {
                "node": {
                  "items": {
                    "nodes": []
                  }
                }
              }
            }
            """;
        List<GithubTask> tasks = GithubParser.parseTasksFromProject(jsonResponse);
        assertTrue(tasks.isEmpty());
    }

    @Test
    void parseTasksFromProject_MissingNodes() {
        String jsonResponse = """
            {
              "data": {
                "node": {
                  "items": {}
                }
              }
            }
            """;
        List<GithubTask> tasks = GithubParser.parseTasksFromProject(jsonResponse);
        assertTrue(tasks.isEmpty());
    }

    @Test
    void parseTasksFromProject_InvalidJson() {
        String jsonResponse = "invalid json";
        List<GithubTask> tasks = GithubParser.parseTasksFromProject(jsonResponse);
        assertTrue(tasks.isEmpty());
    }

    @Test
    void getQuery() throws IOException {
        String queryContent = "query { test }";
        Resource resource = new ByteArrayResource(queryContent.getBytes());
        String result = GithubParser.getQuery(resource);
        assertEquals(queryContent, result);
    }

    private JsonNode generateTaskNode() throws JsonProcessingException {
        String json = """
            {
              "id": "item_id",
              "fieldValues": {
                "nodes": [
                  {},
                  {
                    "name": "Done",
                    "field": {
                      "name": "Status"
                    }
                  },
                  {}
                ]
              },
              "content": {
                "id": "issue_ID_123",
                "number": 1,
                "title": "title_123",
                "body": "do something",
                "state": "OPEN",
                "repository": {
                  "name": "twinkie",
                  "owner": {
                    "login": "user-123"
                  }
                }
              }
            }
            """;
        return mapper.readTree(json);
    }
}
