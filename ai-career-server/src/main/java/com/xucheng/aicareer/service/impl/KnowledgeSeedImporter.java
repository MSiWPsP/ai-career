package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.service.model.KnowledgeChunk;
import com.xucheng.aicareer.service.model.KnowledgeSeedDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 首期受控知识导入器。只有显式启用 import 开关才运行，不暴露公网写接口。
 * 导入失败时阻止该次启动继续作为知识服务运行，避免误认为已更新索引。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = {"ai.rag.enabled", "ai.rag.import"}, havingValue = "true")
public class KnowledgeSeedImporter implements ApplicationRunner {

    private final PgKnowledgeRepository repository;
    private final MarkdownKnowledgeChunker chunker;
    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;
    private final Path directory;
    private final String model;

    public KnowledgeSeedImporter(PgKnowledgeRepository repository, MarkdownKnowledgeChunker chunker,
                                 @Qualifier("ragEmbeddingModel") EmbeddingModel embeddingModel,
                                 ObjectMapper objectMapper,
                                 @Value("${ai.rag.knowledge-directory}") String directory,
                                 @Value("${ai.rag.embedding-model:text-embedding-v4}") String model) {
        this.repository = repository;
        this.chunker = chunker;
        this.embeddingModel = embeddingModel;
        this.objectMapper = objectMapper;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.model = model;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path realDirectory = directory.toRealPath();
        Catalog catalog = objectMapper.readValue(Files.readString(realDirectory.resolve("catalog.json"),
                StandardCharsets.UTF_8), Catalog.class);
        if (catalog.documents() == null || catalog.documents().isEmpty()) {
            throw new IllegalArgumentException("知识目录为空，拒绝导入");
        }
        Set<String> seenIds = new HashSet<>();
        for (KnowledgeSeedDocument document : catalog.documents()) {
            validateDocument(document, realDirectory, seenIds);
        }
        repository.initializeSchema();
        int totalChunks = 0;
        for (KnowledgeSeedDocument document : catalog.documents()) {
            Path source = realDirectory.resolve(document.file()).toRealPath();
            List<KnowledgeChunk> chunks = chunker.split(Files.readString(source, StandardCharsets.UTF_8));
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("知识文档没有有效内容: " + document.id());
            }
            List<float[]> vectors = embeddingModel.embed(chunks.stream()
                    .map(chunk -> document.title() + "\n" + chunk.section() + "\n" + chunk.content())
                    .toList());
            repository.replaceDocument(document, chunks, vectors, model);
            totalChunks += chunks.size();
            log.info("知识导入完成 documentId={} version={} chunks={}",
                    document.id(), document.version(), chunks.size());
        }
        repository.deactivateMissingDocuments(catalog.documents().stream()
                .map(KnowledgeSeedDocument::id).toList());
        log.info("知识目录导入完成 documents={} chunks={} embeddingModel={}",
                catalog.documents().size(), totalChunks, model);
    }

    private void validateDocument(KnowledgeSeedDocument document, Path root, Set<String> seenIds) throws Exception {
        if (document.id() == null || !document.id().matches("[a-z0-9-]{3,100}")
                || !seenIds.add(document.id()) || document.version() < 1
                || document.file() == null || !document.file().matches("[a-z0-9-]+\\.md")) {
            throw new IllegalArgumentException("知识目录存在无效或重复的文档标识");
        }
        if (document.title() == null || document.title().isBlank()
                || document.category() == null || document.category().isBlank()
                || !"PROJECT_ORIGINAL".equals(document.sourceType())
                || document.sourceName() == null || document.sourceName().isBlank()) {
            throw new IllegalArgumentException("知识文档元数据不完整: " + document.id());
        }
        LocalDate reviewedAt = LocalDate.parse(document.reviewedAt());
        LocalDate expiresAt = LocalDate.parse(document.expiresAt());
        if (reviewedAt.isAfter(LocalDate.now()) || expiresAt.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("知识文档未复审或已经过期: " + document.id());
        }
        if (!root.resolve(document.file()).toRealPath().startsWith(root)) {
            throw new IllegalArgumentException("知识文档路径越界: " + document.id());
        }
    }

    public record Catalog(List<KnowledgeSeedDocument> documents) {
    }
}
