<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ArrowRight, MagicStick, RefreshRight, Service, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { chatWithCareerPlanner, getCurrentPlan } from '../../api/career'
import { getProfile } from '../../api/profile'
import type { CareerPlan, UserProfile } from '../../types/api'

interface ChatMessage {
  id: number
  role: 'assistant' | 'user'
  content: string
}

const router = useRouter()
const messageArea = ref<HTMLElement>()
const message = ref('')
const sending = ref(false)
const contextLoading = ref(true)
const conversationId = ref('')
const failedMessage = ref('')
const profile = ref<UserProfile>()
const currentPlan = ref<CareerPlan>()
let messageSequence = 1

const messages = ref<ChatMessage[]>([
  {
    id: messageSequence++,
    role: 'assistant',
    content: '你好，我是你的 AI 职业规划师。你可以和我讨论职业方向、学习路线、实习准备或求职选择。当前是单轮咨询模式，请在问题中带上关键背景，我会给出更有针对性的建议。',
  },
])

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
  const [profileResult, planResult] = await Promise.allSettled([
    getProfile({ silent: true }),
    getCurrentPlan({ silent: true }),
  ])
  if (profileResult.status === 'fulfilled') profile.value = profileResult.value
  if (planResult.status === 'fulfilled') currentPlan.value = planResult.value
  contextLoading.value = false
})

async function scrollToBottom() {
  await nextTick()
  if (messageArea.value) messageArea.value.scrollTop = messageArea.value.scrollHeight
}

async function send(content = message.value) {
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

  failedMessage.value = ''
  messages.value.push({ id: messageSequence++, role: 'user', content: question })
  message.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    const response = await chatWithCareerPlanner({ message: question })
    conversationId.value = response.conversationId
    messages.value.push({ id: messageSequence++, role: 'assistant', content: response.content })
  } catch {
    failedMessage.value = question
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

function chooseQuestion(question: string) {
  message.value = question
}

function formatChatContent(content: string) {
  return content
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/__(.*?)__/g, '$1')
    .replace(/^\s*[-*]\s+/gm, '• ')
}

function retry() {
  if (!failedMessage.value) return
  const question = failedMessage.value
  failedMessage.value = ''
  void send(question)
}
</script>

<template>
  <div class="chat-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">AI CAREER PLANNER</p>
        <h1>AI 职业规划师</h1>
        <p>讨论职业方向、能力差距和下一步行动；ChatMemory 与业务数据读取将在后续阶段接入。</p>
      </div>
      <el-button type="primary" disabled>生成完整职业规划 · 待接入</el-button>
    </header>

    <section class="chat-layout">
      <article class="surface-card chat-card">
        <div ref="messageArea" class="message-area">
          <article v-for="item in messages" :key="item.id" class="chat-message" :class="item.role">
            <span class="message-avatar">
              <el-icon><User v-if="item.role === 'user'" /><Service v-else /></el-icon>
            </span>
            <div class="message-bubble">
              <small>{{ item.role === 'user' ? '你' : 'AI 职业规划师' }}</small>
              <p>{{ formatChatContent(item.content) }}</p>
            </div>
          </article>

          <article v-if="sending" class="chat-message assistant pending-message">
            <span class="message-avatar"><el-icon><Service /></el-icon></span>
            <div class="message-bubble">
              <small>AI 职业规划师</small>
              <div class="typing-dots"><i></i><i></i><i></i></div>
            </div>
          </article>

          <el-alert v-if="failedMessage" class="chat-error" type="error" :closable="false" show-icon>
            <template #title>AI 服务暂时不可用，本次问题未获得回复。</template>
            <el-button size="small" :icon="RefreshRight" :loading="sending" @click="retry">重新发送</el-button>
          </el-alert>
        </div>

        <div class="quick-list">
          <button v-for="question in quickQuestions" :key="question" :disabled="sending" @click="chooseQuestion(question)">
            {{ question }} <el-icon><ArrowRight /></el-icon>
          </button>
        </div>

        <div class="chat-input">
          <el-input
            v-model="message"
            type="textarea"
            :rows="3"
            resize="none"
            maxlength="2000"
            placeholder="输入你的职业问题……"
            @keydown.ctrl.enter.prevent="send()"
          />
          <div>
            <small>{{ conversationId ? `会话标识：${conversationId}` : 'Ctrl + Enter 发送' }}</small>
            <el-button type="primary" :loading="sending" :disabled="!message.trim()" @click="send()">发送</el-button>
          </div>
        </div>
      </article>

      <aside class="surface-card portrait-card" v-loading="contextLoading">
        <div class="portrait-heading">
          <el-icon><MagicStick /></el-icon>
          <div><p class="eyebrow">CURRENT CONTEXT</p><h2>当前职业画像</h2></div>
        </div>
        <template v-if="profile || currentPlan">
          <div class="context-item"><span>目标岗位</span><strong>{{ profile?.targetPosition || currentPlan?.targetPosition || '暂未填写' }}</strong></div>
          <div class="context-item"><span>当前阶段</span><strong>{{ careerStageLabel[profile?.careerStage || ''] || '暂未填写' }}</strong></div>
          <div class="context-item"><span>当前规划</span><strong>{{ currentPlan ? `V${currentPlan.version} · 匹配度 ${currentPlan.matchScore ?? '—'}%` : '暂未生成' }}</strong></div>
        </template>
        <div v-else-if="!contextLoading" class="context-empty">
          <p>还没有可展示的职业画像。</p>
        </div>
        <p class="context-tip">当前对话暂未自动读取这些数据，请在提问时补充相关背景。</p>
        <el-button class="profile-button" @click="router.push('/profile')">查看并完善画像</el-button>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.chat-layout { display: grid; grid-template-columns: minmax(0, 1fr) 310px; gap: 20px; min-height: 640px; }
