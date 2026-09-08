package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CareerPlanVO {

    private Long id;
    private Integer version;
    private String targetPosition;
    private Integer matchScore;
    private String summary;
    private String advantages;
    private String weaknesses;
    private String roadmap;
    private Integer status;
    private Long sourceInterviewId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
