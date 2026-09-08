package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InterviewHistoryRecordVO {

    private Long id;
    private String targetPosition;
    private String interviewType;
    private String difficulty;
    private Integer status;
    private Integer totalScore;
    private LocalDateTime createTime;
}
