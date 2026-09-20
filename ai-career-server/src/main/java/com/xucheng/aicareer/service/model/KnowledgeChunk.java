package com.xucheng.aicareer.service.model;

/** 标题感知的文档切片；编号与文档版本共同决定稳定向量 ID。 */
public record KnowledgeChunk(int index, String section, String content) {
}
