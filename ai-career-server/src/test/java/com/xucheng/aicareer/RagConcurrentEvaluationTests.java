package com.xucheng.aicareer;

import com.xucheng.aicareer.service.impl.PgKnowledgeRetrievalService;
import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/** 真实 Embedding + PGVector 的有限并发抽样；独立开关避免 CI 意外调用外部服务。 */
@EnabledIfEnvironmentVariable(named = "AI_RAG_CONCURRENCY_EVAL", matches = "true")
@SpringBootTest(properties = {"ai.rag.enabled=true", "ai.rag.import=false",
        "spring.main.web-application-type=none"})
class RagConcurrentEvaluationTests {

    private static final List<QueryCase> QUESTIONS = List.of(
            new QueryCase("Java 后端岗位需要哪些能力？", "java-backend-capabilities"),
            new QueryCase("后端项目学习路线怎么安排？", "backend-learning-path"),
            new QueryCase("实习简历如何展示可验证材料？", "internship-preparation"),
            new QueryCase("前端与后端方向如何比较？", "career-decision"));

    @Autowired
    private PgKnowledgeRetrievalService retrievalService;

    @Test
    void observesConcurrentRetrievalLatencyAndIsolation() {
        CareerChatBusinessContext context = new CareerChatBusinessContext(null, List.of(), null);
        List<CompletableFuture<Sample>> futures = new ArrayList<>();
        try (ExecutorService workers = Executors.newFixedThreadPool(4)) {
            for (int index = 0; index < 12; index++) {
                QueryCase query = QUESTIONS.get(index % QUESTIONS.size());
                futures.add(CompletableFuture.supplyAsync(() -> {
                    long start = System.nanoTime();
                    var result = retrievalService.retrieve(query.question(), context);
                    boolean targetMatched = result.references().stream()
                            .anyMatch(reference -> query.expectedDocument().equals(reference.documentId()));
                    return new Sample(query.question(), targetMatched,
                            (System.nanoTime() - start) / 1_000_000);
                }, workers));
            }
            List<Sample> samples = futures.stream().map(CompletableFuture::join).toList();
            List<Long> durations = samples.stream().map(Sample::durationMs).sorted().toList();
            long p95 = durations.get((int) Math.ceil(durations.size() * 0.95) - 1);
            System.out.printf("RAG 并发抽样: requests=%d workers=4 success=%d p50=%dms p95=%dms max=%dms%n",
                    samples.size(), samples.stream().filter(Sample::targetMatched).count(),
                    durations.get((int) Math.ceil(durations.size() * 0.50) - 1), p95, durations.getLast());
            samples.stream().filter(sample -> !sample.targetMatched())
                    .forEach(sample -> System.out.println("RAG 并发未命中: " + sample.question()));
            assertThat(samples).allMatch(Sample::targetMatched);
        }
    }

    private record QueryCase(String question, String expectedDocument) {
    }

    private record Sample(String question, boolean targetMatched, long durationMs) {
    }
}
