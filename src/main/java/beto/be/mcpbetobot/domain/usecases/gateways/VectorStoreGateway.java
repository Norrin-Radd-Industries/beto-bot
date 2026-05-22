package beto.be.mcpbetobot.domain.usecases.gateways;

import beto.be.mcpbetobot.domain.entities.SourceFile;

import java.util.List;

public interface VectorStoreGateway {
    void saveCodeDocuments(List<SourceFile> documents);
    String retrieveContext(String query, String repositoryName);
    void removeDocument(String repoName, String filePath);
}
