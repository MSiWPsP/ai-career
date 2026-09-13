package com.xucheng.aicareer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xucheng.aicareer.agent.career.CareerPlannerAgent;
import com.xucheng.aicareer.agent.career.dto.CareerPlanResult;
import com.xucheng.aicareer.agent.career.dto.CareerTaskResult;
import com.xucheng.aicareer.agent.career.dto.RoadmapStage;
import com.xucheng.aicareer.entity.CareerPlan;
import com.xucheng.aicareer.entity.CareerTask;
import com.xucheng.aicareer.dto.CareerPlanRegenerateDTO;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.mapper.CareerPlanMapper;
import com.xucheng.aicareer.mapper.CareerTaskMapper;
import com.xucheng.aicareer.service.CareerPlanService;
import com.xucheng.aicareer.service.CareerTaskService;
import com.xucheng.aicareer.service.InterviewReportService;
import com.xucheng.aicareer.service.UserProfileService;
import com.xucheng.aicareer.service.UserSkillService;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.CareerPlanVO;
import com.xucheng.aicareer.vo.CareerRoadmapStageVO;
import com.xucheng.aicareer.vo.ProfileCompletionVO;
import com.xucheng.aicareer.vo.UserProfileVO;
import com.xucheng.aicareer.vo.UserSkillVO;
import com.xucheng.aicareer.vo.CareerTaskVO;
import com.xucheng.aicareer.vo.InterviewReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * 结构化职业规划业务实现。
 *
 * <p>先校验画像和技能，再调用 Agent；只有模型结果完整有效时，才在同一事务中保存规划及成长任务。</p>
 */
@Service
@RequiredArgsConstructor
public class CareerPlanServiceImpl implements CareerPlanService {

    private static final int CURRENT_STATUS = 1;
    private static final int HISTORICAL_STATUS = 0;
    private static final int INITIAL_VERSION = 1;
    private static final int TASK_STATUS_WAITING = 0;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<CareerRoadmapStageVO>> ROADMAP_LIST_TYPE = new TypeReference<>() {
    };

    private final CareerPlanMapper careerPlanMapper;
    private final CareerTaskMapper careerTaskMapper;
    private final UserProfileService userProfileService;
    private final UserSkillService userSkillService;
    private final CareerPlannerAgent careerPlannerAgent;
    private final CareerTaskService careerTaskService;
    private final InterviewReportService interviewReportService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Override
    public CareerPlanVO generatePlan() {
        Long userId = UserContext.getUserId();
        if (careerPlanMapper.exists(Wrappers.<CareerPlan>lambdaQuery()
                .eq(CareerPlan::getUserId, userId))) {
            throw new BusinessException(409, "职业规划已存在，请使用重新规划功能");
        }

        ProfileCompletionVO completion = userProfileService.getCompletion();
        if (!Boolean.TRUE.equals(completion.getCompleted())) {
            throw new BusinessException(400, "请先完善职业画像后再生成职业规划");
        }
        UserProfileVO profile = userProfileService.getCurrentProfile();
        List<UserSkillVO> skills = userSkillService.getCurrentUserSkills();
        if (skills.isEmpty()) {
            throw new BusinessException(400, "请先填写至少一项技能后再生成职业规划");
        }

        CareerPlanResult result = careerPlannerAgent.generatePlan(userId, profile, skills);
        CareerPlan savedPlan = transactionTemplate.execute(status -> {
            // Agent 调用不占用数据库事务；落库前再次检查，防止并发请求各自生成一份首版规划。
            if (careerPlanMapper.exists(Wrappers.<CareerPlan>lambdaQuery()
                    .eq(CareerPlan::getUserId, userId))) {
                throw new BusinessException(409, "职业规划已存在，请使用重新规划功能");
            }
            CareerPlan plan = saveCareerPlan(userId, result, INITIAL_VERSION, null);
            saveCareerTasks(userId, plan.getId(), result.getRoadmap());
            CareerPlan refreshedPlan = careerPlanMapper.selectById(plan.getId());
            return refreshedPlan == null ? plan : refreshedPlan;
        });
        if (savedPlan == null) {
            throw new BusinessException(500, "职业规划保存失败，请稍后重试");
        }
        return toCareerPlanVO(savedPlan);
    }

