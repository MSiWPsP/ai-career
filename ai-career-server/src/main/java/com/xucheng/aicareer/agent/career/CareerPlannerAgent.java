package com.xucheng.aicareer.agent.career;

import com.xucheng.aicareer.agent.career.dto.CareerPlanResult;
import com.xucheng.aicareer.agent.career.dto.CareerTaskResult;
import com.xucheng.aicareer.agent.career.dto.RoadmapStage;
import com.xucheng.aicareer.exception.AiServiceException;
import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import com.xucheng.aicareer.vo.UserProfileVO;
import com.xucheng.aicareer.vo.UserSkillVO;
import com.xucheng.aicareer.vo.CareerPlanVO;
import com.xucheng.aicareer.vo.CareerTaskVO;
import com.xucheng.aicareer.vo.InterviewReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 职业规划领域 Agent，封装普通聊天和结构化职业规划两类模型调用。
 *
 * <p>该组件只消费业务 Service 提供的数据，不直接访问数据库。模型返回的结构化结果在离开
 * Agent 边界前会进行业务校验，避免无效数据进入持久化层。</p>
 */
@Slf4j
@Component
public class CareerPlannerAgent {

    private static final int MAX_GENERATION_ATTEMPTS = 2;
    private static final Set<String> TASK_TYPES = Set.of("KNOWLEDGE", "PROJECT", "INTERVIEW", "CAREER");

    private final ChatClient careerPlannerChatClient;
    private final ChatClient careerPlanGenerationChatClient;
    private final ObjectMapper objectMapper;

    public CareerPlannerAgent(
            @Qualifier("careerPlannerChatClient") ChatClient careerPlannerChatClient,
            @Qualifier("careerPlanGenerationChatClient") ChatClient careerPlanGenerationChatClient,
            ObjectMapper objectMapper) {
        this.careerPlannerChatClient = careerPlannerChatClient;
        this.careerPlanGenerationChatClient = careerPlanGenerationChatClient;
        this.objectMapper = objectMapper;
    }

    @Value("${spring.ai.openai.chat.model:unknown}")
    private String model;

