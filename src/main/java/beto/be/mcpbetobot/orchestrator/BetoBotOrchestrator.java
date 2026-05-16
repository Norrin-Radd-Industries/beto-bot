package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.agentic.Agent;
import beto.be.mcpbetobot.agentic.AnalystAgent;
import beto.be.mcpbetobot.agentic.CodingAgent;
import beto.be.mcpbetobot.domain.GithubTask;
import beto.be.mcpbetobot.events.GitHubTaskEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;

@Service
public class BetoBotOrchestrator {

    private final Logger logger = LoggerFactory.getLogger(BetoBotOrchestrator.class);
    private final CodingAgent codingAgent;
    private final AnalystAgent analystAgent;
    private final Semaphore llmSemaphore = new Semaphore(1);


    public BetoBotOrchestrator(CodingAgent codingAgent,
                               AnalystAgent analystAgent) {
        this.codingAgent = codingAgent;
        this.analystAgent = analystAgent;
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
        // added semaphore when running on local ollama agent
        logger.info(">>> Assigning {} agent for task: {}", task.type(), task.number());
        Thread.ofVirtual().start(() -> {
            try {
                llmSemaphore.acquire();
                agent.start(task);
            } catch (InterruptedException  e) {
                logger.error(">>> Agent interrupted for task: {}", task.number());
            } finally {
                llmSemaphore.release();
            }
        });
    }
}
