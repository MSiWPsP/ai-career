package com.xucheng.aicareer.service;

import com.xucheng.aicareer.agent.career.CareerPlannerAgent;
import com.xucheng.aicareer.dto.CareerChatDTO;
import com.xucheng.aicareer.service.impl.CareerChatServiceImpl;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.CareerChatVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
}
