package com.xucheng.aicareer.service.model;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 检索结果与引用随同一轮请求传递，避免模型自行编造来源。 */
public record KnowledgeRetrievalResult(String context, List<KnowledgeReference> references) {

    private static final String CITATION_MARKER = "\n\n<!-- ai-career-knowledge-references -->\n参考依据\n";
    private static final String MARKER_TOKEN = "<!-- ai-career-knowledge-references -->";
    private static final Pattern STORED_SOURCE = Pattern.compile("^\\d+\\. 《([^》\\n]+)》(?:·\\s*(.+))?$");

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

    /** 模型输出不能携带内部来源标记；只有服务端追加的尾注可在历史消息中恢复为来源。 */
    public static String sanitizeGeneratedAnswer(String content) {
        return content.replace(MARKER_TOKEN, "");
    }

    /** SSE 重试回放返回历史尾注；旧消息尚无签名，不能视为审计级来源证明。 */
    public static List<KnowledgeReference> storedReferences(String content) {
        int markerAt = content.lastIndexOf(CITATION_MARKER);
        if (markerAt < 0) {
            return List.of();
        }
        String[] lines = content.substring(markerAt + CITATION_MARKER.length()).strip().split("\\R");
        List<KnowledgeReference> references = new ArrayList<>();
        for (String line : lines) {
            Matcher match = STORED_SOURCE.matcher(line.strip());
            if (!match.matches()) {
                return List.of();
            }
            references.add(new KnowledgeReference("", match.group(1),
                    match.group(2) == null ? "" : match.group(2).strip(), ""));
        }
        return List.copyOf(references);
    }

    /** 会话摘要只显示回答正文，避免把用于历史来源恢复的标记和尾注暴露到侧栏。 */
    public static String answerBody(String content) {
        int markerAt = content.lastIndexOf(CITATION_MARKER);
        return markerAt < 0 ? content : content.substring(0, markerAt);
    }
}
