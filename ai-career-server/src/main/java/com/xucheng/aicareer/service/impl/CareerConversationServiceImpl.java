package com.xucheng.aicareer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xucheng.aicareer.dto.CareerChatSessionUpdateDTO;
import com.xucheng.aicareer.entity.CareerChatMessage;
import com.xucheng.aicareer.entity.CareerChatSession;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.mapper.CareerChatMessageMapper;
import com.xucheng.aicareer.mapper.CareerChatSessionMapper;
import com.xucheng.aicareer.service.CareerConversationService;
import com.xucheng.aicareer.service.model.CareerChatMemoryEntry;
import com.xucheng.aicareer.service.model.CareerChatTurnContext;
import com.xucheng.aicareer.service.model.KnowledgeRetrievalResult;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.CareerChatMessageVO;
import com.xucheng.aicareer.vo.CareerChatSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 职业规划聊天会话与消息的 MySQL 持久化实现。
 *
 * <p>通过 userId 与 conversationId 联合查询保证会话隔离，并利用 clientMessageId 实现消息级幂等。</p>
 */
@Service
@RequiredArgsConstructor
public class CareerConversationServiceImpl implements CareerConversationService {

    private static final int SESSION_ARCHIVED = 0;
    private static final int SESSION_ACTIVE = 1;
    private static final int MESSAGE_PENDING = 0;
    private static final int MESSAGE_COMPLETED = 1;
    private static final int MESSAGE_FAILED = 2;
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String DEFAULT_TITLE = "新对话";
    private static final int TITLE_CODE_POINT_LIMIT = 30;
    private static final int LAST_MESSAGE_CODE_POINT_LIMIT = 100;
    private static final int PENDING_TIMEOUT_MINUTES = 3;

    private final CareerChatSessionMapper sessionMapper;
    private final CareerChatMessageMapper messageMapper;

    @Override
    @Transactional
    public CareerChatSessionVO createConversation() {
        return toSessionVO(createConversation(UserContext.getUserId()));
    }

    @Override
    public List<CareerChatSessionVO> getConversations(boolean includeArchived) {
        Long userId = UserContext.getUserId();
        var query = Wrappers.<CareerChatSession>lambdaQuery()
                .eq(CareerChatSession::getUserId, userId)
                .orderByDesc(CareerChatSession::getLastMessageAt)
                .orderByDesc(CareerChatSession::getCreateTime)
                .orderByDesc(CareerChatSession::getId);
        if (!includeArchived) {
            query.eq(CareerChatSession::getStatus, SESSION_ACTIVE);
        }
        return sessionMapper.selectList(query).stream().map(this::toSessionVO).toList();
    }

    @Override
    public List<CareerChatMessageVO> getMessages(String conversationId) {
        CareerChatSession session = getRequiredConversation(UserContext.getUserId(), conversationId);
        return messageMapper.selectList(Wrappers.<CareerChatMessage>lambdaQuery()
                        .eq(CareerChatMessage::getSessionId, session.getId())
                        .ne(CareerChatMessage::getStatus, MESSAGE_PENDING)
                        .orderByAsc(CareerChatMessage::getMessageOrder)
                        .orderByAsc(CareerChatMessage::getId))
                .stream()
                .map(this::toMessageVO)
                .toList();
    }

    @Override
    @Transactional
    public CareerChatSessionVO updateConversation(String conversationId, CareerChatSessionUpdateDTO updateDTO) {
        CareerChatSession session = getRequiredConversation(UserContext.getUserId(), conversationId);
        if (!StringUtils.hasText(updateDTO.getTitle()) && updateDTO.getStatus() == null) {
            throw new BusinessException("至少需要修改一项会话信息");
        }
        if (StringUtils.hasText(updateDTO.getTitle())) {
            session.setTitle(updateDTO.getTitle().trim());
        }
        if (updateDTO.getStatus() != null) {
            if (updateDTO.getStatus() != SESSION_ACTIVE && updateDTO.getStatus() != SESSION_ARCHIVED) {
                throw new BusinessException("会话状态不正确");
            }
            session.setStatus(updateDTO.getStatus());
        }
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        return toSessionVO(sessionMapper.selectById(session.getId()));
    }

