package beto.be.mcpbetobot.domain;

import java.util.List;

public record GithubTask(
        String itemId,
        String issueId,
        int number,
        String title,
        String body,
        String state,
        String repository,
        String repositoryOwner,
        String type,
        List<GithubTask> blockedBy
) {

    public boolean isRunnable() {
        return !"CLOSED".equalsIgnoreCase(state) &&
                blockedBy.stream().allMatch(blocker -> "CLOSED".equalsIgnoreCase(blocker.state()));
    }
}
