package beto.be.mcpbetobot.domain.usecases;

import beto.be.mcpbetobot.domain.entities.SourceFile;
import beto.be.mcpbetobot.domain.usecases.gateways.GithubProjectGateway;
import beto.be.mcpbetobot.domain.usecases.gateways.VectorStoreGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncCodebaseUseCaseTest {

    @Mock
    private GithubProjectGateway githubProjectGateway;

    @Mock
    private VectorStoreGateway vectorStoreGateway;

    @InjectMocks
    private SyncCodebaseUseCase syncCodebaseUseCase;

    @Test
    void shouldSyncAllRepositoriesSuccessfully() {
        String repo1 = "owner/repo1";
        String repo2 = "owner/repo2";
        List<String> linkedRepos = List.of(repo1, repo2);

        SourceFile file1 = new SourceFile("content1", repo1, "f1", "main");
        SourceFile file2 = new SourceFile("content2", repo2, "f2", "main");

        when(githubProjectGateway.fetchLinkedRepositoryNames()).thenReturn(linkedRepos);
        when(githubProjectGateway.fetchEntireRepository(repo1)).thenReturn(List.of(file1));
        when(githubProjectGateway.fetchEntireRepository(repo2)).thenReturn(List.of(file2));

        syncCodebaseUseCase.syncAllRepositories();

        verify(githubProjectGateway, times(1)).fetchLinkedRepositoryNames();
        verify(githubProjectGateway, times(1)).fetchEntireRepository(repo1);
        verify(githubProjectGateway, times(1)).fetchEntireRepository(repo2);
        verify(vectorStoreGateway, times(1)).saveCodeDocuments(List.of(file1));
        verify(vectorStoreGateway, times(1)).saveCodeDocuments(List.of(file2));
    }

    @Test
    void shouldHandleExceptionWhenFetchingLinkedRepositoryNames() {
        when(githubProjectGateway.fetchLinkedRepositoryNames()).thenThrow(new RuntimeException("API error"));

        // Should not throw exception, just catch and log
        syncCodebaseUseCase.syncAllRepositories();

        verify(githubProjectGateway, times(1)).fetchLinkedRepositoryNames();
        verifyNoMoreInteractions(vectorStoreGateway);
    }

    @Test
    void shouldHandleExceptionWhenSyncingSingleRepository() {
        String repo = "owner/repo";
        when(githubProjectGateway.fetchLinkedRepositoryNames()).thenReturn(List.of(repo));
        when(githubProjectGateway.fetchEntireRepository(repo)).thenThrow(new RuntimeException("Fetch error"));

        // Should catch exception and not crash
        syncCodebaseUseCase.syncAllRepositories();

        verify(githubProjectGateway, times(1)).fetchEntireRepository(repo);
        verify(vectorStoreGateway, never()).saveCodeDocuments(anyList());
    }

    @Test
    void shouldSyncRepositoryRemovalSuccessfully() {
        String repo = "owner/repo";
        String filePath = "src/main/java/App.java";

        syncCodebaseUseCase.syncRepositoryRemoval(repo, filePath);

        verify(vectorStoreGateway, times(1)).removeDocument(repo, filePath);
    }
}
