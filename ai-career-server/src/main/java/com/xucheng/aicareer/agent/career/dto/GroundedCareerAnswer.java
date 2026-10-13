package com.xucheng.aicareer.agent.career.dto;

import java.util.List;

/** RAG 回答的受限草稿：事实只以本轮知识片段的原文短摘录表达，建议另列为待验证行动。 */
public record GroundedCareerAnswer(List<SourceExcerpt> excerpts, List<String> optionalActions) {

    public record SourceExcerpt(Integer sourceIndex, String quote) {
    }
}
