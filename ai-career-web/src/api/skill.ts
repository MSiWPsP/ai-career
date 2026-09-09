import type { SkillInput, UserSkill } from '../types/api'
import { request } from '../utils/request'

export const getSkills = () => request.get<UserSkill[]>('/skill')
export const replaceSkills = (skills: SkillInput[]) =>
  request.put<UserSkill[]>('/skill', { skills })
