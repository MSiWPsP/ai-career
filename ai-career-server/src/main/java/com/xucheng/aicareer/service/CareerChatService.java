package com.xucheng.aicareer.service;

import com.xucheng.aicareer.dto.CareerChatDTO;
import com.xucheng.aicareer.dto.CareerChatSessionUpdateDTO;
import com.xucheng.aicareer.vo.CareerChatMessageVO;
import com.xucheng.aicareer.vo.CareerChatSessionVO;
import com.xucheng.aicareer.vo.CareerChatVO;
import com.xucheng.aicareer.vo.CareerChatStreamVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 职业规划师聊天应用服务。
 *
 * <p>负责协调会话持久化、ChatMemory、业务上下文和 Agent 调用。</p>
 */
public interface CareerChatService {

    /** 执行一次非流式对话，并在成功后持久化完整回复。 */
    CareerChatVO chat(CareerChatDTO chatDTO);

    /** 执行一次流式对话，流结束后统一持久化完整回复。 */
    Flux<CareerChatStreamVO> chatStream(CareerChatDTO chatDTO);

    /** 为当前登录用户创建一个独立会话。 */
    CareerChatSessionVO createConversation();

    /** 查询当前登录用户的会话列表。 */
    List<CareerChatSessionVO> getConversations(boolean includeArchived);

    /** 查询指定会话的完整可展示消息。 */
    List<CareerChatMessageVO> getMessages(String conversationId);

    /** 修改会话标题或归档状态。 */
    CareerChatSessionVO updateConversation(String conversationId, CareerChatSessionUpdateDTO updateDTO);

    /** 清空会话消息，但保留会话本身。 */
    void clearConversation(String conversationId);

    /** 删除会话及其全部消息。 */
    void deleteConversation(String conversationId);
}
