package com.xucheng.aicareer.service.model;

/**
 * 从业务消息表恢复到 Spring AI ChatMemory 的最小消息结构。
 */
public record CareerChatMemoryEntry(String role, String content) {
}
