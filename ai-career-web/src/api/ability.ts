import type { AbilityRadar, AbilityScore, AbilityTrend, PageResult } from '../types/api'
import { request, type RequestConfig } from '../utils/request'

export const getCurrentAbilities = () => request.get<Record<string, number>>('/ability/current')
export const getAbilityRadar = (config?: RequestConfig) =>
  request.get<AbilityRadar>('/ability/radar', config)
export const getAbilityHistory = (params?: { abilityName?: string; page?: number; pageSize?: number }) =>
  request.get<PageResult<AbilityScore>>('/ability/history', { params })
export const getAbilityTrend = (abilityName?: string) =>
  request.get<AbilityTrend>('/ability/trend', { params: { abilityName } })
