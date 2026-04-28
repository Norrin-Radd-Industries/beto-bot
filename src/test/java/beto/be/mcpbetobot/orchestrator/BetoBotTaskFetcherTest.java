package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.events.GitHubTaskEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BetoBotTaskFetcherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private Resource fetchTasksFromProjects;

    private BetoBotTaskFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new BetoBotTaskFetcher(applicationEventPublisher);
        ReflectionTestUtils.setField(fetcher, "restClient", restClient);
        ReflectionTestUtils.setField(fetcher, "fetchTasksFromProjects", fetchTasksFromProjects);
        ReflectionTestUtils.setField(fetcher, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(fetcher, "projectId", "test-project-id");
    }

    @Test
    void checkForAvailableWork_WithTasks_PublishesEvents() throws IOException {
        String jsonResponse = """
                {
                  "data": {
                    "node": {
                      "items": {
                        "nodes": [
                          {
                            "id": "item1",
                            "content": {
                              "id": "issue1",
                              "number": 1,
                              "title": "Task 1",
                              "body": "Body 1",
                              "state": "OPEN",
                              "repository": {
                                "name": "repo",
                                "owner": { "login": "owner" }
                              }
                            },
                            "fieldValues": {
                              "nodes": [
                                { "name": "Backlog" }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  }
                }
                """;

        when(fetchTasksFromProjects.getInputStream()).thenReturn(new ByteArrayInputStream("query".getBytes()));
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(jsonResponse);

        fetcher.checkForAvailableWork();

        verify(applicationEventPublisher, times(1)).publishEvent(any(GitHubTaskEvent.class));
    }

    @Test
    void checkForAvailableWork_NoTasks_DoesNotPublishEvents() throws IOException {
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

        when(fetchTasksFromProjects.getInputStream()).thenReturn(new ByteArrayInputStream("query".getBytes()));
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(jsonResponse);

        fetcher.checkForAvailableWork();

        verify(applicationEventPublisher, never()).publishEvent(any(GitHubTaskEvent.class));
    }
}
