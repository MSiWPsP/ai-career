package com.xucheng.aicareer.controller;

import com.xucheng.aicareer.common.Result;
import com.xucheng.aicareer.service.InterviewService;
import com.xucheng.aicareer.vo.InterviewHistoryRecordVO;
import com.xucheng.aicareer.vo.InterviewMessageVO;
import com.xucheng.aicareer.vo.InterviewReportVO;
import com.xucheng.aicareer.vo.InterviewVO;
import com.xucheng.aicareer.vo.PageResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
@Tag(name = "模拟面试")
@SecurityRequirement(name = "BearerAuth")
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping("/history")
    @Operation(summary = "分页获取历史面试")
    public Result<PageResultVO<InterviewHistoryRecordVO>> getInterviewHistory(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页数量不能小于1")
            @Max(value = 100, message = "每页数量不能大于100") int pageSize) {
        return Result.success(interviewService.getInterviewHistory(page, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取面试详情")
    public Result<InterviewVO> getInterviewById(@PathVariable Long id) {
        return Result.success(interviewService.getInterviewById(id));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "获取完整面试消息记录")
    public Result<List<InterviewMessageVO>> getInterviewMessages(@PathVariable Long id) {
        return Result.success(interviewService.getInterviewMessages(id));
    }

    @GetMapping("/{id}/report")
    @Operation(summary = "获取面试报告")
    public Result<InterviewReportVO> getInterviewReport(@PathVariable Long id) {
        return Result.success(interviewService.getInterviewReport(id));
    }
}
