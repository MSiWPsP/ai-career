package com.xucheng.aicareer.service;

import com.xucheng.aicareer.dto.CareerChatDTO;
import com.xucheng.aicareer.vo.CareerChatVO;
import com.xucheng.aicareer.vo.CareerChatStreamVO;
import reactor.core.publisher.Flux;

public interface CareerChatService {

    CareerChatVO chat(CareerChatDTO chatDTO);

    Flux<CareerChatStreamVO> chatStream(CareerChatDTO chatDTO);
}
