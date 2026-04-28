package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.events.GitHubTaskEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(BetoBotTaskFetcher.class)
class BetoBotTaskFetcherTest {

    @Autowired
    private BetoBotTaskFetcher taskFetcher;

    @Autowired
    private MockRestServiceServer server;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(taskFetcher, "apiKey", "test-key");
        ReflectionTestUtils.setField(taskFetcher, "projectId", "test-project-id");
    }

    @Test
    void checkForAvailableWork() {
        String jsonResponse = """
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
                          "repository": { "name": "R1", "owner": { "login": "O1" } }
                        }
                      }
                    ]
                  }
                }
              }
            }
            """;

        server.expect(requestTo("https://api.github.com/graphql"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        taskFetcher.checkForAvailableWork();

        verify(eventPublisher, times(1)).publishEvent(any(GitHubTaskEvent.class));
        server.verify();
    }
}
