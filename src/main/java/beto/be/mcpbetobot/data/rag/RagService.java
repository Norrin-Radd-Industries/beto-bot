package beto.be.mcpbetobot.data.rag;

import beto.be.mcpbetobot.domain.entities.SourceFile;
import beto.be.mcpbetobot.domain.usecases.gateways.VectorStoreGateway;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RagService implements VectorStoreGateway {

    private final VectorStore vectorStore;

    public RagService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void saveCodeDocuments(List<SourceFile> documents) {
        List<Document> prefixedDocs = documents.stream()
                .map(this::mapToDocument)
                .toList();

        this.vectorStore.add(prefixedDocs);
    }

    private Document mapToDocument(SourceFile file) {
        String uniqueId = file.repoName() + ":" + file.filePath();
        UUID deterministicId = UUID.nameUUIDFromBytes(uniqueId.getBytes(StandardCharsets.UTF_8));
        String cleanedContent = preprocessCode(file.content());
        return Document.builder()
                .id(deterministicId.toString())
                .text("task: retrieval_document: " + cleanedContent)
                .metadata("type", "code_file")
                .metadata("filePath", file.filePath())
                .metadata("repository", file.repoName())
                .metadata("branch", file.branch())
                .build();
    }

    public static String preprocessCode(String content) {
        if (content == null) {
            return "";
        }
        // Match string literals, char literals, block comments, or line comments
        Pattern pattern = Pattern.compile("(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')|/\\*(?s:.*?)\\*/|//.*");
        Matcher matcher = pattern.matcher(content);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String literal = matcher.group(1);
            if (literal != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(literal));
            } else {
                matcher.appendReplacement(sb, "");
            }
        }
        matcher.appendTail(sb);

        // Collapse multiple blank lines and strip trailing whitespace on each line
        return sb.toString()
                .replaceAll("(?m)[ \\t]+$", "")
                .replaceAll("(?m)^[ \\t]*\\r?\\n", "")
                .replaceAll("\\n+", "\n")
                .trim();
    }

    @Override
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

    @Override
    public void removeDocument(String repoName, String filePath) {
        String uniqueId = repoName + ":" + filePath;
        UUID deterministicId = UUID.nameUUIDFromBytes(uniqueId.getBytes(StandardCharsets.UTF_8));
        vectorStore.delete(List.of(deterministicId.toString()));
    }
}
