package beto.be.mcpbetobot.process.github;

import beto.be.mcpbetobot.messages.request.GithubJsonRcpMessage;
import beto.be.mcpbetobot.messages.request.buildingblocks.CallToolParams;
import beto.be.mcpbetobot.messages.request.buildingblocks.Capabilities;
import beto.be.mcpbetobot.messages.request.buildingblocks.ClientInfo;
import beto.be.mcpbetobot.messages.request.buildingblocks.InitializeParams;
import beto.be.mcpbetobot.messages.response.GithubJsonRcpResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class McpClient {

    private final Logger logger = LoggerFactory.getLogger(McpClient.class);

    private final Process process;
    private final BufferedWriter bufferedWriter;
    private final BufferedReader bufferedReader;
    AtomicInteger idCounter = new AtomicInteger(1);
    Map<String, CompletableFuture<String>> requestQueue = new ConcurrentHashMap<>();
    ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void onStartup() {
        logger.info(" >>> Starting Beto-Bot GitHub Client... <<<");
    }

    public McpClient(ProcessBuilder processBuilder) throws IOException {
        this.process = processBuilder.start();
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        this.bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        this.startReaderVirtualThread();
    }

    /**
     * Connection method to initialize handshake with Github's MCP server
     */
    public CompletableFuture<Void> connect() {
        logger.info(">>> Starting Handshake...(wipe hands afterwards.) <<<");
        InitializeParams params = new InitializeParams(
                "2025-11-25",
                new ClientInfo("beto-bot", "1.0.0"),
                new Capabilities()
        );
        // send the request to initialize the handshake
        return sendRequest("initialize", params)
                .thenCompose(response -> {
                    return sendNotification(new Object());
                });
    }

    /**
     * Method our LLM can use to get all available tools
     */
    public CompletableFuture<String> listTools() {
        logger.info(">>>Asking for all available tools<<<");
        return sendRequest(
                "tools/list", new Object());
    }

    /**
     * Method to call a specific tool
     */
    public CompletableFuture<String> callTool(String toolName, Map<String, Object> arguments){
        CallToolParams params = new CallToolParams(toolName, arguments);
        return sendRequest("tools/call", params).thenApply(this::extractTextFromMcpJson);
    }

    /**
     * Second part of the handshake, basically an ACK
     */
    private CompletableFuture<Void> sendNotification(Object params) {
        try {
            GithubJsonRcpMessage message = new GithubJsonRcpMessage(
                    "2.0",
                    null,
                    "notifications/initialized",
                    params
            );
            bufferedWriter.write(mapper.writeValueAsString(message) + "\n");
            bufferedWriter.flush();
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Using virtual thread here because why not :)
     * and we can make sure our application is not stalling on github's response
     */
    private void startReaderVirtualThread() {
        // create a new virtual thread to handle the return coming from the mcp server
        // so we dont block our entire application
        Thread.ofVirtual().start(() -> {
            try {
                String line;
                while (process.isAlive() && (line = bufferedReader.readLine()) != null) {
                    // parse line to get id
                    GithubJsonRcpResponse response = mapper.readValue(line, GithubJsonRcpResponse.class);
                    if (response.id() != null) {
                        // if future is found, complete it
                        CompletableFuture<String> future = requestQueue.remove(response.id());
                        if (future != null) {
                            future.complete(line);
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Error while getting mcp server output: {}", e.getMessage());
            }
        });
    }

    /**
     * Generic method to send a request to the MCP server
     */
    private CompletableFuture<String> sendRequest(String method, Object params) {
        String id = String.valueOf(idCounter.getAndIncrement());
        CompletableFuture<String> future = new CompletableFuture<>();
        requestQueue.put(id, future);

        try {
            GithubJsonRcpMessage message = new GithubJsonRcpMessage(
                    "2.0",
                    id,
                    method,
                    params
            );
            bufferedWriter.write(mapper.writeValueAsString(message) + "\n");
            bufferedWriter.flush();
        } catch (IOException e) {
            logger.error("error occurred while fetching response");
            future.completeExceptionally(e);
            requestQueue.remove(id);
        }
        return future;
    }


    /**
     * Method to parse the raw RCP response into a structured text
     */
    private String extractTextFromMcpJson(String json) {
        JsonNode root = mapper.readTree(json);
        JsonNode content = root.path("result").path("content");

        if (content.isArray() && !content.isEmpty()) {
            return content.get(0).path("text").asString();
        }
        return "no text content found";
    }
}
