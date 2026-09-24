package com.xucheng.aicareer;

import com.xucheng.aicareer.agent.career.CareerPlannerAgent;
import com.xucheng.aicareer.service.impl.PgKnowledgeRetrievalService;
import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import com.xucheng.aicareer.service.model.KnowledgeReference;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** 真实模型回答的抽样材料生成器；事实与引用一致性仍必须由人逐条复核。 */
@EnabledIfEnvironmentVariable(named = "AI_RAG_ANSWER_EVAL", matches = "true")
@SpringBootTest(properties = {"ai.rag.enabled=true", "ai.rag.import=false",
        "spring.main.web-application-type=none"})
class RagAnswerEvaluationTests {

    private static final Path DATASET = Path.of("../docs/知识库/回答评测集-v1.jsonl");
    private static final Path REPORT = Path.of("target/rag-answer-eval.md");

    @Autowired
    private PgKnowledgeRetrievalService retrievalService;

    @Autowired
    private CareerPlannerAgent agent;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void samplesGroundedAnswersForHumanReview() throws Exception {
        List<AnswerCase> cases = Files.readAllLines(DATASET, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank())
                .map(line -> objectMapper.readValue(line, AnswerCase.class))
                .toList();
        assertThat(cases).hasSizeGreaterThanOrEqualTo(12);
        String requestedIds = System.getenv("AI_RAG_ANSWER_CASE_IDS");
        Set<String> selectedIds = requestedIds == null || requestedIds.isBlank()
                ? cases.subList(0, 6).stream().map(AnswerCase::id).collect(Collectors.toSet())
                : List.of(requestedIds.split(",")).stream().map(String::strip).collect(Collectors.toSet());
        List<AnswerCase> selected = cases.stream().filter(item -> selectedIds.contains(item.id())).toList();
        assertThat(selected).hasSize(selectedIds.size());
        CareerChatBusinessContext context = new CareerChatBusinessContext(null, List.of(), null);
        StringBuilder report = new StringBuilder("# 真实模型回答抽样（待人工复核）\n\n");
        Path reportPath = requestedIds == null || requestedIds.isBlank()
                ? REPORT : Path.of("target/rag-answer-eval-selected.md");

        for (AnswerCase item : selected) {
            var knowledge = retrievalService.retrieve(item.question(), context);
            if (item.expectedDocument() != null && !item.expectedDocument().isBlank()) {
                assertThat(knowledge.references()).extracting(KnowledgeReference::documentId)
                        .as(item.id() + " 目标文档召回")
                        .contains(item.expectedDocument());
            }
            String answer = agent.chat(0L, "rag-answer-eval-" + UUID.randomUUID(),
                    item.question(), context, knowledge.context());
            assertThat(answer).isNotBlank();
            report.append("## ").append(item.id()).append(" · ").append(item.scenario()).append("\n\n")
                    .append("问题：").append(item.question()).append("\n\n")
                    .append("人工核对目标：").append(item.expectedFact()).append("\n\n")
                    .append("本轮实际来源：");
            for (KnowledgeReference reference : knowledge.references()) {
                report.append("《").append(reference.title()).append("》· ").append(reference.section()).append("；");
            }
            report.append("\n\n本轮进入 Prompt 的片段：\n\n")
                    .append(knowledge.context())
                    .append("\n\n回答：\n\n").append(answer).append("\n\n");
            Files.writeString(reportPath, report.toString(), StandardCharsets.UTF_8);
        }

        System.out.println("RAG 回答抽样已生成: " + reportPath.toAbsolutePath());
    }

    private record AnswerCase(String id, String scenario, String question,
                              String expectedDocument, String expectedFact) {
    }
}
