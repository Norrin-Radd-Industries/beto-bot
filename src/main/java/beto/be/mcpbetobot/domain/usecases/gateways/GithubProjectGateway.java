package beto.be.mcpbetobot.domain.usecases.gateways;

import beto.be.mcpbetobot.domain.entities.GithubTask;
import beto.be.mcpbetobot.domain.entities.SourceFile;

import java.util.List;

public interface GithubProjectGateway {
    List<GithubTask> getAvailableTasks();
    String moveTask(String itemId, String statusName);
    List<String> fetchLinkedRepositoryNames();
    List<SourceFile> fetchEntireRepository(String repoWithOwner);
}
