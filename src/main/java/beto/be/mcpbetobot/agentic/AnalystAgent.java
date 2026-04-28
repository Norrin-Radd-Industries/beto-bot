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
            Owner: %s
            Repo: %s
            Always provide owner and repo when calling tools.
    
            You are a functional analyst. Your job is to analyse a GitHub issue and prepare it for a coder.
    
            Issue number: %d
            Title: %s
            Description: %s
    
            Todo:
            1. Use 'get_repository_tree' to know what paths and files are present.
            2. Use 'get_file_contents' to read the relevant source files, use the full path string return by 'get_repository_tree.
               If the repository tree is large, focus your analysis on the 'src/main/java' directory first.
            3. Read any files directly relevant to the issue
            4. Write a concise, in-depth analysis into the issue body using 'update_issue' with issue_number=%d, appending your analysis below the original description
            5. Call 'moveTaskToAnalysed' with itemId='%s' to move the issue to the Analysed column
            6. Finish with a short summary of what you analysed and what you recommended
            """,task.repositoryOwner(),
                task.repository(),
                task.number(),
                task.title(),
                task.body(),
                task.number(),
                task.itemId());
    }
}
