import type {
  ApiResult,
  CareerChatPayload,
  CareerChatResponse,
  CareerChatStreamEvent,
  CareerPlan,
} from '../types/api'
import { request, type RequestConfig } from '../utils/request'
import { tokenStorage, userStorage } from '../utils/storage'

interface CareerChatStreamHandlers {
  onDelta: (event: CareerChatStreamEvent) => void
  onDone: (event: CareerChatStreamEvent) => void
  onError?: (event: CareerChatStreamEvent) => void
}

export const getCurrentPlan = (config?: RequestConfig) =>
  request.get<CareerPlan>('/career/plan/current', config)
export const getPlanHistory = (config?: RequestConfig) =>
  request.get<CareerPlan[]>('/career/plan/history', config)
export const getPlanById = (id: number) => request.get<CareerPlan>(`/career/plan/${id}`)
export const chatWithCareerPlanner = (data: CareerChatPayload) =>
  request.post<CareerChatResponse>('/career/chat', data, { timeout: 90000 })

export async function streamCareerPlannerChat(
  data: CareerChatPayload,
  handlers: CareerChatStreamHandlers,
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
