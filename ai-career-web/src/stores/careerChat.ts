import { ref, watch } from 'vue'
import { defineStore } from 'pinia'
import type { CareerChatHistoryMessage, CareerKnowledgeReference } from '../types/api'
import { useAuthStore } from './auth'
import { removeUntrustedCareerKnowledgeMarker, splitCareerKnowledgeSources } from '../utils/rag'

export type CareerChatMessageStatus = 'sending' | 'completed' | 'failed'

export interface CareerChatMessage {
  id: string
  clientMessageId?: string
  role: 'assistant' | 'user'
  content: string
  references?: CareerKnowledgeReference[]
  referencesVerified?: boolean
  ragAttempted?: boolean
  status: CareerChatMessageStatus
}

export interface FailedCareerChatRequest {
  content: string
  clientMessageId: string
}

const WELCOME_MESSAGE = '你好，我是你的 AI 职业规划师。你可以和我讨论职业方向、学习路线、实习准备或求职选择。每次提问时，我都会参考你最新的职业画像、技能和当前规划；如果资料不足，我会明确提醒你补充。'

export const useCareerChatStore = defineStore('careerChat', () => {
  const authStore = useAuthStore()
  let activeStorageKey = buildActiveStorageKey(authStore.session?.userId)
  let draftStorageKey = buildDraftStorageKey(authStore.session?.userId, readActiveConversation(activeStorageKey))

  const messages = ref<CareerChatMessage[]>(welcomeMessages())
  const conversationId = ref(readActiveConversation(activeStorageKey))
  const draft = ref(readDraft(draftStorageKey))
  const sending = ref(false)
  const failedRequest = ref<FailedCareerChatRequest | null>(null)

  watch(
    () => authStore.session?.userId,
    (userId) => {
      activeStorageKey = buildActiveStorageKey(userId)
      const nextConversationId = readActiveConversation(activeStorageKey)
      conversationId.value = nextConversationId
      draftStorageKey = buildDraftStorageKey(userId, nextConversationId)
      draft.value = readDraft(draftStorageKey)
      messages.value = welcomeMessages()
      sending.value = false
      failedRequest.value = null
    },
  )

  watch(draft, (value) => {
    if (value) localStorage.setItem(draftStorageKey, value)
    else localStorage.removeItem(draftStorageKey)
  }, { flush: 'sync' })

  function openConversation(nextConversationId: string, history: CareerChatHistoryMessage[] = []) {
    persistDraft(draftStorageKey, draft.value)
    conversationId.value = nextConversationId
    if (nextConversationId) localStorage.setItem(activeStorageKey, nextConversationId)
    else localStorage.removeItem(activeStorageKey)
    draftStorageKey = buildDraftStorageKey(authStore.session?.userId, nextConversationId)
    draft.value = readDraft(draftStorageKey)
    messages.value = history.length ? history.map(toDisplayMessage) : welcomeMessages()
    sending.value = false
    failedRequest.value = findLatestFailedRequest(messages.value)
  }

  function startSending(content: string, clientMessageId: string) {
    sending.value = true
    failedRequest.value = null
    const existing = messages.value.find(
      (item) => item.role === 'user' && item.clientMessageId === clientMessageId,
    )
    if (existing) {
      existing.status = 'sending'
      return
    }
    if (messages.value.length === 1 && messages.value[0]?.id === 'welcome') {
      messages.value = []
    }
    messages.value.push({
      id: `local:${clientMessageId}:user`,
      clientMessageId,
      role: 'user',
      content,
      status: 'sending',
    })
  }

  function appendAssistantMessage(clientMessageId: string, content: string, ragAttempted = false) {
    let target = messages.value.find(
      (item) => item.role === 'assistant' && item.clientMessageId === clientMessageId,
    )
    if (!target) {
      target = {
        id: `local:${clientMessageId}:assistant`,
        clientMessageId,
        role: 'assistant',
        content: '',
        ragAttempted,
        status: 'sending',
      }
      messages.value.push(target)
    }
    target.content += content
    target.ragAttempted ||= ragAttempted
  }

  function finishSending(
    nextConversationId: string,
    clientMessageId: string,
    references: CareerKnowledgeReference[] = [],
    ragAttempted = false,
  ) {
    conversationId.value = nextConversationId
    localStorage.setItem(activeStorageKey, nextConversationId)
    const assistant = messages.value.find(
      (item) => item.role === 'assistant' && item.clientMessageId === clientMessageId,
    )
    if (assistant) {
      assistant.content = removeUntrustedCareerKnowledgeMarker(assistant.content)
      assistant.references = references
      assistant.referencesVerified = true
      assistant.ragAttempted ||= ragAttempted || assistant.references.length > 0
    }
    messages.value
      .filter((item) => item.clientMessageId === clientMessageId)
      .forEach((item) => { item.status = 'completed' })
    sending.value = false
    failedRequest.value = null
  }

  function failSending(content: string, clientMessageId: string) {
    messages.value = messages.value.filter(
      (item) => item.role !== 'assistant' || item.clientMessageId !== clientMessageId,
    )
    const userMessage = messages.value.find(
      (item) => item.role === 'user' && item.clientMessageId === clientMessageId,
    )
    if (userMessage) userMessage.status = 'failed'
    sending.value = false
    failedRequest.value = { content, clientMessageId }
  }

  function clearFailure() {
    failedRequest.value = null
  }

  function clearCurrentMessages() {
    messages.value = welcomeMessages()
    failedRequest.value = null
    draft.value = ''
    sending.value = false
  }

  function discardNewConversationDraft() {
    const newConversationDraftKey = buildDraftStorageKey(authStore.session?.userId, '')
    localStorage.removeItem(newConversationDraftKey)
    if (!conversationId.value) draft.value = ''
  }

  return {
    messages,
    conversationId,
    draft,
    sending,
    failedRequest,
    openConversation,
    startSending,
    appendAssistantMessage,
    finishSending,
    failSending,
    clearFailure,
    clearCurrentMessages,
    discardNewConversationDraft,
  }
})

