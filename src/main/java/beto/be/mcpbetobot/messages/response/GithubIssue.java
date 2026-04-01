package beto.be.mcpbetobot.messages.response;

public record GithubIssue(
        int number,
        String title,
        String body,
        String state
) {
}
