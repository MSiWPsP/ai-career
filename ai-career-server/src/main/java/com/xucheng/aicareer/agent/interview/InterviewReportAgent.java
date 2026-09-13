package com.xucheng.aicareer.agent.interview;

import com.xucheng.aicareer.agent.interview.dto.InterviewReportResult;
import com.xucheng.aicareer.exception.AiServiceException;
import com.xucheng.aicareer.vo.InterviewSuggestionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 根据业务服务提供的完整面试快照生成结构化报告，不直接访问数据库。 */
@Slf4j
@Component
public class InterviewReportAgent {

    private static final int MAX_ATTEMPTS = 2;
    private static final Set<String> PRIORITIES = Set.of("HIGH", "MEDIUM", "LOW");

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.chat.model:unknown}")
    private String model;

    public InterviewReportAgent(
            @Qualifier("interviewReportChatClient") ChatClient chatClient,
            ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    /** 校验模型返回的分数、能力名称和建议，避免把结构错误的结果写入报告表。 */
    public InterviewReportResult generate(Long userId, Object businessSnapshot) {
        String input;
        try {
            input = objectMapper.writeValueAsString(businessSnapshot);
        } catch (Exception exception) {
            throw new AiServiceException("面试报告输入处理失败", exception);
        }
        RuntimeException lastFailure = null;
        long started = System.currentTimeMillis();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String retryTip = attempt == 1 ? "" : "\n上次输出未通过校验，请严格遵循字段约束重新生成。";
                InterviewReportResult result = chatClient.prompt()
                        .user("请生成本次模拟面试报告。业务快照：\n<interview_data>\n"
                                + input + "\n</interview_data>" + retryTip)
                        .call()
                        .entity(InterviewReportResult.class);
                validate(result);
                log.info("Agent调用成功 agent=InterviewReportAgent userId={} model={} attempt={} durationMs={}",
                        userId, model, attempt, System.currentTimeMillis() - started);
                return result;
            } catch (Exception exception) {
                lastFailure = exception instanceof RuntimeException runtime ? runtime : new RuntimeException(exception);
                log.warn("Agent调用失败 agent=InterviewReportAgent userId={} model={} attempt={} reason={}",
                        userId, model, attempt, exception.getClass().getSimpleName());
            }
        }
        throw new AiServiceException("生成模拟面试报告失败", lastFailure);
    }

    private void validate(InterviewReportResult result) {
        require(result != null, "报告不能为空");
        require(validScore(result.getTotalScore()), "综合评分无效");
        require(result.getScores() != null && !result.getScores().isEmpty()
                && result.getScores().size() <= 12, "分项评分数量无效");
        Set<String> normalizedNames = new HashSet<>();
        result.getScores().forEach((name, score) -> {
            require(hasTextWithin(name, 100) && validScore(score), "分项评分无效");
            require(normalizedNames.add(name.trim().toLowerCase()), "分项能力名称重复");
        });
        require(validTextList(result.getAdvantages(), 5), "优势分析无效");
        require(validTextList(result.getWeaknesses(), 5), "薄弱项分析无效");
        require(result.getSuggestions() != null && result.getSuggestions().size() <= 6,
                "改进建议数量无效");
        for (InterviewSuggestionVO suggestion : result.getSuggestions()) {
            require(suggestion != null
                    && hasTextWithin(suggestion.getTopic(), 100)
                    && PRIORITIES.contains(suggestion.getPriority())
                    && hasTextWithin(suggestion.getContent(), 1000), "改进建议无效");
        }
        require(hasTextWithin(result.getSummary(), 2000), "报告总结无效");
    }

    private boolean validScore(Integer score) {
        return score != null && score >= 0 && score <= 100;
    }

    private boolean validTextList(List<String> values, int maxCount) {
        return values != null && values.size() <= maxCount
                && values.stream().allMatch(value -> hasTextWithin(value, 500));
    }

    private boolean hasTextWithin(String value, int maxLength) {
        return StringUtils.hasText(value) && value.length() <= maxLength;
    }

    private void require(boolean valid, String reason) {
        if (!valid) throw new IllegalArgumentException(reason);
    }
}
