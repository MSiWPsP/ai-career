package com.xucheng.aicareer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 职业规划聊天会话的可变元数据。 */
@Data
@Schema(description = "职业规划聊天会话更新请求")
public class CareerChatSessionUpdateDTO {

    @Size(min = 1, max = 100, message = "会话标题长度应为1到100个字符")
    @Schema(description = "会话标题", example = "Java后端实习准备")
    private String title;

    @Min(value = 0, message = "会话状态不正确")
    @Max(value = 1, message = "会话状态不正确")
    @Schema(description = "会话状态：0归档，1活跃", allowableValues = {"0", "1"})
    private Integer status;
}
