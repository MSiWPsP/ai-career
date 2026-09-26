package com.xucheng.aicareer;

import com.xucheng.aicareer.dto.RegisterDTO;
import com.xucheng.aicareer.entity.AbilityScore;
import com.xucheng.aicareer.entity.Interview;
import com.xucheng.aicareer.entity.InterviewMessage;
import com.xucheng.aicareer.entity.InterviewReport;
import com.xucheng.aicareer.entity.User;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.mapper.AbilityScoreMapper;
import com.xucheng.aicareer.mapper.InterviewMapper;
import com.xucheng.aicareer.mapper.InterviewMessageMapper;
import com.xucheng.aicareer.mapper.InterviewReportMapper;
import com.xucheng.aicareer.mapper.UserMapper;
import com.xucheng.aicareer.service.AbilityService;
import com.xucheng.aicareer.service.AuthService;
import com.xucheng.aicareer.service.CareerToolQueryService;
import com.xucheng.aicareer.service.InterviewService;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.AbilityRadarVO;
import com.xucheng.aicareer.vo.AbilityTrendVO;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class InterviewAbilityBusinessIntegrationTests {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final InterviewMapper interviewMapper;
    private final InterviewMessageMapper interviewMessageMapper;
    private final InterviewReportMapper interviewReportMapper;
    private final AbilityScoreMapper abilityScoreMapper;
    private final InterviewService interviewService;
    private final AbilityService abilityService;
    private final CareerToolQueryService careerToolQueryService;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void interviewQueriesReportsAbilityViewsAndOwnershipWorkTogether() {
        Long userId = createUser();
        UserContext.setUserId(userId);
        LocalDateTime baseTime = LocalDateTime.of(2026, 9, 1, 10, 0);

        Interview firstInterview = createInterview(userId, baseTime, 2);
        Interview latestInterview = createInterview(userId, baseTime.plusDays(9), 2);
        createMessage(userId, latestInterview.getId(), "user", "数组、链表和红黑树", 2, baseTime.plusDays(9).plusMinutes(2));
        createMessage(userId, latestInterview.getId(), "assistant", "请介绍HashMap", 1, baseTime.plusDays(9).plusMinutes(1));
        createReport(userId, latestInterview.getId(), 76, baseTime.plusDays(9).plusHours(1));

        createAbilityScore(userId, "Redis", 40, firstInterview.getId(), baseTime);
        createAbilityScore(userId, "Java", 82, latestInterview.getId(), baseTime.plusDays(8));
        createAbilityScore(userId, "Redis", 55, latestInterview.getId(), baseTime.plusDays(9));

        assertThat(interviewService.getInterviewById(latestInterview.getId()).getQuestionCount()).isEqualTo(2);
        assertThat(interviewService.getInterviewMessages(latestInterview.getId()))
                .extracting("messageOrder")
                .containsExactly(1, 2);
        assertThat(interviewService.getInterviewReport(latestInterview.getId()))
                .satisfies(report -> {
                    assertThat(report.getTotalScore()).isEqualTo(76);
                    assertThat(report.getScores()).containsEntry("Java", 82).containsEntry("Redis", 55);
                    assertThat(report.getAdvantages()).containsExactly("Java集合基础较好");
                    assertThat(report.getSuggestions()).singleElement()
                            .satisfies(suggestion -> assertThat(suggestion.getTopic()).isEqualTo("Redis"));
                });
        assertThat(interviewService.getInterviewHistory(1, 1))
                .satisfies(history -> {
                    assertThat(history.getTotal()).isEqualTo(2);
                    assertThat(history.getRecords()).singleElement()
                            .satisfies(record -> {
                                assertThat(record.getId()).isEqualTo(latestInterview.getId());
                                assertThat(record.getTotalScore()).isEqualTo(76);
                            });
                });
        assertThat(interviewService.getInterviewHistory(3, 1))
                .satisfies(history -> {
                    assertThat(history.getTotal()).isEqualTo(2);
                    assertThat(history.getRecords()).isEmpty();
                });

        assertThat(abilityService.getCurrentAbilities())
                .containsEntry("Java", 82)
                .containsEntry("Redis", 55)
                .hasSize(2);
        assertThat(abilityService.getAbilityHistory("Redis", 1, 10))
                .satisfies(history -> {
                    assertThat(history.getTotal()).isEqualTo(2);
                    assertThat(history.getRecords()).extracting("score").containsExactly(55, 40);
                });

        AbilityRadarVO radar = abilityService.getAbilityRadar();
        assertThat(radar.getIndicators()).extracting("name").containsExactly("Redis", "Java");
        assertThat(radar.getValues()).containsExactly(55, 82);

        AbilityTrendVO trend = abilityService.getAbilityTrend("Redis");
        assertThat(trend.getAbilityName()).isEqualTo("Redis");
        assertThat(trend.getRecords()).extracting("score").containsExactly(40, 55);
        assertThat(abilityService.getAbilityTrend(null).getAbilityName()).isEqualTo("Redis");
        assertThat(careerToolQueryService.getCurrentAbilitySnapshot(userId).abilities())
                .extracting("abilityName")
                .containsExactly("Redis", "Java");
        assertThat(careerToolQueryService.getRecentInterviewSnapshot(userId).interviews())
                .hasSize(2)
                .first()
                .satisfies(interview -> {
                    assertThat(interview.totalScore()).isEqualTo(76);
                    assertThat(interview.weaknesses()).containsExactly("Redis能力较弱");
                });

        Long otherUserId = createUser();
        UserContext.setUserId(otherUserId);
        assertThatThrownBy(() -> interviewService.getInterviewById(latestInterview.getId()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> interviewService.getInterviewMessages(latestInterview.getId()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> interviewService.getInterviewReport(latestInterview.getId()))
                .isInstanceOf(BusinessException.class);
        assertThat(abilityService.getCurrentAbilities()).isEmpty();
        assertThat(careerToolQueryService.getCurrentAbilitySnapshot(otherUserId).abilities()).isEmpty();
        assertThat(careerToolQueryService.getRecentInterviewSnapshot(otherUserId).interviews()).isEmpty();
    }

    private Long createUser() {
        String username = "interview_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername(username);
        registerDTO.setPassword("123456");
        registerDTO.setNickname("面试测试用户");
        authService.register(registerDTO);
        return userMapper.selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, username))
                .getId();
    }

    private Interview createInterview(Long userId, LocalDateTime createTime, int status) {
        Interview interview = new Interview();
        interview.setUserId(userId);
        interview.setTargetPosition("Java后端开发工程师");
        interview.setInterviewType("TECHNICAL");
        interview.setDifficulty("MEDIUM");
        interview.setStatus(status);
        interview.setQuestionCount(2);
        interview.setMaxQuestions(10);
        interview.setStartTime(createTime);
        interview.setEndTime(createTime.plusHours(1));
        interview.setCreateTime(createTime);
        interview.setUpdateTime(createTime.plusHours(1));
        interviewMapper.insert(interview);
        interview.setConversationId("interview:" + interview.getId());
        interviewMapper.updateById(interview);
        return interview;
    }

    private void createMessage(Long userId, Long interviewId, String role, String content,
                               int order, LocalDateTime createTime) {
        InterviewMessage message = new InterviewMessage();
        message.setInterviewId(interviewId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setQuestionCategory("Java");
        message.setQuestionLevel("MEDIUM");
        message.setMessageOrder(order);
        message.setCreateTime(createTime);
        interviewMessageMapper.insert(message);
    }

    private void createReport(Long userId, Long interviewId, int totalScore, LocalDateTime createTime) {
        InterviewReport report = new InterviewReport();
        report.setInterviewId(interviewId);
        report.setUserId(userId);
        report.setTotalScore(totalScore);
        report.setScores("{\"Java\":82,\"Redis\":55}");
        report.setAdvantages("[\"Java集合基础较好\"]");
        report.setWeaknesses("[\"Redis能力较弱\"]");
        report.setSuggestions("[{\"topic\":\"Redis\",\"priority\":\"HIGH\",\"content\":\"复习缓存体系\"}]");
        report.setSummary("具备Java后端基础能力");
        report.setCreateTime(createTime);
        report.setUpdateTime(createTime);
        interviewReportMapper.insert(report);
    }

    private void createAbilityScore(Long userId, String abilityName, int score,
                                    Long interviewId, LocalDateTime createTime) {
        AbilityScore abilityScore = new AbilityScore();
        abilityScore.setUserId(userId);
        abilityScore.setAbilityName(abilityName);
        abilityScore.setScore(score);
        abilityScore.setSourceType("INTERVIEW");
        abilityScore.setSourceId(interviewId);
        abilityScore.setCreateTime(createTime);
        abilityScoreMapper.insert(abilityScore);
    }
}
