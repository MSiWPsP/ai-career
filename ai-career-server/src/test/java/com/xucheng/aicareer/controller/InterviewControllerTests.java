package com.xucheng.aicareer.controller;

import com.xucheng.aicareer.common.Result;
import com.xucheng.aicareer.dto.InterviewAnswerDTO;
import com.xucheng.aicareer.dto.StartInterviewDTO;
import com.xucheng.aicareer.service.InterviewService;
import com.xucheng.aicareer.vo.InterviewFinishVO;
import com.xucheng.aicareer.vo.InterviewStartVO;
import com.xucheng.aicareer.vo.InterviewTurnVO;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewControllerTests {

    @Test
    void interviewFlowEndpointsDelegateToService() {
        InterviewService service = mock(InterviewService.class);
        InterviewController controller = new InterviewController(service);
        StartInterviewDTO startDTO = new StartInterviewDTO();
        startDTO.setTargetPosition("Java后端开发工程师");
        startDTO.setInterviewType("TECHNICAL");
        startDTO.setDifficulty("MEDIUM");
        startDTO.setMaxQuestions(5);
        InterviewAnswerDTO answerDTO = new InterviewAnswerDTO();
        answerDTO.setAnswer("我会先分析问题并验证假设。");
        InterviewStartVO startVO = InterviewStartVO.builder().interviewId(30001L).status(1).build();
        InterviewTurnVO turnVO = InterviewTurnVO.builder().interviewId(30001L).finished(false).build();
        InterviewFinishVO finishVO = InterviewFinishVO.builder().interviewId(30001L).status(3).build();
        when(service.startInterview(startDTO)).thenReturn(startVO);
        when(service.answerInterview(30001L, answerDTO)).thenReturn(turnVO);
        when(service.finishInterview(30001L)).thenReturn(finishVO);

        Result<InterviewStartVO> started = controller.startInterview(startDTO);
        Result<InterviewTurnVO> answered = controller.answerInterview(30001L, answerDTO);
        Result<InterviewFinishVO> finished = controller.finishInterview(30001L);

        assertThat(started.getMessage()).isEqualTo("面试开始");
        assertThat(started.getData()).isSameAs(startVO);
        assertThat(answered.getData()).isSameAs(turnVO);
        assertThat(finished.getMessage()).isEqualTo("面试已结束");
        assertThat(finished.getData()).isSameAs(finishVO);
        verify(service).startInterview(startDTO);
        verify(service).answerInterview(30001L, answerDTO);
        verify(service).finishInterview(30001L);
    }

    @Test
    void interviewIdentifiersAreSerializedAsStringsForJavaScriptSafety() throws Exception {
        InterviewStartVO response = InterviewStartVO.builder()
                .interviewId(2098634308974727170L)
                .conversationId("interview:2098634308974727170")
                .status(1)
                .build();

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json).contains("\"interviewId\":\"2098634308974727170\"");
    }
}
