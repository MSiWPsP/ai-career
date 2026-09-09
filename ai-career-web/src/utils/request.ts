import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '../types/api'
import { tokenStorage, userStorage } from './storage'

export interface RequestConfig extends AxiosRequestConfig {
  silent?: boolean
}

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

instance.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

instance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!(error.config as RequestConfig | undefined)?.silent) {
      ElMessage.error(error.response?.data?.message || '服务暂时不可用，请稍后重试')
    }
    return Promise.reject(error)
  },
)

async function unwrap<T>(promise: Promise<AxiosResponse<ApiResult<T>>>): Promise<T> {
  const response = await promise
  const result = response.data
  if (result.code === 200) return result.data
  if (result.code === 401) {
    tokenStorage.clear()
    userStorage.clear()
    if (window.location.pathname !== '/login') window.location.href = '/login'
  }
  if (!(response.config as RequestConfig).silent) ElMessage.error(result.message || '请求失败')
  return Promise.reject(new Error(result.message || '请求失败'))
}

export const request = {
  get<T>(url: string, config?: RequestConfig): Promise<T> {
    return unwrap(instance.get<ApiResult<T>>(url, config))
  },
  post<T>(url: string, data?: unknown, config?: RequestConfig): Promise<T> {
    return unwrap(instance.post<ApiResult<T>>(url, data, config))
  },
  put<T>(url: string, data?: unknown, config?: RequestConfig): Promise<T> {
    return unwrap(instance.put<ApiResult<T>>(url, data, config))
  },
  delete<T>(url: string, config?: RequestConfig): Promise<T> {
    return unwrap(instance.delete<ApiResult<T>>(url, config))
  },
}
