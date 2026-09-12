package com.xucheng.aicareer.agent.interview.dto;

import lombok.Data;

/**
 * InterviewerAgent 的单轮结构化判断结果。
 *
 * <p>评分、评价和动作只供服务端控制流程与后续报告使用，面试过程中不会直接返回给用户。</p>
 */
@Data
public class InterviewTurnResult {

    private String response;
    private String topic;
    private Integer score;
    private String evaluation;
    private String nextAction;
    private String nextDifficulty;
    private Boolean finished;
}
