package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.service.CareerChatContextService;
import com.xucheng.aicareer.service.CareerPlanService;
import com.xucheng.aicareer.service.UserProfileService;
import com.xucheng.aicareer.service.UserSkillService;
import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import com.xucheng.aicareer.vo.CareerPlanVO;
import com.xucheng.aicareer.vo.CareerRoadmapStageVO;
import com.xucheng.aicareer.vo.UserProfileVO;
import com.xucheng.aicareer.vo.UserSkillVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

/**
 * 基于现有业务 Service 构建职业规划聊天上下文。
 *
 * <p>画像和当前规划不存在是正常的可选状态，会转换为 null 交由 Prompt 判断；其他业务异常继续向上抛出。</p>
 */
@Service
@RequiredArgsConstructor
public class CareerChatContextServiceImpl implements CareerChatContextService {

    private static final int NOT_FOUND = 404;

    private final UserProfileService userProfileService;
    private final UserSkillService userSkillService;
    private final CareerPlanService careerPlanService;

    @Override
    public CareerChatBusinessContext getCurrentContext() {
        // 每次提问都重新读取，保证用户刚更新的画像、技能或规划立即对 Agent 生效。
        UserProfileVO profile = readOptional(userProfileService::getCurrentProfile);
        List<UserSkillVO> skills = userSkillService.getCurrentUserSkills();
        CareerPlanVO currentPlan = readOptional(careerPlanService::getCurrentPlan);
        return new CareerChatBusinessContext(
                toProfileContext(profile),
                safeList(skills).stream().map(this::toSkillContext).toList(),
                toPlanContext(currentPlan));
    }

    private <T> T readOptional(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (BusinessException exception) {
            // 404 表示该类资料尚未创建，不应使普通聊天整体失败。
            if (Integer.valueOf(NOT_FOUND).equals(exception.getCode())) {
                return null;
            }
            throw exception;
        }
    }

    private CareerChatBusinessContext.Profile toProfileContext(UserProfileVO profile) {
        if (profile == null) {
            return null;
        }
        return new CareerChatBusinessContext.Profile(
                profile.getEducation(),
                profile.getMajor(),
                profile.getGrade(),
                profile.getGraduationYear(),
                profile.getCareerStage(),
                profile.getTargetPosition(),
                profile.getTargetCity(),
                profile.getTargetTime(),
                profile.getDailyStudyHours(),
                profile.getCareerGoal(),
                profile.getInterestDescription());
    }

    private CareerChatBusinessContext.Skill toSkillContext(UserSkillVO skill) {
        return new CareerChatBusinessContext.Skill(
                skill.getSkillName(),
                skill.getSkillCategory(),
                skill.getLevel(),
                skill.getScore());
    }

    private CareerChatBusinessContext.Plan toPlanContext(CareerPlanVO plan) {
        if (plan == null) {
            return null;
        }
        return new CareerChatBusinessContext.Plan(
                plan.getVersion(),
                plan.getTargetPosition(),
                plan.getMatchScore(),
                plan.getSummary(),
                plan.getAdvantages(),
                plan.getWeaknesses(),
                safeList(plan.getRoadmap()).stream().map(this::toRoadmapStageContext).toList());
    }

    private CareerChatBusinessContext.RoadmapStage toRoadmapStageContext(CareerRoadmapStageVO stage) {
        return new CareerChatBusinessContext.RoadmapStage(
                stage.getStage(),
                stage.getName(),
                stage.getGoal(),
                stage.getDuration(),
                stage.getTopics());
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
