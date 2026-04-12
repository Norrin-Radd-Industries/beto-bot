package beto.be.mcpbetobot.util;

import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Tool;

import java.util.List;
import java.util.Map;

public class CustomMcpParser {

    public static List<Tool> mapCustomProjectToolsForAgent() {
        List<FunctionDeclaration> declarations = List.of(
                FunctionDeclaration.builder()
                        .name("moveTaskToAnalysed")
                        .description("Move a GitHub project issue to the Analysed column once analysis is complete")
                        .parameters(Schema.builder()
                                .type(com.google.genai.types.Type.Known.OBJECT)
                                .properties(Map.of(
                                        "itemId", Schema.builder()
                                                .type(com.google.genai.types.Type.Known.STRING)
                                                .description("The project item ID")
                                                .build()
                                ))
                                .required(List.of("itemId"))
                                .build())
                        .build(),
                FunctionDeclaration.builder()
                        .name("moveIssueToInProgress")
                        .description("Move a GitHub project issue to In Progress when the coder starts working on it")
                        .parameters(Schema.builder()
                                .type(com.google.genai.types.Type.Known.OBJECT)
                                .properties(Map.of(
                                        "itemId", Schema.builder()
                                                .type(com.google.genai.types.Type.Known.STRING)
                                                .description("")
                                                .build()
                                ))
                                .required(List.of("itemId"))
                                .build())
                        .build());
        return List.of(Tool.builder().functionDeclarations(declarations).build());
    }
}
