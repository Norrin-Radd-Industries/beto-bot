package beto.be.mcpbetobot.events;

import beto.be.mcpbetobot.domain.GithubTask;
import org.springframework.context.ApplicationEvent;

public class GitHubTaskEvent extends ApplicationEvent {

    private final GithubTask githubTask;
    private final String type;

    public GitHubTaskEvent(Object source, GithubTask task, String type) {
        super(source);
        this.githubTask = task;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public GithubTask getGithubTask() {
        return githubTask;
    }
}
