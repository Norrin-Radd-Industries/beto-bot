package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.agentic.Agent;
import beto.be.mcpbetobot.agentic.AnalystAgent;
import beto.be.mcpbetobot.agentic.CodingAgent;
import beto.be.mcpbetobot.domain.GithubTask;
import beto.be.mcpbetobot.events.GitHubTaskEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class BetoBotOrchestrator {

    private final Logger logger = LoggerFactory.getLogger(BetoBotOrchestrator.class);
    private final CodingAgent codingAgent;
    private final AnalystAgent analystAgent;
    private final VectorStore vectorStore;

    public BetoBotOrchestrator(CodingAgent codingAgent,
                               AnalystAgent analystAgent,
                               VectorStore vectorStore) {
        this.codingAgent = codingAgent;
        this.analystAgent = analystAgent;
        this.vectorStore = vectorStore;
    }

    @EventListener
    public void processEvent(GitHubTaskEvent taskEvent){
        GithubTask task = taskEvent.getGithubTask();
        if (task.type().equals("ANALYSIS")) {
            runAgent(task, analystAgent);
        } else if (task.type().equals("CODER")) {
            runAgent(task, codingAgent);
        }
    }

    private void runAgent(GithubTask task, Agent agent){
        // start virtual thread to have agents be non-blocking for platform threads
        logger.info(">>> Assigning {} agent for task: {}", task.type(), task.number());
        Thread.ofVirtual().start(() -> {
            try {
                agent.start(task);
            } catch (Exception e) {
                logger.error(">>> Virtual Thread with agent failed: {}", e.getMessage());
            }
        });
    }
}
