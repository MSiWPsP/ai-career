package com.xucheng.aicareer.service;

import java.util.Map;

/** 将一份已生成报告的分项评分回写为能力历史，并适度调整对应的自评技能。 */
public interface InterviewAbilityWritebackService {

    /** 必须在报告落库事务中调用；相同面试重复调用不应重复叠加评分。 */
    void writeback(Long userId, Long interviewId, Map<String, Integer> scores);
}
