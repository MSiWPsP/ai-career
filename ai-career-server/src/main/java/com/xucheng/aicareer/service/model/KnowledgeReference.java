package com.xucheng.aicareer.service.model;

/** 本轮实际进入 Agent Prompt 的切片来源快照；版本、序号与正文哈希供后续核对。 */
public record KnowledgeReference(String documentId, String title, String section, String sourceName,
                                 Integer documentVersion, Integer chunkIndex, String contentSha256) {

    public KnowledgeReference(String documentId, String title, String section, String sourceName) {
        this(documentId, title, section, sourceName, null, null, null);
    }
}
