package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PgKnowledgeRetrievalServiceTests {

    private final PgKnowledgeRepository repository = mock(PgKnowledgeRepository.class);
    private final KnowledgeEmbeddingClient embeddingClient = mock(KnowledgeEmbeddingClient.class);
    private final PgKnowledgeRetrievalService service = new PgKnowledgeRetrievalService(
            repository, embeddingClient, "text-embedding-v4", 0.55);
    private final CareerChatBusinessContext context = new CareerChatBusinessContext(null, List.of(), null);

    @Test
    void skipsGreetingsAndLocalOnlyRequests() {
        assertThat(service.retrieve("你好", context).hasKnowledge()).isFalse();
        assertThat(service.retrieve("我还有多少未完成任务？", context).hasKnowledge()).isFalse();
        verify(embeddingClient, never()).embed(anyString());
    }

    @Test
    void embeddingFailureFallsBackWithoutBreakingChat() {
        when(embeddingClient.embed(anyString())).thenThrow(new IllegalStateException("provider failed"));

        assertThat(service.retrieve("Java 后端需要什么技能？", context).hasKnowledge()).isFalse();
    }

    @Test
    void removesContactDetailsFromEmbeddingQuery() {
        String safe = service.sanitizeQuery("我叫张三，邮箱是 test@example.com，电话 13812345678，想学 Java 后端");

        assertThat(safe).contains("Java 后端", "[姓名]", "[邮箱]", "[电话]")
                .doesNotContain("张三", "test@example.com", "13812345678");
    }
}
