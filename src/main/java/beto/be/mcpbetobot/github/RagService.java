package beto.be.mcpbetobot.github;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final VectorStore vectorStore;

    public RagService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void saveCodeDocuments(List<Document> documents) {
        if (documents != null && !documents.isEmpty()) {
            this.vectorStore.add(documents);
        }
    }

    public String retrieveContext(String query, String repositoryName) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression filter = b.eq("repository", repositoryName).build();

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(5)
                        .similarityThreshold(0.7)
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
}
