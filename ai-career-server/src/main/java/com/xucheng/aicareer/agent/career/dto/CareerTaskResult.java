package com.xucheng.aicareer.agent.career.dto;

import lombok.Data;

/** 模型在路线阶段中生成的可执行成长任务。 */
@Data
public class CareerTaskResult {

    /** 任务名称。 */
    private String taskName;
    /** 任务目标、完成标准或补充说明。 */
    private String description;
    /** 任务类型：KNOWLEDGE、PROJECT、INTERVIEW 或 CAREER。 */
    private String taskType;
    /** 优先级：1 高、2 中、3 低。 */
    private Integer priority;
}
