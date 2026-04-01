package beto.be.mcpbetobot.events;

import beto.be.mcpbetobot.messages.response.GithubIssue;
import org.springframework.context.ApplicationEvent;

public class GithubIssueEvent extends ApplicationEvent {

    private final GithubIssue githubIssue;

    public GithubIssueEvent(Object source, GithubIssue issue) {
        super(source);
        this.githubIssue = issue;
    }

    public GithubIssue getGithubIssue() {
        return this.githubIssue;
    }
}
