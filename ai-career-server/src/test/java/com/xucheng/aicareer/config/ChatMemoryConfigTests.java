package com.xucheng.aicareer.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMemoryConfigTests {

    @Test
    void careerMemoryKeepsBoundedCompleteTurnsAndSeparatesConversations() {
        ChatMemoryConfig config = new ChatMemoryConfig();
        ChatMemory memory = config.careerPlannerChatMemory(new InMemoryChatMemoryRepository(), 4);

        memory.add("career:10001", List.of(
                new UserMessage("问题一"), new AssistantMessage("回答一"),
                new UserMessage("问题二"), new AssistantMessage("回答二"),
                new UserMessage("问题三"), new AssistantMessage("回答三")
        ));
        memory.add("career:10002", new UserMessage("另一位用户的问题"));

        assertThat(memory.get("career:10001"))
                .extracting(Message::getText)
                .containsExactly("问题二", "回答二", "问题三", "回答三");
        assertThat(memory.get("career:10002"))
                .extracting(Message::getText)
                .containsExactly("另一位用户的问题");
    }
}
