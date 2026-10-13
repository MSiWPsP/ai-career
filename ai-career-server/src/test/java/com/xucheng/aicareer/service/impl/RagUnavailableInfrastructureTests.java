package com.xucheng.aicareer.service.impl;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 使用真实 TCP 连接失败检验降级路径；不依赖本机服务停机或外部凭证。 */
class RagUnavailableInfrastructureTests {

    @Test
    void unavailableEmbeddingEndpointReturnsNoReferences() {
        KnowledgeEmbeddingClient unavailable = new KnowledgeEmbeddingClient(new ObjectMapper(),
                "http://127.0.0.1:1/v1", "test-only-key", "test-model", 2);
        PgKnowledgeRetrievalService retrieval = new PgKnowledgeRetrievalService(
                mock(PgKnowledgeRepository.class), unavailable, "test-model", 0.55);

        var result = retrieval.retrieve("Java 后端岗位能力有哪些？", null);

        assertThat(result.references()).isEmpty();
        assertThat(result.context()).isEmpty();
    }

    @Test
    void unavailablePostgresEndpointReturnsNoReferences() {
        PgKnowledgeRepository unavailable = new PgKnowledgeRepository(
                "jdbc:postgresql://127.0.0.1:1/unavailable", "test", "test", 2, 1, 1, 1);
        KnowledgeEmbeddingClient embedding = mock(KnowledgeEmbeddingClient.class);
        when(embedding.embed(org.mockito.ArgumentMatchers.anyString())).thenReturn(new float[]{0.1f, 0.2f});
        PgKnowledgeRetrievalService retrieval = new PgKnowledgeRetrievalService(
                unavailable, embedding, "test-model", 0.55);

        var result = retrieval.retrieve("Java 后端岗位能力有哪些？", null);

        assertThat(result.references()).isEmpty();
        assertThat(result.context()).isEmpty();
    }
}
