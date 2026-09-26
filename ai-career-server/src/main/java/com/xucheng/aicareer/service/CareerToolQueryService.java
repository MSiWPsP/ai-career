package com.xucheng.aicareer.service;

import com.xucheng.aicareer.service.model.CareerAbilitySnapshot;
import com.xucheng.aicareer.service.model.CareerInterviewSnapshot;
import com.xucheng.aicareer.service.model.CareerTaskSnapshot;

/**
 * Agent Tool 的显式用户只读查询边界。
 *
 * <p>Tool 可能运行在 Reactor 工作线程，不能依赖 Web 请求线程中的 UserContext。</p>
 */
public interface CareerToolQueryService {

    CareerTaskSnapshot getCurrentTaskSnapshot(Long userId);

    CareerAbilitySnapshot getCurrentAbilitySnapshot(Long userId);

    CareerInterviewSnapshot getRecentInterviewSnapshot(Long userId);
}
