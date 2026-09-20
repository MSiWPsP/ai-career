package com.xucheng.aicareer.service.model;

import java.util.List;

/** 检索结果与引用随同一轮请求传递，避免模型自行编造来源。 */
public record KnowledgeRetrievalResult(String context, List<KnowledgeReference> references) {

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
        StringBuilder footer = new StringBuilder("\n\n参考依据\n");
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
}
