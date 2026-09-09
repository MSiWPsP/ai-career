import type { LoginPayload, LoginResponse, RegisterPayload } from '../types/api'
import { request } from '../utils/request'

export const login = (data: LoginPayload) => request.post<LoginResponse>('/auth/login', data)
export const register = (data: RegisterPayload) => request.post<void>('/auth/register', data)
