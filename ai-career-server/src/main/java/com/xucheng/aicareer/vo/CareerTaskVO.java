package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CareerTaskVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long careerPlanId;
    private String stageName;
    private String taskName;
    private String taskDescription;
    private String taskType;
    private Integer priority;
    private Integer status;
    private LocalDate startDate;
    private LocalDate deadline;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
