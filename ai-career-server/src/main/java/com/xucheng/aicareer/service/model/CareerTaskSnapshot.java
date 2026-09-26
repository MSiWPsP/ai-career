package com.xucheng.aicareer.service.model;

import java.time.LocalDate;
import java.util.List;

/** 当前用户生效规划下的任务只读快照，供 Agent Tool 使用。 */
public record CareerTaskSnapshot(
        long total,
        long completed,
        long processing,
        long waiting,
        int completionRate,
        List<TaskItem> tasks) {

    public CareerTaskSnapshot {
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }

    public record TaskItem(
            String stageName,
            String taskName,
            Integer priority,
            Integer status,
            LocalDate deadline) {
    }
}
