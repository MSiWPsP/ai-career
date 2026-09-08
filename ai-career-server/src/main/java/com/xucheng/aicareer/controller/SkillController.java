package com.xucheng.aicareer.controller;

import com.xucheng.aicareer.common.Result;
import com.xucheng.aicareer.dto.UserSkillBatchDTO;
import com.xucheng.aicareer.service.UserSkillService;
import com.xucheng.aicareer.vo.UserSkillVO;
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
public class SkillController {

    private final UserSkillService userSkillService;

    @GetMapping
    public Result<List<UserSkillVO>> getCurrentUserSkills() {
        return Result.success(userSkillService.getCurrentUserSkills());
    }

    @PutMapping
    public Result<List<UserSkillVO>> replaceCurrentUserSkills(
            @Valid @RequestBody UserSkillBatchDTO userSkillBatchDTO) {
        return Result.success(userSkillService.replaceCurrentUserSkills(userSkillBatchDTO));
    }
}
