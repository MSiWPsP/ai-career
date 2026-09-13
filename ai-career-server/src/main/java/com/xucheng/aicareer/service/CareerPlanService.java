package com.xucheng.aicareer.service;

import com.xucheng.aicareer.vo.CareerPlanVO;
import com.xucheng.aicareer.dto.CareerPlanRegenerateDTO;

import java.util.List;

/**
 * 职业规划业务服务，负责结构化规划的生成、持久化和查询。
 */
public interface CareerPlanService {

    /** 基于当前用户的完整画像和技能生成首版职业规划。 */
    CareerPlanVO generatePlan();

    /** 根据归属当前用户的面试报告、最新画像与任务进度生成下一版本，保留旧规划历史。 */
    CareerPlanVO regeneratePlan(CareerPlanRegenerateDTO request);

    /** 获取当前生效的职业规划。 */
    CareerPlanVO getCurrentPlan();

    /** 按版本倒序获取职业规划历史。 */
    List<CareerPlanVO> getPlanHistory();

    /** 获取属于当前登录用户的指定职业规划。 */
    CareerPlanVO getPlanById(Long planId);
}
