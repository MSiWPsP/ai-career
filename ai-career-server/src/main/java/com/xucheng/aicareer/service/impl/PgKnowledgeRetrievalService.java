package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.service.KnowledgeRetrievalService;
import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import com.xucheng.aicareer.service.model.KnowledgeReference;
import com.xucheng.aicareer.service.model.KnowledgeRetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** 在 Service 层检索公共知识，不把用户画像写入向量库；任何检索故障都安全降级。 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.rag.enabled", havingValue = "true")
public class PgKnowledgeRetrievalService implements KnowledgeRetrievalService {

    private static final Pattern KNOWLEDGE_TOPIC = Pattern.compile(
            "(?i)岗位|职业|方向|学习|路线|技能|能力|实习|校招|求职|简历|面试|项目|后端|前端|开发|Java|Spring|MySQL|Redis|数据库");
    private static final Pattern LOCAL_ONLY = Pattern.compile(
            "(?i)多少.*任务|我的.*任务|刚才.*(说|聊)|总结.*(聊天|对话)|只根据我的|仅根据我的");
    private static final Pattern EMAIL = Pattern.compile("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern LONG_NUMBER = Pattern.compile("(?<!\\d)\\d{15,18}[Xx]?(?!\\d)");
    private static final Pattern DECLARED_NAME = Pattern.compile(
            "(我叫|我的名字是|姓名[:：]?)\\s*[\\p{IsHan}·]{2,4}");
    private final PgKnowledgeRepository repository;
    private final KnowledgeEmbeddingClient embeddingClient;
    private final String model;
    private final double minScore;

    public PgKnowledgeRetrievalService(PgKnowledgeRepository repository, KnowledgeEmbeddingClient embeddingClient,
                                       @Value("${ai.rag.embedding-model:text-embedding-v4}") String model,
                                       @Value("${ai.rag.min-score:0.55}") double minScore) {
        this.repository = repository;
        this.embeddingClient = embeddingClient;
        this.model = model;
        this.minScore = minScore;
    }

    @Override
    public KnowledgeRetrievalResult retrieve(String message, CareerChatBusinessContext context) {
        if (!shouldRetrieve(message)) {
            return KnowledgeRetrievalResult.empty();
        }
        long start = System.currentTimeMillis();
        try {
            String target = context == null || context.profile() == null
                    ? null : context.profile().targetPosition();
            String query = sanitizeQuery(message.strip());
            if (query.length() > 400) {
                query = query.substring(0, 400);
            }
            // 仅发送本轮问题到 Embedding 服务；姓名、简历、完整会话和画像不进入向量请求。
            List<PgKnowledgeRepository.KnowledgeHit> candidates = repository.search(
                    embeddingClient.embed(query), target, model, 12);
            List<KnowledgeReference> references = new ArrayList<>();
            StringBuilder knowledge = new StringBuilder();
            Map<String, Integer> perDocument = new HashMap<>();
            for (PgKnowledgeRepository.KnowledgeHit hit : candidates) {
                if (hit.score() < minScore || references.size() >= 5) {
                    continue;
                }
                int count = perDocument.getOrDefault(hit.documentId(), 0);
                if (count >= 2 || knowledge.length() + hit.content().length() > 4000) {
                    continue;
                }
                perDocument.put(hit.documentId(), count + 1);
                references.add(new KnowledgeReference(hit.documentId(), hit.title(),
                        hit.section(), hit.sourceName()));
                knowledge.append('[').append(references.size()).append("] 《")
                        .append(hit.title()).append("》· ").append(hit.section())
                        .append('\n').append(hit.content()).append("\n\n");
            }
            log.info("RAG检索完成 applied=true candidates={} selected={} durationMs={}",
                    candidates.size(), references.size(), System.currentTimeMillis() - start);
            return new KnowledgeRetrievalResult(knowledge.toString(), List.copyOf(references));
        } catch (RuntimeException exception) {
            log.warn("RAG检索降级 reason={} durationMs={}",
                    exception.getClass().getSimpleName(), System.currentTimeMillis() - start);
            return KnowledgeRetrievalResult.empty();
        }
    }

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
}
