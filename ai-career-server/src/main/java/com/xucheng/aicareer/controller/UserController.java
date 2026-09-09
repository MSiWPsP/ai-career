package com.xucheng.aicareer.controller;

import com.xucheng.aicareer.common.Result;
import com.xucheng.aicareer.dto.UpdateUserDTO;
import com.xucheng.aicareer.service.UserService;
import com.xucheng.aicareer.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户")
    public Result<UserVO> getCurrentUser() {
        return Result.success(userService.getCurrentUser());
    }

    @PutMapping("/me")
    @Operation(summary = "修改当前用户")
    public Result<UserVO> updateCurrentUser(@Valid @RequestBody UpdateUserDTO updateUserDTO) {
        return Result.success(userService.updateCurrentUser(updateUserDTO));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传并更新当前用户头像")
    public Result<UserVO> updateAvatar(@RequestPart("file") MultipartFile file) {
        return Result.success(userService.updateAvatar(file));
    }

    @DeleteMapping("/me/avatar")
    @Operation(summary = "移除当前用户头像")
    public Result<UserVO> removeAvatar() {
        return Result.success(userService.removeAvatar());
    }
}
