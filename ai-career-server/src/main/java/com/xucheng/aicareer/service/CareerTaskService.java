package com.xucheng.aicareer.service;

import com.xucheng.aicareer.dto.TaskStatusDTO;
import com.xucheng.aicareer.vo.CareerTaskVO;
import com.xucheng.aicareer.vo.TaskStatisticsVO;

import java.util.List;

public interface CareerTaskService {

    List<CareerTaskVO> getCurrentTasks(Integer status, Long planId);

    CareerTaskVO getTaskById(Long taskId);

    CareerTaskVO updateTaskStatus(Long taskId, TaskStatusDTO taskStatusDTO);

    TaskStatisticsVO getCurrentTaskStatistics();
}
