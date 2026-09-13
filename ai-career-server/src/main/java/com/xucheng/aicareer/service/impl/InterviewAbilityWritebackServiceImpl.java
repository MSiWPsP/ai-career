package com.xucheng.aicareer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xucheng.aicareer.entity.AbilityScore;
import com.xucheng.aicareer.entity.UserSkill;
import com.xucheng.aicareer.mapper.AbilityScoreMapper;
import com.xucheng.aicareer.mapper.UserSkillMapper;
import com.xucheng.aicareer.service.InterviewAbilityWritebackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 面试评分写回实现：保留能力时间序列，不用一次面试成绩直接覆盖长期技能画像。 */
@Service
@RequiredArgsConstructor
public class InterviewAbilityWritebackServiceImpl implements InterviewAbilityWritebackService {

    private static final String INTERVIEW_SOURCE = "INTERVIEW";
    private final AbilityScoreMapper abilityScoreMapper;
    private final UserSkillMapper userSkillMapper;

    @Override
    public void writeback(Long userId, Long interviewId, Map<String, Integer> scores) {
        // 报告与能力回写使用同一事务，整份报告只融合一次，防止重试时 0.6/0.4 权重被重复应用。
        if (abilityScoreMapper.exists(Wrappers.<AbilityScore>lambdaQuery()
                .eq(AbilityScore::getUserId, userId)
                .eq(AbilityScore::getSourceType, INTERVIEW_SOURCE)
                .eq(AbilityScore::getSourceId, interviewId))) return;

        List<UserSkill> skills = userSkillMapper.selectList(Wrappers.<UserSkill>lambdaQuery()
                .eq(UserSkill::getUserId, userId));
        Map<Long, List<Integer>> skillAssessments = new HashMap<>();
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            String abilityName = entry.getKey().trim();
            UserSkill matchingSkill = matchSkill(skills, abilityName);
            Integer previous = matchingSkill == null ? latestAbilityScore(userId, abilityName) : matchingSkill.getScore();
            int score = previous == null ? entry.getValue()
                    : Math.round(previous * 0.6f + entry.getValue() * 0.4f);

            AbilityScore ability = new AbilityScore();
            ability.setUserId(userId);
            ability.setAbilityName(abilityName);
            ability.setScore(score);
            ability.setSourceType(INTERVIEW_SOURCE);
            ability.setSourceId(interviewId);
            abilityScoreMapper.insert(ability);

            if (matchingSkill != null) {
                skillAssessments.computeIfAbsent(matchingSkill.getId(), ignored -> new ArrayList<>())
                        .add(entry.getValue());
            }
        }
        for (UserSkill skill : skills) {
            List<Integer> assessments = skillAssessments.get(skill.getId());
            if (assessments == null) continue;
            // 同一技能可能关联多个面试分项，先平均本次面试评分，再只做一次融合。
            int interviewAverage = Math.round((float) assessments.stream().mapToInt(Integer::intValue).average()
                    .orElseThrow());
            int score = Math.round(skill.getScore() * 0.6f + interviewAverage * 0.4f);
            skill.setScore(score);
            skill.setLevel(Math.max(1, Math.min(5, (score + 19) / 20)));
            skill.setSource(INTERVIEW_SOURCE);
            userSkillMapper.updateById(skill);
        }
    }

    private Integer latestAbilityScore(Long userId, String abilityName) {
        AbilityScore latest = abilityScoreMapper.selectOne(Wrappers.<AbilityScore>lambdaQuery()
                .eq(AbilityScore::getUserId, userId)
                .eq(AbilityScore::getAbilityName, abilityName)
                .orderByDesc(AbilityScore::getCreateTime)
                .orderByDesc(AbilityScore::getId)
                .last("LIMIT 1"));
        return latest == null ? null : latest.getScore();
    }

    private UserSkill matchSkill(List<UserSkill> skills, String abilityName) {
        String normalized = abilityName.toLowerCase(Locale.ROOT);
        UserSkill exact = skills.stream().filter(skill -> skill.getSkillName().trim()
                .equalsIgnoreCase(abilityName)).findFirst().orElse(null);
        if (exact != null) return exact;
        // 模型可能将“Java”细化成“Java集合框架”；仅对长度足够且唯一的包含匹配融合自评技能。
        List<UserSkill> candidates = skills.stream()
                .filter(skill -> skill.getSkillName().trim().length() >= 3)
                .filter(skill -> normalized.contains(skill.getSkillName().trim().toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparingInt((UserSkill skill) -> skill.getSkillName().length()).reversed())
                .toList();
        return candidates.size() == 1 || (candidates.size() > 1
                && candidates.get(0).getSkillName().length() > candidates.get(1).getSkillName().length())
                ? candidates.get(0) : null;
    }
}
