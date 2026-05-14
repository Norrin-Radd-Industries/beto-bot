package beto.be.mcpbetobot.agentic;

import beto.be.mcpbetobot.domain.GithubTask;
import org.springframework.ai.chat.client.ChatClient;

public abstract class Agent {

    private final ChatClient client;

    public Agent(ChatClient client){
        this.client = client;
    }

    public void start(GithubTask task) {
        client.prompt()
                .system(promptSpec -> promptSpec.text(buildPrompt(task)))
                .user("execute the task provided")
                .call();
    }

    abstract String buildPrompt(GithubTask task);
}
