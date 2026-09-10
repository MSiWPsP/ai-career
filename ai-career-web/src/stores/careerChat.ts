import { ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { useAuthStore } from './auth'

export interface CareerChatMessage {
  id: number
  role: 'assistant' | 'user'
  content: string
}

interface StoredCareerChat {
  conversationId: string
  messages: CareerChatMessage[]
}

const MAX_STORED_MESSAGES = 100
const WELCOME_MESSAGE = '你好，我是你的 AI 职业规划师。你可以和我讨论职业方向、学习路线、实习准备或求职选择。我会记住近期对话上下文；职业画像等业务数据暂未自动读取，请先在问题中补充关键背景。'

export const useCareerChatStore = defineStore('careerChat', () => {
  const authStore = useAuthStore()
  let storageKey = buildStorageKey(authStore.session?.userId)
  const restored = restoreChat(storageKey)
  let messageSequence = Math.max(0, ...restored.messages.map((item) => item.id)) + 1

  const messages = ref<CareerChatMessage[]>(restored.messages)
  const conversationId = ref(restored.conversationId)
  const sending = ref(false)
  const failedMessage = ref('')

  watch(
    () => authStore.session?.userId,
    (userId) => {
      storageKey = buildStorageKey(userId)
      const next = restoreChat(storageKey)
      messages.value = next.messages
      conversationId.value = next.conversationId
      sending.value = false
      failedMessage.value = ''
      messageSequence = Math.max(0, ...next.messages.map((item) => item.id)) + 1
    },
  )

  function addUserMessage(content: string) {
    messages.value.push({ id: messageSequence++, role: 'user', content })
    persist()
  }

  function addAssistantMessage(content: string) {
    const id = messageSequence++
    messages.value.push({ id, role: 'assistant', content })
    return id
  }

  function appendAssistantMessage(id: number, content: string) {
    const target = messages.value.find((item) => item.id === id)
    if (target) target.content += content
  }

  function removeMessage(id: number) {
    messages.value = messages.value.filter((item) => item.id !== id)
  }

  function startSending() {
    sending.value = true
    failedMessage.value = ''
  }

  function finishSending(nextConversationId: string) {
    conversationId.value = nextConversationId
    sending.value = false
    persist()
  }

  function failSending(content: string) {
    failedMessage.value = content
    sending.value = false
    persist()
  }

  function setConversationId(nextConversationId: string) {
    conversationId.value = nextConversationId
  }

  function clearFailure() {
    failedMessage.value = ''
  }

  function persist() {
    const state: StoredCareerChat = {
      conversationId: conversationId.value,
      messages: messages.value.slice(-MAX_STORED_MESSAGES),
    }
    sessionStorage.setItem(storageKey, JSON.stringify(state))
  }

  return {
    messages,
    conversationId,
    sending,
    failedMessage,
    addUserMessage,
    addAssistantMessage,
    appendAssistantMessage,
    removeMessage,
    startSending,
    finishSending,
    failSending,
    setConversationId,
    clearFailure,
  }
})

function buildStorageKey(userId?: number) {
  return `ai-career-career-chat:${userId ?? 'anonymous'}`
}

function restoreChat(storageKey: string): StoredCareerChat {
  const fallback: StoredCareerChat = {
    conversationId: '',
    messages: [{ id: 1, role: 'assistant', content: WELCOME_MESSAGE }],
  }
  const value = sessionStorage.getItem(storageKey)
  if (!value) return fallback

  try {
    const parsed = JSON.parse(value) as Partial<StoredCareerChat>
    const messages = Array.isArray(parsed.messages)
      ? parsed.messages.filter(isCareerChatMessage).slice(-MAX_STORED_MESSAGES)
      : []
    return {
      conversationId: typeof parsed.conversationId === 'string' ? parsed.conversationId : '',
      messages: messages.length ? messages : fallback.messages,
    }
  } catch {
    sessionStorage.removeItem(storageKey)
    return fallback
  }
}

function isCareerChatMessage(value: unknown): value is CareerChatMessage {
  if (!value || typeof value !== 'object') return false
  const item = value as Partial<CareerChatMessage>
  return typeof item.id === 'number'
    && (item.role === 'assistant' || item.role === 'user')
    && typeof item.content === 'string'
}
