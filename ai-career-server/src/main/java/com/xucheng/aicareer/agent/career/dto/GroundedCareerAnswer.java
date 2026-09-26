package com.xucheng.aicareer.agent.career.dto;

import java.util.List;

/**
 * 职业咨询回答的受限草稿。知识事实和平台个人数据分别携带可核验的证据编号，
 * Service 在向用户发送内容前逐项验证，建议则单独列为待验证行动。
 */
public record GroundedCareerAnswer(
        List<SourceExcerpt> excerpts,
        List<ToolExcerpt> toolExcerpts,
        List<String> optionalActions) {

    /** 兼容不启用 Tool Calling 的现有 RAG 调用。 */
    public GroundedCareerAnswer(List<SourceExcerpt> excerpts, List<String> optionalActions) {
        this(excerpts, List.of(), optionalActions);
    }

    public GroundedCareerAnswer {
        excerpts = excerpts == null ? List.of() : List.copyOf(excerpts);
        toolExcerpts = toolExcerpts == null ? List.of() : List.copyOf(toolExcerpts);
        optionalActions = optionalActions == null ? List.of() : List.copyOf(optionalActions);
    }

    public record SourceExcerpt(Integer sourceIndex, String quote) {
    }

    public record ToolExcerpt(String evidenceId, String quote) {
    }
}
