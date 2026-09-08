package com.xucheng.aicareer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xucheng.aicareer.entity.CareerPlan;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.mapper.CareerPlanMapper;
import com.xucheng.aicareer.service.CareerPlanService;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.CareerPlanVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerPlanServiceImpl implements CareerPlanService {

    private static final int CURRENT_STATUS = 1;

    private final CareerPlanMapper careerPlanMapper;

    @Override
    public CareerPlanVO getCurrentPlan() {
        Long userId = UserContext.getUserId();
        CareerPlan plan = careerPlanMapper.selectOne(Wrappers.<CareerPlan>lambdaQuery()
                .eq(CareerPlan::getUserId, userId)
                .eq(CareerPlan::getStatus, CURRENT_STATUS)
                .orderByDesc(CareerPlan::getVersion)
                .last("LIMIT 1"));
        if (plan == null) {
            throw new BusinessException(404, "当前职业规划不存在");
        }
        return toCareerPlanVO(plan);
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
        return CareerPlanVO.builder()
                .id(plan.getId())
                .version(plan.getVersion())
                .targetPosition(plan.getTargetPosition())
                .matchScore(plan.getMatchScore())
                .summary(plan.getSummary())
                .advantages(plan.getAdvantages())
                .weaknesses(plan.getWeaknesses())
                .roadmap(plan.getRoadmap())
                .status(plan.getStatus())
                .sourceInterviewId(plan.getSourceInterviewId())
                .createTime(plan.getCreateTime())
                .updateTime(plan.getUpdateTime())
                .build();
    }
}
