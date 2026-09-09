package com.xucheng.aicareer.agent.career;

import com.xucheng.aicareer.exception.AiServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CareerPlannerAgentTests {

    @Test
    void chatReturnsTrimmedModelContent() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().user("我适合做Java后端吗？").call().content())
                .thenReturn("  建议先结合技能基础和目标时间进行评估。  ");
        CareerPlannerAgent agent = createAgent(chatClient);

        String content = agent.chat(10001L, "career:10001", "我适合做Java后端吗？");

        assertThat(content).isEqualTo("建议先结合技能基础和目标时间进行评估。");
    }

    @Test
    void chatConvertsModelFailureToAiServiceException() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().user("测试问题").call().content())
                .thenThrow(new IllegalStateException("provider failed"));
        CareerPlannerAgent agent = createAgent(chatClient);

        assertThatThrownBy(() -> agent.chat(10001L, "career:10001", "测试问题"))
                .isInstanceOf(AiServiceException.class)
                .hasMessage("调用职业规划模型失败");
    }

    @Test
    void chatRejectsBlankModelContent() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().user("测试问题").call().content()).thenReturn("  ");
        CareerPlannerAgent agent = createAgent(chatClient);

        assertThatThrownBy(() -> agent.chat(10001L, "career:10001", "测试问题"))
                .isInstanceOf(AiServiceException.class)
                .hasMessage("模型返回内容为空");
    }

    private CareerPlannerAgent createAgent(ChatClient chatClient) {
        CareerPlannerAgent agent = new CareerPlannerAgent(chatClient);
        ReflectionTestUtils.setField(agent, "model", "qwen3.7-plus");
        return agent;
    }
}
