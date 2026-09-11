package com.xucheng.aicareer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * SSE 职业规划聊天事件。
 *
 * <p>delta 携带文本增量，done 表示本轮已完整落库，error 表示本轮失败且可以使用原 clientMessageId 重试。</p>
 */
@Data
@Builder
@Schema(description = "职业规划师流式聊天事件")
public class CareerChatStreamVO {

    public static final String TYPE_DELTA = "delta";
    public static final String TYPE_DONE = "done";
    public static final String TYPE_ERROR = "error";

    @Schema(description = "事件类型", allowableValues = {TYPE_DELTA, TYPE_DONE, TYPE_ERROR})
    private String type;

    @Schema(description = "会话标识", example = "career:10001:2c08d11b-88b9-4d63-8cc3-0a79d86e4695")
    private String conversationId;

    @Schema(description = "客户端消息标识，用于安全重试")
    private String clientMessageId;

    @Schema(description = "本次增量文本或错误提示")
    private String content;

    public static CareerChatStreamVO delta(String conversationId, String content) {
        return delta(conversationId, null, content);
    }

    public static CareerChatStreamVO delta(String conversationId, String clientMessageId, String content) {
        return CareerChatStreamVO.builder()
                .type(TYPE_DELTA)
                .conversationId(conversationId)
                .clientMessageId(clientMessageId)
                .content(content)
                .build();
    }

    public static CareerChatStreamVO done(String conversationId) {
        return done(conversationId, null);
    }

    public static CareerChatStreamVO done(String conversationId, String clientMessageId) {
        return CareerChatStreamVO.builder()
                .type(TYPE_DONE)
                .conversationId(conversationId)
                .clientMessageId(clientMessageId)
                .build();
    }

    public static CareerChatStreamVO error(String conversationId, String content) {
        return error(conversationId, null, content);
    }

    public static CareerChatStreamVO error(String conversationId, String clientMessageId, String content) {
        return CareerChatStreamVO.builder()
                .type(TYPE_ERROR)
                .conversationId(conversationId)
                .clientMessageId(clientMessageId)
                .content(content)
                .build();
    }
}
