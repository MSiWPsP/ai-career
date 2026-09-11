package com.xucheng.aicareer.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient careerPlannerChatClient(
            ChatClient.Builder builder,
            @Qualifier("careerPlannerChatMemoryAdvisor") MessageChatMemoryAdvisor careerPlannerChatMemoryAdvisor,
            @Value("classpath:prompts/career-planner-system.md") Resource systemPrompt) throws IOException {
        return builder.clone()
                .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                .defaultAdvisors(careerPlannerChatMemoryAdvisor)
                .build();
    }

    @Bean
    public ChatClient careerPlanGenerationChatClient(
            ChatClient.Builder builder,
            @Value("classpath:prompts/career-plan-generation-system.md") Resource systemPrompt) throws IOException {
        return builder.clone()
                .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                .build();
    }
}
