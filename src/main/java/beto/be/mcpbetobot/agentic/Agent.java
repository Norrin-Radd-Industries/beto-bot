package beto.be.mcpbetobot.agentic;

import beto.be.mcpbetobot.domain.GithubTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

public abstract class Agent {

    private final Logger logger = LoggerFactory.getLogger(Agent.class);
    private final ChatClient client;

    public Agent(ChatClient client){
        this.client = client;
    }

    public void start(GithubTask task) {
        String finalResponse = client.prompt()
                .system(promptSpec -> promptSpec.text(buildPrompt(task)))
                .user("execute the task and confirm when finished")
                .call()
                .content();

        logger.info("---Answer: {}", finalResponse);
    }

    abstract String buildPrompt(GithubTask task);
}
