package com.xucheng.aicareer.service.model;

import java.math.BigDecimal;
import java.util.List;

public record CareerChatBusinessContext(
        Profile profile,
        List<Skill> skills,
        Plan currentPlan) {

    public CareerChatBusinessContext {
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
