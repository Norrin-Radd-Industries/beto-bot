package beto.be.mcpbetobot.messages.request;

// used to send RCP message to github
public record GithubJsonRcpMessage(
        String jsonrpc,
        String id,
        String method,
        Object params
) {
}
