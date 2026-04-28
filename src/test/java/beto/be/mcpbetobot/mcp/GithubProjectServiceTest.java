package beto.be.mcpbetobot.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(GithubProjectService.class)
class GithubProjectServiceTest {

    @Autowired
    private GithubProjectService githubProjectService;

    @Autowired
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(githubProjectService, "apiKey", "test-key");
        ReflectionTestUtils.setField(githubProjectService, "projectId", "test-project-id");
    }

    @Test
    void moveTaskToAnalysed() {
        // Mock the columns map which is filled during @PostConstruct
        // Since we are unit testing, we can manually inject it or mock the call
        ReflectionTestUtils.setField(githubProjectService, "statusFieldId", "status-field-id");
        java.util.Map<String, String> columns = new java.util.HashMap<>();
        columns.put("Analysed", "analysed-option-id");
        ReflectionTestUtils.setField(githubProjectService, "columns", columns);

        server.expect(requestTo("https://api.github.com/graphql"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "bearer test-key"))
                .andRespond(withSuccess("{\"data\":{\"updateProjectV2ItemFieldValue\":{\"projectV2Item\":{\"id\":\"item-id\"}}}}", MediaType.APPLICATION_JSON));

        String result = githubProjectService.moveTaskToAnalysed("item-id");
        
        assert result.contains("item-id");
        server.verify();
    }
}
