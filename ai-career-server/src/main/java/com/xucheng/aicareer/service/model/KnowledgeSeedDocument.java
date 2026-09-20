package com.xucheng.aicareer.service.model;

/** 受控知识目录中的文档元数据，字段由维护者审核，不接受用户输入。 */
public record KnowledgeSeedDocument(
        String id, String file, String title, String category, String targetPosition,
        String sourceType, String sourceName, int version, String reviewedAt, String expiresAt) {
}
