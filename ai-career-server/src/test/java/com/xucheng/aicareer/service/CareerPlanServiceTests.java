package com.xucheng.aicareer.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.xucheng.aicareer.agent.career.CareerPlannerAgent;
import com.xucheng.aicareer.agent.career.dto.CareerPlanResult;
import com.xucheng.aicareer.agent.career.dto.CareerTaskResult;
import com.xucheng.aicareer.agent.career.dto.RoadmapStage;
import com.xucheng.aicareer.dto.CareerPlanRegenerateDTO;
import com.xucheng.aicareer.entity.CareerPlan;
import com.xucheng.aicareer.entity.CareerTask;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.mapper.CareerPlanMapper;
import com.xucheng.aicareer.mapper.CareerTaskMapper;
import com.xucheng.aicareer.service.impl.CareerPlanServiceImpl;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.CareerPlanVO;
import com.xucheng.aicareer.vo.InterviewReportVO;
import com.xucheng.aicareer.vo.ProfileCompletionVO;
import com.xucheng.aicareer.vo.UserProfileVO;
import com.xucheng.aicareer.vo.UserSkillVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CareerPlanServiceTests {

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void generatePlanPersistsStructuredPlanAndCreatesTasks() {
        CareerPlanMapper planMapper = mock(CareerPlanMapper.class);
        CareerTaskMapper taskMapper = mock(CareerTaskMapper.class);
        UserProfileService profileService = mock(UserProfileService.class);
        UserSkillService skillService = mock(UserSkillService.class);
        CareerPlannerAgent agent = mock(CareerPlannerAgent.class);
        TransactionTemplate transactionTemplate = immediateTransactionTemplate();
        CareerPlanServiceImpl service = new CareerPlanServiceImpl(
                planMapper, taskMapper, profileService, skillService, agent,
                mock(CareerTaskService.class), mock(InterviewReportService.class),
                new ObjectMapper(), transactionTemplate);
        UserContext.setUserId(10001L);

        UserProfileVO profile = UserProfileVO.builder()
                .major("软件工程")
                .targetPosition("Java后端开发工程师")
                .build();
        List<UserSkillVO> skills = List.of(UserSkillVO.builder()
                .skillName("Java")
                .level(3)
                .score(60)
                .build());
        CareerPlanResult generated = generatedPlan();
        when(planMapper.exists(any(Wrapper.class))).thenReturn(false);
        when(profileService.getCompletion()).thenReturn(ProfileCompletionVO.builder().completed(true).build());
        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(skillService.getCurrentUserSkills()).thenReturn(skills);
        when(agent.generatePlan(10001L, profile, skills)).thenReturn(generated);
        when(planMapper.insert(any(CareerPlan.class))).thenAnswer(invocation -> {
            invocation.<CareerPlan>getArgument(0).setId(20001L);
            return 1;
        });
        when(taskMapper.insert(any(CareerTask.class))).thenReturn(1);

        CareerPlanVO plan = service.generatePlan();

        assertThat(plan.getId()).isEqualTo(20001L);
        assertThat(plan.getVersion()).isEqualTo(1);
        assertThat(plan.getAdvantages()).containsExactly("具备Java基础", "目标岗位明确");
        assertThat(plan.getRoadmap()).hasSize(3);
        assertThat(plan.getRoadmap().getFirst().getName()).isEqualTo("阶段1");
        verify(agent).generatePlan(10001L, profile, skills);

        ArgumentCaptor<CareerTask> taskCaptor = ArgumentCaptor.forClass(CareerTask.class);
        verify(taskMapper, org.mockito.Mockito.times(6)).insert(taskCaptor.capture());
        assertThat(taskCaptor.getAllValues())
                .allSatisfy(task -> {
                    assertThat(task.getUserId()).isEqualTo(10001L);
                    assertThat(task.getCareerPlanId()).isEqualTo(20001L);
                    assertThat(task.getStatus()).isZero();
                });
    }

    @Test
    void generatePlanRequiresCompletedProfileBeforeCallingAgent() {
        CareerPlanMapper planMapper = mock(CareerPlanMapper.class);
        UserProfileService profileService = mock(UserProfileService.class);
        CareerPlannerAgent agent = mock(CareerPlannerAgent.class);
        CareerPlanServiceImpl service = new CareerPlanServiceImpl(
                planMapper,
                mock(CareerTaskMapper.class),
                profileService,
                mock(UserSkillService.class),
                agent,
                mock(CareerTaskService.class),
                mock(InterviewReportService.class),
                new ObjectMapper(),
                immediateTransactionTemplate());
        UserContext.setUserId(10001L);
        when(planMapper.exists(any(Wrapper.class))).thenReturn(false);
        when(profileService.getCompletion()).thenReturn(ProfileCompletionVO.builder()
                .completed(false)
                .missingFields(List.of("careerGoal"))
                .build());

        assertThatThrownBy(service::generatePlan)
                .isInstanceOf(BusinessException.class)
                .hasMessage("请先完善职业画像后再生成职业规划");
        verify(agent, never()).generatePlan(any(), any(), any());
    }

    @Test
    void generatePlanDoesNotOverwriteAnExistingPlan() {
        CareerPlanMapper planMapper = mock(CareerPlanMapper.class);
        CareerPlannerAgent agent = mock(CareerPlannerAgent.class);
        CareerPlanServiceImpl service = new CareerPlanServiceImpl(
                planMapper,
                mock(CareerTaskMapper.class),
                mock(UserProfileService.class),
                mock(UserSkillService.class),
                agent,
                mock(CareerTaskService.class),
                mock(InterviewReportService.class),
                new ObjectMapper(),
                immediateTransactionTemplate());
        UserContext.setUserId(10001L);
        when(planMapper.exists(any(Wrapper.class))).thenReturn(true);

        assertThatThrownBy(service::generatePlan)
                .isInstanceOf(BusinessException.class)
                .hasMessage("职业规划已存在，请使用重新规划功能");
        verify(agent, never()).generatePlan(any(), any(), any());
    }

    @Test
    void regeneratePlanVersionsOldPlanAndLinksSourceInterview() {
        CareerPlanMapper planMapper = mock(CareerPlanMapper.class);
        CareerTaskMapper taskMapper = mock(CareerTaskMapper.class);
        UserProfileService profileService = mock(UserProfileService.class);
        UserSkillService skillService = mock(UserSkillService.class);
        CareerTaskService taskService = mock(CareerTaskService.class);
        InterviewReportService reportService = mock(InterviewReportService.class);
        CareerPlannerAgent agent = mock(CareerPlannerAgent.class);
        CareerPlanServiceImpl service = new CareerPlanServiceImpl(planMapper, taskMapper,
                profileService, skillService, agent, taskService, reportService,
                new ObjectMapper(), immediateTransactionTemplate());
        UserContext.setUserId(10001L);
        CareerPlan oldPlan = new CareerPlan();
        oldPlan.setId(20001L);
        oldPlan.setUserId(10001L);
        oldPlan.setVersion(1);
        oldPlan.setStatus(1);
        oldPlan.setTargetPosition("Java后端开发工程师");
        when(planMapper.selectOne(any(Wrapper.class))).thenReturn(oldPlan);
        when(planMapper.exists(any(Wrapper.class))).thenReturn(false);
        when(planMapper.update(any(), any(Wrapper.class))).thenReturn(1);
        when(planMapper.insert(any(CareerPlan.class))).thenAnswer(invocation -> {
            invocation.<CareerPlan>getArgument(0).setId(20002L);
            return 1;
        });
        UserProfileVO profile = UserProfileVO.builder().targetPosition("Java后端开发工程师").build();
        List<UserSkillVO> skills = List.of(UserSkillVO.builder().skillName("Java").score(69).build());
        InterviewReportVO report = InterviewReportVO.builder().interviewId(30001L)
                .scores(java.util.Map.of("Java集合框架", 69)).build();
        when(reportService.generateReport(30001L)).thenReturn(report);
        when(profileService.getCurrentProfile()).thenReturn(profile);
        when(skillService.getCurrentUserSkills()).thenReturn(skills);
        when(taskService.getCurrentTasks(null, 20001L)).thenReturn(List.of());
        when(agent.regeneratePlan(eq(10001L), eq(profile), eq(skills), any(), eq(List.of()), eq(report)))
                .thenReturn(generatedPlan());
        CareerPlanRegenerateDTO request = new CareerPlanRegenerateDTO();
        request.setReason("INTERVIEW");
        request.setSourceInterviewId(30001L);

        CareerPlanVO newPlan = service.regeneratePlan(request);

        assertThat(newPlan.getId()).isEqualTo(20002L);
        assertThat(newPlan.getVersion()).isEqualTo(2);
        assertThat(newPlan.getSourceInterviewId()).isEqualTo(30001L);
        ArgumentCaptor<CareerPlan> saved = ArgumentCaptor.forClass(CareerPlan.class);
        verify(planMapper).insert(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(1);
        verify(planMapper).update(any(), any(Wrapper.class));
        verify(taskMapper, org.mockito.Mockito.times(6)).insert(any(CareerTask.class));
    }

    private CareerPlanResult generatedPlan() {
        CareerPlanResult plan = new CareerPlanResult();
        plan.setTargetPosition(" Java后端开发工程师 ");
        plan.setMatchScore(68);
        plan.setSummary(" 具备基础，需要加强实践。 ");
        plan.setAdvantages(List.of(" 具备Java基础 ", "目标岗位明确"));
        plan.setWeaknesses(List.of("中间件经验不足", "项目实践需要加强"));

        List<RoadmapStage> roadmap = new ArrayList<>();
        for (int index = 1; index <= 3; index++) {
            RoadmapStage stage = new RoadmapStage();
            stage.setStage(index);
            stage.setName("阶段" + index);
            stage.setGoal("阶段目标" + index);
            stage.setDuration("2周");
            stage.setTopics(List.of("主题A", "主题B"));
            stage.setTasks(List.of(task("任务" + index + "A"), task("任务" + index + "B")));
            roadmap.add(stage);
        }
        plan.setRoadmap(roadmap);
        return plan;
    }

    private CareerTaskResult task(String name) {
        CareerTaskResult task = new CareerTaskResult();
        task.setTaskName(name);
        task.setDescription("完成" + name);
        task.setTaskType("KNOWLEDGE");
        task.setPriority(2);
        return task;
    }

    @SuppressWarnings("unchecked")
    private TransactionTemplate immediateTransactionTemplate() {
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                invocation.<TransactionCallback<Object>>getArgument(0).doInTransaction(null));
        return transactionTemplate;
    }
}
