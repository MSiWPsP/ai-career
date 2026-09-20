package com.xucheng.aicareer.agent.career;

import com.xucheng.aicareer.exception.AiServiceException;
import com.xucheng.aicareer.agent.career.dto.CareerPlanResult;
import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
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
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CareerPlannerAgentTests {

    private static final CareerChatBusinessContext EMPTY_CONTEXT =
            new CareerChatBusinessContext(null, List.of(), null);

    @Test
    void chatReturnsTrimmedModelContent() {
        CareerPlannerAgent agent = createAgent(chatClient(prompt -> response("  建议先结合技能基础和目标时间进行评估。  ")));

        String content = agent.chat(10001L, "career:10001", "我适合做Java后端吗？", EMPTY_CONTEXT);

        assertThat(content).isEqualTo("建议先结合技能基础和目标时间进行评估。");
    }

    @Test
    void chatConvertsModelFailureToAiServiceException() {
        CareerPlannerAgent agent = createAgent(chatClient(prompt -> {
            throw new IllegalStateException("provider failed");
        }));

        assertThatThrownBy(() -> agent.chat(10001L, "career:10001", "测试问题", EMPTY_CONTEXT))
                .isInstanceOf(AiServiceException.class)
                .hasMessage("调用职业规划模型失败");
    }

    @Test
    void chatRejectsBlankModelContent() {
        CareerPlannerAgent agent = createAgent(chatClient(prompt -> response("  ")));

        assertThatThrownBy(() -> agent.chat(10001L, "career:10001", "测试问题", EMPTY_CONTEXT))
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

        agent.chat(10001L, "career:10001", "我想学习Java后端", EMPTY_CONTEXT);
        agent.chat(10001L, "career:10001", "那我下一步学什么？", EMPTY_CONTEXT);
        agent.chat(10002L, "career:10002", "我刚才说想学什么？", EMPTY_CONTEXT);

        assertThat(messageTexts(chatModel.prompts.get(0)))
                .containsExactly("我想学习Java后端");
        assertThat(messageTexts(chatModel.prompts.get(1)))
                .containsExactly("我想学习Java后端", "第一次回答", "那我下一步学什么？");
        assertThat(messageTexts(chatModel.prompts.get(2)))
                .containsExactly("我刚才说想学什么？");
    }

    @Test
    void chatStreamEmitsDeltasAndCarriesConversationContext() {
        RecordingStreamingChatModel chatModel = new RecordingStreamingChatModel(
                List.of("第一段", "第二段"), List.of("追问回答"));
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        CareerPlannerAgent agent = createAgent(chatClient);

        List<String> firstAnswer = agent.chatStream(10001L, "career:10001", "第一个问题", EMPTY_CONTEXT)
                .collectList().block();
        List<String> secondAnswer = agent.chatStream(10001L, "career:10001", "继续追问", EMPTY_CONTEXT)
                .collectList().block();

        assertThat(firstAnswer).containsExactly("第一段", "第二段");
        assertThat(secondAnswer).containsExactly("追问回答");
        assertThat(messageTexts(chatModel.prompts.get(1)))
                .containsExactly("第一个问题", "第一段第二段", "继续追问");
    }

    @Test
    void chatStreamConvertsProviderFailureToAiServiceException() {
        ChatModel chatModel = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.error(new IllegalStateException("provider failed"));
            }
        };
        CareerPlannerAgent agent = createAgent(chatClient(chatModel));

        assertThatThrownBy(() -> agent.chatStream(
                10001L, "career:10001", "测试问题", EMPTY_CONTEXT).blockLast())
                .isInstanceOf(AiServiceException.class)
                .hasMessage("调用职业规划模型失败");
    }

    @Test
    void chatInjectsSanitizedBusinessContextIntoSystemPrompt() {
        AtomicReference<Prompt> capturedPrompt = new AtomicReference<>();
        ChatClient chatClient = ChatClient.builder(prompt -> {
                    capturedPrompt.set(prompt);
                    return response("建议优先补强Redis。");
                })
                .defaultSystem("基础职业规划规则\n<career_context>\n{careerContext}\n</career_context>")
                .build();
        CareerPlannerAgent agent = createAgent(chatClient);
        CareerChatBusinessContext context = new CareerChatBusinessContext(
                new CareerChatBusinessContext.Profile(
                        "本科", "软件工程", "大三", 2027, "INTERNSHIP",
                        "Java后端开发工程师", "杭州", "半年内", null,
                        "找到后端实习", "喜欢工程实践"),
                List.of(new CareerChatBusinessContext.Skill("Java", "PROGRAMMING", 3, 60)),
                null);

        agent.chat(10001L, "career:10001", "我下一步学什么？", context);

        assertThat(messageTexts(capturedPrompt.get()))
                .anyMatch(text -> text.contains("软件工程")
                        && text.contains("Java后端开发工程师")
                        && text.contains("\"skillName\":\"Java\""));
    }

    @Test
    void chatPassesRetrievedKnowledgeAsReferenceContext() {
        AtomicReference<Prompt> capturedPrompt = new AtomicReference<>();
        ChatClient chatClient = ChatClient.builder(prompt -> {
                    capturedPrompt.set(prompt);
                    return response("建议补充接口测试。");
                })
                .defaultSystem("职业规划规则\n<career_context>{careerContext}</career_context>"
                        + "\n<knowledge_context>{knowledgeContext}</knowledge_context>")
                .build();
        CareerPlannerAgent agent = createAgent(chatClient);

        agent.chat(10001L, "career:10001", "Spring Boot 项目要准备什么？",
                EMPTY_CONTEXT, "《Java 后端岗位能力框架》· Spring Boot 项目工程要素");

        assertThat(messageTexts(capturedPrompt.get())).anyMatch(text -> text.contains(
                "<knowledge_context>《Java 后端岗位能力框架》· Spring Boot 项目工程要素</knowledge_context>"));
    }

    @Test
    void generatePlanReturnsStructuredResultAndIncludesBusinessContext() {
        AtomicReference<Prompt> capturedPrompt = new AtomicReference<>();
        ChatClient generationClient = chatClient(prompt -> {
            capturedPrompt.set(prompt);
            return response(validPlanJson());
        });
        CareerPlannerAgent agent = createAgent(chatClient(prompt -> response("unused")), generationClient);

        CareerPlanResult result = agent.generatePlan(
                10001L,
                com.xucheng.aicareer.vo.UserProfileVO.builder()
                        .major("软件工程")
                        .targetPosition("Java后端开发工程师")
                        .build(),
                List.of(com.xucheng.aicareer.vo.UserSkillVO.builder()
                        .skillName("Java")
                        .level(3)
                        .score(60)
                        .build()));

        assertThat(result.getTargetPosition()).isEqualTo("Java后端开发工程师");
        assertThat(result.getRoadmap()).hasSize(3);
        assertThat(messageTexts(capturedPrompt.get()))
                .anyMatch(text -> text.contains("软件工程") && text.contains("Java后端开发工程师"));
    }

    @Test
    void generatePlanRetriesOnceWhenStructuredResultIsInvalid() {
        RecordingChatModel model = new RecordingChatModel("{}", validPlanJson());
        CareerPlannerAgent agent = createAgent(
                chatClient(prompt -> response("unused")), chatClient(model));

        CareerPlanResult result = agent.generatePlan(
                10001L,
                com.xucheng.aicareer.vo.UserProfileVO.builder().targetPosition("Java后端开发工程师").build(),
                List.of(com.xucheng.aicareer.vo.UserSkillVO.builder().skillName("Java").build()));

        assertThat(result.getMatchScore()).isEqualTo(68);
        assertThat(model.prompts).hasSize(2);
        assertThat(messageTexts(model.prompts.get(1)))
                .anyMatch(text -> text.contains("上一次结果未通过结构校验"));
    }

    @Test
    void generatePlanConvertsRepeatedInvalidResultsToAiServiceException() {
        RecordingChatModel model = new RecordingChatModel("{}", "{}");
        CareerPlannerAgent agent = createAgent(
                chatClient(prompt -> response("unused")), chatClient(model));

        assertThatThrownBy(() -> agent.generatePlan(
                10001L,
                com.xucheng.aicareer.vo.UserProfileVO.builder().targetPosition("Java后端开发工程师").build(),
                List.of(com.xucheng.aicareer.vo.UserSkillVO.builder().skillName("Java").build())))
                .isInstanceOf(AiServiceException.class)
                .hasMessage("生成结构化职业规划失败");
        assertThat(model.prompts).hasSize(2);
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
        return createAgent(chatClient, chatClient);
    }

    private CareerPlannerAgent createAgent(ChatClient chatClient, ChatClient generationChatClient) {
        CareerPlannerAgent agent = new CareerPlannerAgent(chatClient, generationChatClient, new ObjectMapper());
        ReflectionTestUtils.setField(agent, "model", "qwen3.7-plus");
        return agent;
    }

    private String validPlanJson() {
        return """
                {
                  "targetPosition": "Java后端开发工程师",
                  "matchScore": 68,
                  "summary": "具备Java基础，需要继续补齐工程实践能力。",
                  "advantages": ["具备Java基础", "目标岗位明确"],
                  "weaknesses": ["中间件经验不足", "项目实践需要加强"],
                  "roadmap": [
                    {
                      "stage": 1,
                      "name": "基础强化",
                      "goal": "夯实Java核心基础",
                      "duration": "2周",
                      "topics": ["集合", "并发"],
                      "tasks": [
                        {"taskName": "复习Java集合", "description": "完成知识梳理", "taskType": "KNOWLEDGE", "priority": 3},
                        {"taskName": "完成并发练习", "description": "编写并发示例", "taskType": "PROJECT", "priority": 2}
                      ]
                    },
                    {
                      "stage": 2,
                      "name": "框架实践",
                      "goal": "掌握Spring Boot项目开发",
                      "duration": "3周",
                      "topics": ["Spring Boot", "MyBatis"],
                      "tasks": [
                        {"taskName": "搭建后端项目", "description": "实现基础接口", "taskType": "PROJECT", "priority": 3},
                        {"taskName": "梳理框架原理", "description": "总结核心机制", "taskType": "KNOWLEDGE", "priority": 2}
                      ]
                    },
                    {
                      "stage": 3,
                      "name": "求职准备",
                      "goal": "形成可展示成果并准备面试",
                      "duration": "2周",
                      "topics": ["项目表达", "面试复盘"],
                      "tasks": [
                        {"taskName": "整理项目亮点", "description": "形成项目介绍", "taskType": "CAREER", "priority": 3},
                        {"taskName": "完成模拟面试", "description": "检验学习成果", "taskType": "INTERVIEW", "priority": 2}
                      ]
                    }
                  ]
                }
                """;
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

    private static final class RecordingStreamingChatModel implements ChatModel {

        private final Deque<List<String>> responses;
        private final List<Prompt> prompts = new ArrayList<>();

        @SafeVarargs
        private RecordingStreamingChatModel(List<String>... responses) {
            this.responses = new ArrayDeque<>(List.of(responses));
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            prompts.add(prompt);
            return Flux.fromIterable(responses.removeFirst())
                    .map(content -> new ChatResponse(List.of(
                            new Generation(new AssistantMessage(content)))));
        }
    }
}
