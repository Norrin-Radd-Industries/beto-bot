package beto.be.mcpbetobot.messages.response.toolresponse;

import java.util.List;
/* Standard mcp result wrapper */
public record ToolCallResponse(List<McpContent> content, Boolean isError) {
}
