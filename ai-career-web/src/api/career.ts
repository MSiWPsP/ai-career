import type { CareerPlan } from '../types/api'
import { request, type RequestConfig } from '../utils/request'

export const getCurrentPlan = (config?: RequestConfig) =>
  request.get<CareerPlan>('/career/plan/current', config)
export const getPlanHistory = (config?: RequestConfig) =>
  request.get<CareerPlan[]>('/career/plan/history', config)
export const getPlanById = (id: number) => request.get<CareerPlan>(`/career/plan/${id}`)
