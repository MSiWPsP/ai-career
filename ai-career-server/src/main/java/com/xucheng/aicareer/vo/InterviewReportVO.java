package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class InterviewReportVO {

    private Long id;
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
