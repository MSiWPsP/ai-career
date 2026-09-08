package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AbilityTrendRecordVO {

    private Integer score;
    private LocalDate date;
}
