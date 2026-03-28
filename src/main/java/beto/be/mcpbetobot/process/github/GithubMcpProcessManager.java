package beto.be.mcpbetobot.process.github;

import beto.be.mcpbetobot.messages.request.GithubJsonRcpMessage;
import beto.be.mcpbetobot.messages.request.buildingblocks.Capabilities;
import beto.be.mcpbetobot.messages.request.buildingblocks.ClientInfo;
import beto.be.mcpbetobot.messages.request.buildingblocks.InitializeParams;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.io.*;

@Service
public class GithubMcpProcessManager implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(GithubMcpProcessManager.class);

    @Value("${GITHUB_PERSONAL_ACCESS_TOKEN}")
    private String githubApiKey;

    @Override
    public void run(String @NonNull ... args) throws IOException, InterruptedException {
        logger.info(" >>> initiated GithubMcpProcessManager <<<");
        String GITHUB_PROCESS_INPUT = "npx -y @modelcontextprotocol/server-github@latest mcp-server-github";
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", GITHUB_PROCESS_INPUT);
        processBuilder.environment().put("GITHUB_PERSONAL_ACCESS_TOKEN", githubApiKey);

        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT); // redirects errors to intelliJ console
        Process process = processBuilder.start();

        // bufferedWriter to send our input ( == output in the process )
        BufferedWriter bufferedWriter = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream()));
        // bufferedReader to read the output coming from the mcp server
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));


        // create a new virtual thread to handle the return coming from the mcp server
        // so we dont block our entire application
        Thread.ofVirtual().start(() -> {
            try {
                String line;
                // only continues after a full line of text has arrived
                while(process.isAlive() && (line = bufferedReader.readLine()) != null){
                    logger.info("MCP SERVER SAYS: {}", line);
                }
            } catch (IOException e) {
                logger.error("Error while getting mcp server output: {}", e.getMessage());
            }
        });

        InitializeParams params = new InitializeParams(
                "2025-11-25",
                new ClientInfo("beto-bot", "1.0.0"),
                new Capabilities()
        );

        GithubJsonRcpMessage message = new GithubJsonRcpMessage(
                "2.0",
                "1",
                "initialize",
                params
        );

        ObjectMapper mapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
                .build();
        String valueAsString = mapper.writeValueAsString(message);
        bufferedWriter.write(valueAsString);
        bufferedWriter.write("\n");
        bufferedWriter.flush();

        Thread.sleep(1000);


        GithubJsonRcpMessage initializedNotification = new GithubJsonRcpMessage(
                "2.0",
                null, // notification id must be null
                "notifications/initialized",
                new Object()
        );

        bufferedWriter.write(mapper.writeValueAsString(initializedNotification));
        bufferedWriter.write("\n");
        bufferedWriter.flush();

        GithubJsonRcpMessage toolsRequest = new GithubJsonRcpMessage(
                "2.0",
                "2", // +1 each time
                "tools/list",
                new Object()
        );

        bufferedWriter.write(mapper.writeValueAsString(toolsRequest));
        bufferedWriter.write("\n");
        bufferedWriter.flush();
    }

}
