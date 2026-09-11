package com.xucheng.aicareer.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerRoadmapStageVO {

    private Integer stage;
    private String name;
    private String goal;
    private String duration;
    private List<String> topics;
}
