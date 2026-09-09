const TOKEN_KEY = 'ai-career-token'
const USER_KEY = 'ai-career-user'

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export const userStorage = {
  get<T>(): T | null {
    const value = localStorage.getItem(USER_KEY)
    if (!value) return null
    try {
      return JSON.parse(value) as T
    } catch {
      localStorage.removeItem(USER_KEY)
      return null
    }
  },
  set: (user: unknown) => localStorage.setItem(USER_KEY, JSON.stringify(user)),
  clear: () => localStorage.removeItem(USER_KEY),
}
