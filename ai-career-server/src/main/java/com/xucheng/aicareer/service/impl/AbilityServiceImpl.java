package com.xucheng.aicareer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xucheng.aicareer.entity.AbilityScore;
import com.xucheng.aicareer.mapper.AbilityScoreMapper;
import com.xucheng.aicareer.service.AbilityService;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.AbilityRadarVO;
import com.xucheng.aicareer.vo.AbilityScoreVO;
import com.xucheng.aicareer.vo.AbilityTrendRecordVO;
import com.xucheng.aicareer.vo.AbilityTrendVO;
import com.xucheng.aicareer.vo.PageResultVO;
import com.xucheng.aicareer.vo.RadarIndicatorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AbilityServiceImpl implements AbilityService {

    private static final int RADAR_MAX_SCORE = 100;

    private final AbilityScoreMapper abilityScoreMapper;

    @Override
    public Map<String, Integer> getCurrentAbilities() {
        List<AbilityScore> scores = abilityScoreMapper.selectList(Wrappers.<AbilityScore>lambdaQuery()
                .eq(AbilityScore::getUserId, UserContext.getUserId())
                .orderByDesc(AbilityScore::getCreateTime)
                .orderByDesc(AbilityScore::getId));
        Map<String, Integer> currentAbilities = new LinkedHashMap<>();
        scores.forEach(score -> currentAbilities.putIfAbsent(score.getAbilityName(), score.getScore()));
        return currentAbilities;
    }

    @Override
    public PageResultVO<AbilityScoreVO> getAbilityHistory(String abilityName, int page, int pageSize) {
        Long userId = UserContext.getUserId();
        String normalizedAbilityName = normalizeAbilityName(abilityName);
        long total = abilityScoreMapper.selectCount(Wrappers.<AbilityScore>lambdaQuery()
                .eq(AbilityScore::getUserId, userId)
                .eq(normalizedAbilityName != null, AbilityScore::getAbilityName, normalizedAbilityName));
        if (total == 0) {
            return PageResultVO.<AbilityScoreVO>builder()
                    .records(List.of())
                    .total(0L)
                    .build();
        }

        long offset = (long) (page - 1) * pageSize;
        List<AbilityScoreVO> records = abilityScoreMapper.selectList(Wrappers.<AbilityScore>lambdaQuery()
                        .eq(AbilityScore::getUserId, userId)
                        .eq(normalizedAbilityName != null, AbilityScore::getAbilityName, normalizedAbilityName)
                        .orderByDesc(AbilityScore::getCreateTime)
                        .orderByDesc(AbilityScore::getId)
                        .last("LIMIT " + offset + ", " + pageSize))
                .stream()
                .map(this::toAbilityScoreVO)
                .toList();
        return PageResultVO.<AbilityScoreVO>builder()
                .records(records)
                .total(total)
                .build();
    }

    @Override
    public AbilityRadarVO getAbilityRadar() {
        Map<String, Integer> currentAbilities = getCurrentAbilities();
        return AbilityRadarVO.builder()
                .indicators(currentAbilities.keySet().stream()
                        .map(name -> RadarIndicatorVO.builder()
                                .name(name)
                                .max(RADAR_MAX_SCORE)
                                .build())
                        .toList())
                .values(List.copyOf(currentAbilities.values()))
                .build();
    }

    @Override
    public AbilityTrendVO getAbilityTrend(String abilityName) {
        Long userId = UserContext.getUserId();
        String normalizedAbilityName = normalizeAbilityName(abilityName);
        if (normalizedAbilityName == null) {
            AbilityScore latest = abilityScoreMapper.selectOne(Wrappers.<AbilityScore>lambdaQuery()
                    .select(AbilityScore::getAbilityName)
                    .eq(AbilityScore::getUserId, userId)
                    .orderByDesc(AbilityScore::getCreateTime)
                    .orderByDesc(AbilityScore::getId)
                    .last("LIMIT 1"));
            normalizedAbilityName = latest == null ? null : latest.getAbilityName();
        }
        if (normalizedAbilityName == null) {
            return AbilityTrendVO.builder()
                    .abilityName(null)
                    .records(List.of())
                    .build();
        }

        String targetAbilityName = normalizedAbilityName;
        List<AbilityTrendRecordVO> records = abilityScoreMapper.selectList(Wrappers.<AbilityScore>lambdaQuery()
                        .eq(AbilityScore::getUserId, userId)
                        .eq(AbilityScore::getAbilityName, targetAbilityName)
                        .orderByAsc(AbilityScore::getCreateTime)
                        .orderByAsc(AbilityScore::getId))
                .stream()
                .map(score -> AbilityTrendRecordVO.builder()
                        .score(score.getScore())
                        .date(score.getCreateTime().toLocalDate())
                        .build())
                .toList();
        return AbilityTrendVO.builder()
                .abilityName(targetAbilityName)
                .records(records)
                .build();
    }

    private String normalizeAbilityName(String abilityName) {
        return StringUtils.hasText(abilityName) ? abilityName.trim() : null;
    }

    private AbilityScoreVO toAbilityScoreVO(AbilityScore score) {
        return AbilityScoreVO.builder()
                .id(score.getId())
                .abilityName(score.getAbilityName())
                .score(score.getScore())
                .sourceType(score.getSourceType())
                .sourceId(score.getSourceId())
                .createTime(score.getCreateTime())
                .build();
    }
}
