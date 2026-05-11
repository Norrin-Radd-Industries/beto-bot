package beto.be.mcpbetobot.agentic;

import beto.be.mcpbetobot.domain.GithubTask;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class AnalystAgent extends Agent{

    public AnalystAgent(ChatClient analystChatClient) {
        super(analystChatClient);
    }

    @Override
    String buildPrompt(GithubTask task) {
        return String.format("""
            System context:
            Owner: %s | Repo: %s
            Always provide owner and repo when calling tools.
            
            Task:
            You are a functional analyst. You have access to a knowledge database of the codebase and past PR history.
            
            RELEVANT CODE & HISTORY FROM KNOWLEDGE DATABASE:
            {question_answer_context}
            
            Issue: %d - %s
            Description: %s
    
            Instructions:
            1. Review the 'RELEVANT CODE' provided. This context contains code found by semantic search.
            2. If you need to see the full content of a specific file mentioned in the context that wasn't fully provided, use 'get_file_contents'.
            4. Write a concise, in-depth analysis into the issue body using 'update_issue' with issue_number=%d, appending your analysis below the original description.
            5. Call 'moveTask' with itemId='%s' and statusName='analyzed' to move the issue.
            """,task.repositoryOwner(),
                task.repository(),
                task.number(),
                task.title(),
                task.body(),
                task.number(),
                task.itemId());
    }
}
