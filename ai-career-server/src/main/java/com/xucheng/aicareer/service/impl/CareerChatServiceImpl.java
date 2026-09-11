package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.agent.career.CareerPlannerAgent;
import com.xucheng.aicareer.dto.CareerChatDTO;
import com.xucheng.aicareer.dto.CareerChatSessionUpdateDTO;
import com.xucheng.aicareer.service.CareerChatService;
import com.xucheng.aicareer.service.CareerChatContextService;
import com.xucheng.aicareer.service.CareerConversationService;
import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import com.xucheng.aicareer.service.model.CareerChatMemoryEntry;
import com.xucheng.aicareer.service.model.CareerChatTurnContext;
import com.xucheng.aicareer.vo.CareerChatMessageVO;
import com.xucheng.aicareer.vo.CareerChatSessionVO;
import com.xucheng.aicareer.vo.CareerChatStreamVO;
import com.xucheng.aicareer.vo.CareerChatVO;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CareerChatServiceImpl implements CareerChatService {

    private final CareerPlannerAgent careerPlannerAgent;
    private final CareerConversationService conversationService;
    private final CareerChatContextService contextService;
    private final ChatMemory careerPlannerChatMemory;
    private final int memoryMessageLimit;

    public CareerChatServiceImpl(
            CareerPlannerAgent careerPlannerAgent,
            CareerConversationService conversationService,
            CareerChatContextService contextService,
            @Qualifier("careerPlannerChatMemory") ChatMemory careerPlannerChatMemory,
            @Value("${ai.chat-memory.career.max-messages:20}") int memoryMessageLimit) {
        this.careerPlannerAgent = careerPlannerAgent;
        this.conversationService = conversationService;
        this.contextService = contextService;
        this.careerPlannerChatMemory = careerPlannerChatMemory;
        this.memoryMessageLimit = memoryMessageLimit;
    }

    @Override
    public CareerChatVO chat(CareerChatDTO chatDTO) {
        CareerChatTurnContext turn = conversationService.prepareTurn(
                chatDTO.getConversationId(), chatDTO.getClientMessageId(), chatDTO.getMessage().trim());
        if (turn.replayContent() != null) {
            return toChatVO(turn, turn.replayContent());
        }

        try {
            restoreMemory(turn);
            CareerChatBusinessContext businessContext = contextService.getCurrentContext();
            String content = careerPlannerAgent.chat(
                    turn.userId(), turn.conversationId(), chatDTO.getMessage().trim(), businessContext);
            conversationService.completeTurn(turn, content);
            return toChatVO(turn, content);
        } catch (RuntimeException exception) {
            conversationService.failTurn(turn);
            restoreMemory(turn);
            throw exception;
        }
    }

    @Override
    public Flux<CareerChatStreamVO> chatStream(CareerChatDTO chatDTO) {
        CareerChatTurnContext turn = conversationService.prepareTurn(
                chatDTO.getConversationId(), chatDTO.getClientMessageId(), chatDTO.getMessage().trim());
        if (turn.replayContent() != null) {
            return Flux.just(
                    CareerChatStreamVO.delta(turn.conversationId(), turn.clientMessageId(), turn.replayContent()),
                    CareerChatStreamVO.done(turn.conversationId(), turn.clientMessageId()));
        }

        StringBuilder response = new StringBuilder();
        AtomicBoolean finalized = new AtomicBoolean(false);
        Flux<String> contentFlux;
        try {
            restoreMemory(turn);
            CareerChatBusinessContext businessContext = contextService.getCurrentContext();
            contentFlux = careerPlannerAgent.chatStream(
                    turn.userId(), turn.conversationId(), chatDTO.getMessage().trim(), businessContext);
        } catch (RuntimeException exception) {
            failTurn(turn, finalized);
            return Flux.just(CareerChatStreamVO.error(
                    turn.conversationId(), turn.clientMessageId(), "AI服务暂时不可用，请稍后重试"));
        }

        return contentFlux
                .doOnNext(response::append)
                .map(content -> CareerChatStreamVO.delta(
                        turn.conversationId(), turn.clientMessageId(), content))
                .doOnComplete(() -> {
                    conversationService.completeTurn(turn, response.toString());
                    finalized.set(true);
                })
                .concatWithValues(CareerChatStreamVO.done(turn.conversationId(), turn.clientMessageId()))
                .onErrorResume(exception -> {
                    failTurn(turn, finalized);
                    return Flux.just(CareerChatStreamVO.error(
                            turn.conversationId(), turn.clientMessageId(), "AI服务暂时不可用，请稍后重试"));
                })
                .doOnCancel(() -> failTurn(turn, finalized));
    }

    @Override
    public CareerChatSessionVO createConversation() {
        return conversationService.createConversation();
    }

    @Override
    public List<CareerChatSessionVO> getConversations(boolean includeArchived) {
        return conversationService.getConversations(includeArchived);
    }

    @Override
    public List<CareerChatMessageVO> getMessages(String conversationId) {
        return conversationService.getMessages(conversationId);
    }

    @Override
    public CareerChatSessionVO updateConversation(
            String conversationId, CareerChatSessionUpdateDTO updateDTO) {
        return conversationService.updateConversation(conversationId, updateDTO);
    }

    @Override
    public void clearConversation(String conversationId) {
        conversationService.clearConversation(conversationId);
        careerPlannerChatMemory.clear(conversationId);
    }

    @Override
    public void deleteConversation(String conversationId) {
        conversationService.deleteConversation(conversationId);
        careerPlannerChatMemory.clear(conversationId);
    }

    private CareerChatVO toChatVO(CareerChatTurnContext turn, String content) {
        return CareerChatVO.builder()
                .conversationId(turn.conversationId())
                .clientMessageId(turn.clientMessageId())
                .content(content)
                .build();
    }

    private void restoreMemory(CareerChatTurnContext turn) {
        careerPlannerChatMemory.clear(turn.conversationId());
        List<Message> messages = conversationService
                .getRecentMemory(turn.userId(), turn.conversationId(), memoryMessageLimit)
                .stream()
                .map(this::toSpringAiMessage)
                .toList();
        if (!messages.isEmpty()) {
            careerPlannerChatMemory.add(turn.conversationId(), messages);
        }
    }

    private Message toSpringAiMessage(CareerChatMemoryEntry entry) {
        if ("assistant".equals(entry.role())) {
            return new AssistantMessage(entry.content());
        }
        return new UserMessage(entry.content());
    }

    private void failTurn(CareerChatTurnContext turn, AtomicBoolean finalized) {
        if (finalized.compareAndSet(false, true)) {
            conversationService.failTurn(turn);
            restoreMemory(turn);
        }
    }
}
