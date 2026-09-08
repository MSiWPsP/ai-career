package com.xucheng.aicareer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("career_task")
public class CareerTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Long careerPlanId;
    private String stageName;
    private String taskName;
    private String taskDescription;
    private String taskType;
    private Integer priority;
    private Integer status;
    private LocalDate startDate;
    private LocalDate deadline;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
