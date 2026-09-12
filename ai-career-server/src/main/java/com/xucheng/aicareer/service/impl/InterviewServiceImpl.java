package com.xucheng.aicareer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xucheng.aicareer.agent.interview.InterviewerAgent;
import com.xucheng.aicareer.agent.interview.dto.InterviewTurnResult;
import com.xucheng.aicareer.dto.InterviewAnswerDTO;
import com.xucheng.aicareer.dto.StartInterviewDTO;
import com.xucheng.aicareer.entity.Interview;
import com.xucheng.aicareer.entity.InterviewMessage;
import com.xucheng.aicareer.entity.InterviewReport;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.mapper.InterviewMapper;
import com.xucheng.aicareer.mapper.InterviewMessageMapper;
import com.xucheng.aicareer.mapper.InterviewReportMapper;
import com.xucheng.aicareer.service.InterviewService;
import com.xucheng.aicareer.service.UserProfileService;
import com.xucheng.aicareer.service.UserSkillService;
import com.xucheng.aicareer.service.model.InterviewBusinessContext;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.InterviewFinishVO;
import com.xucheng.aicareer.vo.InterviewHistoryRecordVO;
import com.xucheng.aicareer.vo.InterviewMessageVO;
import com.xucheng.aicareer.vo.InterviewReportVO;
import com.xucheng.aicareer.vo.InterviewStartVO;
import com.xucheng.aicareer.vo.InterviewSuggestionVO;
import com.xucheng.aicareer.vo.InterviewTurnVO;
import com.xucheng.aicareer.vo.InterviewVO;
import com.xucheng.aicareer.vo.PageResultVO;
import com.xucheng.aicareer.vo.UserProfileVO;
import com.xucheng.aicareer.vo.UserSkillVO;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class InterviewServiceImpl implements InterviewService {

    private static final int STATUS_NOT_STARTED = 0;
    private static final int STATUS_IN_PROGRESS = 1;
    private static final int STATUS_COMPLETED = 2;
    private static final int STATUS_TERMINATED = 3;
    private static final int STATUS_FAILED = 4;
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String ROLE_USER = "user";
    private static final String MANUAL_FINISH_MESSAGE = "本次模拟面试已由你主动结束。完整面试记录已保存，可稍后在历史面试中查看。";
    private static final int LOCK_STRIPE_COUNT = 64;

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
    private final UserProfileService userProfileService;
    private final UserSkillService userSkillService;
    private final InterviewerAgent interviewerAgent;
    private final ChatMemory interviewerChatMemory;
    private final TransactionTemplate transactionTemplate;
    private final int memoryMessageLimit;
    private final Object[] interviewLocks = createLocks();

    public InterviewServiceImpl(
            InterviewMapper interviewMapper,
            InterviewMessageMapper interviewMessageMapper,
            InterviewReportMapper interviewReportMapper,
            ObjectMapper objectMapper,
            UserProfileService userProfileService,
            UserSkillService userSkillService,
            InterviewerAgent interviewerAgent,
            @Qualifier("interviewerChatMemory") ChatMemory interviewerChatMemory,
            TransactionTemplate transactionTemplate,
            @Value("${ai.chat-memory.interview.max-messages:30}") int memoryMessageLimit) {
        this.interviewMapper = interviewMapper;
        this.interviewMessageMapper = interviewMessageMapper;
        this.interviewReportMapper = interviewReportMapper;
        this.objectMapper = objectMapper;
        this.userProfileService = userProfileService;
        this.userSkillService = userSkillService;
        this.interviewerAgent = interviewerAgent;
        this.interviewerChatMemory = interviewerChatMemory;
        this.transactionTemplate = transactionTemplate;
        this.memoryMessageLimit = memoryMessageLimit;
    }

    @Override
    public InterviewStartVO startInterview(StartInterviewDTO startDTO) {
        Long userId = UserContext.getUserId();
        UserProfileVO profile = userProfileService.getCurrentProfile();
        List<UserSkillVO> skills = userSkillService.getCurrentUserSkills();
        Interview interview = transactionTemplate.execute(status -> createInterview(userId, startDTO));
        if (interview == null) {
            throw new BusinessException(500, "创建模拟面试失败，请稍后重试");
        }

        try {
            interviewerChatMemory.clear(interview.getConversationId());
            InterviewTurnResult firstQuestion = interviewerAgent.start(
                    userId, interview.getConversationId(), buildContext(interview, profile, skills));
            if (Boolean.TRUE.equals(firstQuestion.getFinished())) {
                throw new IllegalStateException("初始化问题不能直接结束面试");
            }
            return transactionTemplate.execute(status -> saveFirstQuestion(interview, firstQuestion));
        } catch (RuntimeException exception) {
            markInterviewFailed(interview);
            interviewerChatMemory.clear(interview.getConversationId());
            throw exception;
        }
    }

    @Override
    public InterviewTurnVO answerInterview(Long interviewId, InterviewAnswerDTO answerDTO) {
        synchronized (lockFor(interviewId)) {
            Long userId = UserContext.getUserId();
            Interview interview = getRequiredInterview(userId, interviewId);
            if (interview.getStatus() != STATUS_IN_PROGRESS) {
                throw new BusinessException(409, "当前面试已结束，不能继续提交回答");
            }

            restoreMemory(interview);
            InterviewMessage previousQuestion = getLatestAssistantMessage(userId, interviewId);
            InterviewMessage userMessage = saveUserAnswer(interview, answerDTO.getAnswer().trim(), previousQuestion);
            try {
                boolean forceFinish = interview.getQuestionCount() >= interview.getMaxQuestions();
                UserProfileVO profile = userProfileService.getCurrentProfile();
                List<UserSkillVO> skills = userSkillService.getCurrentUserSkills();
                InterviewTurnResult result = interviewerAgent.answer(
                        userId,
                        interview.getConversationId(),
                        answerDTO.getAnswer().trim(),
                        buildContext(interview, profile, skills),
                        forceFinish);
                if (forceFinish && !Boolean.TRUE.equals(result.getFinished())) {
                    throw new IllegalStateException("达到题目上限后 Agent 仍继续提问");
                }
                InterviewTurnVO response = transactionTemplate.execute(
                        status -> saveAgentTurn(interview, result, forceFinish));
                if (response == null) {
                    throw new BusinessException(500, "保存面试回答失败，请稍后重试");
                }
                return response;
            } catch (RuntimeException exception) {
                // AI 或最终落库失败时撤销本轮孤立回答，使用户可原样重试且消息顺序保持连续。
                interviewMessageMapper.deleteById(userMessage.getId());
                restoreMemory(interview);
                throw exception;
            }
        }
    }

    @Override
    public InterviewFinishVO finishInterview(Long interviewId) {
        synchronized (lockFor(interviewId)) {
            Long userId = UserContext.getUserId();
            Interview interview = getRequiredInterview(userId, interviewId);
            if (interview.getStatus() == STATUS_COMPLETED || interview.getStatus() == STATUS_TERMINATED) {
                return toFinishVO(interview);
            }
            if (interview.getStatus() != STATUS_IN_PROGRESS) {
                throw new BusinessException(409, "当前面试无法结束");
            }

            InterviewFinishVO result = transactionTemplate.execute(status -> {
                saveMessage(interview, ROLE_ASSISTANT, MANUAL_FINISH_MESSAGE, null,
                        interview.getDifficulty(), nextMessageOrder(interview.getId()));
                interview.setStatus(STATUS_TERMINATED);
                interview.setEndTime(LocalDateTime.now());
                interview.setUpdateTime(LocalDateTime.now());
                interviewMapper.updateById(interview);
                return toFinishVO(interview);
            });
            interviewerChatMemory.clear(interview.getConversationId());
            return result;
        }
    }

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
                .maxQuestions(interview.getMaxQuestions())
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

    private Interview createInterview(Long userId, StartInterviewDTO startDTO) {
        int maxQuestions = startDTO.getMaxQuestions() == null ? 5 : startDTO.getMaxQuestions();
        if (maxQuestions < 1 || maxQuestions > 15) {
            throw new BusinessException("问题数量必须在1到15之间");
        }
        Interview interview = new Interview();
        interview.setUserId(userId);
        interview.setTargetPosition(startDTO.getTargetPosition().trim());
        interview.setInterviewType(startDTO.getInterviewType());
        interview.setDifficulty(startDTO.getDifficulty());
        interview.setStatus(STATUS_NOT_STARTED);
        interview.setQuestionCount(0);
        interview.setMaxQuestions(maxQuestions);
        interview.setStartTime(LocalDateTime.now());
        interviewMapper.insert(interview);
        interview.setConversationId("interview:" + interview.getId());
        interviewMapper.updateById(interview);
        return interview;
    }

    private InterviewStartVO saveFirstQuestion(Interview interview, InterviewTurnResult result) {
        saveMessage(interview, ROLE_ASSISTANT, result.getResponse().trim(), result.getTopic().trim(),
                result.getNextDifficulty(), 1);
        interview.setDifficulty(result.getNextDifficulty());
        interview.setQuestionCount(1);
        interview.setStatus(STATUS_IN_PROGRESS);
        interview.setUpdateTime(LocalDateTime.now());
        interviewMapper.updateById(interview);
        return InterviewStartVO.builder()
                .interviewId(interview.getId())
                .conversationId(interview.getConversationId())
                .status(interview.getStatus())
                .question(result.getResponse().trim())
                .questionCount(interview.getQuestionCount())
                .maxQuestions(interview.getMaxQuestions())
                .build();
    }

    private InterviewTurnVO saveAgentTurn(
            Interview interview, InterviewTurnResult result, boolean forceFinish) {
        boolean finished = forceFinish || Boolean.TRUE.equals(result.getFinished());
        int nextOrder = nextMessageOrder(interview.getId());
        saveMessage(interview, ROLE_ASSISTANT, result.getResponse().trim(), result.getTopic().trim(),
                result.getNextDifficulty(), nextOrder);
        interview.setDifficulty(result.getNextDifficulty());
        if (finished) {
            interview.setStatus(STATUS_COMPLETED);
            interview.setEndTime(LocalDateTime.now());
        } else {
            interview.setQuestionCount(interview.getQuestionCount() + 1);
        }
        interview.setUpdateTime(LocalDateTime.now());
        interviewMapper.updateById(interview);
        if (finished) {
            interviewerChatMemory.clear(interview.getConversationId());
        }
        return InterviewTurnVO.builder()
                .interviewId(interview.getId())
                .message(result.getResponse().trim())
                .finished(finished)
                .status(interview.getStatus())
                .questionCount(interview.getQuestionCount())
                .maxQuestions(interview.getMaxQuestions())
                .build();
    }

    private InterviewMessage saveUserAnswer(
            Interview interview, String answer, InterviewMessage previousQuestion) {
        return transactionTemplate.execute(status -> saveMessage(
                interview,
                ROLE_USER,
                answer,
                previousQuestion == null ? null : previousQuestion.getQuestionCategory(),
                previousQuestion == null ? interview.getDifficulty() : previousQuestion.getQuestionLevel(),
                nextMessageOrder(interview.getId())));
    }

    private InterviewMessage saveMessage(
            Interview interview,
            String role,
            String content,
            String category,
            String level,
            int messageOrder) {
        InterviewMessage message = new InterviewMessage();
        message.setInterviewId(interview.getId());
        message.setUserId(interview.getUserId());
        message.setRole(role);
        message.setContent(content);
        message.setQuestionCategory(category);
        message.setQuestionLevel(level);
        message.setMessageOrder(messageOrder);
        interviewMessageMapper.insert(message);
        return message;
    }

    private InterviewMessage getLatestAssistantMessage(Long userId, Long interviewId) {
        return interviewMessageMapper.selectOne(Wrappers.<InterviewMessage>lambdaQuery()
                .eq(InterviewMessage::getInterviewId, interviewId)
                .eq(InterviewMessage::getUserId, userId)
                .eq(InterviewMessage::getRole, ROLE_ASSISTANT)
                .orderByDesc(InterviewMessage::getMessageOrder)
                .last("LIMIT 1"));
    }

    private int nextMessageOrder(Long interviewId) {
        InterviewMessage latest = interviewMessageMapper.selectOne(Wrappers.<InterviewMessage>lambdaQuery()
                .select(InterviewMessage::getMessageOrder)
                .eq(InterviewMessage::getInterviewId, interviewId)
                .orderByDesc(InterviewMessage::getMessageOrder)
                .last("LIMIT 1"));
        return latest == null ? 1 : latest.getMessageOrder() + 1;
    }

    private InterviewBusinessContext buildContext(
            Interview interview, UserProfileVO profile, List<UserSkillVO> skills) {
        return new InterviewBusinessContext(
                interview.getTargetPosition(),
                interview.getInterviewType(),
                interview.getDifficulty(),
                interview.getQuestionCount(),
                interview.getMaxQuestions(),
                profile,
                skills);
    }

    private void restoreMemory(Interview interview) {
        // 数据库是面试历史的事实来源；每轮重建窗口可在后端重启后继续同一场面试。
        interviewerChatMemory.clear(interview.getConversationId());
        List<InterviewMessage> persisted = new ArrayList<>(interviewMessageMapper.selectList(
                Wrappers.<InterviewMessage>lambdaQuery()
                        .eq(InterviewMessage::getInterviewId, interview.getId())
                        .eq(InterviewMessage::getUserId, interview.getUserId())
                        .orderByDesc(InterviewMessage::getMessageOrder)
                        .orderByDesc(InterviewMessage::getId)
                        .last("LIMIT " + Math.max(1, memoryMessageLimit))));
        Collections.reverse(persisted);
        List<Message> messages = persisted.stream().map(this::toSpringAiMessage).toList();
        if (!messages.isEmpty()) {
            interviewerChatMemory.add(interview.getConversationId(), messages);
        }
    }

    private Message toSpringAiMessage(InterviewMessage message) {
        if (ROLE_ASSISTANT.equals(message.getRole())) {
            return new AssistantMessage(message.getContent());
        }
        return new UserMessage(message.getContent());
    }

    private void markInterviewFailed(Interview interview) {
        interview.setStatus(STATUS_FAILED);
        interview.setEndTime(LocalDateTime.now());
        interview.setUpdateTime(LocalDateTime.now());
        interviewMapper.updateById(interview);
    }

    private InterviewFinishVO toFinishVO(Interview interview) {
        return InterviewFinishVO.builder()
                .interviewId(interview.getId())
                .status(interview.getStatus())
                .reportId(null)
                .build();
    }

    private Object lockFor(Long interviewId) {
        int index = Math.floorMod(Long.hashCode(interviewId), interviewLocks.length);
        return interviewLocks[index];
    }

    private static Object[] createLocks() {
        // 固定锁分片避免锁对象无限增长，并阻止同一面试的重复提交与主动结束并发修改消息顺序。
        Object[] locks = new Object[LOCK_STRIPE_COUNT];
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new Object();
        }
        return locks;
    }
}
