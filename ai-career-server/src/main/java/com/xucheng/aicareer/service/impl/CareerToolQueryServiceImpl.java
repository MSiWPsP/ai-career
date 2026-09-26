package com.xucheng.aicareer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xucheng.aicareer.entity.AbilityScore;
import com.xucheng.aicareer.entity.CareerPlan;
import com.xucheng.aicareer.entity.CareerTask;
import com.xucheng.aicareer.entity.Interview;
import com.xucheng.aicareer.entity.InterviewReport;
import com.xucheng.aicareer.mapper.AbilityScoreMapper;
import com.xucheng.aicareer.mapper.CareerPlanMapper;
import com.xucheng.aicareer.mapper.CareerTaskMapper;
import com.xucheng.aicareer.mapper.InterviewMapper;
import com.xucheng.aicareer.mapper.InterviewReportMapper;
import com.xucheng.aicareer.service.CareerToolQueryService;
import com.xucheng.aicareer.service.model.CareerAbilitySnapshot;
import com.xucheng.aicareer.service.model.CareerInterviewSnapshot;
import com.xucheng.aicareer.service.model.CareerTaskSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 直接按已认证 userId 读取 Tool 所需最小数据，不读取或修改其他用户记录。 */
@Service
@RequiredArgsConstructor
public class CareerToolQueryServiceImpl implements CareerToolQueryService {

    private static final int CURRENT_PLAN_STATUS = 1;
    private static final int TASK_WAITING = 0;
    private static final int TASK_PROCESSING = 1;
    private static final int TASK_COMPLETED = 2;
    private static final List<Integer> FINISHED_INTERVIEW_STATUSES = List.of(2, 3);
    private static final int MAX_TASKS = 10;
    private static final int MAX_ABILITIES = 6;
    private static final int MAX_SCORE_POINTS = 6;
    private static final int MAX_INTERVIEWS = 3;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final CareerPlanMapper careerPlanMapper;
    private final CareerTaskMapper careerTaskMapper;
    private final AbilityScoreMapper abilityScoreMapper;
    private final InterviewMapper interviewMapper;
    private final InterviewReportMapper interviewReportMapper;
    private final ObjectMapper objectMapper;

    @Override
    public CareerTaskSnapshot getCurrentTaskSnapshot(Long userId) {
        requireUserId(userId);
        CareerPlan plan = careerPlanMapper.selectOne(Wrappers.<CareerPlan>lambdaQuery()
                .select(CareerPlan::getId)
                .eq(CareerPlan::getUserId, userId)
                .eq(CareerPlan::getStatus, CURRENT_PLAN_STATUS)
                .orderByDesc(CareerPlan::getVersion)
                .last("LIMIT 1"));
        if (plan == null) {
            return new CareerTaskSnapshot(0, 0, 0, 0, 0, List.of());
        }
        List<CareerTask> tasks = careerTaskMapper.selectList(Wrappers.<CareerTask>lambdaQuery()
                .eq(CareerTask::getUserId, userId)
                .eq(CareerTask::getCareerPlanId, plan.getId())
                .orderByDesc(CareerTask::getPriority)
                .orderByAsc(CareerTask::getStartDate)
                .orderByAsc(CareerTask::getCreateTime));
        long completed = countTasks(tasks, TASK_COMPLETED);
        long processing = countTasks(tasks, TASK_PROCESSING);
        long waiting = countTasks(tasks, TASK_WAITING);
        int completionRate = tasks.isEmpty() ? 0 : (int) (completed * 100 / tasks.size());
        List<CareerTaskSnapshot.TaskItem> items = tasks.stream().limit(MAX_TASKS)
                .map(task -> new CareerTaskSnapshot.TaskItem(task.getStageName(), task.getTaskName(),
                        task.getPriority(), task.getStatus(), task.getDeadline()))
                .toList();
        return new CareerTaskSnapshot(tasks.size(), completed, processing, waiting, completionRate, items);
    }

    @Override
    public CareerAbilitySnapshot getCurrentAbilitySnapshot(Long userId) {
        requireUserId(userId);
        List<AbilityScore> scores = abilityScoreMapper.selectList(Wrappers.<AbilityScore>lambdaQuery()
                .eq(AbilityScore::getUserId, userId)
                .orderByDesc(AbilityScore::getCreateTime)
                .last("LIMIT 60"));
        Map<String, List<AbilityScore>> grouped = new LinkedHashMap<>();
        for (AbilityScore score : scores) {
            if (!StringUtils.hasText(score.getAbilityName())) {
                continue;
            }
            List<AbilityScore> values = grouped.get(score.getAbilityName());
            if (values == null) {
                if (grouped.size() >= MAX_ABILITIES) {
                    continue;
                }
                values = new ArrayList<>();
                grouped.put(score.getAbilityName(), values);
            }
            if (values.size() < MAX_SCORE_POINTS) {
                values.add(score);
            }
        }
        List<CareerAbilitySnapshot.AbilityItem> abilities = grouped.entrySet().stream()
                .limit(MAX_ABILITIES)
                .map(entry -> new CareerAbilitySnapshot.AbilityItem(entry.getKey(),
                        entry.getValue().getFirst().getScore(), entry.getValue().stream()
                        .map(score -> new CareerAbilitySnapshot.ScorePoint(score.getScore(),
                                score.getCreateTime() == null ? null : score.getCreateTime().toLocalDate()))
                        .toList()))
                .toList();
        return new CareerAbilitySnapshot(abilities);
    }

    @Override
    public CareerInterviewSnapshot getRecentInterviewSnapshot(Long userId) {
        requireUserId(userId);
        List<Interview> interviews = interviewMapper.selectList(Wrappers.<Interview>lambdaQuery()
                .eq(Interview::getUserId, userId)
                .in(Interview::getStatus, FINISHED_INTERVIEW_STATUSES)
                .orderByDesc(Interview::getEndTime)
                .orderByDesc(Interview::getCreateTime)
                .last("LIMIT " + MAX_INTERVIEWS));
        if (interviews.isEmpty()) {
            return new CareerInterviewSnapshot(List.of());
        }
        List<Long> interviewIds = interviews.stream().map(Interview::getId).toList();
        Map<Long, InterviewReport> reports = new LinkedHashMap<>();
        interviewReportMapper.selectList(Wrappers.<InterviewReport>lambdaQuery()
                        .eq(InterviewReport::getUserId, userId)
                        .in(InterviewReport::getInterviewId, interviewIds))
                .forEach(report -> reports.put(report.getInterviewId(), report));
        List<CareerInterviewSnapshot.InterviewItem> items = interviews.stream().map(interview -> {
            InterviewReport report = reports.get(interview.getId());
            LocalDateTime occurredAt = interview.getEndTime() == null
                    ? interview.getCreateTime() : interview.getEndTime();
            return new CareerInterviewSnapshot.InterviewItem(interview.getTargetPosition(),
                    interview.getInterviewType(), interview.getDifficulty(), occurredAt,
                    report == null ? null : report.getTotalScore(),
                    report == null ? null : report.getSummary(),
                    report == null ? List.of() : readStringList(report.getWeaknesses()));
        }).toList();
        return new CareerInterviewSnapshot(items);
    }

    private long countTasks(List<CareerTask> tasks, int status) {
        return tasks.stream().filter(task -> Integer.valueOf(status).equals(task.getStatus())).count();
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, STRING_LIST_TYPE);
            return values == null ? List.of() : values.stream()
                    .filter(StringUtils::hasText).limit(3).toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private void requireUserId(Long userId) {
        Assert.notNull(userId, "Tool userId 不能为空");
    }
}
