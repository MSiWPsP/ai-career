package com.xucheng.aicareer.service.model;

/**
 * 一次聊天请求在持久化层和 Agent 层之间传递的上下文。
 *
 * @param replayContent 已存在的完整 AI 回复；非空表示本次请求命中幂等重放，无需再次调用模型
 */
public record CareerChatTurnContext(
        Long sessionId,
        Long userId,
        String conversationId,
        String clientMessageId,
        String replayContent) {
}
