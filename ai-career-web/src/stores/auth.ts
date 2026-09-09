import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '../api/auth'
import type { LoginPayload, LoginResponse, RegisterPayload } from '../types/api'
import { tokenStorage, userStorage } from '../utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(tokenStorage.get() || '')
  const session = ref<LoginResponse | null>(userStorage.get<LoginResponse>())
  const isAuthenticated = computed(() => Boolean(token.value))

  async function authenticate(payload: LoginPayload) {
    const result = await authApi.login(payload)
    token.value = result.token
    session.value = result
    tokenStorage.set(result.token)
    userStorage.set(result)
  }

  async function createAccount(payload: RegisterPayload) {
    await authApi.register(payload)
  }

  function logout() {
    token.value = ''
    session.value = null
    tokenStorage.clear()
    userStorage.clear()
  }

  return { token, session, isAuthenticated, authenticate, createAccount, logout }
})
