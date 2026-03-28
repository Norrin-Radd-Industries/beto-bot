package beto.be.mcpbetobot.messages.response.toolresponse;
/* used to indicate which repo we want to see issues from */
public record ListIssuesParams(
        String owner,
        String repo
) {
}
