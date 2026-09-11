package com.xucheng.aicareer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("career_chat_message")
public class CareerChatMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long sessionId;
    private Long userId;
    private String clientMessageId;
    private String role;
    private String content;
    private Integer status;
    private Integer messageOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