    @Override
    @Transactional
    public void deleteConversation(String conversationId) {
        CareerChatSession session = getRequiredConversation(UserContext.getUserId(), conversationId);
        messageMapper.delete(Wrappers.<CareerChatMessage>lambdaQuery()
                .eq(CareerChatMessage::getSessionId, session.getId())
                .eq(CareerChatMessage::getUserId, session.getUserId()));
        sessionMapper.deleteById(session.getId());
    }

    @Override
    @Transactional
    public void clearConversation(String conversationId) {
        CareerChatSession session = getRequiredConversation(UserContext.getUserId(), conversationId);
        messageMapper.delete(Wrappers.<CareerChatMessage>lambdaQuery()
                .eq(CareerChatMessage::getSessionId, session.getId())
                .eq(CareerChatMessage::getUserId, session.getUserId()));
        session.setTitle(DEFAULT_TITLE);
        session.setSummary(null);
        session.setMessageCount(0);
        session.setLastMessage(null);
        session.setLastMessageAt(null);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    @Override
    @Transactional
    public CareerChatTurnContext prepareTurn(String conversationId, String clientMessageId, String content) {
        Long userId = UserContext.getUserId();
        CareerChatSession session = resolveConversation(userId, conversationId);
        // 锁定会话行，使并发请求串行计算 messageOrder 并检查待处理消息。
        session = getConversationForUpdate(userId, session.getConversationId());
        if (session.getStatus() != SESSION_ACTIVE) {
            throw new BusinessException(409, "该会话已归档，请恢复后继续对话");
        }

        String requestId = StringUtils.hasText(clientMessageId)
                ? clientMessageId.trim() : UUID.randomUUID().toString();
        // 客户端异常退出可能遗留 pending 消息，超时后转为 failed 才允许用户继续或重试。
        messageMapper.update(null, Wrappers.<CareerChatMessage>lambdaUpdate()
                .eq(CareerChatMessage::getSessionId, session.getId())
                .eq(CareerChatMessage::getStatus, MESSAGE_PENDING)
                .lt(CareerChatMessage::getUpdateTime, LocalDateTime.now().minusMinutes(PENDING_TIMEOUT_MINUTES))
                .set(CareerChatMessage::getStatus, MESSAGE_FAILED));
        long otherPendingTurns = messageMapper.selectCount(Wrappers.<CareerChatMessage>lambdaQuery()
                .eq(CareerChatMessage::getSessionId, session.getId())
                .eq(CareerChatMessage::getRole, ROLE_USER)
                .eq(CareerChatMessage::getStatus, MESSAGE_PENDING)
                .ne(CareerChatMessage::getClientMessageId, requestId));
        if (otherPendingTurns > 0) {
            throw new BusinessException(409, "当前会话正在生成回复，请稍后再试");
        }
        CareerChatMessage existingUser = findTurnMessage(session.getId(), requestId, ROLE_USER);
        CareerChatMessage existingAssistant = findTurnMessage(session.getId(), requestId, ROLE_ASSISTANT);
        if (existingUser != null && !existingUser.getContent().equals(content)) {
            throw new BusinessException(409, "客户端消息标识已被其他内容使用");
        }
        if (existingAssistant != null && existingAssistant.getStatus() == MESSAGE_COMPLETED) {
            // 已完成的请求直接携带历史答案返回，上层不会再次调用 Agent。
            return new CareerChatTurnContext(
                    session.getId(), userId, session.getConversationId(), requestId, existingAssistant.getContent());
        }

        if (existingUser == null) {
            CareerChatMessage userMessage = new CareerChatMessage();
            userMessage.setSessionId(session.getId());
            userMessage.setUserId(userId);
            userMessage.setClientMessageId(requestId);
            userMessage.setRole(ROLE_USER);
            userMessage.setContent(content);
            userMessage.setStatus(MESSAGE_PENDING);
            userMessage.setMessageOrder(nextMessageOrder(session.getId()));
            messageMapper.insert(userMessage);
        } else {
            existingUser.setStatus(MESSAGE_PENDING);
            existingUser.setUpdateTime(LocalDateTime.now());
            messageMapper.updateById(existingUser);
        }

        if (DEFAULT_TITLE.equals(session.getTitle()) && session.getMessageCount() == 0) {
            session.setTitle(abbreviate(content, TITLE_CODE_POINT_LIMIT));
        }
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        return new CareerChatTurnContext(session.getId(), userId, session.getConversationId(), requestId, null);
    }

    @Override
    @Transactional
    public void completeTurn(CareerChatTurnContext turn, String assistantContent) {
        if (!StringUtils.hasText(assistantContent)) {
            throw new BusinessException(500, "AI回复内容为空");
        }
        CareerChatSession session = getRequiredConversation(turn.userId(), turn.conversationId());
        CareerChatMessage userMessage = findRequiredUserMessage(turn);
        CareerChatMessage assistantMessage = findTurnMessage(turn.sessionId(), turn.clientMessageId(), ROLE_ASSISTANT);
        // 唯一索引和此处判断共同保证重试不会重复插入 assistant 消息或累计消息数。
        boolean newlyCompleted = assistantMessage == null;
        if (newlyCompleted) {
            assistantMessage = new CareerChatMessage();
            assistantMessage.setSessionId(turn.sessionId());
            assistantMessage.setUserId(turn.userId());
            assistantMessage.setClientMessageId(turn.clientMessageId());
            assistantMessage.setRole(ROLE_ASSISTANT);
            assistantMessage.setContent(assistantContent.trim());
            assistantMessage.setStatus(MESSAGE_COMPLETED);
            assistantMessage.setMessageOrder(nextMessageOrder(turn.sessionId()));
            messageMapper.insert(assistantMessage);
        }
        userMessage.setStatus(MESSAGE_COMPLETED);
        userMessage.setUpdateTime(LocalDateTime.now());
        messageMapper.updateById(userMessage);

        if (!newlyCompleted) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 2);
        session.setLastMessage(abbreviate(
                KnowledgeRetrievalResult.answerBody(assistantContent).trim(), LAST_MESSAGE_CODE_POINT_LIMIT));
        session.setLastMessageAt(now);
        session.setUpdateTime(now);
        sessionMapper.updateById(session);
    }

