package beto.be.mcpbetobot.infrastructure.orchestrator;

import beto.be.mcpbetobot.domain.entities.GithubTask;
import beto.be.mcpbetobot.domain.usecases.ProcessTaskUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;

@Service
public class BetoBotOrchestrator {

    private final Logger logger = LoggerFactory.getLogger(BetoBotOrchestrator.class);
    private final ProcessTaskUseCase processTaskUseCase;
    private final Semaphore llmSemaphore = new Semaphore(1);

    public BetoBotOrchestrator(ProcessTaskUseCase processTaskUseCase) {
        this.processTaskUseCase = processTaskUseCase;
    }

    @EventListener
    public void processEvent(GitHubTaskEvent taskEvent){
        GithubTask task = taskEvent.getGithubTask();
        logger.info(">>> Assigning {} agent for task: {}", task.type(), task.number());
        Thread.ofVirtual().start(() -> {
            try {
                llmSemaphore.acquire();
                processTaskUseCase.process(task);
            } catch (InterruptedException  e) {
                logger.error(">>> Agent interrupted for task: {}", task.number());
            } finally {
                llmSemaphore.release();
            }
        });
    }
}
