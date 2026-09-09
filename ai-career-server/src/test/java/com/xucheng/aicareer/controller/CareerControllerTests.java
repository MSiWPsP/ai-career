package com.xucheng.aicareer.controller;

import com.xucheng.aicareer.common.Result;
import com.xucheng.aicareer.dto.CareerChatDTO;
import com.xucheng.aicareer.service.CareerChatService;
import com.xucheng.aicareer.service.CareerPlanService;
import com.xucheng.aicareer.vo.CareerChatVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CareerControllerTests {

    @Test
    void chatReturnsUnifiedSuccessResult() {
        CareerPlanService careerPlanService = mock(CareerPlanService.class);
        CareerChatService careerChatService = mock(CareerChatService.class);
        CareerController controller = new CareerController(careerPlanService, careerChatService);
        CareerChatDTO request = new CareerChatDTO();
        request.setMessage("Redis和微服务应该先学哪个？");
        CareerChatVO chatResponse = CareerChatVO.builder()
                .conversationId("career:10001")
                .content("建议先学习Redis。")
                .build();
        when(careerChatService.chat(request)).thenReturn(chatResponse);

        Result<CareerChatVO> result = controller.chat(request);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).isSameAs(chatResponse);
        verify(careerChatService).chat(request);
    }
}
