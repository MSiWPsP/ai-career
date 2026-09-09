import type {
  InterviewDetail,
  InterviewHistoryRecord,
  InterviewMessage,
  InterviewReport,
  PageResult,
} from '../types/api'
import { request, type RequestConfig } from '../utils/request'

export const getInterviewHistory = (page = 1, pageSize = 10, config?: RequestConfig) =>
  request.get<PageResult<InterviewHistoryRecord>>('/interview/history', {
    ...config,
    params: { page, pageSize },
  })
export const getInterview = (id: number) => request.get<InterviewDetail>(`/interview/${id}`)
export const getInterviewMessages = (id: number) =>
  request.get<InterviewMessage[]>(`/interview/${id}/messages`)
export const getInterviewReport = (id: number) =>
  request.get<InterviewReport>(`/interview/${id}/report`)
