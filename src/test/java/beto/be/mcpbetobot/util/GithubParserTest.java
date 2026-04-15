package beto.be.mcpbetobot.util;

import beto.be.mcpbetobot.domain.GithubTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GithubParserTest {

    @Test
    void parseTaskFromJsonNode() throws JsonProcessingException {
        JsonNode testNode = generateTaskNode();
        GithubTask task = GithubParser.parseTaskFromJsonNode(testNode, "test");

        assert task != null;
        assertEquals("item_id", task.itemId());
        assertEquals("issue_ID_123", task.issueId());
        assertEquals(1, task.number());
        assertEquals("title_123", task.title());
        assertEquals("do something", task.body());
        assertEquals("twinkie", task.repository());
        assertEquals("user-123", task.repositoryOwner());
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