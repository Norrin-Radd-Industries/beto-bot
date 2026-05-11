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
            Owner: %s | Repo: %s
            Always provide owner and repo when calling tools.

            You are senior Java Developer.
            
            HISTORICAL CONTEXT & CODE SNIPPETS:
            {question_answer_context}
            
            Task: Implement the fix for "%s".
            Description: %s

            Instructions:
            1. 1. Use the 'HISTORICAL CONTEXT' to identify existing patterns or past PRs that solved similar issues.
            2. Use MCP tools only if you need to read additional files not covered in the retrieved context.
            3. Implement the fix or functionality on a new branch named 'feature/issue-%d'.
            4. Push changes, create a PR, and call 'moveTask' with itemId='%s and statusName='In review'.
            """,task.repositoryOwner(),
                task.repository(),
                task.title(),
                task.body(),
                task.number(),
                task.itemId());
    }
}
