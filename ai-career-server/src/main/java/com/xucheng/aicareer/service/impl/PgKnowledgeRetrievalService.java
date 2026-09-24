package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.service.KnowledgeRetrievalService;
import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import com.xucheng.aicareer.service.model.KnowledgeReference;
import com.xucheng.aicareer.service.model.KnowledgeRetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/** 在 Service 层检索公共知识，不把用户画像写入向量库；任何检索故障都安全降级。 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.rag.enabled", havingValue = "true")
public class PgKnowledgeRetrievalService implements KnowledgeRetrievalService {

    private static final Pattern KNOWLEDGE_TOPIC = Pattern.compile(
            "(?i)岗位|职业|方向|学习|路线|技能|能力|实习|校招|求职|简历|面试|项目|后端|前端|开发|Java|Spring|MySQL|Redis|数据库|数据模型|鉴权|缓存");
    private static final Pattern LOCAL_ONLY = Pattern.compile(
            "(?i)多少.*任务|我的.*任务|刚才.*(说|聊)|总结.*(聊天|对话)|只根据我的|仅根据我的|"
                    + "你(?:知道|记得|了解)我的.*(?:年级|城市|画像|信息)");
    private static final Pattern EMAIL = Pattern.compile("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern LONG_NUMBER = Pattern.compile("(?<!\\d)\\d{15,18}[Xx]?(?!\\d)");
    private static final Pattern DECLARED_NAME = Pattern.compile(
            "(我叫|我的名字是|姓名[:：]?)\\s*[\\p{IsHan}·]{2,4}");
    private final PgKnowledgeRepository repository;
    private final KnowledgeEmbeddingClient embeddingClient;
    private final String model;
    private final double minScore;
    private final KnowledgeQueryPlanner queryPlanner;

    @Autowired
    public PgKnowledgeRetrievalService(PgKnowledgeRepository repository, KnowledgeEmbeddingClient embeddingClient,
                                       @Value("${ai.rag.embedding-model:text-embedding-v4}") String model,
                                       @Value("${ai.rag.min-score:0.55}") double minScore,
                                       KnowledgeQueryPlanner queryPlanner) {
        this.repository = repository;
        this.embeddingClient = embeddingClient;
        this.model = model;
        this.minScore = minScore;
        this.queryPlanner = queryPlanner;
    }

    /** 测试和独立工具复用无外部依赖的本地查询规划器，避免构造完整 Spring 容器。 */
    public PgKnowledgeRetrievalService(PgKnowledgeRepository repository, KnowledgeEmbeddingClient embeddingClient,
                                       String model, double minScore) {
        this(repository, embeddingClient, model, minScore, new KnowledgeQueryPlanner());
    }

    @Override
    public KnowledgeRetrievalResult retrieve(String message, CareerChatBusinessContext context) {
        if (!shouldRetrieve(message)) {
            return KnowledgeRetrievalResult.empty();
        }
        long start = System.nanoTime();
        long embeddingMillis = 0;
        long databaseMillis = 0;
        try {
            String target = context == null || context.profile() == null
                    ? null : context.profile().targetPosition();
            String query = sanitizeQuery(message.strip());
            if (query.length() > 400) {
                query = query.substring(0, 400);
            }
            List<String> queries = queryPlanner.plan(query);
            List<String> plannedQueries = queries.stream().map(this::sanitizeQuery).toList();
            List<List<PgKnowledgeRepository.KnowledgeHit>> resultSets = new ArrayList<>();
            List<CompletableFuture<QuerySearchResult>> futures = createQueryFutures(plannedQueries, target);
            for (int index = 0; index < futures.size(); index++) {
                try {
                    QuerySearchResult queryResult = futures.get(index).join();
                    embeddingMillis += queryResult.embeddingMillis();
                    databaseMillis += queryResult.databaseMillis();
                    resultSets.add(queryResult.hits());
                } catch (CompletionException exception) {
                    QueryFailure failure = queryFailure(exception);
                    embeddingMillis += failure.embeddingMillis();
                    databaseMillis += failure.databaseMillis();
                    if (index == 0) {
                        throw failure;
                    }
                    log.warn("RAG补充查询降级 queryIndex={} stage={} reason={}",
                            index, failure.stage(), failure.reason());
                }
            }
            List<PgKnowledgeRepository.KnowledgeHit> candidates = mergeRoundRobin(resultSets);
            List<KnowledgeReference> references = new ArrayList<>();
            StringBuilder knowledge = new StringBuilder();
            Map<String, Integer> perDocument = new HashMap<>();
            int perDocumentLimit = plannedQueries.size() > 1 ? 4 : 2;
            int referenceLimit = plannedQueries.size() > 1 ? 6 : 5;
            int contextLimit = plannedQueries.size() > 1 ? 5000 : 4000;
            for (PgKnowledgeRepository.KnowledgeHit hit : candidates) {
                if (hit.score() < minScore || references.size() >= referenceLimit) {
                    continue;
                }
                int count = perDocument.getOrDefault(hit.documentId(), 0);
                if (count >= perDocumentLimit || knowledge.length() + hit.content().length() > contextLimit) {
                    continue;
                }
                perDocument.put(hit.documentId(), count + 1);
                references.add(new KnowledgeReference(hit.documentId(), hit.title(),
                        hit.section(), hit.sourceName(), hit.documentVersion(), hit.chunkIndex(),
                        sha256(hit.content())));
                knowledge.append('[').append(references.size()).append("] 《")
                        .append(hit.title()).append("》· ").append(hit.section())
                        .append('\n').append(hit.content()).append("\n\n");
            }
            log.info("RAG检索完成 applied={} queries={} candidates={} selected={} embeddingMs={} databaseMs={} totalMs={}",
                    !references.isEmpty(), plannedQueries.size(), candidates.size(), references.size(), embeddingMillis,
                    databaseMillis, elapsedMillis(start));
            return new KnowledgeRetrievalResult(knowledge.toString(), List.copyOf(references));
        } catch (RuntimeException exception) {
            String stage = "planning";
            String reason = exception.getClass().getSimpleName();
            if (exception instanceof QueryFailure failure) {
                stage = failure.stage();
                reason = failure.reason();
                embeddingMillis = Math.max(embeddingMillis, failure.embeddingMillis());
                databaseMillis = Math.max(databaseMillis, failure.databaseMillis());
            }
            log.warn("RAG检索降级 stage={} reason={} embeddingMs={} databaseMs={} totalMs={}",
                    stage, reason, embeddingMillis, databaseMillis, elapsedMillis(start));
            return KnowledgeRetrievalResult.empty();
        }
    }

    /**
     * 多意图问题的补充查询彼此独立，使用虚拟线程并行执行以避免线性放大响应时间；
     * 返回的 Future 顺序仍与规划顺序一致，因此后续轮询合并是确定性的。
     */
    private List<CompletableFuture<QuerySearchResult>> createQueryFutures(List<String> queries, String target) {
        if (queries.size() == 1) {
            try {
                return List.of(CompletableFuture.completedFuture(executeQuery(queries.getFirst(), target)));
            } catch (QueryFailure failure) {
                return List.of(CompletableFuture.failedFuture(failure));
            }
        }
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            return queries.stream()
                    .map(query -> CompletableFuture.supplyAsync(() -> executeQuery(query, target), executor))
                    .toList();
        }
    }

    private QuerySearchResult executeQuery(String query, String target) {
        // 仅发送本轮问题及本地生成的职业主题词；画像、简历正文和会话历史不进入向量请求。
        long embeddingStart = System.nanoTime();
        float[] vector;
        try {
            vector = embeddingClient.embed(query);
        } catch (RuntimeException exception) {
            throw new QueryFailure("embedding", elapsedMillis(embeddingStart), 0, exception);
        }
        long embeddingMillis = elapsedMillis(embeddingStart);
        long databaseStart = System.nanoTime();
        try {
            List<PgKnowledgeRepository.KnowledgeHit> hits = repository.search(vector, target, model, 12);
            return new QuerySearchResult(hits, embeddingMillis, elapsedMillis(databaseStart));
        } catch (RuntimeException exception) {
            throw new QueryFailure("database", embeddingMillis, elapsedMillis(databaseStart), exception);
        }
    }

    private QueryFailure queryFailure(CompletionException exception) {
        if (exception.getCause() instanceof QueryFailure failure) {
            return failure;
        }
        return new QueryFailure("query", 0, 0, exception.getCause() == null ? exception : exception.getCause());
    }

    private List<PgKnowledgeRepository.KnowledgeHit> mergeRoundRobin(
            List<List<PgKnowledgeRepository.KnowledgeHit>> resultSets) {
        Map<String, PgKnowledgeRepository.KnowledgeHit> merged = new LinkedHashMap<>();
        int maxSize = resultSets.stream().mapToInt(List::size).max().orElse(0);
        for (int rank = 0; rank < maxSize; rank++) {
            for (List<PgKnowledgeRepository.KnowledgeHit> resultSet : resultSets) {
                if (rank >= resultSet.size()) {
                    continue;
                }
                PgKnowledgeRepository.KnowledgeHit hit = resultSet.get(rank);
                String key = hit.documentId() + ':' + hit.documentVersion() + ':' + hit.chunkIndex();
                merged.putIfAbsent(key, hit);
            }
        }
        return List.copyOf(merged.values());
    }

    @Override
    public boolean shouldRetrieve(String message) {
        if (message == null || message.isBlank() || LOCAL_ONLY.matcher(message).find()) {
            return false;
        }
        return KNOWLEDGE_TOPIC.matcher(message).find();
    }

    String sanitizeQuery(String message) {
        String safe = DECLARED_NAME.matcher(message).replaceAll("[姓名]");
        safe = EMAIL.matcher(safe).replaceAll("[邮箱]");
        safe = PHONE.matcher(safe).replaceAll("[电话]");
        return LONG_NUMBER.matcher(safe).replaceAll("[证件号]");
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private record QuerySearchResult(List<PgKnowledgeRepository.KnowledgeHit> hits,
                                     long embeddingMillis,
                                     long databaseMillis) {
    }

    private static final class QueryFailure extends RuntimeException {
        private final String stage;
        private final long embeddingMillis;
        private final long databaseMillis;

        private QueryFailure(String stage, long embeddingMillis, long databaseMillis, Throwable cause) {
            super(cause);
            this.stage = stage;
            this.embeddingMillis = embeddingMillis;
            this.databaseMillis = databaseMillis;
        }

        private String stage() {
            return stage;
        }

        private long embeddingMillis() {
            return embeddingMillis;
        }

        private long databaseMillis() {
            return databaseMillis;
        }

        private String reason() {
            Throwable cause = getCause();
            return cause == null ? getClass().getSimpleName() : cause.getClass().getSimpleName();
        }
    }
}