    /**
     * 执行一次普通非流式职业咨询。
     *
     * @param userId 用于调用日志关联，不写入 Prompt
     * @param conversationId ChatMemory 的会话隔离标识
     * @param message 用户问题
     * @param businessContext 本次请求读取的最新业务快照
     * @return 去除首尾空白后的模型回复
     */
    public String chat(
            Long userId,
            String conversationId,
            String message,
            CareerChatBusinessContext businessContext) {
        long startTime = System.currentTimeMillis();
        try {
            String content = careerPlannerChatClient.prompt()
                    .system(system -> system.param("careerContext", serializeChatContext(businessContext)))
                    .user(message)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();
            if (!StringUtils.hasText(content)) {
                throw new AiServiceException("模型返回内容为空");
            }
            log.info("Agent调用成功 agent=CareerPlannerAgent conversationId={} userId={} model={} durationMs={}",
                    conversationId, userId, model, System.currentTimeMillis() - startTime);
            return content.trim();
        } catch (AiServiceException exception) {
            log.warn("Agent调用失败 agent=CareerPlannerAgent conversationId={} userId={} model={} durationMs={} reason={}",
                    conversationId, userId, model, System.currentTimeMillis() - startTime, exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            log.error("Agent调用失败 agent=CareerPlannerAgent conversationId={} userId={} model={} durationMs={}",
                    conversationId, userId, model, System.currentTimeMillis() - startTime, exception);
            throw new AiServiceException("调用职业规划模型失败", exception);
        }
    }

    /**
     * 执行一次普通流式职业咨询。
     *
     * <p>流中只暴露有效文本分片，空响应及供应商异常统一映射为 AiServiceException。</p>
     */
    public Flux<String> chatStream(
            Long userId,
            String conversationId,
            String message,
            CareerChatBusinessContext businessContext) {
        long startTime = System.currentTimeMillis();
        try {
            return careerPlannerChatClient.prompt()
                    .system(system -> system.param("careerContext", serializeChatContext(businessContext)))
                    .user(message)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .content()
                    .filter(StringUtils::hasLength)
                    .switchIfEmpty(Flux.error(new AiServiceException("模型返回内容为空")))
                    .doOnComplete(() -> log.info(
                            "Agent流式调用成功 agent=CareerPlannerAgent conversationId={} userId={} model={} durationMs={}",
                            conversationId, userId, model, System.currentTimeMillis() - startTime))
                    .onErrorMap(exception -> mapStreamException(
                            exception, userId, conversationId, System.currentTimeMillis() - startTime));
        } catch (Exception exception) {
            throw mapStreamException(exception, userId, conversationId, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 根据完整职业画像和技能快照生成结构化职业规划。
     *
     * <p>模型结果未通过本地结构校验时最多重试一次，避免偶发格式漂移直接导致请求失败。</p>
     */
    public CareerPlanResult generatePlan(
            Long userId, UserProfileVO profile, List<UserSkillVO> skills) {
        return callStructuredPlan(userId, buildGenerationPrompt(profile, skills));
    }

    /** 模型只消费业务层整理的旧规划、任务进度与报告，不读取数据库。 */
    public CareerPlanResult regeneratePlan(Long userId, UserProfileVO profile, List<UserSkillVO> skills,
                                           CareerPlanVO previousPlan, List<CareerTaskVO> previousTasks,
                                           InterviewReportVO report) {
        try {
            String prompt = "请依据指定面试报告生成下一版职业规划，保留有依据的旧目标，并调整尚未完成的成长任务。"
                    + "以下数据由平台业务服务读取：\n<user_data>\n"
                    + objectMapper.writeValueAsString(new ReplanningInput(profile, skills,
                            previousPlan, previousTasks, report)) + "\n</user_data>";
            return callStructuredPlan(userId, prompt);
        } catch (AiServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiServiceException("重新规划输入数据处理失败", exception);
        }
    }

    private CareerPlanResult callStructuredPlan(Long userId, String userPrompt) {
        long startTime = System.currentTimeMillis();
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            try {
                String prompt = attempt == 1
                        ? userPrompt
                        : userPrompt + "\n上一次结果未通过结构校验。请严格按照输出结构重新生成，所有必填字段均不得为空。";
                CareerPlanResult result = careerPlanGenerationChatClient.prompt()
                        .user(prompt)
                        .call()
                        .entity(CareerPlanResult.class);
                validateGeneratedPlan(result);
                log.info("Agent结构化调用成功 agent=CareerPlannerAgent userId={} model={} attempt={} durationMs={}",
                        userId, model, attempt, System.currentTimeMillis() - startTime);
                return result;
            } catch (Exception exception) {
                lastFailure = exception instanceof RuntimeException runtimeException
                        ? runtimeException : new RuntimeException(exception);
                log.warn("Agent结构化调用失败 agent=CareerPlannerAgent userId={} model={} attempt={} durationMs={} reason={}",
                        userId, model, attempt, System.currentTimeMillis() - startTime,
                        exception.getClass().getSimpleName());
            }
        }

        throw new AiServiceException("生成结构化职业规划失败", lastFailure);
    }

    private String buildGenerationPrompt(UserProfileVO profile, List<UserSkillVO> skills) {
        try {
            return "请生成第一版职业规划。以下数据由平台业务服务读取：\n<user_data>\n"
                    + objectMapper.writeValueAsString(new CareerPlanningInput(profile, skills))
                    + "\n</user_data>";
        } catch (Exception exception) {
            throw new AiServiceException("职业规划输入数据处理失败", exception);
        }
    }

    private String serializeChatContext(CareerChatBusinessContext businessContext) {
        try {
            return objectMapper.writeValueAsString(businessContext);
        } catch (Exception exception) {
            throw new AiServiceException("职业咨询上下文处理失败", exception);
        }
    }

    private void validateGeneratedPlan(CareerPlanResult result) {
        // 结构化映射成功不等于业务数据有效，持久化前仍需校验字段范围、数量和阶段顺序。
        require(result != null, "规划结果不能为空");
        require(hasTextWithin(result.getTargetPosition(), 100), "目标岗位无效");
        require(result.getMatchScore() != null
                && result.getMatchScore() >= 0
                && result.getMatchScore() <= 100, "匹配度无效");
        require(StringUtils.hasText(result.getSummary()), "规划摘要不能为空");
        require(validTextList(result.getAdvantages(), 2, 5), "优势列表无效");
        require(validTextList(result.getWeaknesses(), 2, 5), "短板列表无效");
        require(result.getRoadmap() != null
                && result.getRoadmap().size() >= 3
                && result.getRoadmap().size() <= 5, "成长路线阶段数量无效");

        Set<Integer> stageNumbers = new HashSet<>();
        for (int index = 0; index < result.getRoadmap().size(); index++) {
            RoadmapStage stage = result.getRoadmap().get(index);
            require(stage != null && Integer.valueOf(index + 1).equals(stage.getStage()), "阶段编号无效");
            require(stageNumbers.add(stage.getStage()), "阶段编号重复");
            require(hasTextWithin(stage.getName(), 100), "阶段名称无效");
            require(StringUtils.hasText(stage.getGoal()), "阶段目标不能为空");
            require(StringUtils.hasText(stage.getDuration()), "阶段周期不能为空");
            require(validTextList(stage.getTopics(), 2, 5), "阶段学习主题无效");
            require(stage.getTasks() != null
                    && stage.getTasks().size() >= 2
                    && stage.getTasks().size() <= 4, "阶段任务数量无效");
            stage.getTasks().forEach(this::validateTask);
        }
    }

    private void validateTask(CareerTaskResult task) {
        require(task != null, "任务不能为空");
        require(hasTextWithin(task.getTaskName(), 200), "任务名称无效");
        require(task.getDescription() == null || task.getDescription().length() <= 1000, "任务描述过长");
        require(TASK_TYPES.contains(task.getTaskType()), "任务类型无效");
        require(task.getPriority() != null && task.getPriority() >= 1 && task.getPriority() <= 3,
                "任务优先级无效");
    }

    private boolean validTextList(List<String> values, int minSize, int maxSize) {
        return values != null
                && values.size() >= minSize
                && values.size() <= maxSize
                && values.stream().allMatch(StringUtils::hasText);
    }

    private boolean hasTextWithin(String value, int maxLength) {
        return StringUtils.hasText(value) && value.length() <= maxLength;
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private record CareerPlanningInput(UserProfileVO profile, List<UserSkillVO> skills) {
    }

    private record ReplanningInput(UserProfileVO profile, List<UserSkillVO> skills,
                                   CareerPlanVO previousPlan, List<CareerTaskVO> previousTasks,
                                   InterviewReportVO interviewReport) {
    }

    private AiServiceException mapStreamException(
            Throwable exception, Long userId, String conversationId, long durationMs) {
        if (exception instanceof AiServiceException aiServiceException) {
            log.warn("Agent流式调用失败 agent=CareerPlannerAgent conversationId={} userId={} model={} durationMs={} reason={}",
                    conversationId, userId, model, durationMs, aiServiceException.getMessage());
            return aiServiceException;
        }
        log.error("Agent流式调用失败 agent=CareerPlannerAgent conversationId={} userId={} model={} durationMs={}",
                conversationId, userId, model, durationMs, exception);
        return new AiServiceException("调用职业规划模型失败", exception);
    }
}
