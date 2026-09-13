package com.xucheng.aicareer.service;

import com.xucheng.aicareer.vo.InterviewReportVO;

/** 面试报告业务边界：验证面试归属与完成状态，生成或复用唯一报告。 */
public interface InterviewReportService {
    /** 报告生成可重试且幂等；模型失败不会删除已结束的面试记录。 */
    InterviewReportVO generateReport(Long interviewId);
}
