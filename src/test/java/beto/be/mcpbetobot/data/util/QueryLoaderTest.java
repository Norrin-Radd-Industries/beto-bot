package beto.be.mcpbetobot.data.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryLoaderTest {

    @Test
    void shouldLoadValidGraphQLQuery() {
        String queryName = "list-project-tasks.graphql";
        String content = QueryLoader.loadQuery(queryName);

        assertNotNull(content);
        assertFalse(content.isBlank());
        assertTrue(content.contains("projectV2"));
    }

    @Test
    void shouldThrowRuntimeExceptionWhenQueryDoesNotExist() {
        String nonExistentQuery = "does-not-exist.graphql";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            QueryLoader.loadQuery(nonExistentQuery);
        });

        assertTrue(exception.getMessage().contains("Could not load GraphQL query"));
    }
}
