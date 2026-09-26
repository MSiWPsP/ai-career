package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.agent.career.CareerPlannerAgent;
import com.xucheng.aicareer.agent.career.dto.GroundedCareerAnswer;
import com.xucheng.aicareer.agent.career.tool.CareerReadTools;
import com.xucheng.aicareer.agent.career.tool.CareerToolExecutionTrace;
import com.xucheng.aicareer.service.CareerToolQueryService;
import com.xucheng.aicareer.service.model.CareerAbilitySnapshot;
import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import com.xucheng.aicareer.service.model.CareerInterviewSnapshot;
import com.xucheng.aicareer.service.model.CareerTaskSnapshot;
import com.xucheng.aicareer.service.model.KnowledgeRetrievalResult;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 使用真实模型验证 OpenAI 兼容供应商与 Spring AI Tool Calling 的协议兼容性。 */
@SpringBootTest
class CareerToolCallingEvaluationTests {

    private final ChatClient careerPlannerChatClient;
    private final ChatClient careerPlanGenerationChatClient;
    private final ObjectMapper objectMapper;

    @Autowired
    CareerToolCallingEvaluationTests(
            @Qualifier("careerPlannerChatClient") ChatClient careerPlannerChatClient,
            @Qualifier("careerPlanGenerationChatClient") ChatClient careerPlanGenerationChatClient,
            ObjectMapper objectMapper) {
        this.careerPlannerChatClient = careerPlannerChatClient;
        this.careerPlanGenerationChatClient = careerPlanGenerationChatClient;
        this.objectMapper = objectMapper;
    }

    @Test
    void realModelCallsReadToolAndReturnsVerifiableEvidence() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getenv("AI_TOOL_CALLING_TEST")),
                "设置 AI_TOOL_CALLING_TEST=true 后运行真实模型 Tool Calling 评测");
        CareerToolQueryService stub = new CareerToolQueryService() {
            @Override
            public CareerTaskSnapshot getCurrentTaskSnapshot(Long userId) {
                assertThat(userId).isEqualTo(10001L);
                return new CareerTaskSnapshot(2, 0, 1, 1, 0,
                        List.of(new CareerTaskSnapshot.TaskItem(
                                "项目实践", "完成接口测试", 3, 1, LocalDate.of(2026, 10, 1))));
            }

            @Override
            public CareerAbilitySnapshot getCurrentAbilitySnapshot(Long userId) {
                return new CareerAbilitySnapshot(List.of());
            }

            @Override
            public CareerInterviewSnapshot getRecentInterviewSnapshot(Long userId) {
                return new CareerInterviewSnapshot(List.of());
            }
        };
        CareerPlannerAgent agent = new CareerPlannerAgent(careerPlannerChatClient,
                careerPlanGenerationChatClient, objectMapper, new CareerReadTools(stub));
        ReflectionTestUtils.setField(agent, "model", "real-provider-model");
        CareerToolExecutionTrace trace = new CareerToolExecutionTrace();

        GroundedCareerAnswer draft = agent.chatGroundedWithTools(
                10001L, "tool-eval:10001", "我还有哪些成长任务没有完成？请读取平台数据后回答。",
                new CareerChatBusinessContext(null, List.of(), null), "", trace);
        GroundedCareerAnswerComposer.Composed composed = GroundedCareerAnswerComposer.compose(
                draft, KnowledgeRetrievalResult.empty(), trace.snapshot());

        assertThat(trace.hasEvidence()).isTrue();
        assertThat(composed.accepted()).isTrue();
        assertThat(composed.content()).contains("完成接口测试", "本轮读取的平台数据");
        assertThat(composed.references()).isEmpty();
    }
}
