package com.xucheng.aicareer.controller;

import com.xucheng.aicareer.common.Result;
import com.xucheng.aicareer.service.AbilityService;
import com.xucheng.aicareer.vo.AbilityRadarVO;
import com.xucheng.aicareer.vo.AbilityScoreVO;
import com.xucheng.aicareer.vo.AbilityTrendVO;
import com.xucheng.aicareer.vo.PageResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/ability")
@RequiredArgsConstructor
@Tag(name = "能力成长")
@SecurityRequirement(name = "BearerAuth")
public class AbilityController {

    private final AbilityService abilityService;

    @GetMapping("/current")
    @Operation(summary = "获取当前能力画像")
    public Result<Map<String, Integer>> getCurrentAbilities() {
        return Result.success(abilityService.getCurrentAbilities());
    }

    @GetMapping("/history")
    @Operation(summary = "分页获取能力评分历史")
    public Result<PageResultVO<AbilityScoreVO>> getAbilityHistory(
            @RequestParam(required = false)
            @Size(max = 100, message = "能力名称不能超过100个字符") String abilityName,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页数量不能小于1")
            @Max(value = 100, message = "每页数量不能大于100") int pageSize) {
        return Result.success(abilityService.getAbilityHistory(abilityName, page, pageSize));
    }

    @GetMapping("/radar")
    @Operation(summary = "获取能力雷达图数据")
    public Result<AbilityRadarVO> getAbilityRadar() {
        return Result.success(abilityService.getAbilityRadar());
    }

    @GetMapping("/trend")
    @Operation(summary = "获取单项能力成长趋势")
    public Result<AbilityTrendVO> getAbilityTrend(
            @RequestParam(required = false)
            @Size(max = 100, message = "能力名称不能超过100个字符") String abilityName) {
        return Result.success(abilityService.getAbilityTrend(abilityName));
    }
}
