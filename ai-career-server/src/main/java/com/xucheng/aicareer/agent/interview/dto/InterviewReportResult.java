package com.xucheng.aicareer.agent.interview.dto;

import com.xucheng.aicareer.vo.InterviewSuggestionVO;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** 面试报告 Agent 的结构化输出，进入数据库前必须完成范围和内容校验。 */
@Data
public class InterviewReportResult {
    private Integer totalScore;
    private Map<String, Integer> scores;
    private List<String> advantages;
    private List<String> weaknesses;
    private List<InterviewSuggestionVO> suggestions;
    private String summary;
}
