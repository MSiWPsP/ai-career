package com.xucheng.aicareer.agent.career.tool;

import com.xucheng.aicareer.service.CareerToolQueryService;
import com.xucheng.aicareer.service.model.CareerAbilitySnapshot;
import com.xucheng.aicareer.service.model.CareerInterviewSnapshot;
import com.xucheng.aicareer.service.model.CareerTaskSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 职业规划师可按需调用的只读业务工具，不提供任何数据库写操作。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CareerReadTools {

    public static final String USER_ID_CONTEXT_KEY = "careerUserId";
    public static final String TRACE_CONTEXT_KEY = "careerToolTrace";
    public static final String TASK_TOOL = "get_current_career_tasks";
    public static final String ABILITY_TOOL = "get_current_ability_snapshot";
    public static final String INTERVIEW_TOOL = "get_recent_interview_summary";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);

    private final CareerToolQueryService queryService;

    @Tool(name = TASK_TOOL,
            description = "当用户询问自己的当前成长任务、任务进度、待完成事项、截止日期或完成率时调用。只读取当前登录用户的数据。")
    public CareerToolResult getCurrentCareerTasks(ToolContext context) {
        return execute(TASK_TOOL, context, userId -> taskFacts(queryService.getCurrentTaskSnapshot(userId)));
    }

    @Tool(name = ABILITY_TOOL,
            description = "当用户询问自己的能力评分、能力雷达、能力变化或最近能力趋势时调用。只读取当前登录用户的数据。")
    public CareerToolResult getCurrentAbilitySnapshot(ToolContext context) {
        return execute(ABILITY_TOOL, context, userId -> abilityFacts(queryService.getCurrentAbilitySnapshot(userId)));
    }

    @Tool(name = INTERVIEW_TOOL,
            description = "当用户询问自己最近的模拟面试、面试得分、面试报告或薄弱项时调用。最多读取当前登录用户最近三次已结束面试。")
    public CareerToolResult getRecentInterviewSummary(ToolContext context) {
        return execute(INTERVIEW_TOOL, context,
                userId -> interviewFacts(queryService.getRecentInterviewSnapshot(userId)));
    }

    private CareerToolResult execute(String toolName, ToolContext context, ToolQuery query) {
        Long userId = requireUserId(context);
        CareerToolExecutionTrace trace = requireTrace(context);
        long started = System.nanoTime();
        try {
            List<String> facts = query.read(userId);
            List<CareerToolEvidence> evidence = trace.record(toolName, facts);
            log.info("职业数据Tool调用完成 tool={} userId={} evidenceCount={} durationMs={}",
                    toolName, userId, evidence.size(), elapsedMillis(started));
            return new CareerToolResult(toolName, true, "已读取本轮平台数据", evidence);
        } catch (RuntimeException exception) {
            log.warn("职业数据Tool调用降级 tool={} userId={} reason={} durationMs={}",
                    toolName, userId, exception.getClass().getSimpleName(), elapsedMillis(started));
            return new CareerToolResult(toolName, false, "平台数据暂时不可用", List.of());
        }
    }

    private List<String> taskFacts(CareerTaskSnapshot snapshot) {
        List<String> facts = new ArrayList<>();
        facts.add("当前职业规划共有%d项成长任务：待开始%d项、进行中%d项、已完成%d项，完成率%d%%。"
                .formatted(snapshot.total(), snapshot.waiting(), snapshot.processing(), snapshot.completed(),
                        snapshot.completionRate()));
        snapshot.tasks().forEach(task -> {
            String deadline = task.deadline() == null ? "未设置" : DATE.format(task.deadline());
            facts.add("任务“%s”位于“%s”，状态为%s，优先级为%s，截止日期为%s。"
                    .formatted(safe(task.taskName()), safe(task.stageName()), taskStatus(task.status()),
                            priority(task.priority()), deadline));
        });
        return facts;
    }

    private List<String> abilityFacts(CareerAbilitySnapshot snapshot) {
        if (snapshot.abilities().isEmpty()) {
            return List.of("当前没有可用的能力评分记录。");
        }
        return snapshot.abilities().stream().map(ability -> {
            List<CareerAbilitySnapshot.ScorePoint> scores = ability.recentScores();
            if (scores.size() < 2) {
                return "能力“%s”当前评分为%d分，暂时只有一条评分记录。"
                        .formatted(safe(ability.abilityName()), ability.currentScore());
            }
            int previous = scores.get(1).score();
            int change = ability.currentScore() - previous;
            String direction = change > 0 ? "上升" : change < 0 ? "下降" : "持平";
            return "能力“%s”当前评分为%d分，上一条记录为%d分，变化为%s%d分。"
                    .formatted(safe(ability.abilityName()), ability.currentScore(), previous,
                            direction, Math.abs(change));
        }).toList();
    }

    private List<String> interviewFacts(CareerInterviewSnapshot snapshot) {
        if (snapshot.interviews().isEmpty()) {
            return List.of("当前没有已结束的模拟面试记录。");
        }
        return snapshot.interviews().stream().map(interview -> {
            String date = interview.occurredAt() == null ? "日期未知" : DATE.format(interview.occurredAt());
            String score = interview.totalScore() == null ? "尚无报告得分" : "报告总分%d分".formatted(interview.totalScore());
            String weaknesses = interview.weaknesses().isEmpty()
                    ? "报告未列出薄弱项" : "薄弱项包括" + interview.weaknesses().stream()
                    .map(this::safe).collect(java.util.stream.Collectors.joining("、"));
            String summary = StringUtils.hasText(interview.summary())
                    ? "；报告摘要：" + shorten(normalize(interview.summary()), 180) : "";
            return "%s完成的“%s”%s模拟面试，%s；%s%s。"
                    .formatted(date, safe(interview.targetPosition()), safe(interview.interviewType()),
                            score, weaknesses, summary);
        }).toList();
    }

    private Long requireUserId(ToolContext context) {
        Object value = context.getContext().get(USER_ID_CONTEXT_KEY);
        if (value instanceof Long userId) {
            return userId;
        }
        throw new IllegalStateException("Tool 用户上下文缺失");
    }

    private CareerToolExecutionTrace requireTrace(ToolContext context) {
        Object value = context.getContext().get(TRACE_CONTEXT_KEY);
        if (value instanceof CareerToolExecutionTrace trace) {
            return trace;
        }
        throw new IllegalStateException("Tool 证据上下文缺失");
    }

    private String taskStatus(Integer status) {
        if (Integer.valueOf(0).equals(status)) {
            return "待开始";
        }
        if (Integer.valueOf(1).equals(status)) {
            return "进行中";
        }
        if (Integer.valueOf(2).equals(status)) {
            return "已完成";
        }
        return "未知";
    }

    private String priority(Integer value) {
        if (Integer.valueOf(1).equals(value)) {
            return "低";
        }
        if (Integer.valueOf(2).equals(value)) {
            return "中";
        }
        if (Integer.valueOf(3).equals(value)) {
            return "高";
        }
        return "未知";
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? shorten(normalize(value), 80) : "未填写";
    }

    private String normalize(String value) {
        return value.strip().replaceAll("\\s+", " ");
    }

    private String shorten(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "…";
    }

    private long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    @FunctionalInterface
    private interface ToolQuery {
        List<String> read(Long userId);
    }
}
