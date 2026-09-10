package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.agent.career.CareerPlannerAgent;
import com.xucheng.aicareer.dto.CareerChatDTO;
import com.xucheng.aicareer.service.CareerChatService;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.CareerChatVO;
import com.xucheng.aicareer.vo.CareerChatStreamVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class CareerChatServiceImpl implements CareerChatService {

    private static final String CONVERSATION_PREFIX = "career:";

    private final CareerPlannerAgent careerPlannerAgent;

    @Override
    public CareerChatVO chat(CareerChatDTO chatDTO) {
        Long userId = UserContext.getUserId();
        String conversationId = CONVERSATION_PREFIX + userId;
        String content = careerPlannerAgent.chat(userId, conversationId, chatDTO.getMessage().trim());
        return CareerChatVO.builder()
                .conversationId(conversationId)
                .content(content)
                .build();
    }

    @Override
    public Flux<CareerChatStreamVO> chatStream(CareerChatDTO chatDTO) {
        Long userId = UserContext.getUserId();
        String conversationId = CONVERSATION_PREFIX + userId;
        return careerPlannerAgent.chatStream(userId, conversationId, chatDTO.getMessage().trim())
                .map(content -> CareerChatStreamVO.delta(conversationId, content))
                .concatWithValues(CareerChatStreamVO.done(conversationId))
                .onErrorResume(exception -> Flux.just(CareerChatStreamVO.error(
                        conversationId, "AI服务暂时不可用，请稍后重试")));
    }
}
