package beto.be.mcpbetobot.domain.entities;

public record SourceFile(
        String content,
        String repoName,
        String filePath,
        String branch
) {
}
