package com.xucheng.aicareer.service;

import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import com.xucheng.aicareer.service.model.KnowledgeRetrievalResult;

/** 职业聊天知识检索契约；实现需保证向量服务异常不会中断原有聊天。 */
public interface KnowledgeRetrievalService {

    KnowledgeRetrievalResult retrieve(String message, CareerChatBusinessContext context);
}
