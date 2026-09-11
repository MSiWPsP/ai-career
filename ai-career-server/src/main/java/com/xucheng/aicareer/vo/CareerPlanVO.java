package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 已持久化职业规划的完整业务视图。 */
@Data
@Builder
public class CareerPlanVO {

    private Long id;
    private Integer version;
    private String targetPosition;
    private Integer matchScore;
    private String summary;
    private List<String> advantages;
    private List<String> weaknesses;
    private List<CareerRoadmapStageVO> roadmap;
    private Integer status;
    private Long sourceInterviewId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
