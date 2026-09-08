package com.xucheng.aicareer.controller;

import com.xucheng.aicareer.common.Result;
import com.xucheng.aicareer.dto.UserProfileDTO;
import com.xucheng.aicareer.service.UserProfileService;
import com.xucheng.aicareer.vo.ProfileCompletionVO;
import com.xucheng.aicareer.vo.UserProfileVO;
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
public class ProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public Result<UserProfileVO> getCurrentProfile() {
        return Result.success(userProfileService.getCurrentProfile());
    }

    @PostMapping
    public Result<UserProfileVO> createProfile(
            @Valid @RequestBody UserProfileDTO userProfileDTO) {
        return Result.success(userProfileService.createProfile(userProfileDTO));
    }

    @PutMapping
    public Result<UserProfileVO> updateProfile(
            @Valid @RequestBody UserProfileDTO userProfileDTO) {
        return Result.success(userProfileService.updateProfile(userProfileDTO));
    }

    @GetMapping("/completion")
    public Result<ProfileCompletionVO> getCompletion() {
        return Result.success(userProfileService.getCompletion());
    }
}
