package com.xucheng.aicareer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 职业规划聊天消息实体。
 *
 * <p>同一 sessionId、clientMessageId 和 role 的组合在数据库中唯一，用于保证一轮问答幂等。</p>
 */
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
    private String referencesJson;
    private Integer status;
    private Integer messageOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
