export function parseJsonField<T>(value: T | string | null | undefined, fallback: T): T {
  if (value == null || value === '') return fallback
  if (typeof value !== 'string') return value
  try {
    return JSON.parse(value) as T
  } catch {
    return fallback
  }
}

export function formatDate(value?: string, includeTime = false): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    ...(includeTime ? { hour: '2-digit', minute: '2-digit' } : {}),
  }).format(date)
}

export const interviewTypeLabel: Record<string, string> = {
  TECHNICAL: '技术面',
  PROJECT: '项目面',
  HR: 'HR 面',
  COMPREHENSIVE: '综合面',
}

export const difficultyLabel: Record<string, string> = {
  EASY: '初级',
  MEDIUM: '中级',
  HARD: '高级',
}
