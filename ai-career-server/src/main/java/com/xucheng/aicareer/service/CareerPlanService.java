package com.xucheng.aicareer.service;

import com.xucheng.aicareer.vo.CareerPlanVO;

import java.util.List;

public interface CareerPlanService {

    CareerPlanVO getCurrentPlan();

    List<CareerPlanVO> getPlanHistory();

    CareerPlanVO getPlanById(Long planId);
}
