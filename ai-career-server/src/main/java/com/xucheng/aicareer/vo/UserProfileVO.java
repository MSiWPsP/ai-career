package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UserProfileVO {

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
}
