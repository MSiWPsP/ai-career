package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class InterviewReportVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long interviewId;
    private Integer totalScore;
    private Map<String, Integer> scores;
    private List<String> advantages;
    private List<String> weaknesses;
    private List<InterviewSuggestionVO> suggestions;
    private String summary;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
