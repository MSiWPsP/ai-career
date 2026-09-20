package com.xucheng.aicareer.service;

import com.xucheng.aicareer.service.impl.MarkdownKnowledgeChunker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownKnowledgeChunkerTests {

    @Test
    void preservesSectionBoundariesAndStableIndices() {
        var chunks = new MarkdownKnowledgeChunker().split("""
                # 文档标题
                ## 语言基础
                Java 集合与并发。

                ## 工程交付
                接口测试和部署说明。
                """);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).index()).isZero();
        assertThat(chunks.get(0).section()).isEqualTo("语言基础");
        assertThat(chunks.get(0).content()).contains("Java 集合").doesNotContain("部署说明");
        assertThat(chunks.get(1).index()).isEqualTo(1);
        assertThat(chunks.get(1).section()).isEqualTo("工程交付");
    }
}
