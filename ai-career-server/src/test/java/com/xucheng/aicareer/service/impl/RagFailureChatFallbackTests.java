package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.agent.career.CareerPlannerAgent;
import com.xucheng.aicareer.dto.CareerChatDTO;
import com.xucheng.aicareer.service.CareerChatContextService;
import com.xucheng.aicareer.service.CareerChatService;
import com.xucheng.aicareer.service.CareerConversationService;
import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import com.xucheng.aicareer.service.model.CareerChatTurnContext;
import com.xucheng.aicareer.vo.CareerChatStreamVO;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证 RAG 基础设施故障穿过完整聊天链路后仍安全降级，且不会产生来源卡片。 */
class RagFailureChatFallbackTests {

    private static final String CONVERSATION_ID =
            "career:10001:2c08d11b-88b9-4d63-8cc3-0a79d86e4695";
    private static final String CLIENT_MESSAGE_ID = "f4582584-602f-47b9-9567-6549bda65908";
    private static final String QUESTION = "Java 后端岗位需要哪些核心能力？";
    private static final CareerChatBusinessContext BUSINESS_CONTEXT =
            new CareerChatBusinessContext(null, List.of(), null);

    @Test
    void unavailableEmbeddingStillCompletesStreamWithoutReferences() {
        KnowledgeEmbeddingClient unavailable = new KnowledgeEmbeddingClient(new ObjectMapper(),
                "http://127.0.0.1:1/v1", "test-only-key", "test-model", 2);
        PgKnowledgeRetrievalService retrieval = new PgKnowledgeRetrievalService(
                mock(PgKnowledgeRepository.class), unavailable, "test-model", 0.55);
        Fixture fixture = fixture(retrieval);
        when(fixture.agent.chatStream(10001L, CONVERSATION_ID, QUESTION, BUSINESS_CONTEXT, ""))
                .thenReturn(Flux.just("可以先从 Java 基础和接口开发开始。"));

        List<CareerChatStreamVO> events = fixture.service.chatStream(request()).collectList().block();

        assertSafeFallback(events);
        verify(fixture.conversationService).completeTurn(fixture.turn,
                "可以先从 Java 基础和接口开发开始。");
    }

    @Test
    void unavailablePostgresStillCompletesStreamWithoutReferences() {
        PgKnowledgeRepository unavailable = new PgKnowledgeRepository(
                "jdbc:postgresql://127.0.0.1:1/unavailable", "test", "test", 2, 1, 1, 1);
        KnowledgeEmbeddingClient embedding = mock(KnowledgeEmbeddingClient.class);
        when(embedding.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        PgKnowledgeRetrievalService retrieval = new PgKnowledgeRetrievalService(
                unavailable, embedding, "test-model", 0.55);
        Fixture fixture = fixture(retrieval);
        when(fixture.agent.chatStream(10001L, CONVERSATION_ID, QUESTION, BUSINESS_CONTEXT, ""))
                .thenReturn(Flux.just("可以先从 Java 基础和接口开发开始。"));

        List<CareerChatStreamVO> events = fixture.service.chatStream(request()).collectList().block();

        assertSafeFallback(events);
    }

    @Test
    void postgresTimeoutStillCompletesStreamWithoutReferences() {
        PgKnowledgeRepository timedOut = mock(PgKnowledgeRepository.class);
        when(timedOut.search(any(), any(), anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new IllegalStateException("query timeout"));
        KnowledgeEmbeddingClient embedding = mock(KnowledgeEmbeddingClient.class);
        when(embedding.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        PgKnowledgeRetrievalService retrieval = new PgKnowledgeRetrievalService(
                timedOut, embedding, "test-model", 0.55);
        Fixture fixture = fixture(retrieval);
        when(fixture.agent.chatStream(10001L, CONVERSATION_ID, QUESTION, BUSINESS_CONTEXT, ""))
                .thenReturn(Flux.just("可以先从 Java 基础和接口开发开始。"));

        List<CareerChatStreamVO> events = fixture.service.chatStream(request()).collectList().block();

        assertSafeFallback(events);
    }

    private Fixture fixture(PgKnowledgeRetrievalService retrieval) {
        CareerPlannerAgent agent = mock(CareerPlannerAgent.class);
        CareerConversationService conversationService = mock(CareerConversationService.class);
        CareerChatContextService contextService = mock(CareerChatContextService.class);
        ChatMemory chatMemory = mock(ChatMemory.class);
        CareerChatTurnContext turn = new CareerChatTurnContext(
                20001L, 10001L, CONVERSATION_ID, CLIENT_MESSAGE_ID, null);
        when(conversationService.prepareTurn(CONVERSATION_ID, CLIENT_MESSAGE_ID, QUESTION)).thenReturn(turn);
        when(conversationService.getRecentMemory(10001L, CONVERSATION_ID, 20)).thenReturn(List.of());
        when(contextService.getCurrentContext()).thenReturn(BUSINESS_CONTEXT);
        CareerChatService service = new CareerChatServiceImpl(
                agent, conversationService, contextService, retrieval, chatMemory, 20);
        return new Fixture(agent, conversationService, service, turn);
    }

    private CareerChatDTO request() {
        CareerChatDTO request = new CareerChatDTO();
        request.setConversationId(CONVERSATION_ID);
        request.setClientMessageId(CLIENT_MESSAGE_ID);
        request.setMessage(QUESTION);
        return request;
    }

    private void assertSafeFallback(List<CareerChatStreamVO> events) {
        assertThat(events).isNotNull();
        assertThat(events).extracting(CareerChatStreamVO::getType)
                .containsExactly("phase", "phase", "delta", "done");
        assertThat(events.getFirst().getPhase()).isEqualTo("KNOWLEDGE_RETRIEVAL");
        assertThat(events.getLast().getRagApplied()).isFalse();
        assertThat(events.getLast().getReferences()).isEmpty();
        assertThat(events).noneMatch(event -> "error".equals(event.getType()));
    }

    private record Fixture(CareerPlannerAgent agent,
                           CareerConversationService conversationService,
                           CareerChatService service,
                           CareerChatTurnContext turn) {
    }
}
