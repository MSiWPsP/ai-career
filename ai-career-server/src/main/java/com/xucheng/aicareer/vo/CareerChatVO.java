package com.xucheng.aicareer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "AI职业规划师普通聊天回复")
public class CareerChatVO {

    @Schema(description = "职业规划对话标识", example = "career:10001")
    private String conversationId;

    @Schema(description = "AI职业规划师回复内容")
    private String content;
}
