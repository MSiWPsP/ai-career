package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.service.model.KnowledgeChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 按 Markdown 二级标题切片，避免将不同主题硬拼接后再截断。 */
@Component
public class MarkdownKnowledgeChunker {

    private static final int MAX_CHARS = 900;

    public List<KnowledgeChunk> split(String markdown) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String section = "概述";
        StringBuilder body = new StringBuilder();
        for (String line : markdown.replace("\r\n", "\n").split("\n", -1)) {
            if (line.startsWith("## ")) {
                addSection(chunks, section, body.toString());
                section = line.substring(3).trim();
                body.setLength(0);
            } else if (!line.startsWith("# ")) {
                body.append(line).append('\n');
            }
        }
        addSection(chunks, section, body.toString());
        return chunks;
    }

    private void addSection(List<KnowledgeChunk> chunks, String section, String text) {
        String content = text.trim();
        if (content.isEmpty()) {
            return;
        }
        // 首期来源为受控 Markdown。超长章节按段落切分，避免触发 Embedding 输入上限。
        StringBuilder part = new StringBuilder();
        for (String paragraph : content.split("\\n\\s*\\n")) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!part.isEmpty() && part.length() + trimmed.length() + 2 > MAX_CHARS) {
                chunks.add(new KnowledgeChunk(chunks.size(), section, part.toString()));
                part.setLength(0);
            }
            if (trimmed.length() > MAX_CHARS) {
                if (!part.isEmpty()) {
                    chunks.add(new KnowledgeChunk(chunks.size(), section, part.toString()));
                    part.setLength(0);
                }
                for (int start = 0; start < trimmed.length(); start += MAX_CHARS) {
                    chunks.add(new KnowledgeChunk(chunks.size(), section,
                            trimmed.substring(start, Math.min(start + MAX_CHARS, trimmed.length()))));
                }
            } else {
                if (!part.isEmpty()) {
                    part.append("\n\n");
                }
                part.append(trimmed);
            }
        }
        if (!part.isEmpty()) {
            chunks.add(new KnowledgeChunk(chunks.size(), section, part.toString()));
        }
    }
}
