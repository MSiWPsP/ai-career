package com.xucheng.aicareer.service;

import com.xucheng.aicareer.agent.career.CareerPlannerAgent;
import com.xucheng.aicareer.dto.CareerChatDTO;
import com.xucheng.aicareer.service.impl.CareerChatServiceImpl;
import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import com.xucheng.aicareer.service.model.CareerChatMemoryEntry;
import com.xucheng.aicareer.service.model.CareerChatTurnContext;
import com.xucheng.aicareer.service.model.KnowledgeReference;
import com.xucheng.aicareer.service.model.KnowledgeRetrievalResult;
import com.xucheng.aicareer.vo.CareerChatVO;
import com.xucheng.aicareer.vo.CareerChatStreamVO;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CareerChatServiceTests {

    private static final String CONVERSATION_ID =
            "career:10001:2c08d11b-88b9-4d63-8cc3-0a79d86e4695";
    private static final String CLIENT_MESSAGE_ID = "f4582584-602f-47b9-9567-6549bda65908";
    private static final CareerChatBusinessContext BUSINESS_CONTEXT = new CareerChatBusinessContext(
            new CareerChatBusinessContext.Profile(
                    "本科", "软件工程", "大三", 2027, "INTERNSHIP",
                    "Java后端开发工程师", "杭州", "半年内", null,
                    "找到后端实习", null),
            List.of(new CareerChatBusinessContext.Skill("Java", "PROGRAMMING", 3, 60)),
            null);

    @Test
    void chatUsesPersistedConversationAndCompletesTurn() {
        CareerPlannerAgent agent = mock(CareerPlannerAgent.class);
        CareerConversationService conversationService = mock(CareerConversationService.class);
        CareerChatContextService contextService = mock(CareerChatContextService.class);
        ChatMemory chatMemory = mock(ChatMemory.class);
        CareerChatService service = new CareerChatServiceImpl(
                agent, conversationService, contextService, noKnowledge(), chatMemory, 20);
        CareerChatDTO request = request("  半年后找实习应该怎么准备？  ");
        CareerChatTurnContext turn = turn(null);
        when(conversationService.prepareTurn(CONVERSATION_ID, CLIENT_MESSAGE_ID, "半年后找实习应该怎么准备？"))
                .thenReturn(turn);
        when(conversationService.getRecentMemory(10001L, CONVERSATION_ID, 20))
                .thenReturn(List.of(new CareerChatMemoryEntry("user", "我想做Java后端")));
        when(contextService.getCurrentContext()).thenReturn(BUSINESS_CONTEXT);
        when(agent.chat(10001L, CONVERSATION_ID, "半年后找实习应该怎么准备？", BUSINESS_CONTEXT, ""))
                .thenReturn("建议分三个阶段准备。");

        CareerChatVO response = service.chat(request);

        assertThat(response.getConversationId()).isEqualTo(CONVERSATION_ID);
        assertThat(response.getClientMessageId()).isEqualTo(CLIENT_MESSAGE_ID);
        assertThat(response.getContent()).isEqualTo("建议分三个阶段准备。");
        verify(conversationService).completeTurn(turn, "建议分三个阶段准备。");
        verify(chatMemory).clear(CONVERSATION_ID);
        verify(contextService).getCurrentContext();
    }

    @Test
    void completedDuplicateRequestReplaysStoredAnswerWithoutCallingAgent() {
        CareerPlannerAgent agent = mock(CareerPlannerAgent.class);
        CareerConversationService conversationService = mock(CareerConversationService.class);
        CareerChatContextService contextService = mock(CareerChatContextService.class);
        ChatMemory chatMemory = mock(ChatMemory.class);
        CareerChatService service = new CareerChatServiceImpl(
                agent, conversationService, contextService, noKnowledge(), chatMemory, 20);
        CareerChatDTO request = request("Redis应该怎么学？");
        when(conversationService.prepareTurn(CONVERSATION_ID, CLIENT_MESSAGE_ID, "Redis应该怎么学？"))
                .thenReturn(turn("先掌握数据结构。"));

        CareerChatVO response = service.chat(request);

        assertThat(response.getContent()).isEqualTo("先掌握数据结构。");
        verify(agent, never()).chat(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(contextService, never()).getCurrentContext();
    }

    @Test
    void chatStreamPersistsCompleteAnswerAndEmitsRequestIdentity() {
        CareerPlannerAgent agent = mock(CareerPlannerAgent.class);
        CareerConversationService conversationService = mock(CareerConversationService.class);
        CareerChatContextService contextService = mock(CareerChatContextService.class);
        ChatMemory chatMemory = mock(ChatMemory.class);
        CareerChatService service = new CareerChatServiceImpl(
                agent, conversationService, contextService, noKnowledge(), chatMemory, 20);
        CareerChatDTO request = request("Redis应该怎么学？");
        CareerChatTurnContext turn = turn(null);
        when(conversationService.prepareTurn(CONVERSATION_ID, CLIENT_MESSAGE_ID, "Redis应该怎么学？"))
                .thenReturn(turn);
        when(conversationService.getRecentMemory(10001L, CONVERSATION_ID, 20)).thenReturn(List.of());
        when(contextService.getCurrentContext()).thenReturn(BUSINESS_CONTEXT);
        when(agent.chatStream(10001L, CONVERSATION_ID, "Redis应该怎么学？", BUSINESS_CONTEXT, ""))
                .thenReturn(Flux.just("先掌握数据结构。", "再练习缓存场景。"));

        List<CareerChatStreamVO> events = service.chatStream(request).collectList().block();

        assertThat(events).extracting(CareerChatStreamVO::getType)
                .containsExactly("delta", "delta", "done");
        assertThat(events).extracting(CareerChatStreamVO::getConversationId).containsOnly(CONVERSATION_ID);
        assertThat(events).extracting(CareerChatStreamVO::getClientMessageId).containsOnly(CLIENT_MESSAGE_ID);
        verify(conversationService).completeTurn(turn, "先掌握数据结构。再练习缓存场景。");
    }

    @Test
    void chatStreamMarksFailedTurnAndReturnsSafeErrorEvent() {
        CareerPlannerAgent agent = mock(CareerPlannerAgent.class);
        CareerConversationService conversationService = mock(CareerConversationService.class);
        CareerChatContextService contextService = mock(CareerChatContextService.class);
        ChatMemory chatMemory = mock(ChatMemory.class);
        CareerChatService service = new CareerChatServiceImpl(
                agent, conversationService, contextService, noKnowledge(), chatMemory, 20);
        CareerChatDTO request = request("测试问题");
        CareerChatTurnContext turn = turn(null);
        when(conversationService.prepareTurn(CONVERSATION_ID, CLIENT_MESSAGE_ID, "测试问题"))
                .thenReturn(turn);
        when(conversationService.getRecentMemory(10001L, CONVERSATION_ID, 20)).thenReturn(List.of());
        when(contextService.getCurrentContext()).thenReturn(BUSINESS_CONTEXT);
        when(agent.chatStream(10001L, CONVERSATION_ID, "测试问题", BUSINESS_CONTEXT, ""))
                .thenReturn(Flux.error(new IllegalStateException("provider failed")));

        List<CareerChatStreamVO> events = service.chatStream(request).collectList().block();

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.getType()).isEqualTo("error");
            assertThat(event.getConversationId()).isEqualTo(CONVERSATION_ID);
            assertThat(event.getClientMessageId()).isEqualTo(CLIENT_MESSAGE_ID);
            assertThat(event.getContent()).isEqualTo("AI服务暂时不可用，请稍后重试");
        });
        verify(conversationService).failTurn(turn);
    }

    @Test
    void chatAppendsOnlyRetrievedSourcesAndPersistsSameAnswer() {
        CareerPlannerAgent agent = mock(CareerPlannerAgent.class);
        CareerConversationService conversationService = mock(CareerConversationService.class);
        CareerChatContextService contextService = mock(CareerChatContextService.class);
        ChatMemory chatMemory = mock(ChatMemory.class);
        KnowledgeRetrievalResult knowledge = new KnowledgeRetrievalResult("岗位能力片段",
                List.of(new KnowledgeReference("java-backend-capabilities", "Java 后端岗位能力框架",
                        "工程与交付", "AI职途项目知识库")));
        CareerChatService service = new CareerChatServiceImpl(agent, conversationService, contextService,
                (message, context) -> knowledge, chatMemory, 20);
        CareerChatDTO request = request("后端需要哪些能力？");
        CareerChatTurnContext turn = turn(null);
        when(conversationService.prepareTurn(CONVERSATION_ID, CLIENT_MESSAGE_ID, "后端需要哪些能力？"))
                .thenReturn(turn);
        when(conversationService.getRecentMemory(10001L, CONVERSATION_ID, 20)).thenReturn(List.of());
        when(contextService.getCurrentContext()).thenReturn(BUSINESS_CONTEXT);
        when(agent.chat(10001L, CONVERSATION_ID, "后端需要哪些能力？", BUSINESS_CONTEXT, "岗位能力片段"))
                .thenReturn("先把服务开发练扎实。");

        CareerChatVO answer = service.chat(request);

        assertThat(answer.getContent()).contains("参考依据", "Java 后端岗位能力框架", "工程与交付");
        verify(conversationService).completeTurn(turn, answer.getContent());
    }

    private KnowledgeRetrievalService noKnowledge() {
        return (message, context) -> KnowledgeRetrievalResult.empty();
    }

    private CareerChatDTO request(String message) {
        CareerChatDTO request = new CareerChatDTO();
        request.setConversationId(CONVERSATION_ID);
        request.setClientMessageId(CLIENT_MESSAGE_ID);
        request.setMessage(message);
        return request;
    }

    private CareerChatTurnContext turn(String replayContent) {
        return new CareerChatTurnContext(20001L, 10001L, CONVERSATION_ID, CLIENT_MESSAGE_ID, replayContent);
    }
}
