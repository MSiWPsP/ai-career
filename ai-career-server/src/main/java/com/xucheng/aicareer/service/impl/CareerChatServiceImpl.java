package com.xucheng.aicareer.service.impl;

import com.xucheng.aicareer.agent.career.CareerPlannerAgent;
import com.xucheng.aicareer.dto.CareerChatDTO;
import com.xucheng.aicareer.service.CareerChatService;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.CareerChatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
