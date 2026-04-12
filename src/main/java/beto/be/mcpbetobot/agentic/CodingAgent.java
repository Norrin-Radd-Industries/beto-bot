package beto.be.mcpbetobot.agentic;

import beto.be.mcpbetobot.domain.GithubTask;
import beto.be.mcpbetobot.mcp.ProjectService;
import com.google.genai.Client;
import io.modelcontextprotocol.client.McpAsyncClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CodingAgent extends Agent {

    public CodingAgent(List<McpAsyncClient> customMcpAsyncClientList,
                       ProjectService projectService) {
        super(new Client(),
                "gemini-3-flash-preview",
                customMcpAsyncClientList,
                projectService);
    }

    @Override
    String buildPrompt(GithubTask task) {
        return String.format("""
                System context:
                Repository owner: SilverSurferState
                Repository name: %s
                you must always provide these owner and repo values when calling tools

                Task:
                You are senior Java Developer. You need to fix or implement the following issue:

                Title: %s
                Description: %s

                Todo:
                1. Use 'get_file_contents' with path='.' to list the root directory for this repo and to understand the project
                2. Once you understand the project, implement or fix the issue
                3. Create a new branch named 'feature/issue-%d'
                4. Use 'push_files' to commit your changes and to that branch you just created
                5. Add the label 'beto-bot:in-progress' to the issue you've processed.
                6. Finish by using 'create_pull_request' to create a new pull request and
                summarizing what you changed in the 'body' section of the 'create_pull_request' function.
                """,task.repository() , task.title(), task.body(), task.number());
    }
}
