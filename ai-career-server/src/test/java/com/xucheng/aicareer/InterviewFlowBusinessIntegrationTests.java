package com.xucheng.aicareer;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xucheng.aicareer.agent.interview.InterviewerAgent;
import com.xucheng.aicareer.agent.interview.InterviewReportAgent;
import com.xucheng.aicareer.agent.interview.dto.InterviewReportResult;
import com.xucheng.aicareer.agent.interview.dto.InterviewTurnResult;
import com.xucheng.aicareer.dto.InterviewAnswerDTO;
import com.xucheng.aicareer.dto.RegisterDTO;
import com.xucheng.aicareer.dto.StartInterviewDTO;
import com.xucheng.aicareer.entity.Interview;
import com.xucheng.aicareer.entity.AbilityScore;
import com.xucheng.aicareer.entity.InterviewMessage;
import com.xucheng.aicareer.entity.InterviewReport;
import com.xucheng.aicareer.entity.User;
import com.xucheng.aicareer.entity.UserProfile;
import com.xucheng.aicareer.entity.UserSkill;
import com.xucheng.aicareer.mapper.InterviewMapper;
import com.xucheng.aicareer.mapper.AbilityScoreMapper;
import com.xucheng.aicareer.mapper.InterviewMessageMapper;
import com.xucheng.aicareer.mapper.InterviewReportMapper;
import com.xucheng.aicareer.mapper.UserMapper;
import com.xucheng.aicareer.mapper.UserProfileMapper;
import com.xucheng.aicareer.mapper.UserSkillMapper;
import com.xucheng.aicareer.service.AuthService;
import com.xucheng.aicareer.service.InterviewService;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.InterviewStartVO;
import com.xucheng.aicareer.vo.InterviewTurnVO;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@SpringBootTest
@Transactional
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class InterviewFlowBusinessIntegrationTests {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserSkillMapper userSkillMapper;
    private final InterviewMapper interviewMapper;
    private final InterviewMessageMapper interviewMessageMapper;
    private final InterviewReportMapper interviewReportMapper;
    private final AbilityScoreMapper abilityScoreMapper;
    private final InterviewService interviewService;

    @MockitoBean
    private InterviewerAgent interviewerAgent;

    @MockitoBean
    private InterviewReportAgent reportAgent;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void startAnswerAndAutomaticFinishPersistARecoverableConversation() {
        Long userId = createUserWithContext();
        UserContext.setUserId(userId);
        when(interviewerAgent.start(eq(userId), any(), any())).thenReturn(turn(
                "请介绍一下Spring Boot自动配置原理。", false, "NEXT_TOPIC"));
        when(interviewerAgent.answer(eq(userId), any(), any(), any(), eq(true))).thenReturn(turn(
                "本次面试到这里，感谢你的回答。", true, "FINISH"));
        when(reportAgent.generate(eq(userId), any())).thenReturn(report());

        InterviewStartVO started = interviewService.startInterview(startRequest(1));
        InterviewTurnVO finished = interviewService.answerInterview(
                started.getInterviewId(), answer("我会从自动配置注解和条件装配机制说明。"));

        Interview interview = interviewMapper.selectById(started.getInterviewId());
        List<InterviewMessage> messages = interviewMessageMapper.selectList(
                Wrappers.<InterviewMessage>lambdaQuery()
                        .eq(InterviewMessage::getInterviewId, started.getInterviewId())
                        .orderByAsc(InterviewMessage::getMessageOrder));
        assertThat(started.getConversationId()).isEqualTo("interview:" + started.getInterviewId());
        assertThat(started.getQuestionCount()).isEqualTo(1);
        assertThat(finished.getFinished()).isTrue();
        assertThat(finished.getQuestionCount()).isEqualTo(1);
        assertThat(finished.getReportId()).isNotNull();
        assertThat(interview.getStatus()).isEqualTo(2);
        assertThat(interview.getEndTime()).isNotNull();
        assertThat(messages).extracting(InterviewMessage::getRole)
                .containsExactly("assistant", "user", "assistant");
        assertThat(messages).extracting(InterviewMessage::getMessageOrder).containsExactly(1, 2, 3);
        assertThat(messages.getLast().getScore()).isEqualTo(82);
        assertThat(messages.getLast().getEvaluation()).isEqualTo("回答较完整");
        assertThat(interviewReportMapper.selectCount(Wrappers.<InterviewReport>lambdaQuery()
                .eq(InterviewReport::getInterviewId, interview.getId()))).isEqualTo(1);
        assertThat(abilityScoreMapper.selectList(Wrappers.<AbilityScore>lambdaQuery()
                .eq(AbilityScore::getSourceType, "INTERVIEW")
                .eq(AbilityScore::getSourceId, interview.getId())))
                .singleElement().satisfies(score -> {
                    assertThat(score.getAbilityName()).isEqualTo("Java集合框架");
                    assertThat(score.getScore()).isEqualTo(69);
                });
        UserSkill updatedSkill = userSkillMapper.selectOne(Wrappers.<UserSkill>lambdaQuery()
                .eq(UserSkill::getUserId, userId).eq(UserSkill::getSkillName, "Java"));
        assertThat(updatedSkill.getScore()).isEqualTo(69);
        assertThat(updatedSkill.getSource()).isEqualTo("INTERVIEW");
        assertThat(interviewService.generateReport(interview.getId()).getId()).isEqualTo(finished.getReportId());
        assertThat(interviewReportMapper.selectCount(Wrappers.<InterviewReport>lambdaQuery()
                .eq(InterviewReport::getInterviewId, interview.getId()))).isEqualTo(1);
        assertThat(abilityScoreMapper.selectCount(Wrappers.<AbilityScore>lambdaQuery()
                .eq(AbilityScore::getSourceId, interview.getId()))).isEqualTo(1);
        assertThat(userSkillMapper.selectById(updatedSkill.getId()).getScore()).isEqualTo(69);
    }

    @Test
    void manualFinishIsIdempotentAndAddsClosingMessage() {
        Long userId = createUserWithContext();
        UserContext.setUserId(userId);
        when(interviewerAgent.start(eq(userId), any(), any())).thenReturn(turn(
                "请介绍一个你最熟悉的项目。", false, "NEXT_TOPIC"));
        InterviewStartVO started = interviewService.startInterview(startRequest(5));

        assertThat(interviewService.finishInterview(started.getInterviewId()).getStatus()).isEqualTo(3);
        assertThat(interviewService.finishInterview(started.getInterviewId()).getStatus()).isEqualTo(3);

        List<InterviewMessage> messages = interviewMessageMapper.selectList(
                Wrappers.<InterviewMessage>lambdaQuery()
                        .eq(InterviewMessage::getInterviewId, started.getInterviewId())
                        .orderByAsc(InterviewMessage::getMessageOrder));
        assertThat(messages).hasSize(2);
        assertThat(messages.getLast().getContent()).contains("主动结束");
    }

    @Test
    void answerCanDynamicallyIncreaseDifficultyAndContinue() {
        Long userId = createUserWithContext();
        UserContext.setUserId(userId);
        when(interviewerAgent.start(eq(userId), any(), any())).thenReturn(turn(
                "请介绍一下HashMap的数据结构。", false, "NEXT_TOPIC"));
        InterviewTurnResult harderQuestion = turn(
                "请进一步说明HashMap扩容时如何迁移数据。", false, "INCREASE_DIFFICULTY");
        harderQuestion.setNextDifficulty("HARD");
        when(interviewerAgent.answer(eq(userId), any(), any(), any(), eq(false))).thenReturn(harderQuestion);
        InterviewStartVO started = interviewService.startInterview(startRequest(5));

        InterviewTurnVO nextTurn = interviewService.answerInterview(
                started.getInterviewId(), answer("JDK8主要使用数组、链表和红黑树。"));

        Interview interview = interviewMapper.selectById(started.getInterviewId());
        assertThat(nextTurn.getFinished()).isFalse();
        assertThat(nextTurn.getQuestionCount()).isEqualTo(2);
        assertThat(interview.getDifficulty()).isEqualTo("HARD");
        assertThat(interview.getStatus()).isEqualTo(1);
        assertThat(interviewMessageMapper.selectList(Wrappers.<InterviewMessage>lambdaQuery()
                        .eq(InterviewMessage::getInterviewId, started.getInterviewId())
                        .orderByAsc(InterviewMessage::getMessageOrder)))
                .extracting(InterviewMessage::getMessageOrder)
                .containsExactly(1, 2, 3);
    }

    @Test
    void failedReportCanBeRetriedWithoutRepeatingTheInterviewTurn() {
        Long userId = createUserWithContext();
        UserContext.setUserId(userId);
        when(interviewerAgent.start(eq(userId), any(), any())).thenReturn(turn(
                "请简述索引的作用。", false, "NEXT_TOPIC"));
        when(interviewerAgent.answer(eq(userId), any(), any(), any(), eq(true))).thenReturn(turn(
                "本次面试结束。", true, "FINISH"));
        when(reportAgent.generate(eq(userId), any()))
                .thenThrow(new IllegalStateException("模型暂不可用"))
                .thenReturn(report());

        InterviewStartVO started = interviewService.startInterview(startRequest(1));
        InterviewTurnVO turn = interviewService.answerInterview(started.getInterviewId(), answer("索引可以加速查询。"));
        assertThat(turn.getFinished()).isTrue();
        assertThat(turn.getReportId()).isNull();
        assertThat(interviewMapper.selectById(started.getInterviewId()).getStatus()).isEqualTo(2);

        assertThat(interviewService.generateReport(started.getInterviewId()).getTotalScore()).isEqualTo(82);
        assertThat(interviewMessageMapper.selectCount(Wrappers.<InterviewMessage>lambdaQuery()
                .eq(InterviewMessage::getInterviewId, started.getInterviewId()))).isEqualTo(3);
        verify(reportAgent, times(2)).generate(eq(userId), any());
    }

    private Long createUserWithContext() {
        String username = "flow_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername(username);
        registerDTO.setPassword("123456");
        registerDTO.setNickname("面试流程用户");
        authService.register(registerDTO);
        Long userId = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username)).getId();

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setMajor("软件工程");
        profile.setTargetPosition("Java后端开发工程师");
        userProfileMapper.insert(profile);
        UserSkill skill = new UserSkill();
        skill.setUserId(userId);
        skill.setSkillName("Java");
        skill.setSkillCategory("PROGRAMMING");
        skill.setLevel(3);
        skill.setScore(60);
        skill.setSource("SELF");
        userSkillMapper.insert(skill);
        return userId;
    }

    private StartInterviewDTO startRequest(int maxQuestions) {
        StartInterviewDTO request = new StartInterviewDTO();
        request.setTargetPosition("Java后端开发工程师");
        request.setInterviewType("TECHNICAL");
        request.setDifficulty("MEDIUM");
        request.setMaxQuestions(maxQuestions);
        return request;
    }

    private InterviewAnswerDTO answer(String content) {
        InterviewAnswerDTO request = new InterviewAnswerDTO();
        request.setAnswer(content);
        return request;
    }

    private InterviewTurnResult turn(String response, boolean finished, String action) {
        InterviewTurnResult result = new InterviewTurnResult();
        result.setResponse(response);
        result.setTopic("Spring Boot");
        result.setScore(finished ? 82 : 0);
        result.setEvaluation(finished ? "回答较完整" : "初始化问题");
        result.setNextAction(action);
        result.setNextDifficulty("MEDIUM");
        result.setFinished(finished);
        return result;
    }

    private InterviewReportResult report() {
        InterviewReportResult result = new InterviewReportResult();
        result.setTotalScore(82);
        result.setScores(java.util.Map.of("Java集合框架", 82));
        result.setAdvantages(List.of("条件装配思路清晰"));
        result.setWeaknesses(List.of("缺少边界场景说明"));
        result.setSuggestions(List.of(com.xucheng.aicareer.vo.InterviewSuggestionVO.builder()
                .topic("Spring Boot").priority("MEDIUM").content("补充自动配置加载过程").build()));
        result.setSummary("本次回答有一定基础，建议继续练习。");
        return result;
    }
}
