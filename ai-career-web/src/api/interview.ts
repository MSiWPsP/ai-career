import type {
  InterviewDetail,
  InterviewFinishResponse,
  InterviewHistoryRecord,
  InterviewMessage,
  InterviewReport,
  InterviewStartResponse,
  InterviewTurnResponse,
  PageResult,
  StartInterviewPayload,
} from '../types/api'
import { request, type RequestConfig } from '../utils/request'

const aiRequestConfig: RequestConfig = { timeout: 90000 }

export const startInterview = (payload: StartInterviewPayload) =>
  request.post<InterviewStartResponse>('/interview/start', payload, aiRequestConfig)
export const answerInterview = (id: string, answer: string) =>
  request.post<InterviewTurnResponse>(`/interview/${id}/answer`, { answer }, aiRequestConfig)
export const finishInterview = (id: string) =>
  request.post<InterviewFinishResponse>(`/interview/${id}/finish`, undefined, aiRequestConfig)

export const getInterviewHistory = (page = 1, pageSize = 10, config?: RequestConfig) =>
  request.get<PageResult<InterviewHistoryRecord>>('/interview/history', {
    ...config,
    params: { page, pageSize },
  })
export const getInterview = (id: string) => request.get<InterviewDetail>(`/interview/${id}`)
export const getInterviewMessages = (id: string) =>
  request.get<InterviewMessage[]>(`/interview/${id}/messages`)
export const getInterviewReport = (id: string, config?: RequestConfig) =>
  request.get<InterviewReport>(`/interview/${id}/report`, config)
export const generateInterviewReport = (id: string) =>
  request.post<InterviewReport>(`/interview/${id}/report/generate`, undefined, aiRequestConfig)
