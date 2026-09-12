package com.xucheng.aicareer.agent.interview;

import com.xucheng.aicareer.agent.interview.dto.InterviewTurnResult;
import com.xucheng.aicareer.exception.AiServiceException;
import com.xucheng.aicareer.service.model.InterviewBusinessContext;
import com.xucheng.aicareer.vo.UserProfileVO;
import com.xucheng.aicareer.vo.UserSkillVO;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewerAgentTests {

    @Test
    void startReturnsStructuredQuestionAndInjectsBusinessContext() {
        RecordingChatModel model = new RecordingChatModel(validTurn(false, "NEXT_TOPIC"));
        InterviewerAgent agent = createAgent(model);

        InterviewTurnResult result = agent.start(10001L, "interview:30001", context(0, 5));

        assertThat(result.getResponse()).contains("Spring Boot");
        assertThat(result.getFinished()).isFalse();
        assertThat(messageTexts(model.prompts.getFirst()))
                .anyMatch(text -> text.contains("Java后端开发工程师")
                        && text.contains("软件工程")
                        && text.contains("\"skillName\":\"Java\""));
    }

    @Test
    void answerForcesStructuredFinishAtQuestionLimit() {
        RecordingChatModel model = new RecordingChatModel(validTurn(true, "FINISH"));
        InterviewerAgent agent = createAgent(model);

        InterviewTurnResult result = agent.answer(
                10001L, "interview:30001", "我会先定位慢查询。", context(5, 5), true);

        assertThat(result.getFinished()).isTrue();
        assertThat(result.getNextAction()).isEqualTo("FINISH");
        assertThat(messageTexts(model.prompts.getFirst()))
                .anyMatch(text -> text.contains("已达到问题数量上限") && text.contains("定位慢查询"));
    }

    @Test
    void invalidStructuredResultsAreRetriedThenMappedToAiServiceException() {
        RecordingChatModel model = new RecordingChatModel("{}", "{}");
        InterviewerAgent agent = createAgent(model);

        assertThatThrownBy(() -> agent.start(10001L, "interview:30001", context(0, 5)))
                .isInstanceOf(AiServiceException.class)
                .hasMessage("生成模拟面试问题失败");
        assertThat(model.prompts).hasSize(2);
    }

    private InterviewerAgent createAgent(RecordingChatModel model) {
        InterviewerAgent agent = new InterviewerAgent(ChatClient.builder(model)
                .defaultSystem("面试规则\n<interview_context>\n{interviewContext}\n</interview_context>")
                .build(), new ObjectMapper());
        ReflectionTestUtils.setField(agent, "model", "qwen3.7-plus");
        return agent;
    }

    private InterviewBusinessContext context(int questionCount, int maxQuestions) {
        return new InterviewBusinessContext(
                "Java后端开发工程师",
                "TECHNICAL",
                "MEDIUM",
                questionCount,
                maxQuestions,
                UserProfileVO.builder().major("软件工程").targetPosition("Java后端开发工程师").build(),
                List.of(UserSkillVO.builder().skillName("Java").level(3).score(60).build()));
    }

    private String validTurn(boolean finished, String action) {
        return """
                {
                  "response": "%s",
                  "topic": "Spring Boot",
                  "score": 78,
                  "evaluation": "回答有条理",
                  "nextAction": "%s",
                  "nextDifficulty": "MEDIUM",
                  "finished": %s
                }
                """.formatted(finished ? "本次面试到这里，感谢你的回答。" : "请介绍一下Spring Boot自动配置原理。",
                action, finished);
    }

    private List<String> messageTexts(Prompt prompt) {
        return prompt.getInstructions().stream().map(Message::getText).toList();
    }

    private static final class RecordingChatModel implements org.springframework.ai.chat.model.ChatModel {

        private final Deque<String> responses;
        private final List<Prompt> prompts = new ArrayList<>();

        private RecordingChatModel(String... responses) {
            this.responses = new ArrayDeque<>(List.of(responses));
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(responses.removeFirst()))));
        }
    }
}
