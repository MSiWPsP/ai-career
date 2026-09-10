package com.xucheng.aicareer.service;

import com.xucheng.aicareer.agent.career.CareerPlannerAgent;
import com.xucheng.aicareer.dto.CareerChatDTO;
import com.xucheng.aicareer.service.impl.CareerChatServiceImpl;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.CareerChatVO;
import com.xucheng.aicareer.vo.CareerChatStreamVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CareerChatServiceTests {

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void chatBuildsConversationIdFromCurrentUserAndDelegatesToAgent() {
        CareerPlannerAgent agent = mock(CareerPlannerAgent.class);
        CareerChatService service = new CareerChatServiceImpl(agent);
        CareerChatDTO request = new CareerChatDTO();
        request.setMessage("  半年后找实习应该怎么准备？  ");
        UserContext.setUserId(10001L);
        when(agent.chat(10001L, "career:10001", "半年后找实习应该怎么准备？"))
                .thenReturn("建议分三个阶段准备。");

        CareerChatVO response = service.chat(request);

        assertThat(response.getConversationId()).isEqualTo("career:10001");
        assertThat(response.getContent()).isEqualTo("建议分三个阶段准备。");
        verify(agent).chat(10001L, "career:10001", "半年后找实习应该怎么准备？");
    }

    @Test
    void chatStreamMapsDeltasAndCompletionEvent() {
        CareerPlannerAgent agent = mock(CareerPlannerAgent.class);
        CareerChatService service = new CareerChatServiceImpl(agent);
        CareerChatDTO request = new CareerChatDTO();
        request.setMessage("  Redis应该怎么学？  ");
        UserContext.setUserId(10001L);
        when(agent.chatStream(10001L, "career:10001", "Redis应该怎么学？"))
                .thenReturn(Flux.just("先掌握数据结构。", "再练习缓存场景。"));

        List<CareerChatStreamVO> events = service.chatStream(request).collectList().block();

        assertThat(events).extracting(CareerChatStreamVO::getType)
                .containsExactly("delta", "delta", "done");
        assertThat(events).extracting(CareerChatStreamVO::getConversationId)
                .containsOnly("career:10001");
        assertThat(events).extracting(CareerChatStreamVO::getContent)
                .containsExactly("先掌握数据结构。", "再练习缓存场景。", null);
    }

    @Test
    void chatStreamConvertsFailureToSafeErrorEvent() {
        CareerPlannerAgent agent = mock(CareerPlannerAgent.class);
        CareerChatService service = new CareerChatServiceImpl(agent);
        CareerChatDTO request = new CareerChatDTO();
        request.setMessage("测试问题");
        UserContext.setUserId(10001L);
        when(agent.chatStream(10001L, "career:10001", "测试问题"))
                .thenReturn(Flux.error(new IllegalStateException("provider failed")));

        List<CareerChatStreamVO> events = service.chatStream(request).collectList().block();

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.getType()).isEqualTo("error");
            assertThat(event.getConversationId()).isEqualTo("career:10001");
            assertThat(event.getContent()).isEqualTo("AI服务暂时不可用，请稍后重试");
        });
    }
}
