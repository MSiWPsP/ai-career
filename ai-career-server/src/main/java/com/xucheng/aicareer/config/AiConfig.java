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

/**
 * AI 对话客户端配置。
 *
 * <p>职业聊天、结构化规划和模拟面试使用独立的 ChatClient，避免记忆及输出约束相互污染。</p>
 */
@Configuration
public class AiConfig {

    /**
     * 创建带职业规划师 System Prompt 和会话记忆的普通聊天客户端。
     */
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

    /**
     * 创建结构化职业规划客户端。该客户端不挂载聊天记忆，保证每次生成只依赖本次业务快照。
     */
    @Bean
    public ChatClient careerPlanGenerationChatClient(
            ChatClient.Builder builder,
            @Value("classpath:prompts/career-plan-generation-system.md") Resource systemPrompt) throws IOException {
        return builder.clone()
                .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                .build();
    }

    /** 创建带独立会话记忆和面试规则的 InterviewerAgent 客户端。 */
    @Bean
    public ChatClient interviewerChatClient(
            ChatClient.Builder builder,
            @Qualifier("interviewerChatMemoryAdvisor") MessageChatMemoryAdvisor interviewerChatMemoryAdvisor,
            @Value("classpath:prompts/interviewer-system.md") Resource systemPrompt) throws IOException {
        return builder.clone()
                .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                .defaultAdvisors(interviewerChatMemoryAdvisor)
                .build();
    }

    /** 报告生成不接入实时面试记忆，始终依据数据库恢复的完整记录评估。 */
    @Bean
    public ChatClient interviewReportChatClient(
            ChatClient.Builder builder,
            @Value("classpath:prompts/interview-report-system.md") Resource systemPrompt) throws IOException {
        return builder.clone()
                .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                .build();
    }
}
