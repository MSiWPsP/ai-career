package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.service.model.KnowledgeChunk;
import com.xucheng.aicareer.service.model.KnowledgeSeedDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * 独立 PostgreSQL 向量副本。MySQL 主数据源不受影响；只有受控导入操作会创建表和更新向量。
 * 导入先在表外完成 Embedding，再以单个事务切换某文档的生效切片，避免读到半成品。
 */
@Repository
@ConditionalOnProperty(name = "ai.rag.enabled", havingValue = "true")
public class PgKnowledgeRepository {

    private static final String TABLE = "ai_career_knowledge_chunk";
    private final DataSource dataSource;
    private final int dimension;
    private final int queryTimeoutSeconds;

    public PgKnowledgeRepository(
            @Value("${ai.rag.jdbc-url}") String jdbcUrl,
            @Value("${ai.rag.username}") String username,
            @Value("${ai.rag.password}") String password,
            @Value("${ai.rag.embedding-dimension:1024}") int dimension,
            @Value("${ai.rag.pg-connect-timeout-seconds:3}") int connectTimeoutSeconds,
            @Value("${ai.rag.pg-socket-timeout-seconds:5}") int socketTimeoutSeconds,
            @Value("${ai.rag.pg-query-timeout-seconds:3}") int queryTimeoutSeconds) {
        if (dimension < 1 || dimension > 2000) {
            throw new IllegalArgumentException("RAG 向量维度必须在 1 到 2000 之间");
        }
        if (connectTimeoutSeconds < 1 || socketTimeoutSeconds < 1 || queryTimeoutSeconds < 1) {
            throw new IllegalArgumentException("RAG PostgreSQL 超时必须大于零");
        }
        DriverManagerDataSource postgres = new DriverManagerDataSource();
        postgres.setDriverClassName("org.postgresql.Driver");
        postgres.setUrl(jdbcUrl);
        postgres.setUsername(username);
        postgres.setPassword(password);
        // 仅约束知识库连接，不改动 MySQL 主数据源；查询超时后由 Service 降级为空依据。
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("connectTimeout", Integer.toString(connectTimeoutSeconds));
        connectionProperties.setProperty("loginTimeout", Integer.toString(connectTimeoutSeconds));
        connectionProperties.setProperty("socketTimeout", Integer.toString(socketTimeoutSeconds));
        postgres.setConnectionProperties(connectionProperties);
        this.dataSource = postgres;
        this.dimension = dimension;
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }

