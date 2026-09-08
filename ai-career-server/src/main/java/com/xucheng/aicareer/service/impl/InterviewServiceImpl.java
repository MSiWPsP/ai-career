package com.xucheng.aicareer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xucheng.aicareer.entity.Interview;
import com.xucheng.aicareer.entity.InterviewMessage;
import com.xucheng.aicareer.entity.InterviewReport;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.mapper.InterviewMapper;
import com.xucheng.aicareer.mapper.InterviewMessageMapper;
import com.xucheng.aicareer.mapper.InterviewReportMapper;
import com.xucheng.aicareer.service.InterviewService;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.InterviewHistoryRecordVO;
import com.xucheng.aicareer.vo.InterviewMessageVO;
import com.xucheng.aicareer.vo.InterviewReportVO;
import com.xucheng.aicareer.vo.InterviewSuggestionVO;
import com.xucheng.aicareer.vo.InterviewVO;
import com.xucheng.aicareer.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private static final TypeReference<Map<String, Integer>> SCORE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<InterviewSuggestionVO>> SUGGESTION_LIST_TYPE = new TypeReference<>() {
    };

    private final InterviewMapper interviewMapper;
    private final InterviewMessageMapper interviewMessageMapper;
    private final InterviewReportMapper interviewReportMapper;
    private final ObjectMapper objectMapper;

    @Override
    public InterviewVO getInterviewById(Long interviewId) {
        return toInterviewVO(getRequiredInterview(UserContext.getUserId(), interviewId));
    }

    @Override
    public List<InterviewMessageVO> getInterviewMessages(Long interviewId) {
        Long userId = UserContext.getUserId();
        getRequiredInterview(userId, interviewId);
        return interviewMessageMapper.selectList(Wrappers.<InterviewMessage>lambdaQuery()
                        .eq(InterviewMessage::getInterviewId, interviewId)
                        .eq(InterviewMessage::getUserId, userId)
                        .orderByAsc(InterviewMessage::getMessageOrder)
                        .orderByAsc(InterviewMessage::getId))
                .stream()
                .map(this::toInterviewMessageVO)
                .toList();
    }

    @Override
    public InterviewReportVO getInterviewReport(Long interviewId) {
        Long userId = UserContext.getUserId();
        getRequiredInterview(userId, interviewId);
        InterviewReport report = interviewReportMapper.selectOne(Wrappers.<InterviewReport>lambdaQuery()
                .eq(InterviewReport::getInterviewId, interviewId)
                .eq(InterviewReport::getUserId, userId));
        if (report == null) {
            throw new BusinessException(404, "面试报告不存在");
        }
        return toInterviewReportVO(report);
    }

    @Override
    public PageResultVO<InterviewHistoryRecordVO> getInterviewHistory(int page, int pageSize) {
        Long userId = UserContext.getUserId();
        long total = interviewMapper.selectCount(Wrappers.<Interview>lambdaQuery()
                .eq(Interview::getUserId, userId));
        if (total == 0) {
            return PageResultVO.<InterviewHistoryRecordVO>builder()
                    .records(List.of())
                    .total(0L)
                    .build();
        }

        long offset = (long) (page - 1) * pageSize;
        List<Interview> interviews = interviewMapper.selectList(Wrappers.<Interview>lambdaQuery()
                .eq(Interview::getUserId, userId)
                .orderByDesc(Interview::getCreateTime)
                .orderByDesc(Interview::getId)
                .last("LIMIT " + offset + ", " + pageSize));
        if (interviews.isEmpty()) {
            return PageResultVO.<InterviewHistoryRecordVO>builder()
                    .records(List.of())
                    .total(total)
                    .build();
        }

        List<Long> interviewIds = interviews.stream().map(Interview::getId).toList();
        Map<Long, Integer> scoreByInterviewId = new LinkedHashMap<>();
        interviewReportMapper.selectList(Wrappers.<InterviewReport>lambdaQuery()
                        .select(InterviewReport::getInterviewId, InterviewReport::getTotalScore)
                        .eq(InterviewReport::getUserId, userId)
                        .in(InterviewReport::getInterviewId, interviewIds))
                .forEach(report -> scoreByInterviewId.put(report.getInterviewId(), report.getTotalScore()));

        List<InterviewHistoryRecordVO> records = interviews.stream()
                .map(interview -> toHistoryRecordVO(
                        interview,
                        scoreByInterviewId.get(interview.getId())))
                .toList();
        return PageResultVO.<InterviewHistoryRecordVO>builder()
                .records(records)
                .total(total)
                .build();
    }

    private Interview getRequiredInterview(Long userId, Long interviewId) {
        Interview interview = interviewMapper.selectOne(Wrappers.<Interview>lambdaQuery()
                .eq(Interview::getId, interviewId)
                .eq(Interview::getUserId, userId));
        if (interview == null) {
            throw new BusinessException(404, "模拟面试不存在");
        }
        return interview;
    }

    private InterviewVO toInterviewVO(Interview interview) {
        return InterviewVO.builder()
                .id(interview.getId())
                .targetPosition(interview.getTargetPosition())
                .interviewType(interview.getInterviewType())
                .difficulty(interview.getDifficulty())
                .status(interview.getStatus())
                .conversationId(interview.getConversationId())
                .questionCount(interview.getQuestionCount())
                .startTime(interview.getStartTime())
                .endTime(interview.getEndTime())
                .createTime(interview.getCreateTime())
                .updateTime(interview.getUpdateTime())
                .build();
    }

    private InterviewMessageVO toInterviewMessageVO(InterviewMessage message) {
        return InterviewMessageVO.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .questionCategory(message.getQuestionCategory())
                .questionLevel(message.getQuestionLevel())
                .messageOrder(message.getMessageOrder())
                .createTime(message.getCreateTime())
                .build();
    }

    private InterviewHistoryRecordVO toHistoryRecordVO(Interview interview, Integer totalScore) {
        return InterviewHistoryRecordVO.builder()
                .id(interview.getId())
                .targetPosition(interview.getTargetPosition())
                .interviewType(interview.getInterviewType())
                .difficulty(interview.getDifficulty())
                .status(interview.getStatus())
                .totalScore(totalScore)
                .createTime(interview.getCreateTime())
                .build();
    }

    private InterviewReportVO toInterviewReportVO(InterviewReport report) {
        return InterviewReportVO.builder()
                .id(report.getId())
                .interviewId(report.getInterviewId())
                .totalScore(report.getTotalScore())
                .scores(readJson(report.getScores(), SCORE_TYPE, LinkedHashMap::new))
                .advantages(readJson(report.getAdvantages(), STRING_LIST_TYPE, Collections::emptyList))
                .weaknesses(readJson(report.getWeaknesses(), STRING_LIST_TYPE, Collections::emptyList))
                .suggestions(readJson(report.getSuggestions(), SUGGESTION_LIST_TYPE, Collections::emptyList))
                .summary(report.getSummary())
                .createTime(report.getCreateTime())
                .updateTime(report.getUpdateTime())
                .build();
    }

    private <T> T readJson(String json, TypeReference<T> type, Supplier<T> emptyValueSupplier) {
        if (json == null || json.isBlank()) {
            return emptyValueSupplier.get();
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new BusinessException(500, "面试报告数据格式异常");
        }
    }
}
