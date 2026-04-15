package beto.be.mcpbetobot.agentic;

import beto.be.mcpbetobot.domain.GithubTask;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class CodingAgent extends Agent {

    public CodingAgent(ChatClient coderChatClient) {
        super(coderChatClient);
    }

    @Override
    String buildPrompt(GithubTask task) {
        return String.format("""
            System context:
            Owner: %s
            Repo: %s
            you must always provide these owner and repo values when calling tools
            NOTE: this issue has been pre-analysed, the analysis is in the description.

            Task:
            You are senior Java Developer. You need to fix or implement the following issue:

            Title: %s
            Description: %s

            Todo:
            1. Identify the files mentioned in the analysis using 'get_repository_tree'
            2. Use 'get_file_contents' for those specific files to get the current code.
            3. Implement the fix or functionality on a new branch named 'feature/issue-%d' for %s.
            4. Use 'push_files' to commit your changes.
            5. Create a new pull request on %s
            6. Call 'moveTaskToInProgress' with itemId='%s' to move the issue to the In progress column
            7. Finish by replying you've finished the task.
            """,task.repositoryOwner(),
                task.repository(),
                task.title(),
                task.body(),
                task.number(),
                task.repository(),
                task.repository(),
                task.itemId());
    }
}