    @Override
    public CareerPlanVO regeneratePlan(CareerPlanRegenerateDTO request) {
        Long userId = UserContext.getUserId();
        CareerPlan oldPlan = findCurrentPlan(userId);
        if (oldPlan == null) throw new BusinessException(409, "请先生成首版职业规划");
        Long interviewId = request.getSourceInterviewId();
        if (careerPlanMapper.exists(Wrappers.<CareerPlan>lambdaQuery()
                .eq(CareerPlan::getUserId, userId)
                .eq(CareerPlan::getSourceInterviewId, interviewId))) {
            throw new BusinessException(409, "该面试报告已用于重新规划");
        }

        // 报告服务验证面试归属与结束状态；旧报告也会在这里补齐能力回写。
        InterviewReportVO report = interviewReportService.generateReport(interviewId);
        UserProfileVO profile = userProfileService.getCurrentProfile();
        List<UserSkillVO> skills = userSkillService.getCurrentUserSkills();
        if (skills.isEmpty()) throw new BusinessException(400, "请先填写至少一项技能后再重新规划");
        List<CareerTaskVO> oldTasks = careerTaskService.getCurrentTasks(null, oldPlan.getId());
        CareerPlanResult result = careerPlannerAgent.regeneratePlan(userId, profile, skills,
                toCareerPlanVO(oldPlan), oldTasks, report);

        CareerPlan savedPlan = transactionTemplate.execute(status -> {
            // 模型调用在事务外；再次校验当前版本与来源，避免并发请求覆盖新近生成的规划。
            if (careerPlanMapper.exists(Wrappers.<CareerPlan>lambdaQuery()
                    .eq(CareerPlan::getUserId, userId)
                    .eq(CareerPlan::getSourceInterviewId, interviewId))) {
                throw new BusinessException(409, "该面试报告已用于重新规划");
            }
            CareerPlan latest = findCurrentPlan(userId);
            if (latest == null || !latest.getId().equals(oldPlan.getId())) {
                throw new BusinessException(409, "职业规划已更新，请刷新后重试");
            }
            int archived = careerPlanMapper.update(null, Wrappers.<CareerPlan>lambdaUpdate()
                    .eq(CareerPlan::getId, oldPlan.getId())
                    .eq(CareerPlan::getUserId, userId)
                    .eq(CareerPlan::getStatus, CURRENT_STATUS)
                    .set(CareerPlan::getStatus, HISTORICAL_STATUS));
            if (archived != 1) throw new BusinessException(409, "职业规划已更新，请刷新后重试");
            CareerPlan plan = saveCareerPlan(userId, result, oldPlan.getVersion() + 1, interviewId);
            saveCareerTasks(userId, plan.getId(), result.getRoadmap());
            CareerPlan refreshed = careerPlanMapper.selectById(plan.getId());
            return refreshed == null ? plan : refreshed;
        });
        if (savedPlan == null) throw new BusinessException(500, "重新规划保存失败，请稍后重试");
        return toCareerPlanVO(savedPlan);
    }

    @Override
    public CareerPlanVO getCurrentPlan() {
        Long userId = UserContext.getUserId();
        CareerPlan plan = findCurrentPlan(userId);
        if (plan == null) {
            throw new BusinessException(404, "当前职业规划不存在");
        }
        return toCareerPlanVO(plan);
    }

    private CareerPlan findCurrentPlan(Long userId) {
        return careerPlanMapper.selectOne(Wrappers.<CareerPlan>lambdaQuery()
                .eq(CareerPlan::getUserId, userId)
                .eq(CareerPlan::getStatus, CURRENT_STATUS)
                .orderByDesc(CareerPlan::getVersion)
                .last("LIMIT 1"));
    }

    @Override
    public List<CareerPlanVO> getPlanHistory() {
        return careerPlanMapper.selectList(Wrappers.<CareerPlan>lambdaQuery()
                        .eq(CareerPlan::getUserId, UserContext.getUserId())
                        .orderByDesc(CareerPlan::getVersion)
                        .orderByDesc(CareerPlan::getCreateTime))
                .stream()
                .map(this::toCareerPlanVO)
                .toList();
    }

