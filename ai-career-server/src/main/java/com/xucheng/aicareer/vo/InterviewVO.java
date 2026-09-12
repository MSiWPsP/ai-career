package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

@Data
@Builder
public class InterviewVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String targetPosition;
    private String interviewType;
    private String difficulty;
    private Integer status;
    private String conversationId;
    private Integer questionCount;
    private Integer maxQuestions;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
