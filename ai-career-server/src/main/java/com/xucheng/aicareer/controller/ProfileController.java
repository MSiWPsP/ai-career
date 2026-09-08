package com.xucheng.aicareer.controller;

import com.xucheng.aicareer.common.Result;
import com.xucheng.aicareer.dto.UserProfileDTO;
import com.xucheng.aicareer.service.UserProfileService;
import com.xucheng.aicareer.vo.ProfileCompletionVO;
import com.xucheng.aicareer.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "用户画像")
@SecurityRequirement(name = "BearerAuth")
public class ProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    @Operation(summary = "获取当前用户画像")
    public Result<UserProfileVO> getCurrentProfile() {
        return Result.success(userProfileService.getCurrentProfile());
    }

    @PostMapping
    @Operation(summary = "创建当前用户画像")
    public Result<UserProfileVO> createProfile(
            @Valid @RequestBody UserProfileDTO userProfileDTO) {
        return Result.success(userProfileService.createProfile(userProfileDTO));
    }

    @PutMapping
    @Operation(summary = "修改当前用户画像")
    public Result<UserProfileVO> updateProfile(
            @Valid @RequestBody UserProfileDTO userProfileDTO) {
        return Result.success(userProfileService.updateProfile(userProfileDTO));
    }

    @GetMapping("/completion")
    @Operation(summary = "获取用户画像完整度")
    public Result<ProfileCompletionVO> getCompletion() {
        return Result.success(userProfileService.getCompletion());
    }
}
