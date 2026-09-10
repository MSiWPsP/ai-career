package com.xucheng.aicareer.agent.career;

import com.xucheng.aicareer.exception.AiServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CareerPlannerAgentTests {

    @Test
    void chatReturnsTrimmedModelContent() {
        CareerPlannerAgent agent = createAgent(chatClient(prompt -> response("  建议先结合技能基础和目标时间进行评估。  ")));

        String content = agent.chat(10001L, "career:10001", "我适合做Java后端吗？");

        assertThat(content).isEqualTo("建议先结合技能基础和目标时间进行评估。");
    }

    @Test
    void chatConvertsModelFailureToAiServiceException() {
        CareerPlannerAgent agent = createAgent(chatClient(prompt -> {
            throw new IllegalStateException("provider failed");
        }));

        assertThatThrownBy(() -> agent.chat(10001L, "career:10001", "测试问题"))
                .isInstanceOf(AiServiceException.class)
                .hasMessage("调用职业规划模型失败");
    }

    @Test
    void chatRejectsBlankModelContent() {
        CareerPlannerAgent agent = createAgent(chatClient(prompt -> response("  ")));

        assertThatThrownBy(() -> agent.chat(10001L, "career:10001", "测试问题"))
                .isInstanceOf(AiServiceException.class)
                .hasMessage("模型返回内容为空");
    }

    @Test
    void chatCarriesContextWithinConversationAndIsolatesOtherUsers() {
        RecordingChatModel chatModel = new RecordingChatModel("第一次回答", "追问回答", "另一位用户回答");
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        CareerPlannerAgent agent = createAgent(chatClient);

        agent.chat(10001L, "career:10001", "我想学习Java后端");
        agent.chat(10001L, "career:10001", "那我下一步学什么？");
        agent.chat(10002L, "career:10002", "我刚才说想学什么？");

        assertThat(messageTexts(chatModel.prompts.get(0)))
                .containsExactly("我想学习Java后端");
        assertThat(messageTexts(chatModel.prompts.get(1)))
                .containsExactly("我想学习Java后端", "第一次回答", "那我下一步学什么？");
        assertThat(messageTexts(chatModel.prompts.get(2)))
                .containsExactly("我刚才说想学什么？");
    }

    private ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    private ChatResponse response(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    private List<String> messageTexts(Prompt prompt) {
        return prompt.getInstructions().stream().map(Message::getText).toList();
    }

    private CareerPlannerAgent createAgent(ChatClient chatClient) {
        CareerPlannerAgent agent = new CareerPlannerAgent(chatClient);
        ReflectionTestUtils.setField(agent, "model", "qwen3.7-plus");
        return agent;
    }

    private static final class RecordingChatModel implements ChatModel {

        private final Deque<String> responses;
        private final List<Prompt> prompts = new ArrayList<>();

        private RecordingChatModel(String... responses) {
            this.responses = new ArrayDeque<>(List.of(responses));
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(responses.removeFirst()))));
        }
    }
}
