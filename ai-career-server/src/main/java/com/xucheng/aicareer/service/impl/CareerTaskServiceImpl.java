package com.xucheng.aicareer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xucheng.aicareer.dto.TaskStatusDTO;
import com.xucheng.aicareer.entity.CareerPlan;
import com.xucheng.aicareer.entity.CareerTask;
import com.xucheng.aicareer.exception.BusinessException;
import com.xucheng.aicareer.mapper.CareerPlanMapper;
import com.xucheng.aicareer.mapper.CareerTaskMapper;
import com.xucheng.aicareer.service.CareerTaskService;
import com.xucheng.aicareer.utils.UserContext;
import com.xucheng.aicareer.vo.CareerTaskVO;
import com.xucheng.aicareer.vo.TaskStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerTaskServiceImpl implements CareerTaskService {

    private static final int CURRENT_PLAN_STATUS = 1;
    private static final int TASK_STATUS_WAITING = 0;
    private static final int TASK_STATUS_PROCESSING = 1;
    private static final int TASK_STATUS_COMPLETED = 2;

    private final CareerPlanMapper careerPlanMapper;
    private final CareerTaskMapper careerTaskMapper;

    @Override
    public List<CareerTaskVO> getCurrentTasks(Integer status, Long planId) {
        Long userId = UserContext.getUserId();
        Long targetPlanId = resolvePlanId(userId, planId);
        if (targetPlanId == null) {
            return List.of();
        }

        return careerTaskMapper.selectList(Wrappers.<CareerTask>lambdaQuery()
                        .eq(CareerTask::getUserId, userId)
                        .eq(CareerTask::getCareerPlanId, targetPlanId)
                        .eq(status != null, CareerTask::getStatus, status)
                        .orderByDesc(CareerTask::getPriority)
                        .orderByAsc(CareerTask::getStartDate)
                        .orderByAsc(CareerTask::getCreateTime))
                .stream()
                .map(this::toCareerTaskVO)
                .toList();
    }

    @Override
    public CareerTaskVO getTaskById(Long taskId) {
        return toCareerTaskVO(getRequiredTask(UserContext.getUserId(), taskId));
    }

    @Override
    @Transactional
    public CareerTaskVO updateTaskStatus(Long taskId, TaskStatusDTO taskStatusDTO) {
        Long userId = UserContext.getUserId();
        getRequiredTask(userId, taskId);
        Integer status = taskStatusDTO.getStatus();

        careerTaskMapper.update(null, Wrappers.<CareerTask>lambdaUpdate()
                .eq(CareerTask::getId, taskId)
                .eq(CareerTask::getUserId, userId)
                .set(CareerTask::getStatus, status)
                .set(CareerTask::getFinishTime,
                        Integer.valueOf(TASK_STATUS_COMPLETED).equals(status)
                                ? LocalDateTime.now() : null));
        return toCareerTaskVO(getRequiredTask(userId, taskId));
    }

    @Override
    public TaskStatisticsVO getCurrentTaskStatistics() {
        List<CareerTaskVO> tasks = getCurrentTasks(null, null);
        long total = tasks.size();
        long completed = countByStatus(tasks, TASK_STATUS_COMPLETED);
        long processing = countByStatus(tasks, TASK_STATUS_PROCESSING);
        long waiting = countByStatus(tasks, TASK_STATUS_WAITING);
        int completionRate = total == 0 ? 0 : (int) (completed * 100 / total);

        return TaskStatisticsVO.builder()
                .total(total)
                .completed(completed)
                .processing(processing)
                .waiting(waiting)
                .completionRate(completionRate)
                .build();
    }

    private Long resolvePlanId(Long userId, Long planId) {
        if (planId != null) {
            boolean exists = careerPlanMapper.exists(Wrappers.<CareerPlan>lambdaQuery()
                    .eq(CareerPlan::getId, planId)
                    .eq(CareerPlan::getUserId, userId));
            if (!exists) {
                throw new BusinessException(404, "职业规划不存在");
            }
            return planId;
        }

        CareerPlan currentPlan = careerPlanMapper.selectOne(Wrappers.<CareerPlan>lambdaQuery()
                .select(CareerPlan::getId)
                .eq(CareerPlan::getUserId, userId)
                .eq(CareerPlan::getStatus, CURRENT_PLAN_STATUS)
                .orderByDesc(CareerPlan::getVersion)
                .last("LIMIT 1"));
        return currentPlan == null ? null : currentPlan.getId();
    }

    private CareerTask getRequiredTask(Long userId, Long taskId) {
        CareerTask task = careerTaskMapper.selectOne(Wrappers.<CareerTask>lambdaQuery()
                .eq(CareerTask::getId, taskId)
                .eq(CareerTask::getUserId, userId));
        if (task == null) {
            throw new BusinessException(404, "成长任务不存在");
        }
        return task;
    }

    private long countByStatus(List<CareerTaskVO> tasks, int status) {
        return tasks.stream()
                .filter(task -> Integer.valueOf(status).equals(task.getStatus()))
                .count();
    }

    private CareerTaskVO toCareerTaskVO(CareerTask task) {
        return CareerTaskVO.builder()
                .id(task.getId())
                .careerPlanId(task.getCareerPlanId())
                .stageName(task.getStageName())
                .taskName(task.getTaskName())
                .taskDescription(task.getTaskDescription())
                .taskType(task.getTaskType())
                .priority(task.getPriority())
                .status(task.getStatus())
                .startDate(task.getStartDate())
                .deadline(task.getDeadline())
                .finishTime(task.getFinishTime())
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();
    }
}
