package com.xucheng.aicareer.service;

import com.xucheng.aicareer.dto.CareerChatSessionUpdateDTO;
import com.xucheng.aicareer.service.model.CareerChatMemoryEntry;
import com.xucheng.aicareer.service.model.CareerChatTurnContext;
import com.xucheng.aicareer.service.model.KnowledgeReference;
import com.xucheng.aicareer.vo.CareerChatMessageVO;
import com.xucheng.aicareer.vo.CareerChatSessionVO;

import java.util.List;

/**
 * 职业规划聊天的会话与消息持久化服务。
 *
 * <p>所有按 conversationId 的操作都会同时校验当前用户归属，不能仅凭会话标识访问数据。</p>
 */
public interface CareerConversationService {

    /** 创建属于当前登录用户的新会话。 */
    CareerChatSessionVO createConversation();

    /** 查询当前用户的活跃会话，并可选择包含已归档会话。 */
    List<CareerChatSessionVO> getConversations(boolean includeArchived);

    /** 查询指定会话中已完成或失败的消息，不返回仍在生成的临时消息。 */
    List<CareerChatMessageVO> getMessages(String conversationId);

    /** 更新指定会话的标题或状态。 */
    CareerChatSessionVO updateConversation(String conversationId, CareerChatSessionUpdateDTO updateDTO);

    /** 删除指定会话：会话按逻辑删除，关联消息同步清理。 */
    void deleteConversation(String conversationId);

    /** 清空消息并将会话元数据恢复到初始状态。 */
    void clearConversation(String conversationId);

    /**
     * 在调用 Agent 前创建或恢复一轮用户消息，并执行会话锁定和幂等判断。
     *
     * @return 本轮上下文；replayContent 非空时应直接重放已完成回复
     */
    CareerChatTurnContext prepareTurn(String conversationId, String clientMessageId, String content);

    /** 将本轮用户消息和 AI 回复标记为完成，并更新会话摘要信息。 */
    void completeTurn(CareerChatTurnContext turn, String assistantContent);

    /** 原子保存回答和本轮实际进入 Prompt 的来源快照。 */
    default void completeTurn(CareerChatTurnContext turn, String assistantContent,
                              List<KnowledgeReference> references) {
        completeTurn(turn, assistantContent);
    }

    /** 将尚未完成的用户消息标记为失败，以支持后续安全重试。 */
    void failTurn(CareerChatTurnContext turn);

    /** 从数据库按时间顺序读取供模型恢复上下文的最近消息。 */
    List<CareerChatMemoryEntry> getRecentMemory(Long userId, String conversationId, int limit);
}
