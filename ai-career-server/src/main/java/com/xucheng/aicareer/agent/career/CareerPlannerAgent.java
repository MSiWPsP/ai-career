package com.xucheng.aicareer.agent.career;

import com.xucheng.aicareer.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class CareerPlannerAgent {

    private final ChatClient careerPlannerChatClient;

    @Value("${spring.ai.openai.chat.model:unknown}")
    private String model;

    public String chat(Long userId, String conversationId, String message) {
        long startTime = System.currentTimeMillis();
        try {
            String content = careerPlannerChatClient.prompt()
                    .user(message)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();
            if (!StringUtils.hasText(content)) {
                throw new AiServiceException("模型返回内容为空");
            }
            log.info("Agent调用成功 agent=CareerPlannerAgent conversationId={} userId={} model={} durationMs={}",
                    conversationId, userId, model, System.currentTimeMillis() - startTime);
            return content.trim();
        } catch (AiServiceException exception) {
            log.warn("Agent调用失败 agent=CareerPlannerAgent conversationId={} userId={} model={} durationMs={} reason={}",
                    conversationId, userId, model, System.currentTimeMillis() - startTime, exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            log.error("Agent调用失败 agent=CareerPlannerAgent conversationId={} userId={} model={} durationMs={}",
                    conversationId, userId, model, System.currentTimeMillis() - startTime, exception);
            throw new AiServiceException("调用职业规划模型失败", exception);
        }
    }
}
