package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.service.KnowledgeRetrievalService;
import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import com.xucheng.aicareer.service.model.KnowledgeRetrievalResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 默认关闭 RAG 时的无操作实现，不访问 PostgreSQL 或 Embedding API。 */
@Service
@ConditionalOnProperty(name = "ai.rag.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledKnowledgeRetrievalService implements KnowledgeRetrievalService {

    @Override
    public KnowledgeRetrievalResult retrieve(String message, CareerChatBusinessContext context) {
        return KnowledgeRetrievalResult.empty();
    }
}
