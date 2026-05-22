package beto.be.mcpbetobot.data.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagServiceTest {

    @Test
    void shouldStripSingleLineComments() {
        String code = """
                // This is a comment
                int x = 10; // Inline comment
                // Another comment
                int y = 20;
                """;

        String expected = """
                int x = 10;
                int y = 20;""";

        assertEquals(expected, RagService.preprocessCode(code));
    }

    @Test
    void shouldStripBlockComments() {
        String code = """
                /*
                 * Multi-line comment
                 */
                public class Test {
                    /* inline block */int val = 5;
                }
                """;

        String expected = """
                public class Test {
                    int val = 5;
                }""";

        assertEquals(expected, RagService.preprocessCode(code));
    }

    @Test
    void shouldPreserveUrlsAndStrings() {
        String code = """
                String url = "http://github.com";
                String text = "This // is not a comment and /* this */ is not either";
                char commentChar = '/';
                """;

        String expected = """
                String url = "http://github.com";
                String text = "This // is not a comment and /* this */ is not either";
                char commentChar = '/';""";

        assertEquals(expected, RagService.preprocessCode(code));
    }

    @Test
    void shouldCollapseWhitespaceAndBlankLines() {
        String code = """
                package demo;


                import java.util.List;

                public class Sample {

                    public void doWork() {
                        System.out.println("Hello");
                    }

                }
                """;

        String expected = """
                package demo;
                import java.util.List;
                public class Sample {
                    public void doWork() {
                        System.out.println("Hello");
                    }
                }""";

        assertEquals(expected, RagService.preprocessCode(code));
    }
}
