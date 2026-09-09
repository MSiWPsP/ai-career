export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload extends LoginPayload {
  nickname: string
}

export interface LoginResponse {
  token: string
  userId: number
  nickname: string
  avatar?: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar?: string
  role: string
}

export interface UserProfile {
  id?: number
  education?: string
  major?: string
  grade?: string
  graduationYear?: number
  careerStage?: string
  targetPosition?: string
  targetCity?: string
  targetTime?: string
  dailyStudyHours?: number
  careerGoal?: string
  interestDescription?: string
}

export interface ProfileCompletion {
  completed: boolean
  score: number
  missingFields: string[]
}

export interface UserSkill {
  id: number
  skillName: string
  skillCategory: string
  level: number
  score: number
  source: string
}

export interface SkillInput {
  skillName: string
  skillCategory: string
  level: number
}

export interface RoadmapStage {
  stage?: number
  name?: string
  goal?: string
  duration?: string
  topics?: string[]
}

export interface CareerPlan {
  id: number
  version: number
  targetPosition: string
  matchScore?: number
  summary?: string
  advantages?: string | string[]
  weaknesses?: string | string[]
  roadmap?: string | RoadmapStage[]
  status: number
  sourceInterviewId?: number
  createTime?: string
  updateTime?: string
}

export interface CareerTask {
  id: number
  careerPlanId: number
  stageName: string
  taskName: string
  taskDescription?: string
  taskType: string
  priority: number
  status: number
  startDate?: string
  deadline?: string
  finishTime?: string
  createTime?: string
  updateTime?: string
}

export interface TaskStatistics {
  total: number
  completed: number
  processing: number
  waiting: number
  completionRate: number
}

export interface PageResult<T> {
  records: T[]
  total: number
}

export interface InterviewHistoryRecord {
  id: number
  targetPosition: string
  interviewType: string
  difficulty: string
  status: number
  totalScore?: number
  createTime?: string
}

export interface InterviewDetail extends InterviewHistoryRecord {
  conversationId?: string
  questionCount: number
  startTime?: string
  endTime?: string
  updateTime?: string
}

export interface InterviewMessage {
  id: number
  role: 'assistant' | 'user' | 'system'
  content: string
  questionCategory?: string
  questionLevel?: string
  messageOrder: number
  createTime?: string
}

export interface InterviewSuggestion {
  topic: string
  priority: string
  content: string
}

export interface InterviewReport {
  id: number
  interviewId: number
  totalScore: number
  scores: Record<string, number>
  advantages: string[]
  weaknesses: string[]
  suggestions: InterviewSuggestion[]
  summary?: string
  createTime?: string
  updateTime?: string
}

export interface RadarIndicator {
  name: string
  max: number
}

export interface AbilityRadar {
  indicators: RadarIndicator[]
  values: number[]
}

export interface AbilityScore {
  id: number
  abilityName: string
  score: number
  sourceType: string
  sourceId?: number
  createTime: string
}

export interface AbilityTrend {
  abilityName?: string
  records: Array<{ score: number; date: string }>
}
