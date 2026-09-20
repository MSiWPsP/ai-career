package com.xucheng.aicareer.service.model;

import java.util.List;

/** 检索结果与引用随同一轮请求传递，避免模型自行编造来源。 */
public record KnowledgeRetrievalResult(String context, List<KnowledgeReference> references) {

    private static final String CITATION_MARKER = "\n\n<!-- ai-career-knowledge-references -->\n参考依据\n";

    public static KnowledgeRetrievalResult empty() {
        return new KnowledgeRetrievalResult("", List.of());
    }

    public boolean hasKnowledge() {
        return !references.isEmpty();
    }

    public String citationFooter() {
        if (!hasKnowledge()) {
            return "";
        }
        // 机器标记仅用于历史消息恢复来源展示；Markdown 渲染时不会显示。
        StringBuilder footer = new StringBuilder(CITATION_MARKER);
        for (int index = 0; index < references.size(); index++) {
            KnowledgeReference reference = references.get(index);
            footer.append(index + 1).append(". 《").append(reference.title()).append("》");
            if (reference.section() != null && !reference.section().isBlank()) {
                footer.append("· ").append(reference.section());
            }
            footer.append('\n');
        }
        return footer.toString().stripTrailing();
    }

    /** 会话摘要只显示回答正文，避免把用于历史来源恢复的标记和尾注暴露到侧栏。 */
    public static String answerBody(String content) {
        int markerAt = content.lastIndexOf(CITATION_MARKER);
        return markerAt < 0 ? content : content.substring(0, markerAt);
    }
}
