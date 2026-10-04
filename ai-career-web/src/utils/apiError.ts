/** 保留业务错误码，让“尚未创建”与网络/服务故障能被页面分别处理。 */
export class ApiRequestError extends Error {
  code: number
  constructor(message: string, code: number) {
    super(message)
    this.code = code
    this.name = 'ApiRequestError'
  }
}

export function isNotFound(error: unknown): boolean {
  if (error instanceof ApiRequestError) return error.code === 404
  const response = (error as { response?: { status?: number; data?: { code?: number } } } | null)?.response
  return response?.status === 404 || response?.data?.code === 404
}