function welcomeMessages(): CareerChatMessage[] {
  return [{ id: 'welcome', role: 'assistant', content: WELCOME_MESSAGE, status: 'completed' }]
}

function toDisplayMessage(message: CareerChatHistoryMessage): CareerChatMessage {
  const parsed = message.role === 'assistant'
    ? splitCareerKnowledgeSources(message.content)
    : { body: message.content, references: [] }
  return {
    id: `server:${message.id}`,
    clientMessageId: message.clientMessageId,
    role: message.role,
    content: parsed.body,
    references: message.references ?? parsed.references,
    referencesVerified: message.references !== null && message.references !== undefined,
    ragAttempted: (message.references ?? parsed.references).length > 0,
    status: message.status === 1 ? 'completed' : 'failed',
  }
}

function findLatestFailedRequest(messages: CareerChatMessage[]): FailedCareerChatRequest | null {
  const failed = [...messages].reverse().find(
    (item) => item.role === 'user' && item.status === 'failed' && item.clientMessageId,
  )
  return failed?.clientMessageId
    ? { content: failed.content, clientMessageId: failed.clientMessageId }
    : null
}

function buildActiveStorageKey(userId?: number) {
  return `ai-career-career-chat-active:${userId ?? 'anonymous'}`
}

function buildDraftStorageKey(userId: number | undefined, conversationId: string) {
  return `ai-career-career-chat-draft:${userId ?? 'anonymous'}:${conversationId || 'new'}`
}

function readActiveConversation(storageKey: string) {
  return localStorage.getItem(storageKey) || ''
}

function readDraft(storageKey: string) {
  return localStorage.getItem(storageKey) || ''
}

function persistDraft(storageKey: string, value: string) {
  if (value) localStorage.setItem(storageKey, value)
  else localStorage.removeItem(storageKey)
}
