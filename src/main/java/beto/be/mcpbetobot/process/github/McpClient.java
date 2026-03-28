package beto.be.mcpbetobot.process.github;

import beto.be.mcpbetobot.messages.request.GithubJsonRcpMessage;
import beto.be.mcpbetobot.messages.request.buildingblocks.Capabilities;
import beto.be.mcpbetobot.messages.request.buildingblocks.ClientInfo;
import beto.be.mcpbetobot.messages.request.buildingblocks.InitializeParams;
import beto.be.mcpbetobot.messages.response.GithubJsonRcpResponse;
import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class McpClient implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(McpClient.class);

    @Value("${GITHUB_PERSONAL_ACCESS_TOKEN}")
    private String githubApiKey;

    AtomicInteger idCounter = new AtomicInteger(1);
    Map<String, CompletableFuture<String>> requestQueue = new ConcurrentHashMap<>();
    ObjectMapper mapper = new ObjectMapper();
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;

    @PostConstruct
    public void onStartup(){
        logger.info(" >>> Starting Beto-Bot GitHub Client... <<<");
    }

    public Process startProcessBuilder() throws IOException {
        String GITHUB_PROCESS_INPUT = "npx -y @modelcontextprotocol/server-github@latest mcp-server-github";
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", GITHUB_PROCESS_INPUT);
        processBuilder.environment().put("GITHUB_PERSONAL_ACCESS_TOKEN", githubApiKey);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        return processBuilder.start();
    }

    @Override
    public void run(String... args) throws IOException {
        Process process = startProcessBuilder();
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        this.bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        // create a new virtual thread to handle the return coming from the mcp server
        // so we dont block our entire application
        Thread.ofVirtual().start(() -> {
            try {
                String line;
                while(process.isAlive() && (line = bufferedReader.readLine()) != null){
                    // parse line to get id
                    GithubJsonRcpResponse response = mapper.readValue(line, GithubJsonRcpResponse.class);
                    if (response.id() != null){
                        // if future is found, complete it
                        CompletableFuture<String> future = requestQueue.remove(response.id());
                        if (future != null){
                            future.complete(line);
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Error while getting mcp server output: {}", e.getMessage());
            }
        });

        logger.info(">>> Starting Handshake...(wipe hands afterwards.) <<<");
        InitializeParams params = new InitializeParams(
                "2025-11-25",
                new ClientInfo("beto-bot", "1.0.0"),
                new Capabilities()
        );

        sendRequest("initialize", params)
                .thenCompose(initResponse -> {
                    logger.info(">>> Handshake success 'start cleaning!'<<<");
                    return sendRequest("tools/list", new Object());
                })
                .thenAccept(toolsJson -> {
                    logger.info("Tools received, you tool!");
                    logger.info(toolsJson);
                })
                .exceptionally(ex -> {
                    logger.error("Failure because : {}", ex.getMessage());
                    return null;
                });
    }


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
        } catch (IOException e){
            logger.error("error occurred while fetching response");
            future.completeExceptionally(e);
            requestQueue.remove(id);
        }
        return future;
    }
}
