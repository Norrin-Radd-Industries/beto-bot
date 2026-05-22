package beto.be.mcpbetobot.infrastructure.agentic;

import beto.be.mcpbetobot.domain.entities.GithubTask;
import beto.be.mcpbetobot.domain.usecases.gateways.AgentGateway;
import beto.be.mcpbetobot.domain.usecases.gateways.VectorStoreGateway;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AnalystAgent extends Agent implements AgentGateway {

    public AnalystAgent(ChatClient analystChatClient,
                        VectorStoreGateway vectorStoreGateway,
                        SyncMcpToolCallbackProvider toolCallbackProvider) {
        super(analystChatClient, vectorStoreGateway, toolCallbackProvider);
    }

    @Override
    public void execute(GithubTask task) {
        this.start(task);
    }

    @Override
    String buildPrompt(GithubTask task, String context) {
        return String.format("""
            CRITICAL RULES - NEVER VIOLATE:
            - Repository owner is ALWAYS: %s
            - Repository name is ALWAYS: %s
            - NEVER use any other owner or repo. Not 'demo', not 'Norrin', not anything else.
            - If unsure, use exactly: owner=%s repo=%s
            
            Task:
            You are a functional analyst. You have access to a knowledge database of the codebase and past PR history.
            
            RELEVANT CODE & HISTORY FROM KNOWLEDGE DATABASE:
            %s
            
            Issue: %d - %s
            Description: %s
    
            Instructions:
            1. Review the 'RELEVANT CODE' provided above.
            2. Write your COMPLETE analysis (concise, clear, no fluff or filler) before calling any tools.
            3. Your analysis must be thorough but compact - do NOT use placeholders like "to be populated".
            4. Only AFTER your analysis is complete, call 'issue_write' to update the current issue (use parameters: method='update', issue_number=%d, and set 'body' to your complete analysis).
            5. Then call 'moveTask' with itemId='%s' and statusName='Analyzed'.
            
            Do NOT call any tools until you have written the full analysis in your response.
            """, task.repositoryOwner(),
                task.repository(),
                task.repositoryOwner(),
                task.repository(),
                context,
                task.number(),
                task.title(),
                task.body(),
                task.number(),
                task.itemId());
    }

    @Override
    Set<String> getAllowedTools() {
        return Set.of(
                "get_file_contents",
                "issue_write",
                "moveTask");
    }
}
