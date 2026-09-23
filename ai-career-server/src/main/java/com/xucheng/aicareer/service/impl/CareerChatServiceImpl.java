package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.agent.career.CareerPlannerAgent;
import com.xucheng.aicareer.dto.CareerChatDTO;
import com.xucheng.aicareer.dto.CareerChatSessionUpdateDTO;
import com.xucheng.aicareer.service.CareerChatService;
import com.xucheng.aicareer.service.CareerChatContextService;
import com.xucheng.aicareer.service.CareerConversationService;
import com.xucheng.aicareer.service.KnowledgeRetrievalService;
import com.xucheng.aicareer.service.model.CareerChatBusinessContext;
import com.xucheng.aicareer.service.model.CareerChatMemoryEntry;
import com.xucheng.aicareer.service.model.CareerChatTurnContext;
import com.xucheng.aicareer.service.model.KnowledgeRetrievalResult;
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
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 职业规划师聊天编排实现。
 *
 * <p>数据库消息是事实来源；ChatMemory 仅在调用模型前由数据库近期消息重建。这样既支持服务重启恢复，
 * 又能保证页面历史记录与模型实际上下文一致。</p>
 */
@Service
public class CareerChatServiceImpl implements CareerChatService {

    private final CareerPlannerAgent careerPlannerAgent;
    private final CareerConversationService conversationService;
    private final CareerChatContextService contextService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final ChatMemory careerPlannerChatMemory;
    private final int memoryMessageLimit;

    public CareerChatServiceImpl(
            CareerPlannerAgent careerPlannerAgent,
            CareerConversationService conversationService,
            CareerChatContextService contextService,
            KnowledgeRetrievalService knowledgeRetrievalService,
            @Qualifier("careerPlannerChatMemory") ChatMemory careerPlannerChatMemory,
            @Value("${ai.chat-memory.career.max-messages:20}") int memoryMessageLimit) {
        this.careerPlannerAgent = careerPlannerAgent;
        this.conversationService = conversationService;
        this.contextService = contextService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.careerPlannerChatMemory = careerPlannerChatMemory;
        this.memoryMessageLimit = memoryMessageLimit;
    }

    @Override
    public CareerChatVO chat(CareerChatDTO chatDTO) {
        CareerChatTurnContext turn = conversationService.prepareTurn(
                chatDTO.getConversationId(), chatDTO.getClientMessageId(), chatDTO.getMessage().trim());
        // 相同 clientMessageId 已完成时直接返回历史答案，避免重试重复调用模型和重复落库。
        if (turn.replayContent() != null) {
            return toChatVO(turn, turn.replayContent());
        }

        try {
            restoreMemory(turn);
            CareerChatBusinessContext businessContext = contextService.getCurrentContext();
            KnowledgeRetrievalResult knowledge = knowledgeRetrievalService.retrieve(
                    chatDTO.getMessage().trim(), businessContext);
            String generated = KnowledgeRetrievalResult.sanitizeGeneratedAnswer(careerPlannerAgent.chat(
                    turn.userId(), turn.conversationId(), chatDTO.getMessage().trim(),
                    businessContext, knowledge.context()));
            RagAnswerFactGuard.Review review = knowledge.hasKnowledge()
                    || RagAnswerFactGuard.requiresPreflight(chatDTO.getMessage())
                    ? RagAnswerFactGuard.review(generated)
                    : new RagAnswerFactGuard.Review(generated, true);
            String content = review.content() + (review.accepted() ? knowledge.citationFooter() : "");
            conversationService.completeTurn(turn, content,
                    review.accepted() ? knowledge.references() : List.of());
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
                    CareerChatStreamVO.delta(turn.conversationId(), turn.clientMessageId(),
                            KnowledgeRetrievalResult.answerBody(turn.replayContent())),
                    CareerChatStreamVO.done(turn.conversationId(), turn.clientMessageId(),
                            turn.replayReferences() == null ? List.of() : turn.replayReferences()));
        }

        // Reactor 的错误和取消回调可能竞争触发，原子标记确保失败收尾只执行一次。
        AtomicBoolean finalized = new AtomicBoolean(false);
        CareerChatBusinessContext businessContext;
        try {
            restoreMemory(turn);
            businessContext = contextService.getCurrentContext();
        } catch (RuntimeException exception) {
            failTurn(turn, finalized);
            return Flux.just(CareerChatStreamVO.error(
                    turn.conversationId(), turn.clientMessageId(), "AI服务暂时不可用，请稍后重试"));
        }

