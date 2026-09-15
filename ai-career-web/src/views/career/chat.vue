<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import {
  ArrowRight,
  ChatDotRound,
  Clock,
  Close,
  Delete,
  EditPen,
  Folder,
  MagicStick,
  MoreFilled,
  Plus,
  RefreshRight,
  User,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  clearCareerConversation,
  createCareerConversation,
  deleteCareerConversation,
  generateCareerPlan,
  getCareerConversationMessages,
  getCareerConversations,
  getCurrentPlan,
  streamCareerPlannerChat,
  updateCareerConversation,
} from '../../api/career'
import { getProfile } from '../../api/profile'
import { getSkills } from '../../api/skill'
import AgentAvatar from '../../components/AgentAvatar.vue'
import { useCareerChatStore } from '../../stores/careerChat'
import type { CareerChatSession, CareerPlan, UserProfile, UserSkill } from '../../types/api'
import { renderMarkdown } from '../../utils/markdown'

const router = useRouter()
const careerChatStore = useCareerChatStore()
const { messages, conversationId, draft: message, sending, failedRequest } = storeToRefs(careerChatStore)
const messageArea = ref<HTMLElement>()
const showQuickQuestions = ref(true)
const contextLoading = ref(true)
const sessionsLoading = ref(true)
const showSessions = ref(false)
const includeArchived = ref(false)
const sessions = ref<CareerChatSession[]>([])
const profile = ref<UserProfile>()
const skills = ref<UserSkill[]>([])
const currentPlan = ref<CareerPlan>()
const planGenerating = ref(false)
let activeRequestController: AbortController | undefined

const activeSession = computed(() =>
  sessions.value.find((item) => item.conversationId === conversationId.value),
)
const conversationArchived = computed(() => activeSession.value?.status === 0)
const contextTip = computed(() => {
  const available: string[] = []
  const missing: string[] = []
  if (profile.value) available.push('职业画像')
  else missing.push('职业画像')
  if (skills.value.length) available.push(`${skills.value.length} 项技能`)
  else missing.push('技能')
  if (currentPlan.value) available.push(`V${currentPlan.value.version} 职业规划`)

  if (!available.length) {
    return '尚未查询到职业画像或技能，AI 会在信息不足时提醒你补充。'
  }
  const suffix = missing.length ? `尚未填写${missing.join('和')}。` : ''
  return `AI 每次提问都会自动读取最新的${available.join('、')}。${suffix}`
})

const quickQuestions = [
  '我适合做 Java 后端吗？',
  '半年后找实习应该怎么准备？',
  'Redis 和微服务应该先学哪个？',
]

const careerStageLabel: Record<string, string> = {
  EXPLORING: '探索方向',
  LEARNING: '学习提升',
  INTERNSHIP: '实习准备',
  JOB_HUNTING: '校招准备',
}

onMounted(async () => {
  const contextPromise = loadContext()
  await initializeConversations()
  await contextPromise
  await scrollToBottom()
})

onBeforeUnmount(() => activeRequestController?.abort())

async function loadContext() {
  contextLoading.value = true
  const [profileResult, skillResult, planResult] = await Promise.allSettled([
    getProfile({ silent: true }),
    getSkills({ silent: true }),
    getCurrentPlan({ silent: true }),
  ])
  profile.value = profileResult.status === 'fulfilled' ? profileResult.value : undefined
  skills.value = skillResult.status === 'fulfilled' ? skillResult.value : []
  currentPlan.value = planResult.status === 'fulfilled' ? planResult.value : undefined
  contextLoading.value = false
}

async function initializeConversations() {
  sessionsLoading.value = true
  // 每次进入页面都从新的空白窗口开始；历史会话仅在用户从会话记录中主动选择时加载。
  careerChatStore.openConversation('', [])
  try {
    sessions.value = await getCareerConversations(false, { silent: true })
  } catch {
    ElMessage.error('聊天记录加载失败，请稍后重试')
  } finally {
    sessionsLoading.value = false
  }
}

