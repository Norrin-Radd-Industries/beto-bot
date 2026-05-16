package beto.be.mcpbetobot.github;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final VectorStore vectorStore;

    public RagService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void saveCodeDocuments(List<Document> documents) {
        List<Document> prefixedDocs = documents.stream()
                .map(doc -> {
                    String prefixedContent = "task: retrieval_document: " + doc.getFormattedContent();
                    return new Document(doc.getId(), prefixedContent, doc.getMetadata());
                })
                .toList();

        this.vectorStore.add(prefixedDocs);
    }

    public String retrieveContext(String query, String repositoryName) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression filter = b.eq("repository", repositoryName).build();

        String prefixedQuery = "task: retrieval_query: " + query;

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(prefixedQuery)
                        .topK(10)
                        .similarityThreshold(0.4)
                        .filterExpression(filter)
                        .build()
        );

        return docs.stream()
                .map(doc -> String.format(
                        "--- FILE: %s | BRANCH: %s ---\n%s\n",
                        doc.getMetadata().get("filePath"),
                        doc.getMetadata().get("branch"),
                        doc.getText()
                ))
                .collect(Collectors.joining("\n"));
    }

    public void removeDocument(String repoName, String filePath) {
        String uniqueId = repoName + ":" + filePath;
        UUID deterministicId = UUID.nameUUIDFromBytes(uniqueId.getBytes(StandardCharsets.UTF_8));
        vectorStore.delete(List.of(deterministicId.toString()));
    }
}
