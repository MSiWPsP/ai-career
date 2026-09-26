package com.xucheng.aicareer.service.model;

import java.time.LocalDateTime;
import java.util.List;

/** 最近模拟面试及其已有报告摘要的只读快照。 */
public record CareerInterviewSnapshot(List<InterviewItem> interviews) {

    public CareerInterviewSnapshot {
        interviews = interviews == null ? List.of() : List.copyOf(interviews);
    }

    public record InterviewItem(
            String targetPosition,
            String interviewType,
            String difficulty,
            LocalDateTime occurredAt,
            Integer totalScore,
            String summary,
            List<String> weaknesses) {

        public InterviewItem {
            weaknesses = weaknesses == null ? List.of() : List.copyOf(weaknesses);
        }
    }
}
