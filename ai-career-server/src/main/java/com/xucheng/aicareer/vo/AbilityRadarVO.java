package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AbilityRadarVO {

    private List<RadarIndicatorVO> indicators;
    private List<Integer> values;
}
