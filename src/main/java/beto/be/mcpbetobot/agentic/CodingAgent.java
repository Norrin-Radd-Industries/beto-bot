package beto.be.mcpbetobot.agentic;

import beto.be.mcpbetobot.domain.GithubTask;
import beto.be.mcpbetobot.github.RagService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CodingAgent extends Agent {

    public CodingAgent(ChatClient coderChatClient,
                       RagService ragService,
                       SyncMcpToolCallbackProvider toolCallbackProvider) {
        super(coderChatClient, ragService, toolCallbackProvider);
    }

    @Override
    String buildPrompt(GithubTask task, String context) {
        return String.format("""
        CRITICAL RULES - NEVER VIOLATE:
        - Repository owner is ALWAYS: %s
        - Repository name is ALWAYS: %s
        - NEVER use any other owner or repo. Not 'demo', not 'Norrin', not anything else.
        - If unsure, use exactly: owner=%s repo=%s
        - The branch name is ALWAYS: feature/issue-%d
        - NEVER push directly to main or master.

        You are a senior Java developer working on a Spring Boot application.

        HISTORICAL CONTEXT & CODE SNIPPETS FROM KNOWLEDGE DATABASE:
        %s

        Issue: %d - %s
        Description: %s

        Instructions:
        1. Review the 'HISTORICAL CONTEXT' above to understand existing patterns and conventions.
        2. Write your COMPLETE implementation plan before calling any tools.
        3. Only AFTER your plan is written, execute it in this order:
           a. Create branch 'feature/issue-%d' using 'create_branch'.
           b. Use 'get_file_contents' sparingly - only for files strictly missing from the context above.
           c. Implement all changes and push them using 'push_files'.
           d. Create a PR using 'create_pull_request'.
           e. Link the issue %s to the PR using 'issue_write'.
           f. Call 'moveTask' with itemId='%s' and statusName='Developed' as your final action.

        Do NOT call any tools until you have written your full implementation plan.
        You MUST call 'moveTask' as your final action.
        """,
                task.repositoryOwner(),
                task.repository(),
                task.repositoryOwner(),
                task.repository(),
                task.number(),
                context,
                task.number(),
                task.title(),
                task.body(),
                task.number(),
                task.number(),
                task.itemId());
    }

    @Override
    Set<String> getAllowedTools() {
        return Set.of(
                "get_file_contents",
                "create_branch",
                "push_files",
                "create_pull_request",
                "issue_write",
                "moveTask");
    }
}
