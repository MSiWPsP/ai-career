package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AbilityScoreVO {

    private Long id;
    private String abilityName;
    private Integer score;
    private String sourceType;
    private Long sourceId;
    private LocalDateTime createTime;
}
