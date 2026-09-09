import type { UserInfo } from '../types/api'
import { request, type RequestConfig } from '../utils/request'

export const getCurrentUser = (config?: RequestConfig) => request.get<UserInfo>('/user/me', config)
export const updateCurrentUser = (data: Pick<UserInfo, 'nickname' | 'avatar'>) =>
  request.put<UserInfo>('/user/me', data)
