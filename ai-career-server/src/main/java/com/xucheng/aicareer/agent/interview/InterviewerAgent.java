package com.xucheng.aicareer.agent.interview;

import com.xucheng.aicareer.agent.interview.dto.InterviewTurnResult;
import com.xucheng.aicareer.exception.AiServiceException;
import com.xucheng.aicareer.service.model.InterviewBusinessContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

/**
 * AI 模拟面试官，负责生成问题、分析回答并决定下一轮动作。
 *
 * <p>该组件不访问数据库，也不修改面试状态；所有业务事实均由 InterviewService 以快照形式传入。</p>
 */
@Slf4j
@Component
public class InterviewerAgent {

    private static final int MAX_ATTEMPTS = 2;
    private static final Set<String> ACTIONS = Set.of(
            "FOLLOW_UP", "NEXT_TOPIC", "INCREASE_DIFFICULTY", "DECREASE_DIFFICULTY", "FINISH");
    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    private final ChatClient interviewerChatClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.chat.model:unknown}")
    private String model;

    public InterviewerAgent(
            @Qualifier("interviewerChatClient") ChatClient interviewerChatClient,
            ObjectMapper objectMapper) {
        this.interviewerChatClient = interviewerChatClient;
        this.objectMapper = objectMapper;
    }

    /** 根据岗位、画像与技能快照生成第一道题。 */
    public InterviewTurnResult start(
            Long userId, String conversationId, InterviewBusinessContext context) {
        return call(userId, conversationId, context,
                "请开始本次模拟面试。直接提出第一道主要问题，不要寒暄，不要给出答案。"
                        + "这是初始化轮次，score 可以为 0，evaluation 写为‘初始化问题’，finished 必须为 false。");
    }

    /**
     * 分析候选人的当前回答并生成追问、下一主题或结束语。
     *
     * @param forceFinish 已达到题目上限时为 true，Agent 必须停止继续提问
     */
    public InterviewTurnResult answer(
            Long userId,
            String conversationId,
            String answer,
            InterviewBusinessContext context,
            boolean forceFinish) {
        String instruction = forceFinish
                ? "已达到问题数量上限。请评价本轮回答后，用专业简短的结束语结束面试，"
                + "nextAction 必须为 FINISH，finished 必须为 true，不得继续提问。"
                : "请分析候选人的回答并决定动态追问、切换知识点、调整难度或结束面试。";
        return call(userId, conversationId, context,
                "候选人对上一题的回答如下：\n<answer>\n" + answer + "\n</answer>\n" + instruction);
    }

    private InterviewTurnResult call(
            Long userId, String conversationId, InterviewBusinessContext context, String userPrompt) {
        long startTime = System.currentTimeMillis();
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String prompt = attempt == 1
                        ? userPrompt
                        : userPrompt + "\n上一次输出未通过结构校验，请严格按字段约束重新生成。";
                InterviewTurnResult result = interviewerChatClient.prompt()
                        .system(system -> system.param("interviewContext", serializeContext(context)))
                        .user(prompt)
                        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .call()
                        .entity(InterviewTurnResult.class);
                validate(result);
                log.info("Agent调用成功 agent=InterviewerAgent conversationId={} userId={} model={} attempt={} durationMs={}",
                        conversationId, userId, model, attempt, System.currentTimeMillis() - startTime);
                return result;
            } catch (Exception exception) {
                lastFailure = exception instanceof RuntimeException runtimeException
                        ? runtimeException : new RuntimeException(exception);
                log.warn("Agent调用失败 agent=InterviewerAgent conversationId={} userId={} model={} attempt={} durationMs={} reason={}",
                        conversationId, userId, model, attempt, System.currentTimeMillis() - startTime,
                        exception.getClass().getSimpleName());
            }
        }
        throw new AiServiceException("生成模拟面试问题失败", lastFailure);
    }

    private String serializeContext(InterviewBusinessContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (Exception exception) {
            throw new AiServiceException("模拟面试上下文处理失败", exception);
        }
    }

    private void validate(InterviewTurnResult result) {
        require(result != null, "面试轮次结果不能为空");
        require(hasTextWithin(result.getResponse(), 3000), "面试官回复无效");
        require(hasTextWithin(result.getTopic(), 100), "问题分类无效");
        require(result.getScore() != null && result.getScore() >= 0 && result.getScore() <= 100,
                "回答评分无效");
        require(StringUtils.hasText(result.getEvaluation()), "回答评价不能为空");
        require(ACTIONS.contains(result.getNextAction()), "下一步动作无效");
        require(DIFFICULTIES.contains(result.getNextDifficulty()), "下一题难度无效");
        require(result.getFinished() != null, "结束标记不能为空");
        require(Boolean.TRUE.equals(result.getFinished()) == "FINISH".equals(result.getNextAction()),
                "结束动作与结束标记不一致");
    }

    private boolean hasTextWithin(String value, int maxLength) {
        return StringUtils.hasText(value) && value.length() <= maxLength;
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
