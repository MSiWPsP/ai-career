package com.xucheng.aicareer.controller;

import com.xucheng.aicareer.common.Result;
import com.xucheng.aicareer.dto.CareerChatDTO;
import com.xucheng.aicareer.dto.CareerChatSessionUpdateDTO;
import com.xucheng.aicareer.service.CareerChatService;
import com.xucheng.aicareer.service.CareerPlanService;
import com.xucheng.aicareer.vo.CareerChatVO;
import com.xucheng.aicareer.vo.CareerChatMessageVO;
import com.xucheng.aicareer.vo.CareerChatSessionVO;
import com.xucheng.aicareer.vo.CareerChatStreamVO;
import com.xucheng.aicareer.vo.CareerPlanVO;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

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

    @Test
    void chatStreamReturnsNamedServerSentEvents() {
        CareerPlanService careerPlanService = mock(CareerPlanService.class);
        CareerChatService careerChatService = mock(CareerChatService.class);
        CareerController controller = new CareerController(careerPlanService, careerChatService);
        CareerChatDTO request = new CareerChatDTO();
        request.setMessage("Redis应该怎么学？");
        CareerChatStreamVO delta = CareerChatStreamVO.delta("career:10001", "先掌握数据结构。");
        CareerChatStreamVO done = CareerChatStreamVO.done("career:10001");
        when(careerChatService.chatStream(request)).thenReturn(Flux.just(delta, done));

        List<ServerSentEvent<CareerChatStreamVO>> events = controller.chatStream(request).collectList().block();

        assertThat(events).extracting(ServerSentEvent::event).containsExactly("delta", "done");
        assertThat(events).extracting(ServerSentEvent::data).containsExactly(delta, done);
        verify(careerChatService).chatStream(request);
    }

    @Test
    void conversationManagementEndpointsDelegateToService() {
        CareerPlanService careerPlanService = mock(CareerPlanService.class);
        CareerChatService careerChatService = mock(CareerChatService.class);
        CareerController controller = new CareerController(careerPlanService, careerChatService);
        String conversationId = "career:10001:2c08d11b-88b9-4d63-8cc3-0a79d86e4695";
        CareerChatSessionVO session = CareerChatSessionVO.builder()
                .conversationId(conversationId)
                .title("Java实习准备")
                .status(1)
                .messageCount(2)
                .build();
        CareerChatMessageVO message = CareerChatMessageVO.builder()
                .id(1L)
                .clientMessageId("f4582584-602f-47b9-9567-6549bda65908")
                .role("user")
                .content("如何准备实习？")
                .status(1)
                .messageOrder(1)
                .build();
        CareerChatSessionUpdateDTO update = new CareerChatSessionUpdateDTO();
        update.setTitle("Java实习准备");
        when(careerChatService.createConversation()).thenReturn(session);
        when(careerChatService.getConversations(true)).thenReturn(List.of(session));
        when(careerChatService.getMessages(conversationId)).thenReturn(List.of(message));
        when(careerChatService.updateConversation(conversationId, update)).thenReturn(session);

        assertThat(controller.createConversation().getData()).isSameAs(session);
        assertThat(controller.getConversations(true).getData()).containsExactly(session);
        assertThat(controller.getConversationMessages(conversationId).getData()).containsExactly(message);
        assertThat(controller.updateConversation(conversationId, update).getData()).isSameAs(session);
        assertThat(controller.clearConversation(conversationId).getCode()).isEqualTo(200);
        assertThat(controller.deleteConversation(conversationId).getCode()).isEqualTo(200);

        verify(careerChatService).clearConversation(conversationId);
        verify(careerChatService).deleteConversation(conversationId);
    }

    @Test
    void generatePlanReturnsUnifiedSuccessResult() {
        CareerPlanService careerPlanService = mock(CareerPlanService.class);
        CareerChatService careerChatService = mock(CareerChatService.class);
        CareerController controller = new CareerController(careerPlanService, careerChatService);
        CareerPlanVO plan = CareerPlanVO.builder()
                .id(20001L)
                .version(1)
                .targetPosition("Java后端开发工程师")
                .build();
        when(careerPlanService.generatePlan()).thenReturn(plan);

        Result<CareerPlanVO> result = controller.generatePlan();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("职业规划生成成功");
        assertThat(result.getData()).isSameAs(plan);
        verify(careerPlanService).generatePlan();
    }
}
