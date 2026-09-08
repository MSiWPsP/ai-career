package com.xucheng.aicareer.controller;

import com.xucheng.aicareer.common.Result;
import com.xucheng.aicareer.service.CareerPlanService;
import com.xucheng.aicareer.vo.CareerPlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/career")
@RequiredArgsConstructor
@Tag(name = "职业规划")
@SecurityRequirement(name = "BearerAuth")
public class CareerController {

    private final CareerPlanService careerPlanService;

    @GetMapping("/plan/current")
    @Operation(summary = "获取当前职业规划")
    public Result<CareerPlanVO> getCurrentPlan() {
        return Result.success(careerPlanService.getCurrentPlan());
    }

    @GetMapping("/plan/history")
    @Operation(summary = "获取职业规划历史")
    public Result<List<CareerPlanVO>> getPlanHistory() {
        return Result.success(careerPlanService.getPlanHistory());
    }

    @GetMapping("/plan/{id}")
    @Operation(summary = "获取职业规划详情")
    public Result<CareerPlanVO> getPlanById(@PathVariable Long id) {
        return Result.success(careerPlanService.getPlanById(id));
    }
}
