package beto.be.mcpbetobot.domain.usecases;

import beto.be.mcpbetobot.domain.entities.GithubTask;
import beto.be.mcpbetobot.domain.usecases.gateways.AgentGateway;

public class ProcessTaskUseCase {

    private final AgentGateway analystAgentGateway;
    private final AgentGateway codingAgentGateway;

    public ProcessTaskUseCase(AgentGateway analystAgentGateway, AgentGateway codingAgentGateway) {
        this.analystAgentGateway = analystAgentGateway;
        this.codingAgentGateway = codingAgentGateway;
    }

    public void process(GithubTask task) {
        if ("ANALYSIS".equalsIgnoreCase(task.type())) {
            analystAgentGateway.execute(task);
        } else if ("CODER".equalsIgnoreCase(task.type())) {
            codingAgentGateway.execute(task);
        }
    }
}
