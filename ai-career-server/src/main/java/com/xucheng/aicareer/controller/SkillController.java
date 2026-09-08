package com.xucheng.aicareer.controller;

import com.xucheng.aicareer.common.Result;
import com.xucheng.aicareer.dto.UserSkillBatchDTO;
import com.xucheng.aicareer.service.UserSkillService;
import com.xucheng.aicareer.vo.UserSkillVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/skill")
@RequiredArgsConstructor
@Tag(name = "用户技能")
@SecurityRequirement(name = "BearerAuth")
public class SkillController {

    private final UserSkillService userSkillService;

    @GetMapping
    @Operation(summary = "获取当前用户技能")
    public Result<List<UserSkillVO>> getCurrentUserSkills() {
        return Result.success(userSkillService.getCurrentUserSkills());
    }

    @PutMapping
    @Operation(summary = "批量替换当前用户技能")
    public Result<List<UserSkillVO>> replaceCurrentUserSkills(
            @Valid @RequestBody UserSkillBatchDTO userSkillBatchDTO) {
        return Result.success(userSkillService.replaceCurrentUserSkills(userSkillBatchDTO));
    }
}
