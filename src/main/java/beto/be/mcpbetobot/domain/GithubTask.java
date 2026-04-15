package beto.be.mcpbetobot.domain;

public record GithubTask(
        String itemId,
        String issueId,
        int number,
        String title,
        String body,
        String state,
        String repository,
        String repositoryOwner,
        String type
) {
}
