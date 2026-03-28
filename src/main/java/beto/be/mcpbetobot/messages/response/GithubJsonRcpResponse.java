package beto.be.mcpbetobot.messages.response;

public record GithubJsonRcpResponse(
        String jsonrcp,
        String id,
        Object result,
        Object error
) {
}