async function refreshConversations() {
  sessionsLoading.value = true
  try {
    sessions.value = await getCareerConversations(includeArchived.value, { silent: true })
  } catch {
    ElMessage.error('会话列表刷新失败，请稍后重试')
  } finally {
    sessionsLoading.value = false
  }
}

async function openConversation(session: CareerChatSession, closeDrawer = true) {
  if (sending.value) {
    ElMessage.warning('请等待当前回复完成后再切换会话')
    return
  }
  sessionsLoading.value = true
  try {
    const history = await getCareerConversationMessages(session.conversationId, { silent: true })
    careerChatStore.openConversation(session.conversationId, history)
    if (closeDrawer) showSessions.value = false
    await scrollToBottom()
  } catch {
    ElMessage.error('会话消息加载失败，请稍后重试')
  } finally {
    sessionsLoading.value = false
  }
}

async function createConversation() {
  if (sending.value) return
  try {
    const session = await createCareerConversation()
    sessions.value = [session, ...sessions.value.filter((item) => item.conversationId !== session.conversationId)]
    careerChatStore.openConversation(session.conversationId, [])
    showSessions.value = false
    await scrollToBottom()
  } catch {
    // 全局请求层已展示错误提示。
  }
}

async function ensureConversation() {
  if (conversationId.value) return conversationId.value
  const session = await createCareerConversation()
  sessions.value.unshift(session)
  careerChatStore.openConversation(session.conversationId, [])
  // 新窗口中的文字已作为首条消息消费，不应在下次进入页面时再次成为草稿。
  careerChatStore.discardNewConversationDraft()
  return session.conversationId
}

async function send(content = message.value, retryClientMessageId?: string) {
  const question = content.trim()
  if (!question) {
    ElMessage.warning('请输入你的职业问题')
    return
  }
  if (question.length > 2000) {
    ElMessage.warning('聊天内容不能超过2000个字符')
    return
  }
  if (sending.value) return
  if (conversationArchived.value) {
    ElMessage.warning('该会话已归档，请先恢复后再继续')
    return
  }

  let currentConversationId = ''
  try {
    currentConversationId = await ensureConversation()
  } catch {
    return
  }
  const clientMessageId = retryClientMessageId || crypto.randomUUID()
  careerChatStore.startSending(question, clientMessageId)
  message.value = ''
  await scrollToBottom()

  activeRequestController = new AbortController()
  let completed = false
  try {
    await streamCareerPlannerChat(
      { conversationId: currentConversationId, clientMessageId, message: question },
      {
        onDelta(event) {
          careerChatStore.appendAssistantMessage(event.clientMessageId || clientMessageId, event.content || '')
          void scrollToBottom()
        },
        onDone(event) {
          completed = true
          careerChatStore.finishSending(event.conversationId, event.clientMessageId || clientMessageId)
        },
      },
      { signal: activeRequestController.signal },
    )
  } catch {
    careerChatStore.failSending(question, clientMessageId)
  } finally {
    activeRequestController = undefined
    if (completed) await Promise.all([refreshConversations(), loadContext()])
    await scrollToBottom()
  }
}

function chooseQuestion(question: string) {
  message.value = question
}

function retry() {
  if (!failedRequest.value) return
  const request = failedRequest.value
  careerChatStore.clearFailure()
  void send(request.content, request.clientMessageId)
}

