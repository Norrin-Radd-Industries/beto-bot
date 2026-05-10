package beto.be.mcpbetobot.github;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CodeBaseVectorService {

    private final VectorStore vectorStore;

    public CodeBaseVectorService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void saveCodeDocuments(List<Document> documents) {
        if (documents != null && !documents.isEmpty()) {
            this.vectorStore.add(documents);
        }
    }
}
