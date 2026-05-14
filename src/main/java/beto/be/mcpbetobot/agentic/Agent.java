package beto.be.mcpbetobot.agentic;

import beto.be.mcpbetobot.domain.GithubTask;
import beto.be.mcpbetobot.github.RagService;
import org.springframework.ai.chat.client.ChatClient;

public abstract class Agent {

    private final ChatClient client;
    private final RagService ragService;

    public Agent(ChatClient client,
                 RagService ragService){
        this.client = client;
        this.ragService = ragService;
    }

    public void start(GithubTask task) {
        String fullRepoName = task.repositoryOwner() + "/" + task.repository();
        String context = ragService.retrieveContext(task.body(), fullRepoName);

        client.prompt()
                .system(promptSpec -> promptSpec
                        .text(buildPrompt(task))
                        .param("question_answer_context", context))
                .user("execute the task provided")
                .call()
                .content();
    }

    abstract String buildPrompt(GithubTask task);
}
