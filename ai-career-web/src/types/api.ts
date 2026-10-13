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
  id: string
  version: number
  targetPosition: string
  matchScore?: number
  summary?: string
  advantages?: string[]
  weaknesses?: string[]
  roadmap?: RoadmapStage[]
  status: number
  sourceInterviewId?: string
  createTime?: string
  updateTime?: string
}

export interface CareerChatPayload {
  conversationId?: string
  clientMessageId?: string
  message: string
}

export interface CareerChatResponse {
  conversationId: string
  clientMessageId: string
  content: string
}

export interface CareerKnowledgeReference {
  documentId: string
  title: string
  section: string
  sourceName: string
  documentVersion?: number | null
  chunkIndex?: number | null
  contentSha256?: string | null
}

export type CareerChatPhase = 'KNOWLEDGE_RETRIEVAL' | 'GENERATING'
export type CareerChatStreamEventType = 'phase' | 'delta' | 'done' | 'error'

export interface CareerChatStreamEvent {
  type: CareerChatStreamEventType
  phase?: CareerChatPhase
  conversationId: string
  clientMessageId: string
  content?: string
  ragApplied?: boolean
  references?: CareerKnowledgeReference[]
}

export interface CareerChatSession {
  conversationId: string
  title: string
  status: number
  messageCount: number
  lastMessage?: string
  lastMessageAt?: string
  createTime?: string
  updateTime?: string
}

export interface CareerChatHistoryMessage {
  id: number
  clientMessageId: string
  role: 'assistant' | 'user'
  content: string
  references?: CareerKnowledgeReference[] | null
  status: number
  messageOrder: number
  createTime?: string
}

export interface CareerChatSessionUpdatePayload {
  title?: string
  status?: number
}

export interface CareerTask {
  id: string
  careerPlanId: string
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
  id: string
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
  maxQuestions: number
  startTime?: string
  endTime?: string
  updateTime?: string
}

export interface StartInterviewPayload {
  targetPosition: string
  interviewType: string
  difficulty: string
  maxQuestions: number
}

export interface InterviewStartResponse {
  interviewId: string
  conversationId: string
  status: number
  question: string
  questionCount: number
  maxQuestions: number
}

export interface InterviewTurnResponse {
  interviewId: string
  message: string
  finished: boolean
  status: number
  questionCount: number
  maxQuestions: number
  reportId: string | null
}

export interface InterviewFinishResponse {
  interviewId: string
  status: number
  reportId: string | null
}

export interface InterviewMessage {
  id: string
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
  id: string
  interviewId: string
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
