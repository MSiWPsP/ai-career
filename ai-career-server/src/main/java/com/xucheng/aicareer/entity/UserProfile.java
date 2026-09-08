package com.xucheng.aicareer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_profile")
public class UserProfile {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String education;
    private String major;
    private String grade;
    private Integer graduationYear;
    private String careerStage;
    private String targetPosition;
    private String targetCity;
    private String targetTime;
    private BigDecimal dailyStudyHours;
    private String careerGoal;
    private String interestDescription;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
