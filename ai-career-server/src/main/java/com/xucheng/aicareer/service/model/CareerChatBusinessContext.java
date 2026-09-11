package com.xucheng.aicareer.service.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单次职业规划聊天使用的只读业务快照。
 *
 * <p>该对象仅向 Agent 暴露回答所需字段，避免直接传递持久化实体及内部字段。</p>
 */
public record CareerChatBusinessContext(
        Profile profile,
        List<Skill> skills,
        Plan currentPlan) {

    public CareerChatBusinessContext {
        // 使用不可变空集合统一表达“未填写技能”，便于 Prompt 明确区分缺失数据。
        skills = skills == null ? List.of() : List.copyOf(skills);
    }

    public boolean hasProfile() {
        return profile != null;
    }

    public boolean hasSkills() {
        return !skills.isEmpty();
    }

    public boolean hasCurrentPlan() {
        return currentPlan != null;
    }

    public record Profile(
            String education,
            String major,
            String grade,
            Integer graduationYear,
            String careerStage,
            String targetPosition,
            String targetCity,
            String targetTime,
            BigDecimal dailyStudyHours,
            String careerGoal,
            String interestDescription) {
    }

    public record Skill(
            String skillName,
            String skillCategory,
            Integer level,
            Integer score) {
    }

    public record Plan(
            Integer version,
            String targetPosition,
            Integer matchScore,
            String summary,
            List<String> advantages,
            List<String> weaknesses,
            List<RoadmapStage> roadmap) {

        public Plan {
            // 防止模型调用期间上游集合被修改，同时消除可选集合的 null 分支。
            advantages = advantages == null ? List.of() : List.copyOf(advantages);
            weaknesses = weaknesses == null ? List.of() : List.copyOf(weaknesses);
            roadmap = roadmap == null ? List.of() : List.copyOf(roadmap);
        }
    }

    public record RoadmapStage(
            Integer stage,
            String name,
            String goal,
            String duration,
            List<String> topics) {

        public RoadmapStage {
            topics = topics == null ? List.of() : List.copyOf(topics);
        }
    }
}
