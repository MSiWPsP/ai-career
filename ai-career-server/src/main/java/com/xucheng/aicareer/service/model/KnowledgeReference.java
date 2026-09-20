package com.xucheng.aicareer.service.model;

/** 本轮检索实际进入 Agent Prompt 的知识来源；不包含私有存储地址和向量标识。 */
public record KnowledgeReference(String documentId, String title, String section, String sourceName) {
}
