package beto.be.mcpbetobot.domain;

public record GithubIssue(
        int number,
        String title,
        String body,
        String state
) {
}
