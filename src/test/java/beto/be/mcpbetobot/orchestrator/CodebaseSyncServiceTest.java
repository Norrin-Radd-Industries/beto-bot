package beto.be.mcpbetobot.orchestrator;

import beto.be.mcpbetobot.github.GithubProjectService;
import beto.be.mcpbetobot.github.RagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodebaseSyncServiceTest {

    @Mock
    private GithubProjectService githubProjectService;

    @Mock
    private RagService ragService;

    @InjectMocks
    private CodebaseSyncService codebaseSyncService;

    @Test
    void shouldSyncAllRepositoriesSuccessfully() throws Exception {
        String repo1 = "owner/repo1";
        String repo2 = "owner/repo2";
        List<String> linkedRepos = List.of(repo1, repo2);

        Document doc1 = new Document("content1");
        Document doc2 = new Document("content2");

        when(githubProjectService.fetchLinkedRepositoryNames()).thenReturn(linkedRepos);
        when(githubProjectService.fetchEntireRepository(repo1)).thenReturn(List.of(doc1));
        when(githubProjectService.fetchEntireRepository(repo2)).thenReturn(List.of(doc2));

        codebaseSyncService.syncAllRepositories();

        verify(githubProjectService, times(1)).fetchLinkedRepositoryNames();
        verify(githubProjectService, times(1)).fetchEntireRepository(repo1);
        verify(githubProjectService, times(1)).fetchEntireRepository(repo2);
        verify(ragService, times(1)).saveCodeDocuments(List.of(doc1));
        verify(ragService, times(1)).saveCodeDocuments(List.of(doc2));
    }

    @Test
    void shouldHandleExceptionWhenFetchingLinkedRepositoryNames() {
        when(githubProjectService.fetchLinkedRepositoryNames()).thenThrow(new RuntimeException("API error"));

        // Should not throw exception, just catch and log
        codebaseSyncService.syncAllRepositories();

        verify(githubProjectService, times(1)).fetchLinkedRepositoryNames();
        verifyNoMoreInteractions(ragService);
    }

    @Test
    void shouldHandleExceptionWhenSyncingSingleRepository() throws Exception {
        String repo = "owner/repo";
        when(githubProjectService.fetchLinkedRepositoryNames()).thenReturn(List.of(repo));
        when(githubProjectService.fetchEntireRepository(repo)).thenThrow(new RuntimeException("Fetch error"));

        // Should catch exception and not crash
        codebaseSyncService.syncAllRepositories();

        verify(githubProjectService, times(1)).fetchEntireRepository(repo);
        verify(ragService, never()).saveCodeDocuments(anyList());
    }

    @Test
    void shouldSyncRepositoryRemovalSuccessfully() {
        String repo = "owner/repo";
        String filePath = "src/main/java/App.java";

        codebaseSyncService.syncRepositoryRemoval(repo, filePath);

        verify(ragService, times(1)).removeDocument(repo, filePath);
    }
}
