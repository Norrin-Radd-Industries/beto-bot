package beto.be.mcpbetobot.domain.usecases;

import beto.be.mcpbetobot.domain.entities.SourceFile;
import beto.be.mcpbetobot.domain.usecases.gateways.GithubProjectGateway;
import beto.be.mcpbetobot.domain.usecases.gateways.VectorStoreGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SyncCodebaseUseCase {

    private final Logger logger = LoggerFactory.getLogger(SyncCodebaseUseCase.class);
    private final GithubProjectGateway githubProjectGateway;
    private final VectorStoreGateway vectorStoreGateway;

    public SyncCodebaseUseCase(GithubProjectGateway githubProjectGateway, VectorStoreGateway vectorStoreGateway) {
        this.githubProjectGateway = githubProjectGateway;
        this.vectorStoreGateway = vectorStoreGateway;
    }

    public void syncAllRepositories() {
        logger.info(">>> Starting codebase to vector sync for all linked repositories");
        try {
            List<String> myRepos = githubProjectGateway.fetchLinkedRepositoryNames();
            for (String repo : myRepos) {
                syncRepository(repo);
            }
        } catch (Exception e) {
            logger.error(">>> Failure during fetch of repos", e);
        }
    }

    public void syncRepository(String repoWithOwner) {
        try {
            logger.info(">>> Syncing repository: {}", repoWithOwner);
            List<SourceFile> repoCodeFiles = githubProjectGateway.fetchEntireRepository(repoWithOwner);
            vectorStoreGateway.saveCodeDocuments(repoCodeFiles);
            logger.info(">>> Synced vectorDB with {} files for {}", repoCodeFiles.size(), repoWithOwner);
        } catch (Exception e) {
            logger.error(">>> Failure during sync of repo: {}", repoWithOwner, e);
        }
    }

    public void syncRepositoryRemoval(String repoName, String filePath) {
        vectorStoreGateway.removeDocument(repoName, filePath);
    }
}