    /** 仅在显式导入时执行 DDL，普通请求不会修改数据库结构。 */
    public void initializeSchema() {
        String createTable = """
                CREATE TABLE IF NOT EXISTS ai_career_knowledge_chunk (
                  id uuid PRIMARY KEY,
                  document_id varchar(100) NOT NULL,
                  document_version integer NOT NULL,
                  chunk_index integer NOT NULL,
                  title varchar(200) NOT NULL,
                  section_name varchar(200) NOT NULL,
                  category varchar(50) NOT NULL,
                  target_position varchar(100),
                  source_type varchar(30) NOT NULL,
                  source_name varchar(200) NOT NULL,
                  reviewed_at date NOT NULL,
                  expires_at date NOT NULL,
                  content text NOT NULL,
                  embedding_model varchar(100) NOT NULL,
                  embedding vector(%d) NOT NULL,
                  active boolean NOT NULL DEFAULT true,
                  indexed_at timestamptz NOT NULL DEFAULT now(),
                  UNIQUE (document_id, document_version, chunk_index)
                )
                """.formatted(dimension);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE EXTENSION IF NOT EXISTS vector");
            statement.execute(createTable);
            statement.execute("CREATE INDEX IF NOT EXISTS ai_career_knowledge_embedding_idx "
                    + "ON " + TABLE + " USING hnsw (embedding vector_cosine_ops)");
            statement.execute("CREATE INDEX IF NOT EXISTS ai_career_knowledge_active_idx "
                    + "ON " + TABLE + " (active, expires_at, embedding_model)");
        } catch (SQLException exception) {
            throw new IllegalStateException("PGVector 知识表初始化失败", exception);
        }
    }

    public void replaceDocument(KnowledgeSeedDocument document, List<KnowledgeChunk> chunks,
                                List<float[]> vectors, String embeddingModel) {
        if (chunks.isEmpty() || chunks.size() != vectors.size()) {
            throw new IllegalArgumentException("知识切片与向量数量不匹配");
        }
        String insert = """
                INSERT INTO ai_career_knowledge_chunk
                (id, document_id, document_version, chunk_index, title, section_name, category,
                 target_position, source_type, source_name, reviewed_at, expires_at, content,
                 embedding_model, embedding, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS vector), true)
                ON CONFLICT (id) DO UPDATE SET
                  title = EXCLUDED.title, section_name = EXCLUDED.section_name,
                  category = EXCLUDED.category, target_position = EXCLUDED.target_position,
                  source_type = EXCLUDED.source_type, source_name = EXCLUDED.source_name,
                  reviewed_at = EXCLUDED.reviewed_at, expires_at = EXCLUDED.expires_at,
                  content = EXCLUDED.content, embedding_model = EXCLUDED.embedding_model,
                  embedding = EXCLUDED.embedding, active = true, indexed_at = now()
                """;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deactivate = connection.prepareStatement(
                    "UPDATE " + TABLE + " SET active = false WHERE document_id = ?");
                 PreparedStatement upsert = connection.prepareStatement(insert)) {
                deactivate.setString(1, document.id());
                deactivate.executeUpdate();
                for (int index = 0; index < chunks.size(); index++) {
                    KnowledgeChunk chunk = chunks.get(index);
                    upsert.setObject(1, UUID.nameUUIDFromBytes(("ai-career/" + document.id() + "/"
                            + document.version() + "/" + chunk.index()).getBytes(StandardCharsets.UTF_8)));
                    upsert.setString(2, document.id());
                    upsert.setInt(3, document.version());
                    upsert.setInt(4, chunk.index());
                    upsert.setString(5, document.title());
                    upsert.setString(6, chunk.section());
                    upsert.setString(7, document.category());
                    upsert.setString(8, document.targetPosition());
                    upsert.setString(9, document.sourceType());
                    upsert.setString(10, document.sourceName());
                    upsert.setDate(11, Date.valueOf(LocalDate.parse(document.reviewedAt())));
                    upsert.setDate(12, Date.valueOf(LocalDate.parse(document.expiresAt())));
                    upsert.setString(13, chunk.content());
                    upsert.setString(14, embeddingModel);
                    upsert.setString(15, vectorLiteral(vectors.get(index)));
                    upsert.addBatch();
                }
                upsert.executeBatch();
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("知识文档索引写入失败: " + document.id(), exception);
        }
    }

    public List<KnowledgeHit> search(float[] queryVector, String targetPosition,
                                     String embeddingModel, int limit) {
        String sql = """
                SELECT document_id, title, section_name, source_name, content,
                       1 - (embedding <=> CAST(? AS vector)) AS score
                FROM ai_career_knowledge_chunk
                WHERE active = true AND expires_at >= CURRENT_DATE AND embedding_model = ?
                  AND (? IS NULL OR target_position IS NULL OR target_position = ?)
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """;
        String vector = vectorLiteral(queryVector);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement query = connection.prepareStatement(sql)) {
            query.setQueryTimeout(queryTimeoutSeconds);
            query.setString(1, vector);
            query.setString(2, embeddingModel);
            query.setString(3, targetPosition);
            query.setString(4, targetPosition);
            query.setString(5, vector);
            query.setInt(6, limit);
            List<KnowledgeHit> hits = new ArrayList<>();
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    hits.add(new KnowledgeHit(rows.getString("document_id"), rows.getString("title"),
                            rows.getString("section_name"), rows.getString("source_name"),
                            rows.getString("content"), rows.getDouble("score")));
                }
            }
            return hits;
        } catch (SQLException exception) {
            throw new IllegalStateException("PGVector 知识检索失败", exception);
        }
    }

    /** 目录中移除的文档立即停止召回；只影响首期专用知识表。 */
    public void deactivateMissingDocuments(List<String> activeDocumentIds) {
        if (activeDocumentIds.isEmpty()) {
            throw new IllegalArgumentException("不能用空知识目录停用全部文档");
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(activeDocumentIds.size(), "?"));
        String sql = "UPDATE " + TABLE + " SET active = false WHERE document_id NOT IN (" + placeholders + ")";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement update = connection.prepareStatement(sql)) {
            for (int index = 0; index < activeDocumentIds.size(); index++) {
                update.setString(index + 1, activeDocumentIds.get(index));
            }
            update.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("停用已移出目录的知识文档失败", exception);
        }
    }

    private String vectorLiteral(float[] vector) {
        if (vector.length != dimension) {
            throw new IllegalArgumentException("Embedding 维度与 PGVector 表不一致");
        }
        StringBuilder literal = new StringBuilder("[");
        for (int index = 0; index < vector.length; index++) {
            if (!Float.isFinite(vector[index])) {
                throw new IllegalArgumentException("Embedding 包含无效数值");
            }
            if (index > 0) {
                literal.append(',');
            }
            literal.append(vector[index]);
        }
        return literal.append(']').toString();
    }

    public record KnowledgeHit(String documentId, String title, String section,
                               String sourceName, String content, double score) {
    }
}
