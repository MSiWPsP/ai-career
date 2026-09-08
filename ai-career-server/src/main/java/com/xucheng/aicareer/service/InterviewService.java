package com.xucheng.aicareer.service;

import com.xucheng.aicareer.vo.InterviewHistoryRecordVO;
import com.xucheng.aicareer.vo.InterviewMessageVO;
import com.xucheng.aicareer.vo.InterviewReportVO;
import com.xucheng.aicareer.vo.InterviewVO;
import com.xucheng.aicareer.vo.PageResultVO;

import java.util.List;

public interface InterviewService {

    InterviewVO getInterviewById(Long interviewId);

    List<InterviewMessageVO> getInterviewMessages(Long interviewId);

    InterviewReportVO getInterviewReport(Long interviewId);

    PageResultVO<InterviewHistoryRecordVO> getInterviewHistory(int page, int pageSize);
}
