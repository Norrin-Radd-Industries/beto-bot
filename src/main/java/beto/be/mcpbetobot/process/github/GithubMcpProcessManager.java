package beto.be.mcpbetobot.process.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class GithubMcpProcessManager {

    private final Logger logger = LoggerFactory.getLogger("GithubMcpProcessManager");

    @Value("${github.api.key}")
    private final String GITHUB_API_KEY = "test123";

    private final ProcessBuilder processBuilder;

    public GithubMcpProcessManager() {
        String GITHUB_PROCESS_INPUT = "npx.cmd -y @modelcontextprotocol/server-github";
        processBuilder = new ProcessBuilder(GITHUB_PROCESS_INPUT);
        processBuilder.environment().put("GITHUB_PERSONAL_ACCESS_TOKEN", GITHUB_API_KEY);
    }

    public void processStream() throws IOException {
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT); // redirects to intelliJ console
        Process process = processBuilder.start();

        // bufferedWriter to send our input ( == output in the process )
        BufferedWriter bufferedWriter = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream()));
        // bufferedReader to read the output coming from the mcp server
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));


        // create a new virtual thread to handle the input reading
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

    }

}
