package com.xucheng.aicareer.config;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemory careerPlannerChatMemory(
            ChatMemoryRepository chatMemoryRepository,
            @Value("${ai.chat-memory.career.max-messages:20}") int maxMessages) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
    }

    @Bean
    public MessageChatMemoryAdvisor careerPlannerChatMemoryAdvisor(
            @Qualifier("careerPlannerChatMemory") ChatMemory careerPlannerChatMemory) {
        return MessageChatMemoryAdvisor.builder(careerPlannerChatMemory).build();
    }
}
