package com.xucheng.aicareer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 基于指定面试报告重新规划的请求；当前仅支持面试触发原因。 */
@Data
@Schema(description = "面试反馈重新规划请求")
public class CareerPlanRegenerateDTO {

    @Pattern(regexp = "INTERVIEW", message = "目前仅支持根据面试反馈重新规划")
    @NotNull(message = "重新规划原因不能为空")
    private String reason;

    @NotNull(message = "来源面试ID不能为空")
    @Positive(message = "来源面试ID必须为正数")
    @Schema(description = "已完成且已生成报告的面试ID，建议客户端用字符串传递以避免精度丢失")
    private Long sourceInterviewId;
}
