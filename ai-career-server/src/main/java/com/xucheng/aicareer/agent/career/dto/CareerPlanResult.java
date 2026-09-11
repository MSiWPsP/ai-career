package com.xucheng.aicareer.agent.career.dto;

import lombok.Data;

import java.util.List;

/**
 * 模型生成的结构化职业规划结果。
 *
 * <p>该对象是 Agent 输出边界，业务 Service 校验并转换后才会写入职业规划表。</p>
 */
@Data
public class CareerPlanResult {

    /** 推荐或沿用的目标岗位。 */
    private String targetPosition;
    /** 当前能力与目标岗位的匹配度，取值 0～100。 */
    private Integer matchScore;
    /** 对当前职业发展状态的总体说明。 */
    private String summary;
    /** 已具备的主要优势。 */
    private List<String> advantages;
    /** 当前需要补齐的主要短板。 */
    private List<String> weaknesses;
    /** 按执行顺序排列的成长路线。 */
    private List<RoadmapStage> roadmap;
}
