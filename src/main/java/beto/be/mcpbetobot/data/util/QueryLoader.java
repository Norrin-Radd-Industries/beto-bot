package beto.be.mcpbetobot.data.util;

import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class QueryLoader {
    public static String loadQuery(String fileName) {
        try {
            return new ClassPathResource("graphql/" + fileName)
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not load GraphQL query: " + fileName, e);
        }
    }
}
