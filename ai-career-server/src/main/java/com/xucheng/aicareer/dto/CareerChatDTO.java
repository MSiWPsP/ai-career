package com.xucheng.aicareer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 职业规划师聊天请求参数。
 *
 * <p>clientMessageId 由客户端生成，同一条消息重试时必须保持不变。</p>
 */
@Data
@Schema(description = "AI职业规划师普通聊天请求")
public class CareerChatDTO {

    @Size(max = 100, message = "会话标识长度不能超过100个字符")
    @Pattern(regexp = "^career:[0-9]+:[0-9a-fA-F-]{32,36}$", message = "会话标识格式不正确")
    @Schema(description = "会话标识；首次对话可不传，后续对话应回传服务端返回值",
            example = "career:10001:2c08d11b-88b9-4d63-8cc3-0a79d86e4695")
    private String conversationId;

    @Size(max = 64, message = "客户端消息标识长度不能超过64个字符")
    @Pattern(regexp = "^[A-Za-z0-9_-]{8,64}$", message = "客户端消息标识格式不正确")
    @Schema(description = "客户端生成的幂等消息标识；重试时保持不变", example = "f4582584-602f-47b9-9567-6549bda65908")
    private String clientMessageId;

    @NotBlank(message = "聊天内容不能为空")
    @Size(max = 2000, message = "聊天内容不能超过2000个字符")
    @Schema(description = "用户的职业咨询问题", example = "我现在应该先学Redis还是微服务？")
    private String message;
}
