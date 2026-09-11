package com.xucheng.aicareer.service;

import com.xucheng.aicareer.dto.CareerChatSessionUpdateDTO;
import com.xucheng.aicareer.service.model.CareerChatMemoryEntry;
import com.xucheng.aicareer.service.model.CareerChatTurnContext;
import com.xucheng.aicareer.vo.CareerChatMessageVO;
import com.xucheng.aicareer.vo.CareerChatSessionVO;

import java.util.List;

public interface CareerConversationService {

    CareerChatSessionVO createConversation();

    List<CareerChatSessionVO> getConversations(boolean includeArchived);

    List<CareerChatMessageVO> getMessages(String conversationId);

    CareerChatSessionVO updateConversation(String conversationId, CareerChatSessionUpdateDTO updateDTO);

    void deleteConversation(String conversationId);

    void clearConversation(String conversationId);

    CareerChatTurnContext prepareTurn(String conversationId, String clientMessageId, String content);

    void completeTurn(CareerChatTurnContext turn, String assistantContent);

    void failTurn(CareerChatTurnContext turn);

    List<CareerChatMemoryEntry> getRecentMemory(Long userId, String conversationId, int limit);
}
