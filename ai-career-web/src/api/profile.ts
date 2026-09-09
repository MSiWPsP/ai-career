import type { ProfileCompletion, UserProfile } from '../types/api'
import { request, type RequestConfig } from '../utils/request'

export const getProfile = (config?: RequestConfig) => request.get<UserProfile>('/profile', config)
export const createProfile = (data: UserProfile) => request.post<UserProfile>('/profile', data)
export const updateProfile = (data: UserProfile) => request.put<UserProfile>('/profile', data)
export const getProfileCompletion = (config?: RequestConfig) =>
  request.get<ProfileCompletion>('/profile/completion', config)
