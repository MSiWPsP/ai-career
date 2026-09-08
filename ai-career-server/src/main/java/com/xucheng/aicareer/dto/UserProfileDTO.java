package com.xucheng.aicareer.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserProfileDTO {

    @Size(max = 50, message = "学历长度不能超过50个字符")
    private String education;

    @Size(max = 100, message = "专业长度不能超过100个字符")
    private String major;

    @Size(max = 50, message = "年级长度不能超过50个字符")
    private String grade;

    @Min(value = 2000, message = "毕业年份不能早于2000年")
    @Max(value = 2100, message = "毕业年份不能晚于2100年")
    private Integer graduationYear;

    @Size(max = 50, message = "职业阶段长度不能超过50个字符")
    private String careerStage;

    @Size(max = 100, message = "目标岗位长度不能超过100个字符")
    private String targetPosition;

    @Size(max = 100, message = "目标城市长度不能超过100个字符")
    private String targetCity;

    @Size(max = 100, message = "目标时间长度不能超过100个字符")
    private String targetTime;

    @DecimalMin(value = "0.0", message = "每日学习时间不能小于0")
    @DecimalMax(value = "24.0", message = "每日学习时间不能超过24小时")
    private BigDecimal dailyStudyHours;

    @Size(max = 500, message = "职业目标长度不能超过500个字符")
    private String careerGoal;

    @Size(max = 1000, message = "职业兴趣描述不能超过1000个字符")
    private String interestDescription;
}
