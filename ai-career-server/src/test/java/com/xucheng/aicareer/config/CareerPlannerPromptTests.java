package com.xucheng.aicareer.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CareerPlannerPromptTests {

    @Test
    void ordinaryChatPromptAllowsConciseMarkdown() throws IOException {
        String prompt = readPrompt("prompts/career-planner-system.md");

        assertThat(prompt)
                .contains("可按内容需要使用简洁的 Markdown")
                .contains("带语言标识的代码块")
                .doesNotContain("使用纯文本回答");
    }

    @Test
    void structuredPlanPromptStillRejectsMarkdown() throws IOException {
        String prompt = readPrompt("prompts/career-plan-generation-system.md");

        assertThat(prompt).contains("不输出推理过程、Markdown 或额外说明");
    }

    @Test
    void interviewerPromptRequiresDynamicSingleQuestionAndStructuredOutput() throws IOException {
        String prompt = readPrompt("prompts/interviewer-system.md");

        assertThat(prompt)
                .contains("一次只提出一个主要问题")
                .contains("{interviewContext}")
                .contains("FOLLOW_UP")
                .contains("严格返回结构化对象")
                .contains("不输出 Markdown");
    }

    private String readPrompt(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
