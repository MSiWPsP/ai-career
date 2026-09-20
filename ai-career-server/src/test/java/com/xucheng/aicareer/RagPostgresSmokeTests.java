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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 显式开启的本地 PGVector + Embedding 冒烟测试，不依赖 CI 中存在外部模型或 PostgreSQL。 */
@EnabledIfEnvironmentVariable(named = "AI_RAG_TEST", matches = "true")
@SpringBootTest(properties = {"ai.rag.enabled=true", "ai.rag.import=false",
        "spring.main.web-application-type=none"})
class RagPostgresSmokeTests {

    @Autowired
    private PgKnowledgeRetrievalService retrievalService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void retrievesPublishedCareerKnowledgeAndSkipsLocalOnlyQuestion() {
        CareerChatBusinessContext context = new CareerChatBusinessContext(
                new CareerChatBusinessContext.Profile(null, null, null, null, null,
                        "Java后端开发工程师", null, null, null, null, null), List.of(), null);

        var result = retrievalService.retrieve("Java 后端岗位需要哪些核心能力？", context);

        assertThat(result.references()).extracting(KnowledgeReference::documentId)
                .contains("java-backend-capabilities");
        assertThat(result.context()).contains("岗位能力");
        assertThat(retrievalService.retrieve("我还有多少未完成任务？", context).hasKnowledge()).isFalse();
    }

    @Test
    void evaluatesInitialKnowledgeQuestions() throws Exception {
        CareerChatBusinessContext context = new CareerChatBusinessContext(null, List.of(), null);
        int positives = 0;
        int matched = 0;
        int negatives = 0;
        int skipped = 0;
        for (String line : Files.readAllLines(Path.of("../docs/知识库/检索评测集.jsonl"),
                StandardCharsets.UTF_8)) {
            EvaluationCase item = objectMapper.readValue(line, EvaluationCase.class);
            var result = retrievalService.retrieve(item.question(), context);
            if (item.retrieve()) {
                positives++;
                if (result.references().stream().map(KnowledgeReference::documentId)
                        .anyMatch(item.expectedDocument()::equals)) {
                    matched++;
                } else {
                    System.out.printf("RAG 未命中：%s -> %s%n", item.question(), item.expectedDocument());
                }
            } else {
                negatives++;
                if (!result.hasKnowledge()) {
                    skipped++;
                }
            }
        }
        assertThat(positives).isEqualTo(10);
        assertThat(matched).as("首批问题目标文档召回").isGreaterThanOrEqualTo(8);
        assertThat(negatives).isEqualTo(5);
        assertThat(skipped).as("无需检索问题正确跳过").isEqualTo(5);
        System.out.printf("RAG 初始评测：目标文档召回 %d/%d，跳过检索 %d/%d%n",
                matched, positives, skipped, negatives);
    }

    private record EvaluationCase(String question, String expectedDocument, boolean retrieve) {
    }
}
