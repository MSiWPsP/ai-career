package com.xucheng.aicareer.agent.career.dto;

import lombok.Data;

import java.util.List;

/** 模型生成的单个职业成长路线阶段。 */
@Data
public class RoadmapStage {

    /** 从 1 开始且连续递增的阶段编号。 */
    private Integer stage;
    /** 阶段名称。 */
    private String name;
    /** 阶段完成后应达到的目标。 */
    private String goal;
    /** 建议执行周期。 */
    private String duration;
    /** 本阶段需要学习或巩固的主题。 */
    private List<String> topics;
    /** 本阶段对应的可执行任务。 */
    private List<CareerTaskResult> tasks;
}
