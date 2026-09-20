package com.xucheng.aicareer.service;

import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import com.xucheng.aicareer.service.model.KnowledgeRetrievalResult;

/** 职业聊天知识检索契约；实现需保证向量服务异常不会中断原有聊天。 */
public interface KnowledgeRetrievalService {

    /** 仅做本地规则判断；用于决定是否向流式客户端报告真实检索阶段。 */
    default boolean shouldRetrieve(String message) {
        return false;
    }

    KnowledgeRetrievalResult retrieve(String message, CareerChatBusinessContext context);
}
