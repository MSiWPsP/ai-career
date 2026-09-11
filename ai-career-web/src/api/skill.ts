import type { SkillInput, UserSkill } from '../types/api'
import { request, type RequestConfig } from '../utils/request'

export const getSkills = (config?: RequestConfig) => request.get<UserSkill[]>('/skill', config)
export const replaceSkills = (skills: SkillInput[]) =>
  request.put<UserSkill[]>('/skill', { skills })
