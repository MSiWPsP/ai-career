package com.xucheng.aicareer.controller;

import com.xucheng.aicareer.common.Result;
import com.xucheng.aicareer.dto.TaskStatusDTO;
import com.xucheng.aicareer.service.CareerTaskService;
import com.xucheng.aicareer.vo.CareerTaskVO;
import com.xucheng.aicareer.vo.TaskStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
@Tag(name = "成长任务")
@SecurityRequirement(name = "BearerAuth")
public class TaskController {

    private final CareerTaskService careerTaskService;

    @GetMapping
    @Operation(summary = "获取成长任务列表")
    public Result<List<CareerTaskVO>> getCurrentTasks(
            @RequestParam(required = false)
            @Min(value = 0, message = "任务状态不能小于0")
            @Max(value = 3, message = "任务状态不能大于3") Integer status,
            @RequestParam(required = false) Long planId) {
        return Result.success(careerTaskService.getCurrentTasks(status, planId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取成长任务详情")
    public Result<CareerTaskVO> getTaskById(@PathVariable Long id) {
        return Result.success(careerTaskService.getTaskById(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新成长任务状态")
    public Result<CareerTaskVO> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody TaskStatusDTO taskStatusDTO) {
        return Result.success(careerTaskService.updateTaskStatus(id, taskStatusDTO));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取当前规划任务统计")
    public Result<TaskStatisticsVO> getCurrentTaskStatistics() {
        return Result.success(careerTaskService.getCurrentTaskStatistics());
    }
}
