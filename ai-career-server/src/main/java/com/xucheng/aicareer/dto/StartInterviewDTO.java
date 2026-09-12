package com.xucheng.aicareer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StartInterviewDTO {

    @NotBlank(message = "目标岗位不能为空")
    @Size(max = 100, message = "目标岗位长度不能超过100个字符")
    private String targetPosition;

    @NotBlank(message = "面试类型不能为空")
    @Pattern(regexp = "TECHNICAL|PROJECT|HR|COMPREHENSIVE", message = "面试类型不正确")
    private String interviewType;

    @NotBlank(message = "面试难度不能为空")
    @Pattern(regexp = "EASY|MEDIUM|HARD", message = "面试难度不正确")
    private String difficulty;

    @Min(value = 1, message = "问题数量不能小于1")
    @Max(value = 15, message = "问题数量不能大于15")
    private Integer maxQuestions = 5;
}
