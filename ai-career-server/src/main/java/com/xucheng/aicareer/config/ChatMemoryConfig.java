package com.xucheng.aicareer.config;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CareerPlannerAgent 的短期对话记忆配置。
 *
 * <p>完整聊天记录仍以业务数据库为准，MessageWindowChatMemory 只保存模型调用所需的最近窗口。</p>
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * 创建固定消息窗口的职业规划聊天记忆。
     */
    @Bean
    public ChatMemory careerPlannerChatMemory(
            ChatMemoryRepository chatMemoryRepository,
            @Value("${ai.chat-memory.career.max-messages:20}") int maxMessages) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
    }

    /**
     * 将聊天记忆接入普通职业规划 ChatClient 的 Advisor 链。
     */
    @Bean
    public MessageChatMemoryAdvisor careerPlannerChatMemoryAdvisor(
            @Qualifier("careerPlannerChatMemory") ChatMemory careerPlannerChatMemory) {
        return MessageChatMemoryAdvisor.builder(careerPlannerChatMemory).build();
    }
}
