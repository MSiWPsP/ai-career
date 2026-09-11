package com.xucheng.aicareer.service;

import com.xucheng.aicareer.service.model.CareerChatBusinessContext;

/**
 * 聚合职业规划聊天所需的最新业务上下文。
 *
 * <p>Agent 不直接访问数据库，而是通过已有业务 Service 获取职业画像、技能和当前规划。</p>
 */
public interface CareerChatContextService {

    /**
     * 获取当前登录用户在本次提问时的业务数据快照。
     *
     * @return 可安全序列化并注入 System Prompt 的业务上下文
     */
    CareerChatBusinessContext getCurrentContext();
}