    @Override
    public CareerPlanVO getPlanById(Long planId) {
        CareerPlan plan = careerPlanMapper.selectOne(Wrappers.<CareerPlan>lambdaQuery()
                .eq(CareerPlan::getId, planId)
                .eq(CareerPlan::getUserId, UserContext.getUserId()));
        if (plan == null) {
            throw new BusinessException(404, "职业规划不存在");
        }
        return toCareerPlanVO(plan);
    }

    private CareerPlanVO toCareerPlanVO(CareerPlan plan) {
        // JSON 字段在业务边界统一反序列化，Controller 不直接感知数据库存储格式。
        return CareerPlanVO.builder()
                .id(plan.getId())
                .version(plan.getVersion())
                .targetPosition(plan.getTargetPosition())
                .matchScore(plan.getMatchScore())
                .summary(plan.getSummary())
                .advantages(readJson(plan.getAdvantages(), STRING_LIST_TYPE, Collections::emptyList))
                .weaknesses(readJson(plan.getWeaknesses(), STRING_LIST_TYPE, Collections::emptyList))
                .roadmap(readJson(plan.getRoadmap(), ROADMAP_LIST_TYPE, Collections::emptyList))
                .status(plan.getStatus())
                .sourceInterviewId(plan.getSourceInterviewId())
                .createTime(plan.getCreateTime())
                .updateTime(plan.getUpdateTime())
                .build();
    }

    private CareerPlan saveCareerPlan(Long userId, CareerPlanResult result, int version, Long sourceInterviewId) {
        CareerPlan plan = new CareerPlan();
        plan.setUserId(userId);
        plan.setVersion(version);
        plan.setSourceInterviewId(sourceInterviewId);
        plan.setTargetPosition(result.getTargetPosition().trim());
        plan.setMatchScore(result.getMatchScore());
        plan.setSummary(result.getSummary().trim());
        plan.setAdvantages(writeJson(trimmedValues(result.getAdvantages())));
        plan.setWeaknesses(writeJson(trimmedValues(result.getWeaknesses())));
        plan.setRoadmap(writeJson(result.getRoadmap().stream()
                .map(this::toRoadmapStageVO)
                .toList()));
        plan.setStatus(CURRENT_STATUS);
        careerPlanMapper.insert(plan);
        return plan;
    }

    private void saveCareerTasks(Long userId, Long planId, List<RoadmapStage> roadmap) {
        // Agent 输出的阶段任务拆分为独立成长任务，后续可单独更新完成状态。
        for (RoadmapStage stage : roadmap) {
            for (CareerTaskResult result : stage.getTasks()) {
                CareerTask task = new CareerTask();
                task.setUserId(userId);
                task.setCareerPlanId(planId);
                task.setStageName(stage.getName().trim());
                task.setTaskName(result.getTaskName().trim());
                task.setTaskDescription(trimToNull(result.getDescription()));
                task.setTaskType(result.getTaskType());
                task.setPriority(result.getPriority());
                task.setStatus(TASK_STATUS_WAITING);
                careerTaskMapper.insert(task);
            }
        }
    }

    private CareerRoadmapStageVO toRoadmapStageVO(RoadmapStage stage) {
        return CareerRoadmapStageVO.builder()
                .stage(stage.getStage())
                .name(stage.getName().trim())
                .goal(stage.getGoal().trim())
                .duration(stage.getDuration().trim())
                .topics(trimmedValues(stage.getTopics()))
                .build();
    }

    private List<String> trimmedValues(List<String> values) {
        return values.stream().map(String::trim).toList();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BusinessException(500, "职业规划保存失败，请稍后重试");
        }
    }

    private <T> T readJson(String json, TypeReference<T> type, Supplier<T> emptyValueSupplier) {
        if (json == null || json.isBlank()) {
            return emptyValueSupplier.get();
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new BusinessException(500, "职业规划数据异常，请联系管理员");
        }
    }
}
