package com.xucheng.aicareer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_skill")
public class UserSkill {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String skillName;
    private String skillCategory;
    private Integer level;
    private Integer score;
    private String source;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
