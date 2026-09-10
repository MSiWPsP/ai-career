package com.xucheng.aicareer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "职业规划师流式聊天事件")
public class CareerChatStreamVO {

    public static final String TYPE_DELTA = "delta";
    public static final String TYPE_DONE = "done";
    public static final String TYPE_ERROR = "error";

    @Schema(description = "事件类型", allowableValues = {TYPE_DELTA, TYPE_DONE, TYPE_ERROR})
    private String type;

    @Schema(description = "会话标识", example = "career:10001")
    private String conversationId;

    @Schema(description = "本次增量文本或错误提示")
    private String content;

    public static CareerChatStreamVO delta(String conversationId, String content) {
        return CareerChatStreamVO.builder()
                .type(TYPE_DELTA)
                .conversationId(conversationId)
                .content(content)
                .build();
    }

    public static CareerChatStreamVO done(String conversationId) {
        return CareerChatStreamVO.builder()
                .type(TYPE_DONE)
                .conversationId(conversationId)
                .build();
    }

    public static CareerChatStreamVO error(String conversationId, String content) {
        return CareerChatStreamVO.builder()
                .type(TYPE_ERROR)
                .conversationId(conversationId)
                .content(content)
                .build();
    }
}