async function clearCurrentConversation() {
  if (!conversationId.value || sending.value) return
  try {
    await ElMessageBox.confirm('清空后当前会话的全部消息将无法恢复。', '清空当前对话', {
      confirmButtonText: '确认清空',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await clearCareerConversation(conversationId.value)
    careerChatStore.clearCurrentMessages()
    await refreshConversations()
    ElMessage.success('当前对话已清空')
  } catch {
    // 全局请求层已展示错误提示。
  }
}

async function handleSessionCommand(command: string, session: CareerChatSession) {
  if (sending.value) {
    ElMessage.warning('请等待当前回复完成后再管理会话')
    return
  }
  if (command === 'rename') {
    await renameConversation(session)
  } else if (command === 'archive') {
    await changeConversationStatus(session, 0)
  } else if (command === 'restore') {
    await changeConversationStatus(session, 1)
  } else if (command === 'delete') {
    await removeConversation(session)
  }
}

async function renameConversation(session: CareerChatSession) {
  let title = ''
  try {
    const result = await ElMessageBox.prompt('请输入新的会话标题', '重命名会话', {
      inputValue: session.title,
      inputPlaceholder: '会话标题',
      inputValidator: (value) => {
        const length = value.trim().length
        return (length >= 1 && length <= 100) || '标题长度应为1到100个字符'
      },
    })
    title = result.value.trim()
  } catch {
    return
  }
  try {
    const updated = await updateCareerConversation(session.conversationId, { title })
    replaceSession(updated)
  } catch {
    // 全局请求层已展示错误提示。
  }
}

async function changeConversationStatus(session: CareerChatSession, status: number) {
  try {
    const updated = await updateCareerConversation(session.conversationId, { status })
    if (!includeArchived.value && status === 0) {
      sessions.value = sessions.value.filter((item) => item.conversationId !== session.conversationId)
    } else {
      replaceSession(updated)
    }
    if (conversationId.value === session.conversationId && status === 0) {
      careerChatStore.openConversation('', [])
    }
    ElMessage.success(status === 1 ? '会话已恢复' : '会话已归档')
  } catch {
    // 全局请求层已展示错误提示。
  }
}

async function removeConversation(session: CareerChatSession) {
  try {
    await ElMessageBox.confirm('删除后该会话及全部消息将无法恢复。', '删除会话', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await deleteCareerConversation(session.conversationId)
    sessions.value = sessions.value.filter((item) => item.conversationId !== session.conversationId)
    if (conversationId.value === session.conversationId) {
      const next = sessions.value.find((item) => item.status === 1)
      if (next) await openConversation(next, false)
      else careerChatStore.openConversation('', [])
    }
    ElMessage.success('会话已删除')
  } catch {
    // 全局请求层已展示错误提示。
  }
}

function replaceSession(updated: CareerChatSession) {
  const index = sessions.value.findIndex((item) => item.conversationId === updated.conversationId)
  if (index >= 0) sessions.value[index] = updated
  else sessions.value.unshift(updated)
}

function formatSessionTime(value?: string) {
  if (!value) return '暂无消息'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

async function scrollToBottom() {
  await nextTick()
  if (!messageArea.value) return
  if (messages.value.length === 1 && messages.value[0]?.id === 'welcome') {
    messageArea.value.scrollTop = 0
    return
  }
  messageArea.value.scrollTop = messageArea.value.scrollHeight
}

async function handlePlanAction() {
  if (currentPlan.value) {
    await router.push('/career/plan')
    return
  }
  if (planGenerating.value) return

  planGenerating.value = true
  try {
    currentPlan.value = await generateCareerPlan()
    ElMessage.success('职业规划生成成功')
    await router.push('/career/plan')
  } finally {
    planGenerating.value = false
  }
}
</script>

<template>
  <div class="chat-page">
    <section class="chat-layout">
      <article class="surface-card chat-card">
        <div class="chat-toolbar">
          <div class="conversation-identity">
            <AgentAvatar role="planner" :size="42" />
            <div class="active-conversation">
              <span>AI 职业规划师</span>
              <strong>{{ activeSession?.title || '新对话' }}</strong>
              <small>{{ conversationArchived ? '已归档，只能查看历史消息' : '当前会话将自动保存' }}</small>
            </div>
          </div>
          <div class="chat-actions">
            <el-button :icon="Clock" :disabled="sending" @click="showSessions = true">会话记录</el-button>
            <el-button :icon="Plus" :disabled="sending" @click="createConversation">新建对话</el-button>
            <el-button
              v-if="conversationId"
              text
              :icon="Delete"
              :disabled="!conversationId || sending"
              @click="clearCurrentConversation"
            >
              清空内容
            </el-button>
          </div>
        </div>

        <div ref="messageArea" class="message-area">
          <article v-for="item in messages" :key="item.id" class="chat-message" :class="[item.role, item.status]">
            <span v-if="item.role === 'user'" class="message-avatar"><el-icon><User /></el-icon></span>
            <AgentAvatar v-else role="planner" :size="36" />
            <div class="message-bubble">
              <small v-if="item.role === 'assistant'">AI 职业规划师</small>
              <div
                v-if="item.role === 'assistant'"
                class="markdown-content"
                :class="{ 'streaming-content': sending && item.id === messages[messages.length - 1]?.id }"
                v-html="renderMarkdown(item.content)"
              />
              <p v-else>{{ item.content }}</p>
            </div>
          </article>

          <article v-if="sending && messages[messages.length - 1]?.role !== 'assistant'" class="chat-message assistant pending-message">
            <AgentAvatar role="planner" :size="36" />
            <div class="message-bubble">
              <small>AI 职业规划师</small>
              <div class="typing-dots"><i></i><i></i><i></i></div>
            </div>
          </article>

          <el-alert v-if="failedRequest" class="chat-error" type="error" :closable="false" show-icon>
            <template #title>本次回复未完成，已保留你的问题，可安全重试。</template>
            <el-button size="small" :icon="RefreshRight" :loading="sending" @click="retry">重新发送</el-button>
          </el-alert>
        </div>

        <div v-if="showQuickQuestions" class="quick-list">
          <div class="quick-questions">
            <button
              v-for="question in quickQuestions"
              :key="question"
              class="quick-question"
              :disabled="sending"
              @click="chooseQuestion(question)"
            >
              {{ question }} <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
          <el-button
            class="quick-close"
            text
            circle
            :icon="Close"
            aria-label="关闭预设问题"
            title="关闭预设问题"
            @click="showQuickQuestions = false"
          />
        </div>

        <div class="chat-input">
          <el-input
            v-model="message"
            type="textarea"
            :rows="3"
            resize="vertical"
            maxlength="2000"
            placeholder="输入你的职业问题……"
            :disabled="conversationArchived"
            @keydown.ctrl.enter.prevent="send()"
          />
          <div>
            <span class="input-meta">
              <small>{{ conversationArchived ? '恢复会话后可继续提问' : conversationId ? '最近对话已保存 · Ctrl + Enter 发送' : '发送后自动创建新会话' }}</small>
              <el-button
                v-if="!showQuickQuestions"
                class="quick-restore"
                text
                size="small"
                :icon="ChatDotRound"
                @click="showQuickQuestions = true"
              >
                显示预设问题
              </el-button>
            </span>
            <el-button type="primary" :loading="sending" :disabled="!message.trim() || conversationArchived" @click="send()">发送</el-button>
          </div>
        </div>
      </article>

      <aside class="surface-card portrait-card" v-loading="contextLoading">
        <div class="portrait-heading">
          <el-icon><MagicStick /></el-icon>
          <div><p class="eyebrow">CURRENT CONTEXT</p><h2>当前职业画像</h2></div>
        </div>
        <template v-if="profile || skills.length || currentPlan">
          <div class="context-item"><span>目标岗位</span><strong>{{ profile?.targetPosition || currentPlan?.targetPosition || '暂未填写' }}</strong></div>
          <div class="context-item"><span>当前阶段</span><strong>{{ careerStageLabel[profile?.careerStage || ''] || '暂未填写' }}</strong></div>
          <div class="context-item"><span>已记录技能</span><strong>{{ skills.length ? `${skills.length} 项` : '暂未填写' }}</strong></div>
          <div class="context-item"><span>当前规划</span><strong>{{ currentPlan ? `V${currentPlan.version} · 匹配度 ${currentPlan.matchScore ?? '—'}%` : '暂未生成' }}</strong></div>
        </template>
        <div v-else-if="!contextLoading" class="context-empty">
          <p>还没有可展示的职业画像。</p>
        </div>
        <p class="context-tip">{{ contextTip }}</p>
        <el-button class="profile-button" @click="router.push('/profile')">查看并完善画像</el-button>
        <el-button class="plan-button" type="primary" :loading="planGenerating" @click="handlePlanAction">
          {{ currentPlan ? '查看职业规划' : '生成完整职业规划' }}
        </el-button>
      </aside>
    </section>

    <el-drawer v-model="showSessions" title="职业咨询记录" direction="ltr" size="340px">
      <div class="session-filter">
        <span>历史会话</span>
        <el-switch v-model="includeArchived" active-text="显示已归档" @change="refreshConversations" />
      </div>
      <div v-loading="sessionsLoading" class="session-list">
        <el-empty v-if="!sessionsLoading && !sessions.length" description="还没有聊天会话" :image-size="72" />
        <article
          v-for="session in sessions"
          :key="session.conversationId"
          class="session-item"
          :class="{ active: session.conversationId === conversationId }"
        >
          <button class="session-main" @click="openConversation(session)">
            <span>
              <strong>{{ session.title }}</strong>
              <el-tag v-if="session.status === 0" size="small" type="info">已归档</el-tag>
            </span>
            <small>{{ session.lastMessage || '开始一段新的职业咨询' }}</small>
            <em>{{ formatSessionTime(session.lastMessageAt || session.createTime) }} · {{ session.messageCount }} 条消息</em>
          </button>
          <el-dropdown trigger="click" @command="handleSessionCommand($event, session)">
            <el-button text circle :icon="MoreFilled" aria-label="管理会话" @click.stop />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename" :icon="EditPen">重命名</el-dropdown-item>
                <el-dropdown-item v-if="session.status === 1" command="archive" :icon="Folder">归档</el-dropdown-item>
                <el-dropdown-item v-else command="restore" :icon="RefreshRight">恢复</el-dropdown-item>
                <el-dropdown-item command="delete" :icon="Delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </article>
      </div>
      <template #footer>
        <el-button type="primary" :icon="Plus" :disabled="sending" @click="createConversation">新建对话</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.chat-page { display: flex; min-height: calc(100vh - 142px); flex-direction: column; }
.chat-layout { display: grid; grid-template-columns: minmax(0, 1fr) 250px; height: clamp(620px, calc(100vh - 120px), 920px); height: clamp(620px, calc(100dvh - 120px), 920px); flex: 0 0 auto; gap: 16px; min-height: 0; }
.chat-card { display: flex; height: 100%; min-height: 0; flex-direction: column; overflow: hidden; border-color: #b9d2f4; box-shadow: 0 18px 44px rgb(23 49 92 / 12%); }
.chat-toolbar { position: relative; display: flex; min-height: 72px; align-items: center; justify-content: space-between; gap: 14px; overflow: hidden; padding: 12px 20px; border-bottom: 0; color: #fff; background: radial-gradient(circle at 78% 0%, rgb(103 232 249 / 24%), transparent 34%), linear-gradient(110deg, #102f68, #1d4ed8 65%, #2581df); }
.chat-toolbar::after { position: absolute; right: 18%; bottom: -85px; width: 170px; height: 170px; border: 1px solid rgb(255 255 255 / 15%); border-radius: 50%; box-shadow: 0 0 0 28px rgb(255 255 255 / 4%); content: ''; pointer-events: none; }
.chat-toolbar > * { position: relative; z-index: 1; }
.conversation-identity { display: flex; min-width: 0; align-items: center; gap: 11px; }
.active-conversation { min-width: 0; }
.active-conversation > span { display: block; margin-bottom: 2px; color: #93c5fd; font-size: 9px; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; }
.active-conversation strong,
.active-conversation small { display: block; }
.active-conversation strong { overflow: hidden; color: #fff; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.active-conversation small { margin-top: 4px; color: #bfdbfe; font-size: 10px; }
.chat-actions { display: flex; flex: 0 0 auto; align-items: center; gap: 4px; }
.chat-actions .el-button + .el-button { margin-left: 0; }
.chat-actions .el-button { border-color: rgb(255 255 255 / 26%); color: #e0f2fe; background: rgb(255 255 255 / 10%); }
.chat-actions .el-button:hover { border-color: #fff; color: #1746a2; background: #fff; }
.message-area { min-height: 0; flex: 1; overflow-y: auto; padding: 24px 24px 8px; background: radial-gradient(circle at 12% 12%, rgb(59 130 246 / 8%), transparent 28%), linear-gradient(180deg, #f8fbff, #f3f7fc); scroll-behavior: smooth; }
.chat-message { display: flex; gap: 12px; max-width: 88%; margin-bottom: 18px; }
.chat-message > :first-child { margin-top: 1px; }
.message-avatar { display: grid; width: 36px; height: 36px; flex: 0 0 36px; place-items: center; border: 1px solid #c7d9f4; border-radius: 9px; color: var(--primary); background: var(--primary-soft); }
.message-bubble { padding: 14px 17px; border: 1px solid #d5e4f5; border-radius: 4px 12px 12px; background: rgb(255 255 255 / 96%); box-shadow: 0 8px 22px rgb(23 49 92 / 6%); }
.message-bubble small { color: var(--primary); font-weight: 700; }
.message-bubble > p { margin: 7px 0 0; color: var(--text); line-height: 1.75; white-space: pre-wrap; }
.markdown-content { min-width: 0; margin-top: 7px; color: var(--text); line-height: 1.75; overflow-wrap: anywhere; }
.markdown-content :deep(> :first-child) { margin-top: 0; }
.markdown-content :deep(> :last-child) { margin-bottom: 0; }
.markdown-content :deep(p) { margin: 0 0 10px; }
.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) { margin: 16px 0 8px; color: var(--text); line-height: 1.4; }
.markdown-content :deep(h1) { font-size: 20px; }
.markdown-content :deep(h2) { font-size: 18px; }
.markdown-content :deep(h3) { font-size: 16px; }
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) { font-size: 14px; }
.markdown-content :deep(ul),
.markdown-content :deep(ol) { margin: 8px 0 10px; padding-left: 24px; }
.markdown-content :deep(li + li) { margin-top: 4px; }
.markdown-content :deep(blockquote) { margin: 10px 0; padding: 7px 12px; border-left: 3px solid #9bbbea; color: var(--muted); background: #f3f7fd; }
.markdown-content :deep(a) { color: var(--primary); text-decoration: underline; text-underline-offset: 3px; }
.markdown-content :deep(code) { padding: 2px 5px; border-radius: 5px; color: var(--primary-dark); background: #eef3f9; font-family: Consolas, Monaco, monospace; font-size: 0.9em; }
.markdown-content :deep(pre) { max-width: 100%; margin: 10px 0; padding: 12px 14px; overflow-x: auto; border-radius: 9px; color: #e8e9f2; background: #282b3d; line-height: 1.6; }
.markdown-content :deep(pre code) { padding: 0; color: inherit; background: transparent; font-size: 12px; }
.markdown-content :deep(table) { display: block; max-width: 100%; margin: 10px 0; overflow-x: auto; border-spacing: 0; border-collapse: collapse; }
.markdown-content :deep(th),
.markdown-content :deep(td) { padding: 7px 10px; border: 1px solid var(--line); text-align: left; white-space: nowrap; }
.markdown-content :deep(th) { color: var(--text); background: #f3f7fc; }
.markdown-content :deep(hr) { margin: 14px 0; border: 0; border-top: 1px solid var(--line); }
.markdown-content.streaming-content :deep(> :last-child)::after { display: inline-block; width: 2px; height: 1em; margin-left: 3px; background: var(--primary); content: ''; vertical-align: -2px; animation: cursor-blink 0.8s steps(1) infinite; }
.chat-message.user { margin-left: auto; flex-direction: row-reverse; }
.chat-message.user .message-avatar { border-color: var(--line); color: var(--primary-dark); background: #edf2f7; }
.chat-message.user .message-bubble { border-color: #cbdcf4; border-radius: 12px 4px 12px 12px; background: var(--primary-soft); }
.chat-message.user .message-bubble > p { margin-top: 0; }
.chat-message.failed .message-bubble { border-color: #efb5b5; background: #fff7f7; }
.typing-dots { display: flex; gap: 5px; min-width: 50px; padding: 10px 2px 3px; }
.typing-dots i { width: 7px; height: 7px; border-radius: 50%; background: #8da5c3; animation: pulse 1.2s infinite ease-in-out; }
.typing-dots i:nth-child(2) { animation-delay: 0.15s; }
.typing-dots i:nth-child(3) { animation-delay: 0.3s; }
.chat-error { max-width: 620px; margin: 0 auto 20px; }
.chat-error :deep(.el-alert__content) { width: 100%; }
.chat-error .el-button { margin-top: 9px; }
.quick-list { display: flex; align-items: flex-start; gap: 8px; padding: 12px 14px 0 20px; border-top: 1px solid var(--line); }
.quick-questions { display: flex; min-width: 0; flex: 1; flex-wrap: wrap; gap: 8px; }
.quick-question { display: inline-flex; align-items: center; gap: 6px; padding: 9px 13px; border: 1px solid var(--line); border-radius: 7px; color: var(--muted); background: #fff; cursor: pointer; font-size: 12px; }
.quick-question:hover { border-color: #9bbbea; color: var(--primary); background: #f8fbff; }
.quick-question:disabled { cursor: not-allowed; opacity: 0.55; }
.quick-list .el-icon { color: var(--primary); }
.quick-close { flex: 0 0 auto; color: var(--muted); }
.quick-close:hover { color: var(--primary); background: var(--primary-soft); }
.chat-input { margin: 12px 20px 20px; padding: 13px 14px; border: 1px solid #c9dbf4; border-radius: 11px; background: #fff; box-shadow: 0 8px 22px rgb(23 49 92 / 6%); }
.chat-input:focus-within { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.09), 0 12px 28px rgb(29 78 216 / 9%); }
.chat-input :deep(.el-textarea__inner),
.chat-input :deep(.el-textarea__inner:focus) {
  min-height: 54px !important;
  max-height: 220px;
  padding: 0;
  border: 0;
  outline: 0;
  box-shadow: none !important;
}
.chat-input > div { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; }
.chat-input small { color: var(--muted); }
.input-meta { display: flex; min-width: 0; align-items: center; gap: 8px; }
.input-meta small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.quick-restore { flex: 0 0 auto; color: var(--primary); }
.portrait-card { position: relative; align-self: start; min-height: 0; overflow: hidden; padding: 19px; border-color: #2563eb; color: #fff; background: radial-gradient(circle at 100% 100%, rgb(56 189 248 / 26%), transparent 45%), linear-gradient(145deg, #102f68, #1d4ed8); box-shadow: 0 16px 38px rgb(23 70 154 / 16%); }
.portrait-card::after { position: absolute; right: -62px; bottom: -82px; width: 170px; height: 170px; border: 1px solid rgb(255 255 255 / 16%); border-radius: 50%; box-shadow: 0 0 0 26px rgb(255 255 255 / 4%); content: ''; pointer-events: none; }
.portrait-card > * { position: relative; z-index: 1; }
.portrait-heading { display: flex; align-items: center; gap: 9px; }
.portrait-heading > .el-icon { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 10px; color: #fff; background: rgb(255 255 255 / 14%); font-size: 16px; box-shadow: inset 0 0 0 1px rgb(255 255 255 / 15%); }
.portrait-heading .eyebrow { margin-bottom: 3px; }
.portrait-heading .eyebrow { color: #93c5fd; }
.portrait-heading h2 { margin: 0; color: #fff; font-size: 16px; }
.context-item { padding: 11px 0; border-bottom: 1px solid rgb(255 255 255 / 14%); }
.context-item span,
.context-item strong { display: block; }
.context-item span { color: #bfdbfe; font-size: 11px; }
.context-item strong { margin-top: 6px; color: #fff; font-size: 13px; }
.context-empty { display: grid; min-height: 90px; place-items: center; color: #dbeafe; font-size: 12px; text-align: center; }
.context-tip { margin: 12px 0 0; padding: 9px 10px; border-radius: 7px; color: #dbeafe; background: rgb(255 255 255 / 10%); font-size: 10px; line-height: 1.55; }
.profile-button { width: 100%; margin-top: 12px; }
.plan-button { width: 100%; margin: 8px 0 0; }
.portrait-card .plan-button { border-color: #fff; color: #1746a2; background: #fff; }
.portrait-card .profile-button { border-color: rgb(255 255 255 / 45%); color: #fff; background: rgb(255 255 255 / 8%); }
.portrait-card .profile-button:hover { border-color: #fff; color: #1746a2; background: #fff; }
.session-filter { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; color: var(--text); font-size: 13px; font-weight: 700; }
.session-list { min-height: 180px; }
.session-item { display: grid; grid-template-columns: minmax(0, 1fr) 32px; align-items: start; gap: 4px; margin-bottom: 8px; padding: 4px; border: 1px solid transparent; border-radius: 9px; background: #f8fbff; }
.session-item:hover { border-color: #cbdcf4; background: #f3f7fd; }
.session-item.active { border-color: #b9d0f2; background: var(--primary-soft); }
.session-main { min-width: 0; padding: 8px 7px; border: 0; color: inherit; background: transparent; cursor: pointer; text-align: left; }
.session-main > span { display: flex; min-width: 0; align-items: center; gap: 6px; }
.session-main strong { overflow: hidden; flex: 1; color: var(--text); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.session-main small,
.session-main em { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-main small { margin-top: 7px; color: var(--muted); font-size: 11px; }
.session-main em { margin-top: 6px; color: #8493a5; font-size: 10px; font-style: normal; }
.session-item > .el-dropdown { margin-top: 5px; }
.chat-page :deep(.el-drawer__body) { padding-top: 8px; }
@keyframes pulse { 0%, 60%, 100% { opacity: 0.35; transform: translateY(0); } 30% { opacity: 1; transform: translateY(-3px); } }
@keyframes cursor-blink { 50% { opacity: 0; } }
@media (max-width: 1100px) { .chat-layout { grid-template-columns: minmax(0, 1fr) 230px; } }
@media (max-width: 960px) { .chat-page { min-height: auto; } .chat-layout { grid-template-columns: 1fr; height: auto; } .chat-card { height: clamp(560px, calc(100dvh - 120px), 760px); } .portrait-card { width: 100%; } }
@media (max-width: 600px) {
  .chat-toolbar { align-items: flex-start; flex-direction: column; }
  .conversation-identity { width: 100%; }
  .chat-actions { display: grid; width: 100%; grid-template-columns: 1fr 1fr; gap: 8px; }
  .chat-actions .el-button { width: 100%; margin: 0; }
  .message-area { padding: 22px 14px 6px; }
  .chat-message { width: 100%; max-width: 100%; gap: 9px; }
  .chat-message .message-bubble { min-width: 0; flex: 1; padding: 13px 14px; }
  .chat-message.user { width: auto; max-width: 94%; }
  .chat-input { margin-inline: 14px; }
  .quick-list { padding-inline: 14px 10px; }
  .quick-questions { flex-wrap: nowrap; overflow-x: auto; padding-bottom: 2px; scrollbar-width: none; }
  .quick-questions::-webkit-scrollbar { display: none; }
  .quick-question { flex: 0 0 auto; }
  .input-meta small { max-width: 145px; }
}
</style>
