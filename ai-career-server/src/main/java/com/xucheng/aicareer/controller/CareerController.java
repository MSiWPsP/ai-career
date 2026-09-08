package com.xucheng.aicareer.controller;

import com.xucheng.aicareer.common.Result;
import com.xucheng.aicareer.service.CareerPlanService;
import com.xucheng.aicareer.vo.CareerPlanVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/career")
@RequiredArgsConstructor
public class CareerController {

    private final CareerPlanService careerPlanService;

    @GetMapping("/plan/current")
    public Result<CareerPlanVO> getCurrentPlan() {
        return Result.success(careerPlanService.getCurrentPlan());
    }

    @GetMapping("/plan/history")
    public Result<List<CareerPlanVO>> getPlanHistory() {
        return Result.success(careerPlanService.getPlanHistory());
    }

    @GetMapping("/plan/{id}")
    public Result<CareerPlanVO> getPlanById(@PathVariable Long id) {
        return Result.success(careerPlanService.getPlanById(id));
    }
}
