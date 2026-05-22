package beto.be.mcpbetobot.domain.usecases.gateways;

import beto.be.mcpbetobot.domain.entities.GithubTask;

public interface AgentGateway {
    void execute(GithubTask task);
}
