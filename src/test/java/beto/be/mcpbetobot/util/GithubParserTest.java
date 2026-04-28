package beto.be.mcpbetobot.util;

import beto.be.mcpbetobot.domain.GithubTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GithubParserTest {

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
    void parseTasksFromProject() {
        String json = """
            {
              "data": {
                "node": {
                  "items": {
                    "nodes": [
                      {
                        "id": "item1",
                        "fieldValues": {
                          "nodes": [
                            { "name": "Backlog" }
                          ]
                        },
                        "content": {
                          "id": "issue1",
                          "number": 101,
                          "title": "Analysis Task",
                          "body": "Need analysis",
                          "state": "OPEN",
                          "repository": {
                            "name": "repo1",
                            "owner": { "login": "owner1" }
                          }
                        }
                      },
                      {
                        "id": "item2",
                        "fieldValues": {
                          "nodes": [
                            { "name": "Todo" }
                          ]
                        },
                        "content": {
                          "id": "issue2",
                          "number": 102,
                          "title": "Coding Task",
                          "body": "Need code",
                          "state": "OPEN",
                          "repository": {
                            "name": "repo1",
                            "owner": { "login": "owner1" }
                          }
                        }
                      }
                    ]
                  }
                }
              }
            }
            """;

        List<GithubTask> tasks = GithubParser.parseTasksFromProject(json);

        assertEquals(2, tasks.size());
        assertEquals("ANALYSIS", tasks.get(0).type());
        assertEquals("CODER", tasks.get(1).type());
        assertEquals(101, tasks.get(0).number());
        assertEquals(102, tasks.get(1).number());
    }

    @Test
    void parseTasksFromProject_Empty() {
        String json = "{}";
        List<GithubTask> tasks = GithubParser.parseTasksFromProject(json);
        assertTrue(tasks.isEmpty());
    }

    private JsonNode generateTaskNode() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
            {
              "id": "item_id",
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
