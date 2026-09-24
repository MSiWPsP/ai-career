package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.agent.career.CareerPlannerAgent;
import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 真实模型结构化草稿与交付内容抽样；支持关系仍需人工审查。 */
@EnabledIfEnvironmentVariable(named = "AI_RAG_STRUCTURED_EVAL", matches = "true")
@SpringBootTest(properties = {"ai.rag.enabled=true", "ai.rag.import=false",
        "spring.main.web-application-type=none"})
class RagStructuredAnswerEvaluationTests {

    @Autowired
    private PgKnowledgeRetrievalService retrieval;
    @Autowired
    private CareerPlannerAgent agent;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void collectsGroundedReplies() throws Exception {
        Set<String> ids = Set.of(System.getenv().getOrDefault("AI_RAG_STRUCTURED_CASE_IDS", "A003,A004,A006")
                .split(","));
        List<AnswerCase> cases = Files.readAllLines(Path.of("../docs/知识库/回答评测集-v1.jsonl"),
                        StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank())
                .map(line -> objectMapper.readValue(line, AnswerCase.class))
                .filter(item -> ids.contains(item.id()))
                .toList();
        assertThat(cases).hasSize(ids.size());
        CareerChatBusinessContext context = new CareerChatBusinessContext(null, List.of(), null);
        Path reportPath = Path.of("target/rag-structured-answer-eval.md");
        StringBuilder report = new StringBuilder("# 结构化 RAG 回答抽样\n\n");
        int groundedCases = 0;
        int noKnowledgeCases = 0;
        for (AnswerCase item : cases) {
            var knowledge = retrieval.retrieve(item.question(), context);
            if (!knowledge.hasKnowledge()) {
                assertThat(item.expectedDocument())
                        .as("%s 预期应命中知识文档", item.id()).isBlank();
                noKnowledgeCases++;
                String raw = agent.chat(0L, "rag-structured-eval-" + UUID.randomUUID(),
                        item.question(), context, "");
                String delivered = RagAnswerFactGuard.requiresPreflight(item.question())
                        ? RagAnswerFactGuard.review(raw).content() : raw;
                report.append("## ").append(item.id()).append("\n\n问题：").append(item.question())
                        .append("\n\n人工核对目标：").append(item.expectedFact())
                        .append("\n\n无知识片段，普通聊天交付内容：\n\n")
                        .append(delivered).append("\n\n");
                Files.writeString(reportPath, report.toString(), StandardCharsets.UTF_8);
                continue;
            }
            var draft = agent.chatGrounded(0L, "rag-structured-eval-" + UUID.randomUUID(),
                    item.question(), context, knowledge.context());
            var delivered = GroundedCareerAnswerComposer.compose(draft, knowledge);
            assertThat(delivered.accepted()).as("%s 结构化摘录校验", item.id()).isTrue();
            assertThat(delivered.references()).extracting(reference -> reference.documentId())
                    .as("%s 交付回答必须实际引用目标文档", item.id())
                    .contains(item.expectedDocument());
            assertThat(delivered.references()).allSatisfy(reference -> {
                assertThat(reference.documentVersion()).isPositive();
                assertThat(reference.chunkIndex()).isNotNegative();
                assertThat(reference.contentSha256()).hasSize(64);
            });
            groundedCases++;
            report.append("## ").append(item.id()).append("\n\n问题：").append(item.question())
                    .append("\n\n人工核对目标：").append(item.expectedFact())
                    .append("\n\n本轮片段：\n\n").append(knowledge.context())
                    .append("\n\n结构校验：").append(delivered.accepted() ? "通过" : "拒绝")
                    .append("；目标文档引用：通过")
                    .append("\n\n交付回答：\n\n").append(delivered.content()).append("\n\n");
            Files.writeString(reportPath, report.toString(), StandardCharsets.UTF_8);
        }
        report.append("## 自动校验汇总\n\n")
                .append("- 有依据并通过逐字摘录、目标文档和来源指纹校验：")
                .append(groundedCases).append(" 条\n")
                .append("- 按评测集预期不展示知识来源：").append(noKnowledgeCases).append(" 条\n")
                .append("- 人工事实核对目标总数：").append(cases.size()).append(" 条\n");
        Files.writeString(reportPath, report.toString(), StandardCharsets.UTF_8);
        System.out.println("结构化回答抽样已生成: " + reportPath.toAbsolutePath());
    }

    private record AnswerCase(String id, String scenario, String question,
                              String expectedDocument, String expectedFact) {
    }
}