    @Override
    @Transactional
    public void failTurn(CareerChatTurnContext turn) {
        CareerChatMessage userMessage = findTurnMessage(turn.sessionId(), turn.clientMessageId(), ROLE_USER);
        if (userMessage != null && userMessage.getStatus() == MESSAGE_PENDING) {
            userMessage.setStatus(MESSAGE_FAILED);
            userMessage.setUpdateTime(LocalDateTime.now());
            messageMapper.updateById(userMessage);
        }
    }

    @Override
    public List<CareerChatMemoryEntry> getRecentMemory(Long userId, String conversationId, int limit) {
        CareerChatSession session = getRequiredConversation(userId, conversationId);
        // 数据库倒序查询便于使用 LIMIT 截取最近窗口，返回给模型前再恢复为正序。
        List<CareerChatMessage> messages = new ArrayList<>(messageMapper.selectList(
                Wrappers.<CareerChatMessage>lambdaQuery()
                        .eq(CareerChatMessage::getSessionId, session.getId())
                        .eq(CareerChatMessage::getStatus, MESSAGE_COMPLETED)
                        .orderByDesc(CareerChatMessage::getMessageOrder)
                        .orderByDesc(CareerChatMessage::getId)
                        .last("LIMIT " + Math.max(1, limit))));
        Collections.reverse(messages);
        return messages.stream()
                .map(message -> new CareerChatMemoryEntry(message.getRole(), message.getContent()))
                .toList();
    }

