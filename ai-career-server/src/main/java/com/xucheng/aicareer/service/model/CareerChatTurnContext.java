package com.xucheng.aicareer.service.model;

public record CareerChatTurnContext(
        Long sessionId,
        Long userId,
        String conversationId,
        String clientMessageId,
        String replayContent) {
}
