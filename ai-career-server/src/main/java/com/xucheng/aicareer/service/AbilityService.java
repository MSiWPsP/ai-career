package com.xucheng.aicareer.service;

import com.xucheng.aicareer.vo.AbilityRadarVO;
import com.xucheng.aicareer.vo.AbilityScoreVO;
import com.xucheng.aicareer.vo.AbilityTrendVO;
import com.xucheng.aicareer.vo.PageResultVO;

import java.util.Map;

public interface AbilityService {

    Map<String, Integer> getCurrentAbilities();

    PageResultVO<AbilityScoreVO> getAbilityHistory(String abilityName, int page, int pageSize);

    AbilityRadarVO getAbilityRadar();

    AbilityTrendVO getAbilityTrend(String abilityName);
}