    private CareerChatSession resolveConversation(Long userId, String conversationId) {
        if (StringUtils.hasText(conversationId)) {
            return getRequiredConversation(userId, conversationId.trim());
        }
        // 兼容首次不传 conversationId 的客户端：优先沿用最近活跃会话，没有时再自动创建。
        CareerChatSession latest = sessionMapper.selectOne(Wrappers.<CareerChatSession>lambdaQuery()
                .eq(CareerChatSession::getUserId, userId)
                .eq(CareerChatSession::getStatus, SESSION_ACTIVE)
                .orderByDesc(CareerChatSession::getLastMessageAt)
                .orderByDesc(CareerChatSession::getCreateTime)
                .last("LIMIT 1"));
        return latest == null ? createConversation(userId) : latest;
    }

    private CareerChatSession createConversation(Long userId) {
        CareerChatSession session = new CareerChatSession();
        session.setUserId(userId);
        session.setConversationId("career:" + userId + ":" + UUID.randomUUID());
        session.setTitle(DEFAULT_TITLE);
        session.setStatus(SESSION_ACTIVE);
        session.setMessageCount(0);
        sessionMapper.insert(session);
        return session;
    }

    private CareerChatSession getRequiredConversation(Long userId, String conversationId) {
        CareerChatSession session = sessionMapper.selectOne(Wrappers.<CareerChatSession>lambdaQuery()
                .eq(CareerChatSession::getConversationId, conversationId)
                .eq(CareerChatSession::getUserId, userId));
        if (session == null) {
            throw new BusinessException(404, "聊天会话不存在");
        }
        return session;
    }

    private CareerChatSession getConversationForUpdate(Long userId, String conversationId) {
        CareerChatSession session = sessionMapper.selectOne(Wrappers.<CareerChatSession>lambdaQuery()
                .eq(CareerChatSession::getConversationId, conversationId)
                .eq(CareerChatSession::getUserId, userId)
                .last("FOR UPDATE"));
        if (session == null) {
            throw new BusinessException(404, "聊天会话不存在");
        }
        return session;
    }

    private CareerChatMessage findRequiredUserMessage(CareerChatTurnContext turn) {
        CareerChatMessage message = findTurnMessage(turn.sessionId(), turn.clientMessageId(), ROLE_USER);
        if (message == null || !message.getUserId().equals(turn.userId())) {
            throw new BusinessException(404, "待处理的用户消息不存在");
        }
        return message;
    }

    private CareerChatMessage findTurnMessage(Long sessionId, String clientMessageId, String role) {
        return messageMapper.selectOne(Wrappers.<CareerChatMessage>lambdaQuery()
                .eq(CareerChatMessage::getSessionId, sessionId)
                .eq(CareerChatMessage::getClientMessageId, clientMessageId)
                .eq(CareerChatMessage::getRole, role));
    }

    private int nextMessageOrder(Long sessionId) {
        CareerChatMessage latest = messageMapper.selectOne(Wrappers.<CareerChatMessage>lambdaQuery()
                .select(CareerChatMessage::getMessageOrder)
                .eq(CareerChatMessage::getSessionId, sessionId)
                .orderByDesc(CareerChatMessage::getMessageOrder)
                .last("LIMIT 1"));
        return latest == null ? 1 : latest.getMessageOrder() + 1;
    }

    private String abbreviate(String value, int codePointLimit) {
        int codePoints = value.codePointCount(0, value.length());
        if (codePoints <= codePointLimit) {
            return value;
        }
        int endIndex = value.offsetByCodePoints(0, codePointLimit);
        return value.substring(0, endIndex) + "…";
    }

    private CareerChatSessionVO toSessionVO(CareerChatSession session) {
        return CareerChatSessionVO.builder()
                .conversationId(session.getConversationId())
                .title(session.getTitle())
                .status(session.getStatus())
                .messageCount(session.getMessageCount())
                .lastMessage(session.getLastMessage())
                .lastMessageAt(session.getLastMessageAt())
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    private CareerChatMessageVO toMessageVO(CareerChatMessage message) {
        return CareerChatMessageVO.builder()
                .id(message.getId())
                .clientMessageId(message.getClientMessageId())
                .role(message.getRole())
                .content(message.getContent())
                .status(message.getStatus())
                .messageOrder(message.getMessageOrder())
                .createTime(message.getCreateTime())
                .build();
    }
}
