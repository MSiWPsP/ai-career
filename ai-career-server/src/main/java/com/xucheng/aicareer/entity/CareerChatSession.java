package com.xucheng.aicareer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("career_chat_session")
public class CareerChatSession {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String conversationId;
    private String title;
    private String summary;
    private Integer status;
    private Integer messageCount;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
