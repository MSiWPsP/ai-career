package com.xucheng.aicareer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskStatusDTO {

    @NotNull(message = "任务状态不能为空")
    @Min(value = 0, message = "任务状态不能小于0")
    @Max(value = 3, message = "任务状态不能大于3")
    private Integer status;
}
