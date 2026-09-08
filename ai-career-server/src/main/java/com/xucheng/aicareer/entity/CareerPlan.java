package com.xucheng.aicareer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("career_plan")
public class CareerPlan {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Integer version;
    private String targetPosition;
    private Integer matchScore;
    private String summary;
    private String advantages;
    private String weaknesses;
    private String roadmap;
    private Integer status;
    private Long sourceInterviewId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
