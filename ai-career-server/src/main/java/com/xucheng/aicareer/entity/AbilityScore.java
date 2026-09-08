package com.xucheng.aicareer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ability_score")
public class AbilityScore {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String abilityName;
    private Integer score;
    private String sourceType;
    private Long sourceId;
    private LocalDateTime createTime;
}
