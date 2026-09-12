package com.xucheng.aicareer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview")
public class Interview {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String targetPosition;
    private String interviewType;
    private String difficulty;
    private Integer status;
    private String conversationId;
    private Integer questionCount;
    private Integer maxQuestions;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
