package com.xucheng.aicareer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xucheng.aicareer.agent.interview.InterviewReportAgent;
import com.xucheng.aicareer.agent.interview.dto.InterviewReportResult;
import com.xucheng.aicareer.entity.Interview;
import com.xucheng.aicareer.entity.InterviewMessage;
import com.xucheng.aicareer.entity.InterviewReport;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.mapper.InterviewMapper;
import com.xucheng.aicareer.mapper.InterviewMessageMapper;
import com.xucheng.aicareer.mapper.InterviewReportMapper;
import com.xucheng.aicareer.service.InterviewReportService;
import com.xucheng.aicareer.service.InterviewAbilityWritebackService;
import com.xucheng.aicareer.service.UserProfileService;
import com.xucheng.aicareer.service.UserSkillService;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.InterviewReportVO;
import com.xucheng.aicareer.vo.InterviewSuggestionVO;
import com.xucheng.aicareer.vo.UserProfileVO;
import com.xucheng.aicareer.vo.UserSkillVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/** 面试报告生成与落库实现；单面试串行化及唯一索引共同保证重复请求不会产生两份报告。 */
@Service
public class InterviewReportServiceImpl implements InterviewReportService {

    private static final TypeReference<Map<String, Integer>> SCORE_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<InterviewSuggestionVO>> SUGGESTION_TYPE = new TypeReference<>() {};

    private final InterviewMapper interviewMapper;
    private final InterviewMessageMapper messageMapper;
    private final InterviewReportMapper reportMapper;
    private final UserProfileService profileService;
    private final UserSkillService skillService;
    private final InterviewReportAgent reportAgent;
    private final InterviewAbilityWritebackService abilityWritebackService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final Object[] locks = new Object[64];

    public InterviewReportServiceImpl(
            InterviewMapper interviewMapper, InterviewMessageMapper messageMapper,
            InterviewReportMapper reportMapper, UserProfileService profileService,
            UserSkillService skillService, InterviewReportAgent reportAgent,
            InterviewAbilityWritebackService abilityWritebackService,
            TransactionTemplate transactionTemplate, ObjectMapper objectMapper) {
        this.interviewMapper = interviewMapper;
        this.messageMapper = messageMapper;
        this.reportMapper = reportMapper;
        this.profileService = profileService;
        this.skillService = skillService;
        this.reportAgent = reportAgent;
        this.abilityWritebackService = abilityWritebackService;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
        for (int index = 0; index < locks.length; index++) locks[index] = new Object();
    }

    @Override
    public InterviewReportVO generateReport(Long interviewId) {
        synchronized (locks[Math.floorMod(Long.hashCode(interviewId), locks.length)]) {
            Long userId = UserContext.getUserId();
            Interview interview = interviewMapper.selectOne(Wrappers.<Interview>lambdaQuery()
                    .eq(Interview::getId, interviewId).eq(Interview::getUserId, userId));
            if (interview == null) throw new BusinessException(404, "模拟面试不存在");
            if (interview.getStatus() != 2 && interview.getStatus() != 3) {
                throw new BusinessException(409, "面试结束后才能生成报告");
            }
            InterviewReport existing = findReport(interviewId, userId);
            if (existing != null) {
                // 已有报告可能生成于能力回写功能上线前，显式重试时补齐一次写回。
                transactionTemplate.execute(status -> {
                    abilityWritebackService.writeback(userId, interviewId,
                            objectMapper.readValue(existing.getScores(), SCORE_TYPE));
                    return null;
                });
                return toVO(existing);
            }

            List<InterviewMessage> messages = messageMapper.selectList(Wrappers.<InterviewMessage>lambdaQuery()
                    .eq(InterviewMessage::getInterviewId, interviewId)
                    .eq(InterviewMessage::getUserId, userId)
                    .orderByAsc(InterviewMessage::getMessageOrder));
            if (messages.stream().noneMatch(message -> "user".equals(message.getRole()))) {
                throw new BusinessException(409, "尚未回答任何问题，无法生成面试报告");
            }
            UserProfileVO profile = profileService.getCurrentProfile();
            List<UserSkillVO> skills = skillService.getCurrentUserSkills();
            // 只传业务所需字段；隐藏评分保留在数据库，不通过历史消息接口暴露给候选人。
            InterviewReportResult result = reportAgent.generate(userId,
                    new ReportInput(interview.getTargetPosition(), interview.getInterviewType(),
                            interview.getDifficulty(), interview.getQuestionCount(), profile, skills,
                            messages.stream().map(message -> new ReportMessage(message.getRole(),
                                    message.getContent(), message.getQuestionCategory(),
                                    message.getScore(), message.getEvaluation())).toList()));
            InterviewReport saved = transactionTemplate.execute(status -> {
                InterviewReport duplicate = findReport(interviewId, userId);
                if (duplicate != null) {
                    abilityWritebackService.writeback(userId, interviewId,
                            objectMapper.readValue(duplicate.getScores(), SCORE_TYPE));
                    return duplicate;
                }
                InterviewReport report = new InterviewReport();
                report.setInterviewId(interviewId);
                report.setUserId(userId);
                report.setTotalScore(result.getTotalScore());
                report.setScores(writeJson(result.getScores()));
                report.setAdvantages(writeJson(result.getAdvantages()));
                report.setWeaknesses(writeJson(result.getWeaknesses()));
                report.setSuggestions(writeJson(result.getSuggestions()));
                report.setSummary(result.getSummary().trim());
                reportMapper.insert(report);
                abilityWritebackService.writeback(userId, interviewId, result.getScores());
                return report;
            });
            if (saved == null) throw new BusinessException(500, "面试报告保存失败");
            return toVO(saved);
        }
    }

    private InterviewReport findReport(Long interviewId, Long userId) {
        return reportMapper.selectOne(Wrappers.<InterviewReport>lambdaQuery()
                .eq(InterviewReport::getInterviewId, interviewId)
                .eq(InterviewReport::getUserId, userId));
    }

    private InterviewReportVO toVO(InterviewReport report) {
        try {
            return InterviewReportVO.builder().id(report.getId()).interviewId(report.getInterviewId())
                    .totalScore(report.getTotalScore())
                    .scores(objectMapper.readValue(report.getScores(), SCORE_TYPE))
                    .advantages(objectMapper.readValue(report.getAdvantages(), STRING_LIST_TYPE))
                    .weaknesses(objectMapper.readValue(report.getWeaknesses(), STRING_LIST_TYPE))
                    .suggestions(objectMapper.readValue(report.getSuggestions(), SUGGESTION_TYPE))
                    .summary(report.getSummary()).createTime(report.getCreateTime())
                    .updateTime(report.getUpdateTime()).build();
        } catch (Exception exception) {
            throw new BusinessException(500, "面试报告数据异常");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BusinessException(500, "面试报告保存失败");
        }
    }

    private record ReportInput(String targetPosition, String interviewType, String difficulty,
                               Integer questionCount, UserProfileVO profile,
                               List<UserSkillVO> skills, List<ReportMessage> messages) {}

    private record ReportMessage(String role, String content, String topic,
                                 Integer score, String evaluation) {}
}
