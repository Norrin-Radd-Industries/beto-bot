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
        ObjectMapper mapper = new ObjectMapper();
        JsonNode testNode = mapper.readTree("{\"id\": \"item_id\"}");
        GithubTask task = GithubParser.parseTaskFromJsonNode(testNode, "test");

        assertNull(task);
    }

    @Test
    void parseTasksFromProject_HappyPath() {
        String json = """
            {
              "data": {
                "node": {
                  "items": {
                    "nodes": [
                      {
                        "id": "item1",
                        "fieldValues": { "nodes": [ { "name": "Backlog" } ] },
                        "content": {
                          "id": "issue1", "number": 1, "title": "T1", "body": "B1", "state": "OPEN",
                          "repository": { "name": "repo1", "owner": { "login": "owner1" } }
                        }
                      },
                      {
                        "id": "item2",
                        "fieldValues": { "nodes": [ { "name": "Todo" } ] },
                        "content": {
                          "id": "issue2", "number": 2, "title": "T2", "body": "B2", "state": "OPEN",
                          "repository": { "name": "repo2", "owner": { "login": "owner2" } }
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
    }

    @Test
    void parseTasksFromProject_EmptyNodes() {
        String json = "{\"data\": {\"node\": {\"items\": {\"nodes\": []}}}}";
        List<GithubTask> tasks = GithubParser.parseTasksFromProject(json);
        assertTrue(tasks.isEmpty());
    }

    @Test
    void parseTasksFromProject_MissingNodes() {
        String json = "{\"data\": {}}";
        List<GithubTask> tasks = GithubParser.parseTasksFromProject(json);
        assertTrue(tasks.isEmpty());
    }

    @Test
    void parseTasksFromProject_InvalidJson() {
        String json = "invalid-json";
        List<GithubTask> tasks = GithubParser.parseTasksFromProject(json);
        assertTrue(tasks.isEmpty());
    }

    @Test
    void getQuery() throws IOException {
        String expected = "query { test }";
        Resource resource = new ByteArrayResource(expected.getBytes());
        String result = GithubParser.getQuery(resource);
        assertEquals(expected, result);
    }

    private JsonNode generateTaskNode() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
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
