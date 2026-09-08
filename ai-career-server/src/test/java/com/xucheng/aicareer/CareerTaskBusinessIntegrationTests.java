package com.xucheng.aicareer;

import com.xucheng.aicareer.dto.RegisterDTO;
import com.xucheng.aicareer.dto.TaskStatusDTO;
import com.xucheng.aicareer.entity.CareerPlan;
import com.xucheng.aicareer.entity.CareerTask;
import com.xucheng.aicareer.entity.User;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.mapper.CareerPlanMapper;
import com.xucheng.aicareer.mapper.CareerTaskMapper;
import com.xucheng.aicareer.mapper.UserMapper;
import com.xucheng.aicareer.service.AuthService;
import com.xucheng.aicareer.service.CareerPlanService;
import com.xucheng.aicareer.service.CareerTaskService;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.TaskStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CareerTaskBusinessIntegrationTests {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final CareerPlanMapper careerPlanMapper;
    private final CareerTaskMapper careerTaskMapper;
    private final CareerPlanService careerPlanService;
    private final CareerTaskService careerTaskService;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void currentPlanHistoryTaskFilteringStatusAndStatisticsWorkTogether() {
        Long userId = createUser();
        UserContext.setUserId(userId);

        CareerPlan historicalPlan = createPlan(userId, 1, 0);
        CareerPlan currentPlan = createPlan(userId, 2, 1);
        createTask(userId, historicalPlan.getId(), "历史任务", 2, 0);
        CareerTask waitingTask = createTask(userId, currentPlan.getId(), "复习Java集合", 3, 0);
        createTask(userId, currentPlan.getId(), "学习Redis", 2, 1);
        createTask(userId, currentPlan.getId(), "了解岗位要求", 1, 3);

        assertThat(careerPlanService.getCurrentPlan().getVersion()).isEqualTo(2);
        assertThat(careerPlanService.getPlanHistory())
                .extracting("version")
                .containsExactly(2, 1);
        assertThat(careerPlanService.getPlanById(historicalPlan.getId()).getStatus()).isZero();

        assertThat(careerTaskService.getCurrentTasks(null, null)).hasSize(3);
        assertThat(careerTaskService.getCurrentTasks(1, null))
                .singleElement()
                .satisfies(task -> assertThat(task.getTaskName()).isEqualTo("学习Redis"));

        TaskStatusDTO completed = new TaskStatusDTO();
        completed.setStatus(2);
        assertThat(careerTaskService.updateTaskStatus(waitingTask.getId(), completed).getFinishTime())
                .isNotNull();

        TaskStatisticsVO statistics = careerTaskService.getCurrentTaskStatistics();
        assertThat(statistics.getTotal()).isEqualTo(3);
        assertThat(statistics.getCompleted()).isEqualTo(1);
        assertThat(statistics.getProcessing()).isEqualTo(1);
        assertThat(statistics.getWaiting()).isZero();
        assertThat(statistics.getCompletionRate()).isEqualTo(33);

        TaskStatusDTO processing = new TaskStatusDTO();
        processing.setStatus(1);
        assertThat(careerTaskService.updateTaskStatus(waitingTask.getId(), processing).getFinishTime())
                .isNull();

        UserContext.setUserId(createUser());
        assertThatThrownBy(() -> careerPlanService.getPlanById(currentPlan.getId()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> careerTaskService.getTaskById(waitingTask.getId()))
                .isInstanceOf(BusinessException.class);
    }

    private Long createUser() {
        String username = "career_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername(username);
        registerDTO.setPassword("123456");
        registerDTO.setNickname("规划测试用户");
        authService.register(registerDTO);
        return userMapper.selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, username))
                .getId();
    }

    private CareerPlan createPlan(Long userId, int version, int status) {
        CareerPlan plan = new CareerPlan();
        plan.setUserId(userId);
        plan.setVersion(version);
        plan.setTargetPosition("Java后端开发工程师");
        plan.setMatchScore(70 + version);
        plan.setSummary("职业规划版本" + version);
        plan.setAdvantages("[]");
        plan.setWeaknesses("[]");
        plan.setRoadmap("[]");
        plan.setStatus(status);
        careerPlanMapper.insert(plan);
        return plan;
    }

    private CareerTask createTask(Long userId, Long planId, String taskName,
                                  int priority, int status) {
        CareerTask task = new CareerTask();
        task.setUserId(userId);
        task.setCareerPlanId(planId);
        task.setStageName("基础强化");
        task.setTaskName(taskName);
        task.setTaskDescription(taskName);
        task.setTaskType("KNOWLEDGE");
        task.setPriority(priority);
        task.setStatus(status);
        task.setStartDate(LocalDate.now());
        task.setDeadline(LocalDate.now().plusDays(7));
        careerTaskMapper.insert(task);
        return task;
    }
}
