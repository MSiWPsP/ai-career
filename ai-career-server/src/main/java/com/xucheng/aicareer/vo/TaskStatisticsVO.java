package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskStatisticsVO {

    private Long total;
    private Long completed;
    private Long processing;
    private Long waiting;
    private Integer completionRate;
}
