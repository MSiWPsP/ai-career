package com.xucheng.aicareer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "AI职业规划师普通聊天请求")
public class CareerChatDTO {

    @NotBlank(message = "聊天内容不能为空")
    @Size(max = 2000, message = "聊天内容不能超过2000个字符")
    @Schema(description = "用户的职业咨询问题", example = "我现在应该先学Redis还是微服务？")
    private String message;
}
