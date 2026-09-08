package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AbilityTrendVO {

    private String abilityName;
    private List<AbilityTrendRecordVO> records;
}
