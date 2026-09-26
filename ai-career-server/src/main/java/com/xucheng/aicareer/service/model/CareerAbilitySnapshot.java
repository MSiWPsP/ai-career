package com.xucheng.aicareer.service.model;

import java.time.LocalDate;
import java.util.List;

/** 当前用户能力评分及有限历史记录的只读快照。 */
public record CareerAbilitySnapshot(List<AbilityItem> abilities) {

    public CareerAbilitySnapshot {
        abilities = abilities == null ? List.of() : List.copyOf(abilities);
    }

    public record AbilityItem(String abilityName, Integer currentScore, List<ScorePoint> recentScores) {
        public AbilityItem {
            recentScores = recentScores == null ? List.of() : List.copyOf(recentScores);
        }
    }

    public record ScorePoint(Integer score, LocalDate date) {
    }
}
