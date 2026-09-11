package com.xucheng.aicareer.agent.career.dto;

import lombok.Data;

import java.util.List;

@Data
public class CareerPlanResult {

    private String targetPosition;
    private Integer matchScore;
    private String summary;
    private List<String> advantages;
    private List<String> weaknesses;
    private List<RoadmapStage> roadmap;
}
