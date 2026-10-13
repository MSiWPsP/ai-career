package com.xucheng.aicareer;

import com.xucheng.aicareer.dto.CareerChatSessionUpdateDTO;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.service.CareerConversationService;
import com.xucheng.aicareer.service.model.CareerChatTurnContext;
import com.xucheng.aicareer.service.model.KnowledgeReference;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.CareerChatSessionVO;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CareerConversationBusinessIntegrationTests {

    private final CareerConversationService conversationService;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void conversationLifecyclePersistsMessagesPreventsDuplicatesAndChecksOwnership() {
        long userId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        String clientMessageId = UUID.randomUUID().toString();
        UserContext.setUserId(userId);

        CareerChatSessionVO session = conversationService.createConversation();
        assertThat(session.getConversationId()).startsWith("career:" + userId + ":");

        CareerChatTurnContext turn = conversationService.prepareTurn(
                session.getConversationId(), clientMessageId, "我应该如何准备Java实习？");
        conversationService.completeTurn(turn, "建议先完成一个可展示的项目。");
        conversationService.completeTurn(turn, "建议先完成一个可展示的项目。");

        assertThat(conversationService.getMessages(session.getConversationId()))
                .extracting(message -> message.getRole() + ":" + message.getContent())
                .containsExactly(
                        "user:我应该如何准备Java实习？",
                        "assistant:建议先完成一个可展示的项目。");
        assertThat(conversationService.prepareTurn(
                session.getConversationId(), clientMessageId, "我应该如何准备Java实习？").replayContent())
                .isEqualTo("建议先完成一个可展示的项目。");
        assertThat(conversationService.getConversations(false)).singleElement()
                .extracting(CareerChatSessionVO::getMessageCount).isEqualTo(2);

        CareerChatSessionUpdateDTO archive = new CareerChatSessionUpdateDTO();
        archive.setTitle("Java实习准备");
        archive.setStatus(0);
        assertThat(conversationService.updateConversation(session.getConversationId(), archive).getTitle())
                .isEqualTo("Java实习准备");
        assertThat(conversationService.getConversations(false)).isEmpty();
        assertThat(conversationService.getConversations(true)).singleElement()
                .extracting(CareerChatSessionVO::getStatus).isEqualTo(0);

        UserContext.setUserId(userId + 1);
        assertThatThrownBy(() -> conversationService.getMessages(session.getConversationId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("聊天会话不存在");

        UserContext.setUserId(userId);
        CareerChatSessionUpdateDTO restore = new CareerChatSessionUpdateDTO();
        restore.setStatus(1);
        conversationService.updateConversation(session.getConversationId(), restore);
        conversationService.clearConversation(session.getConversationId());
        assertThat(conversationService.getMessages(session.getConversationId())).isEmpty();
        assertThat(conversationService.getConversations(false)).singleElement()
                .satisfies(item -> {
                    assertThat(item.getTitle()).isEqualTo("新对话");
                    assertThat(item.getMessageCount()).isZero();
                });

        CareerChatTurnContext pendingTurn = conversationService.prepareTurn(
                session.getConversationId(), UUID.randomUUID().toString(), "第一个并发问题");
        assertThatThrownBy(() -> conversationService.prepareTurn(
                session.getConversationId(), UUID.randomUUID().toString(), "第二个并发问题"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前会话正在生成回复，请稍后再试");
        conversationService.failTurn(pendingTurn);

        conversationService.deleteConversation(session.getConversationId());
        assertThat(conversationService.getConversations(true)).isEmpty();
    }

    @Test
    void referenceSnapshotSurvivesHistoryAndIdempotentReplay() {
        long userId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        UserContext.setUserId(userId);
        CareerChatSessionVO session = conversationService.createConversation();
        String requestId = UUID.randomUUID().toString();
        CareerChatTurnContext turn = conversationService.prepareTurn(
                session.getConversationId(), requestId, "后端岗位需要哪些能力？");
        KnowledgeReference reference = new KnowledgeReference("java-backend-capabilities",
                "Java 后端岗位能力框架", "服务开发与数据", "AI职途项目知识库");
        conversationService.completeTurn(turn, "先练习接口开发。", List.of(reference));

        assertThat(conversationService.getMessages(session.getConversationId()).getLast().getReferences())
                .containsExactly(reference);
        CareerChatTurnContext replay = conversationService.prepareTurn(
                session.getConversationId(), requestId, "后端岗位需要哪些能力？");
        assertThat(replay.replayReferences()).containsExactly(reference);
    }
}