        String question = chatDTO.getMessage().trim();
        Flux<CareerChatStreamVO> retrievalPhase = knowledgeRetrievalService.shouldRetrieve(question)
                ? Flux.just(CareerChatStreamVO.phase(turn.conversationId(), turn.clientMessageId(),
                        CareerChatStreamVO.PHASE_KNOWLEDGE_RETRIEVAL))
                : Flux.empty();
        // 阶段事件先输出；随后才在后台执行阻塞式 Embedding/PGVector 检索。
        return retrievalPhase.concatWith(Flux.defer(() -> generateStream(turn, question, businessContext, finalized))
                .subscribeOn(Schedulers.boundedElastic()))
                .doOnCancel(() -> failTurn(turn, finalized));
    }

    private Flux<CareerChatStreamVO> generateStream(
            CareerChatTurnContext turn, String question, CareerChatBusinessContext businessContext,
            AtomicBoolean finalized) {
        StringBuilder response = new StringBuilder();
        KnowledgeRetrievalResult knowledge;
        Flux<String> contentFlux;
        try {
            knowledge = knowledgeRetrievalService.retrieve(question, businessContext);
            if (knowledge.hasKnowledge() || RagAnswerFactGuard.requiresPreflight(question)) {
                // RAG 内容先完整生成并校验，任何不可靠分片都不能先通过 SSE 发给用户。
                String generated = KnowledgeRetrievalResult.sanitizeGeneratedAnswer(careerPlannerAgent.chat(
                        turn.userId(), turn.conversationId(), question, businessContext, knowledge.context()));
                RagAnswerFactGuard.Review review = RagAnswerFactGuard.review(generated);
                KnowledgeRetrievalResult citedKnowledge = review.accepted()
                        ? knowledge : KnowledgeRetrievalResult.empty();
                return Flux.just(CareerChatStreamVO.phase(turn.conversationId(), turn.clientMessageId(),
                                CareerChatStreamVO.PHASE_GENERATING))
                        .concatWith(Flux.defer(() -> {
                            conversationService.completeTurn(turn, review.content() + citedKnowledge.citationFooter(),
                                    citedKnowledge.references());
                            finalized.set(true);
                            return Flux.just(CareerChatStreamVO.delta(turn.conversationId(),
                                            turn.clientMessageId(), review.content()),
                                    CareerChatStreamVO.done(turn.conversationId(), turn.clientMessageId(),
                                            citedKnowledge.references()));
                        }))
                        .onErrorResume(exception -> {
                            failTurn(turn, finalized);
                            return Flux.just(CareerChatStreamVO.error(turn.conversationId(),
                                    turn.clientMessageId(), "AI服务暂时不可用，请稍后重试"));
                        });
            }
            contentFlux = careerPlannerAgent.chatStream(turn.userId(), turn.conversationId(), question,
                    businessContext, knowledge.context());
        } catch (RuntimeException exception) {
            failTurn(turn, finalized);
            return Flux.just(CareerChatStreamVO.error(
                    turn.conversationId(), turn.clientMessageId(), "AI服务暂时不可用，请稍后重试"));
        }
        Flux<CareerChatStreamVO> answer = contentFlux
                .doOnNext(response::append)
                .map(content -> CareerChatStreamVO.delta(
                        turn.conversationId(), turn.clientMessageId(), content))
                .doOnComplete(() -> {
                    conversationService.completeTurn(turn,
                            KnowledgeRetrievalResult.sanitizeGeneratedAnswer(response.toString())
                                    + knowledge.citationFooter());
                    finalized.set(true);
                })
                // done 事件必须位于持久化成功之后，避免前端显示完成但数据库仍未落库。
                .concatWithValues(CareerChatStreamVO.done(
                        turn.conversationId(), turn.clientMessageId(), knowledge.references()))
                .onErrorResume(exception -> {
                    failTurn(turn, finalized);
                    return Flux.just(CareerChatStreamVO.error(
                            turn.conversationId(), turn.clientMessageId(), "AI服务暂时不可用，请稍后重试"));
                })
                .doOnCancel(() -> failTurn(turn, finalized));
        return Flux.just(CareerChatStreamVO.phase(turn.conversationId(), turn.clientMessageId(),
                CareerChatStreamVO.PHASE_GENERATING)).concatWith(answer);
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
        // 每轮先清空再从数据库恢复，避免内存残留、重复消息以及后端重启后的上下文偏差。
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
