import type { AbilityRadar, SkillInput, UserSkill } from '../types/api'

/** 自评只作为初始画像，不写入面试评估历史，不冒充已验证能力。 */
export function selfAssessmentRadar(skills: UserSkill[]): AbilityRadar {
  const self = skills.filter(skill => skill.source === 'SELF')
  return {
    indicators: self.map(skill => ({ name: skill.skillName, max: 100 })),
    values: self.map(skill => skill.score),
  }
}

export function skillsChanged(saved: UserSkill[], input: SkillInput[]): boolean {
  const normalize = (items: SkillInput[]) => items
    .map(skill => JSON.stringify([skill.skillName, skill.skillCategory, skill.level]))
    .sort().join('|')
  return normalize(saved) !== normalize(input)
}
