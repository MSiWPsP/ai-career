import type {
  ApiResult,
  CareerChatPayload,
  CareerChatHistoryMessage,
  CareerChatResponse,
  CareerChatSession,
  CareerChatSessionUpdatePayload,
  CareerChatStreamEvent,
  CareerPlan,
} from '../types/api'
import { request, type RequestConfig } from '../utils/request'
import { tokenStorage, userStorage } from '../utils/storage'

interface CareerChatStreamHandlers {
  onPhase?: (event: CareerChatStreamEvent) => void
  onDelta: (event: CareerChatStreamEvent) => void
  onDone: (event: CareerChatStreamEvent) => void
  onError?: (event: CareerChatStreamEvent) => void
}

export const getCurrentPlan = (config?: RequestConfig) =>
  request.get<CareerPlan>('/career/plan/current', config)
export const getPlanHistory = (config?: RequestConfig) =>
  request.get<CareerPlan[]>('/career/plan/history', config)
export const getPlanById = (id: string) => request.get<CareerPlan>(`/career/plan/${id}`)
export const generateCareerPlan = () =>
  request.post<CareerPlan>('/career/plan/generate', {}, { timeout: 150000 })
export const regenerateCareerPlan = (sourceInterviewId: string) =>
  request.post<CareerPlan>('/career/plan/regenerate',
    { reason: 'INTERVIEW', sourceInterviewId }, { timeout: 150000 })
export const chatWithCareerPlanner = (data: CareerChatPayload) =>
  request.post<CareerChatResponse>('/career/chat', data, { timeout: 90000 })
export const createCareerConversation = () =>
  request.post<CareerChatSession>('/career/conversations')
export const getCareerConversations = (includeArchived = false, config?: RequestConfig) =>
  request.get<CareerChatSession[]>(`/career/conversations?includeArchived=${includeArchived}`, config)
export const getCareerConversationMessages = (conversationId: string, config?: RequestConfig) =>
  request.get<CareerChatHistoryMessage[]>(
    `/career/conversations/${encodeURIComponent(conversationId)}/messages`,
    config,
  )
export const updateCareerConversation = (
  conversationId: string,
  data: CareerChatSessionUpdatePayload,
) => request.put<CareerChatSession>(`/career/conversations/${encodeURIComponent(conversationId)}`, data)
export const clearCareerConversation = (conversationId: string) =>
  request.delete<void>(`/career/conversations/${encodeURIComponent(conversationId)}/messages`)
export const deleteCareerConversation = (conversationId: string) =>
  request.delete<void>(`/career/conversations/${encodeURIComponent(conversationId)}`)

export async function streamCareerPlannerChat(
  data: CareerChatPayload,
  handlers: CareerChatStreamHandlers,
  options?: { signal?: AbortSignal },
): Promise<void> {
  const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
  const token = tokenStorage.get()
  const response = await fetch(`${apiBaseUrl}/career/chat/stream`, {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(data),
    signal: options?.signal,
  })

  const contentType = response.headers.get('content-type') || ''
  if (!response.ok || !contentType.includes('text/event-stream')) {
    const result = await readErrorResult(response)
    if (result.code === 401) {
      tokenStorage.clear()
      userStorage.clear()
      if (window.location.pathname !== '/login') window.location.href = '/login'
    }
    throw new Error(result.message || 'AI服务暂时不可用，请稍后重试')
  }
  if (!response.body) throw new Error('当前浏览器不支持流式响应')

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let completed = false

  const dispatch = (block: string) => {
    const dataText = block
      .split('\n')
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).trimStart())
      .join('\n')
    if (!dataText) return

    const event = JSON.parse(dataText) as CareerChatStreamEvent
    if (event.type === 'phase') {
      handlers.onPhase?.(event)
      return
    }
    if (event.type === 'delta') {
      handlers.onDelta(event)
      return
    }
    if (event.type === 'done') {
      completed = true
      handlers.onDone(event)
      return
    }
    if (event.type === 'error') {
      handlers.onError?.(event)
      throw new Error(event.content || 'AI服务暂时不可用，请稍后重试')
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    buffer += decoder.decode(value, { stream: !done })
    buffer = buffer.replace(/\r\n/g, '\n')

    let boundary = buffer.indexOf('\n\n')
    while (boundary >= 0) {
      dispatch(buffer.slice(0, boundary))
      buffer = buffer.slice(boundary + 2)
      boundary = buffer.indexOf('\n\n')
    }
    if (done) break
  }

  if (buffer.trim()) dispatch(buffer)
  if (!completed) throw new Error('AI服务连接意外中断，请重试')
}

async function readErrorResult(response: Response): Promise<ApiResult<null>> {
  try {
    return await response.json() as ApiResult<null>
  } catch {
    return {
      code: response.status,
      message: 'AI服务暂时不可用，请稍后重试',
      data: null,
    }
  }
}
