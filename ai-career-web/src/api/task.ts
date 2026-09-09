import type { CareerTask, TaskStatistics } from '../types/api'
import { request, type RequestConfig } from '../utils/request'

export const getTasks = (params?: { status?: number; planId?: number }, config?: RequestConfig) =>
  request.get<CareerTask[]>('/task', { ...config, params })
export const getTaskStatistics = (config?: RequestConfig) =>
  request.get<TaskStatistics>('/task/statistics', config)
export const updateTaskStatus = (id: number, status: number) =>
  request.put<CareerTask>(`/task/${id}/status`, { status })
