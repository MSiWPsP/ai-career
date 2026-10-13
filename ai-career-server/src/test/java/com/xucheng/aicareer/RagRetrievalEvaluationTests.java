package com.xucheng.aicareer;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** 使用真实 Embedding 和 PGVector 对扩展评测集做可重复检索评测；需显式开启以控制外部调用成本。 */
@EnabledIfEnvironmentVariable(named = "AI_RAG_EVAL", matches = "true")
@SpringBootTest(properties = {"ai.rag.enabled=true", "ai.rag.import=false",
        "spring.main.web-application-type=none"})
class RagRetrievalEvaluationTests {

    private static final Path DATASET = Path.of("../docs/知识库/检索评测集-v2.jsonl");

    @Autowired
    private PgKnowledgeRetrievalService retrievalService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void evaluatesRoutingRecallAndLatency() throws Exception {
        List<EvaluationCase> cases = loadCases();
        CareerChatBusinessContext context = new CareerChatBusinessContext(null, List.of(), null);
        int positives = 0;
        int negatives = 0;
        int routedCorrectly = 0;
        int documentHits = 0;
        int sectionHits = 0;
        int emptyResults = 0;
        List<Long> retrievalMillis = new ArrayList<>();
        List<String> misses = new ArrayList<>();

        for (EvaluationCase item : cases) {
            boolean routed = retrievalService.shouldRetrieve(item.question());
            if (routed == item.retrieve()) {
                routedCorrectly++;
            } else {
                misses.add(item.id() + " 路由错误 expected=" + item.retrieve() + " actual=" + routed);
            }
            if (!item.retrieve()) {
                negatives++;
                continue;
            }
            positives++;
            if (!routed) {
                continue;
            }
            long start = System.nanoTime();
            var result = retrievalService.retrieve(item.question(), context);
            retrievalMillis.add((System.nanoTime() - start) / 1_000_000);
            if (!result.hasKnowledge()) {
                emptyResults++;
            }
            boolean documentHit = result.references().stream()
                    .map(KnowledgeReference::documentId)
                    .anyMatch(item.expectedDocument()::equals);
            boolean sectionHit = result.references().stream()
                    .anyMatch(reference -> item.expectedDocument().equals(reference.documentId())
                            && item.expectedSection().equals(reference.section()));
            if (documentHit) {
                documentHits++;
            } else {
                misses.add(item.id() + " 文档未召回: " + item.expectedDocument());
            }
            if (sectionHit) {
                sectionHits++;
            } else {
                misses.add(item.id() + " 章节未召回: " + item.expectedSection());
            }
        }

        retrievalMillis.sort(Long::compareTo);
        System.out.printf("RAG 扩展评测: cases=%d positives=%d negatives=%d route=%d/%d "
                        + "documentRecall@5=%d/%d sectionRecall@5=%d/%d empty=%d p50=%dms p95=%dms%n",
                cases.size(), positives, negatives, routedCorrectly, cases.size(), documentHits, positives,
                sectionHits, positives, emptyResults, percentile(retrievalMillis, 0.50),
                percentile(retrievalMillis, 0.95));
        misses.forEach(miss -> System.out.println("RAG 评测问题: " + miss));

        assertThat(cases).hasSizeGreaterThanOrEqualTo(80);
        assertThat(positives).isGreaterThanOrEqualTo(60);
        assertThat(negatives).isGreaterThanOrEqualTo(20);
        assertThat(routedCorrectly).as("按需检索路由").isEqualTo(cases.size());
        assertThat(documentHits * 100.0 / positives).as("目标文档 Recall@5").isGreaterThanOrEqualTo(85.0);
    }

    private List<EvaluationCase> loadCases() throws Exception {
        List<EvaluationCase> cases = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (String line : Files.readAllLines(DATASET, StandardCharsets.UTF_8)) {
            EvaluationCase item = objectMapper.readValue(line, EvaluationCase.class);
            assertThat(item.id()).isNotBlank();
            assertThat(item.question()).isNotBlank();
            assertThat(ids.add(item.id())).as("重复评测 ID: " + item.id()).isTrue();
            if (item.retrieve()) {
                assertThat(item.expectedDocument()).isNotBlank();
                assertThat(item.expectedSection()).isNotBlank();
            }
            cases.add(item);
        }
        return cases;
    }

    private long percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) {
            return 0;
        }
        return values.get(Math.max(0, (int) Math.ceil(values.size() * percentile) - 1));
    }

    private record EvaluationCase(String id, String scenario, String question,
                                  String expectedDocument, String expectedSection, boolean retrieve) {
    }
}
