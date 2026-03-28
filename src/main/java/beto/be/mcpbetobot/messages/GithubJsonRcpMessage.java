package beto.be.mcpbetobot.messages;

// used to send RCP message to github
public record GithubJsonRcpMessage(
        String jsonRpc,
        String id,
        String method,
        Object params
) {
}
