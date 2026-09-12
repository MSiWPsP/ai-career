package com.xucheng.aicareer.service.model;

import com.xucheng.aicareer.vo.UserProfileVO;
import com.xucheng.aicareer.vo.UserSkillVO;

import java.util.List;

/** 当前面试轮次提供给 InterviewerAgent 的只读业务快照。 */
public record InterviewBusinessContext(
        String targetPosition,
        String interviewType,
        String configuredDifficulty,
        Integer questionCount,
        Integer maxQuestions,
        UserProfileVO profile,
        List<UserSkillVO> skills) {
}
