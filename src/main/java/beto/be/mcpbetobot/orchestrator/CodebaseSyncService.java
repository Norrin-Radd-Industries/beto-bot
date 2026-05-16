package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.github.GithubProjectService;
import beto.be.mcpbetobot.github.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CodebaseSyncService {

    private final Logger logger = LoggerFactory.getLogger(CodebaseSyncService.class);
    private final GithubProjectService githubProjectService;
    private final RagService ragService;

    public CodebaseSyncService(GithubProjectService githubProjectService, RagService ragService) {
        this.githubProjectService = githubProjectService;
        this.ragService = ragService;
    }

    public void syncAllRepositories() {
        logger.info(">>> Starting codebase to vector sync for all linked repositories");
        List<String> myRepos = githubProjectService.fetchLinkedRepositoryNames();

        try {
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
            List<Document> repoCodeFiles = githubProjectService.fetchEntireRepository(repoWithOwner);
            ragService.saveCodeDocuments(repoCodeFiles);
            logger.info(">>> Synced vectorDB with {} files for {}", repoCodeFiles.size(), repoWithOwner);
        } catch (Exception e) {
            logger.error(">>> Failure during sync of repo: {}", repoWithOwner, e);
        }
    }

    public void syncRepositoryRemoval(String repoName, String filePath) {
        ragService.removeDocument(repoName, filePath);
    }
}
