package com.xucheng.aicareer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "职业规划聊天会话")
public class CareerChatSessionVO {

    @Schema(description = "会话标识")
    private String conversationId;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "会话状态：0归档，1活跃")
    private Integer status;

    @Schema(description = "已完成消息数")
    private Integer messageCount;

    @Schema(description = "最近一条AI回复摘要")
    private String lastMessage;

    private LocalDateTime lastMessageAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
