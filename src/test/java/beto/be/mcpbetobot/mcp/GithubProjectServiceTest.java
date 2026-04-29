package beto.be.mcpbetobot.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubProjectServiceTest {

    private GithubProjectService githubProjectService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        githubProjectService = new GithubProjectService(builder);

        ReflectionTestUtils.setField(githubProjectService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(githubProjectService, "projectId", "test-project-id");
        ReflectionTestUtils.setField(githubProjectService, "fetchColumnsFromProjects", new ClassPathResource("graphql/fetch-columns-from-projects.graphql"));
        ReflectionTestUtils.setField(githubProjectService, "updateItemMove", new ClassPathResource("graphql/update-item-move.graphql"));

        // Mock the @PostConstruct call
        String columnsResponse = """
                {
                  "data": {
                    "node": {
                      "fields": {
                        "nodes": [
                          {
                            "id": "status-field-id",
                            "name": "Status",
                            "options": [
                              {"id": "analysed-id", "name": "Analysed"},
                              {"id": "inprogress-id", "name": "In progress"}
                            ]
                          }
                        ]
                      }
                    }
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/graphql"))
                .andExpect(header("Authorization", "bearer test-api-key"))
                .andRespond(withSuccess(columnsResponse, MediaType.APPLICATION_JSON));

        ReflectionTestUtils.invokeMethod(githubProjectService, "getColumnFields");
        mockServer.verify();
        mockServer.reset();
    }

    @Test
    void moveTaskToAnalysed() {
        String expectedResponse = "{\\"data\\": {\\"updateProjectV2ItemFieldValue\\": {\\"projectV2Item\\": {\\"id\\": \\"item-id\\"}}}}";
        
        mockServer.expect(requestTo("https://api.github.com/graphql"))
                .andExpect(header("Authorization", "bearer test-api-key"))
                .andExpect(jsonPath("$.variables.projectId").value("test-project-id"))
                .andExpect(jsonPath("$.variables.itemId").value("item-id"))
                .andExpect(jsonPath("$.variables.fieldId").value("status-field-id"))
                .andExpect(jsonPath("$.variables.optionId").value("analysed-id"))
                .andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON));

        String result = githubProjectService.moveTaskToAnalysed("item-id");
        
        assertEquals(expectedResponse, result);
        mockServer.verify();
    }

    @Test
    void moveTaskToInProgress() {
        String expectedResponse = "{\\"data\\": {\\"updateProjectV2ItemFieldValue\\": {\\"projectV2Item\\": {\\"id\\": \\"item-id\\"}}}}";

        mockServer.expect(requestTo("https://api.github.com/graphql"))
                .andExpect(header("Authorization", "bearer test-api-key"))
                .andExpect(jsonPath("$.variables.projectId").value("test-project-id"))
                .andExpect(jsonPath("$.variables.itemId").value("item-id"))
                .andExpect(jsonPath("$.variables.fieldId").value("status-field-id"))
                .andExpect(jsonPath("$.variables.optionId").value("inprogress-id"))
                .andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON));

        String result = githubProjectService.moveTaskToInProgress("item-id");

        assertEquals(expectedResponse, result);
        mockServer.verify();
    }
}