.chat-card { display: flex; min-height: 640px; flex-direction: column; overflow: hidden; }
.message-area { flex: 1; max-height: 500px; overflow-y: auto; padding: 28px 30px 8px; background: #fafafe; scroll-behavior: smooth; }
.chat-message { display: flex; gap: 12px; max-width: 82%; margin-bottom: 22px; }
.message-avatar { display: grid; width: 36px; height: 36px; flex: 0 0 36px; place-items: center; border-radius: 11px; color: #fff; background: linear-gradient(135deg, #595dd8, #8872e7); }
.message-bubble { padding: 14px 17px; border: 1px solid var(--line); border-radius: 4px 15px 15px; background: #fff; box-shadow: 0 5px 16px rgba(55, 57, 104, 0.04); }
.message-bubble small { color: var(--primary); font-weight: 700; }
.message-bubble p { margin: 7px 0 0; color: #44495e; line-height: 1.75; white-space: pre-wrap; }
.chat-message.user { margin-left: auto; flex-direction: row-reverse; }
.chat-message.user .message-avatar { background: #2e9c77; }
.chat-message.user .message-bubble { border-radius: 15px 4px 15px 15px; background: #eff9f5; }
.chat-message.user .message-bubble small { color: #268366; }
.typing-dots { display: flex; gap: 5px; min-width: 50px; padding: 10px 2px 3px; }
.typing-dots i { width: 7px; height: 7px; border-radius: 50%; background: #9699af; animation: pulse 1.2s infinite ease-in-out; }
.typing-dots i:nth-child(2) { animation-delay: 0.15s; }
.typing-dots i:nth-child(3) { animation-delay: 0.3s; }
.chat-error { max-width: 620px; margin: 0 auto 20px; }
.chat-error :deep(.el-alert__content) { width: 100%; }
.chat-error .el-button { margin-top: 9px; }
.quick-list { display: flex; flex-wrap: wrap; gap: 9px; padding: 16px 24px 0; border-top: 1px solid var(--line); }
.quick-list button { display: inline-flex; align-items: center; gap: 6px; padding: 9px 13px; border: 1px solid #dddff1; border-radius: 999px; color: #595e76; background: #fff; cursor: pointer; font-size: 12px; }
.quick-list button:hover { border-color: #a8aae9; color: var(--primary); background: #f8f8ff; }
.quick-list button:disabled { cursor: not-allowed; opacity: 0.55; }
.quick-list .el-icon { color: var(--primary); }
.chat-input { margin: 16px 24px 24px; padding: 15px; border: 1px solid #dfe1ed; border-radius: 14px; }
.chat-input:focus-within { border-color: #a6a8e8; box-shadow: 0 0 0 3px rgba(89, 93, 216, 0.08); }
.chat-input :deep(.el-textarea__inner) { padding: 0; border: 0; box-shadow: none; }
.chat-input > div { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; }
.chat-input small { color: var(--muted); }
.portrait-card { align-self: start; min-height: 330px; padding: 24px; }
.portrait-heading { display: flex; align-items: center; gap: 11px; }
.portrait-heading > .el-icon { display: grid; width: 38px; height: 38px; place-items: center; border-radius: 11px; color: var(--primary); background: var(--primary-soft); font-size: 18px; }
.portrait-heading .eyebrow { margin-bottom: 3px; }
.portrait-heading h2 { margin: 0; font-size: 18px; }
.context-item { padding: 14px 0; border-bottom: 1px solid var(--line); }
.context-item span,
.context-item strong { display: block; }
.context-item span { color: var(--muted); font-size: 11px; }
.context-item strong { margin-top: 6px; font-size: 13px; }
.context-empty { display: grid; min-height: 125px; place-items: center; color: var(--muted); font-size: 13px; text-align: center; }
.context-tip { margin: 16px 0 0; padding: 10px 11px; border-radius: 9px; color: #777b91; background: #f7f7fb; font-size: 11px; line-height: 1.6; }
.profile-button { width: 100%; margin-top: 16px; }
@keyframes pulse { 0%, 60%, 100% { opacity: 0.35; transform: translateY(0); } 30% { opacity: 1; transform: translateY(-3px); } }
@media (max-width: 960px) { .chat-layout { grid-template-columns: 1fr; } .message-area { max-height: none; } }
@media (max-width: 600px) { .message-area { padding: 22px 16px 6px; } .chat-message { max-width: 94%; } .chat-input { margin-inline: 16px; } .quick-list { padding-inline: 16px; } }
</style>
