package com.xucheng.aicareer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_message")
public class InterviewMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long interviewId;
    private Long userId;
    private String role;
    private String content;
    private String questionCategory;
    private String questionLevel;
    private Integer score;
    private String evaluation;
    private Integer messageOrder;
    private LocalDateTime createTime;
}
