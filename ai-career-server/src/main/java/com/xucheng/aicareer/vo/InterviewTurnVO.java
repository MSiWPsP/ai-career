package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

@Data
@Builder
public class InterviewTurnVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long interviewId;
    private String message;
    private Boolean finished;
    private Integer status;
    private Integer questionCount;
    private Integer maxQuestions;
}
