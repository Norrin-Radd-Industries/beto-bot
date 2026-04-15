package beto.be.mcpbetobot.agentic;

import beto.be.mcpbetobot.domain.GithubTask;
import beto.be.mcpbetobot.mcp.ProjectService;
import com.google.genai.Client;
import io.modelcontextprotocol.client.McpAsyncClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnalystAgent extends Agent{

    public AnalystAgent(List<McpAsyncClient> customMcpAsyncClientList,
                        ProjectService projectService) {
        super(new Client(),
                "gemini-3.1-pro-preview",
                customMcpAsyncClientList,
                projectService);
    }

    @Override
    String buildPrompt(GithubTask task) {
        return String.format("""
            System context:
            Repository owner: %s
            Repository name: %s
            Always provide owner='%s' and repo='%s' when calling tools.
    
            You are a functional analyst. Your job is to analyse a GitHub issue and prepare it for a coder.
    
            Issue number: %d
            Title: %s
            Description: %s
    
            Todo:
            1. Use 'get_file_contents' with owner='%s', repo='%s', path='.' to read the relevant source files
            2. Read any files directly relevant to the issue
            3. Write a concise, in-depth analysis into the issue body using 'update_issue' with issue_number=%d, appending your analysis below the original description
            4. Call 'moveTaskToAnalysed' with itemId='%s' to move the issue to the Analysed column
            5. Reply with a short summary of what you analysed and what you recommended
    
            Important: always complete all 4 steps before replying.
            """, task.repositoryOwner(), task.repository(), task.repositoryOwner(), task.repository(), task.number(), task.title(), task.body(),
                    task.repositoryOwner(), task.repository(), task.number(), task.itemId());
    }
}
