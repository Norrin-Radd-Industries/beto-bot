package beto.be.mcpbetobot.infrastructure.agentic;

import beto.be.mcpbetobot.domain.entities.GithubTask;
import beto.be.mcpbetobot.domain.usecases.gateways.VectorStoreGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.NullMarked;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Arrays;
import java.util.Set;

public abstract class Agent {

    private final ChatClient client;
    private final VectorStoreGateway vectorStoreGateway;
    private final SyncMcpToolCallbackProvider toolCallbackProvider;
    private final ObjectMapper objectMapper;


    public Agent(ChatClient client,
                 VectorStoreGateway vectorStoreGateway,
                 SyncMcpToolCallbackProvider toolCallbackProvider){
        this.client = client;
        this.vectorStoreGateway = vectorStoreGateway;
        this.toolCallbackProvider = toolCallbackProvider;
        this.objectMapper = new ObjectMapper();
    }

    public void start(GithubTask task) {
        String fullRepoName = task.repositoryOwner() + "/" + task.repository();
        String ragQuery = task.title() + " " + task.body();
        String context = vectorStoreGateway.retrieveContext(ragQuery, fullRepoName);

        ToolCallback[] filteredTools = Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .filter(cb -> getAllowedTools().contains(cb.getToolDefinition().name()))
                .map(toolCallback -> guardedCallback(toolCallback, task))
                .toArray(ToolCallback[]::new);

        client.prompt()
                .system(promptSpec -> promptSpec
                        .text(buildPrompt(task, context)))
                .user("execute the task provided")
                .advisors(new SimpleLoggerAdvisor(), new CavemanRAGAdvisor())
                .toolCallbacks(filteredTools)
                .call()
                .content();
    }

    @NullMarked
    private ToolCallback guardedCallback(ToolCallback cb, GithubTask task) {
        return new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return cb.getToolDefinition();
            }

            @Override
            public String call(String toolInput) {
                try {
                    ObjectNode args = (ObjectNode) objectMapper.readTree(toolInput);

                    if (!args.has("owner") || args.get("owner").asText().isBlank()
                            || !args.get("owner").asText().equals(task.repositoryOwner())) {
                        args.put("owner", task.repositoryOwner());
                    }
                    if (!args.has("repo") || args.get("repo").asText().isBlank()
                            || !args.get("repo").asText().equals(task.repository())) {
                        args.put("repo", task.repository());
                    }

                    return cb.call(objectMapper.writeValueAsString(args));
                } catch (Exception e) {
                    return cb.call(toolInput);
                }
            }
        };
    }

    abstract String buildPrompt(GithubTask task, String context);
    abstract Set<String> getAllowedTools();
}
