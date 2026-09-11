package com.xucheng.aicareer.agent.career.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoadmapStage {

    private Integer stage;
    private String name;
    private String goal;
    private String duration;
    private List<String> topics;
    private List<CareerTaskResult> tasks;
}
