package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InterviewVO {

    private Long id;
    private String targetPosition;
    private String interviewType;
    private String difficulty;
    private Integer status;
    private String conversationId;
    private Integer questionCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
