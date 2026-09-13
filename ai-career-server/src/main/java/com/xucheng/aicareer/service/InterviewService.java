package com.xucheng.aicareer.service;

import com.xucheng.aicareer.dto.InterviewAnswerDTO;
import com.xucheng.aicareer.dto.StartInterviewDTO;
import com.xucheng.aicareer.vo.InterviewFinishVO;
import com.xucheng.aicareer.vo.InterviewHistoryRecordVO;
import com.xucheng.aicareer.vo.InterviewMessageVO;
import com.xucheng.aicareer.vo.InterviewReportVO;
import com.xucheng.aicareer.vo.InterviewStartVO;
import com.xucheng.aicareer.vo.InterviewTurnVO;
import com.xucheng.aicareer.vo.InterviewVO;
import com.xucheng.aicareer.vo.PageResultVO;

import java.util.List;

/** 模拟面试应用服务，负责会话状态、消息持久化、权限校验和 Agent 编排。 */
public interface InterviewService {

    /** 创建独立面试会话，并在 Agent 成功生成第一题后进入进行中状态。 */
    InterviewStartVO startInterview(StartInterviewDTO startDTO);

    /** 保存当前回答，调用 Agent 动态决策并返回下一问题或结束语。 */
    InterviewTurnVO answerInterview(Long interviewId, InterviewAnswerDTO answerDTO);

    /** 由用户主动结束进行中的面试；本阶段不生成面试报告。 */
    InterviewFinishVO finishInterview(Long interviewId);

    /** 对已结束面试生成或复用报告，供自动生成失败后的显式重试。 */
    InterviewReportVO generateReport(Long interviewId);

    InterviewVO getInterviewById(Long interviewId);

    List<InterviewMessageVO> getInterviewMessages(Long interviewId);

    InterviewReportVO getInterviewReport(Long interviewId);

    PageResultVO<InterviewHistoryRecordVO> getInterviewHistory(int page, int pageSize);
}
