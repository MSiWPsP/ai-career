<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ChatDotRound, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { answerInterview, finishInterview, getInterview, getInterviewMessages } from '../../api/interview'
import AgentAvatar from '../../components/AgentAvatar.vue'
import type { InterviewDetail, InterviewMessage } from '../../types/api'
import { difficultyLabel, interviewTypeLabel } from '../../utils/data'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const finishing = ref(false)
const loadError = ref(false)
const detail = ref<InterviewDetail>()
const messages = ref<InterviewMessage[]>([])
const answer = ref('')
const pendingAnswer = ref('')
const messageArea = ref<HTMLElement>()

const interviewId = computed(() => String(route.params.id || ''))
const isActive = computed(() => detail.value?.status === 1)
const statusText = computed(() => {
  if (detail.value?.status === 2) return '已完成'
  if (detail.value?.status === 3) return '已主动结束'
  if (detail.value?.status === 4) return '异常结束'
  return '进行中'
})

async function loadInterview() {
  if (!interviewId.value) {
    loadError.value = true
    return
  }
  loading.value = true
  loadError.value = false
  try {
    ;[detail.value, messages.value] = await Promise.all([
      getInterview(interviewId.value),
      getInterviewMessages(interviewId.value),
    ])
    await scrollToBottom()
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(loadInterview)

async function submitAnswer() {
  if (!isActive.value || submitting.value) return
  const content = answer.value.trim()
  if (!content) {
    ElMessage.warning('请输入你的回答')
    return
  }
  pendingAnswer.value = content
  submitting.value = true
  try {
    const result = await answerInterview(interviewId.value, content)
    answer.value = ''
    await loadInterview()
    if (result.finished) {
      ElMessage.success(result.reportId ? '面试报告已生成' : '面试已完成，可在报告页重试生成报告')
      await router.push(`/interview/${interviewId.value}/report`)
    }
  } finally {
    pendingAnswer.value = ''
    submitting.value = false
  }
}

async function endInterview() {
  if (!isActive.value || finishing.value) return
  try {
    await ElMessageBox.confirm('主动结束后不能继续回答，确认结束本次面试吗？', '结束面试', {
      confirmButtonText: '确认结束',
      cancelButtonText: '继续面试',
      type: 'warning',
    })
  } catch {
    return
  }
  finishing.value = true
  try {
    const result = await finishInterview(interviewId.value)
    await loadInterview()
    ElMessage.success(result.reportId ? '面试报告已生成' : '面试已结束，可在报告页生成报告')
    await router.push(`/interview/${interviewId.value}/report`)
  } finally {
    finishing.value = false
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messageArea.value) messageArea.value.scrollTop = messageArea.value.scrollHeight
}
</script>

<template>
  <section class="surface-card interview-room" v-loading="loading">
    <header class="interview-bar">
      <div><AgentAvatar role="interviewer" :size="44" /><div><strong>{{ detail?.targetPosition || 'AI 模拟面试' }}</strong><p>AI 模拟面试官 · {{ interviewTypeLabel[detail?.interviewType || 'TECHNICAL'] }} · {{ difficultyLabel[detail?.difficulty || 'MEDIUM'] }}</p></div></div>
      <div class="interview-progress"><span>{{ statusText }}</span><strong>{{ detail?.questionCount || 0 }} / {{ detail?.maxQuestions || 0 }} 题</strong></div>
      <el-button type="danger" plain :loading="finishing" :disabled="!isActive" @click="endInterview">结束面试</el-button>
    </header>

    <div ref="messageArea" class="message-area">
      <el-alert v-if="loadError" type="error" :closable="false" show-icon title="面试会话加载失败">
        <el-button link type="danger" :icon="RefreshRight" @click="loadInterview">重新加载</el-button>
      </el-alert>
      <template v-if="messages.length">
        <article v-for="item in messages" :key="item.id" class="message" :class="item.role">
          <span v-if="item.role === 'user'">我</span>
          <AgentAvatar v-else role="interviewer" :size="36" />
          <div><small>{{ item.role === 'user' ? '你的回答' : 'AI 面试官' }}</small><p>{{ item.content }}</p></div>
        </article>
      </template>
      <article v-if="pendingAnswer" class="message user pending-message">
        <span>我</span>
        <div><small>你的回答</small><p>{{ pendingAnswer }}</p></div>
      </article>
      <article v-if="submitting" class="message assistant thinking-message">
        <AgentAvatar role="interviewer" :size="36" />
        <div><small>AI 面试官正在分析你的回答</small><p class="thinking-dots"><i></i><i></i><i></i></p></div>
      </article>
      <div v-else-if="!messages.length && !loadError" class="empty-panel">
        <div><el-icon class="empty-icon"><ChatDotRound /></el-icon><strong>暂时没有面试消息</strong><span>返回配置页重新开始一场模拟面试。</span></div>
      </div>
    </div>

    <footer class="answer-box">
      <el-input v-model="answer" type="textarea" :rows="3" resize="none" :disabled="!isActive || submitting" :placeholder="isActive ? '请输入你的回答……' : '本次面试已结束'" @keydown.ctrl.enter.prevent="submitAnswer" />
      <div>
        <small>{{ isActive ? 'Ctrl + Enter 提交 · 面试过程中不会显示单题评分' : '完整面试记录已保存，可查看或生成面试报告' }}</small>
        <el-button v-if="isActive" type="primary" :loading="submitting" @click="submitAnswer">提交回答</el-button>
        <el-button v-else type="primary" plain @click="router.push(`/interview/${interviewId}/report`)">查看报告</el-button>
      </div>
    </footer>
  </section>
</template>

<style scoped>
.interview-room { display: flex; min-height: calc(100vh - 128px); flex-direction: column; overflow: hidden; border-color: #b9d2f4; box-shadow: 0 20px 48px rgb(23 49 92 / 13%); }
.interview-bar { position: relative; display: grid; grid-template-columns: 1fr auto auto; align-items: center; gap: 25px; overflow: hidden; padding: 18px 22px; border-bottom: 0; color: #fff; background: radial-gradient(circle at 78% 0%, rgb(103 232 249 / 23%), transparent 38%), linear-gradient(110deg, #102f68, #1d4ed8 66%, #2581df); }
.interview-bar::after { position: absolute; right: 12%; bottom: -88px; width: 170px; height: 170px; border: 1px solid rgb(255 255 255 / 16%); border-radius: 50%; box-shadow: 0 0 0 28px rgb(255 255 255 / 4%); content: ''; pointer-events: none; }
.interview-bar > * { position: relative; z-index: 1; }
.interview-bar > div:first-child { display: flex; align-items: center; gap: 11px; }
.interview-bar strong { font-size: 14px; }
.interview-bar p { margin: 4px 0 0; color: #bfdbfe; font-size: 11px; }
.interview-progress { text-align: right; }
.interview-progress span,
.interview-progress strong { display: block; }
.interview-progress { padding: 8px 13px; border: 1px solid rgb(255 255 255 / 16%); border-radius: 9px; background: rgb(255 255 255 / 10%); backdrop-filter: blur(6px); }
.interview-progress span { color: #bfdbfe; font-size: 10px; }
.interview-progress strong { margin-top: 4px; font-size: 13px; }
.interview-bar > .el-button { border-color: rgb(255 255 255 / 48%); color: #fff; background: rgb(255 255 255 / 8%); }
.interview-bar > .el-button:hover { border-color: #fff; color: #1746a2; background: #fff; }
.message-area { flex: 1; overflow-y: auto; padding: 30px 28px; background: radial-gradient(circle at 10% 10%, rgb(59 130 246 / 8%), transparent 28%), linear-gradient(180deg, #f8fbff, #f2f7fc); }
.message { display: flex; gap: 12px; max-width: 78%; margin-bottom: 24px; }
.message > span { display: grid; width: 34px; height: 34px; flex: 0 0 34px; place-items: center; border: 1px solid #c7d9f4; border-radius: 8px; color: var(--primary); background: var(--primary-soft); font-size: 11px; font-weight: 700; }
.message > :first-child { margin-top: 1px; }
.message > div { padding: 14px 16px; border: 1px solid #d3e2f4; border-radius: 4px 14px 14px; background: #fff; box-shadow: 0 9px 24px rgb(23 49 92 / 7%); }
.message small { color: var(--muted); }
.message p { margin: 6px 0 0; line-height: 1.75; }
.message.user { margin-left: auto; flex-direction: row-reverse; }
.message.user > span { border-color: var(--line); color: var(--primary-dark); background: #edf2f7; }
.message.user > div { border-color: #cbdcf4; border-radius: 12px 4px 12px 12px; background: var(--primary-soft); }
.answer-box { padding: 17px 20px; border-top: 1px solid #c7dcf6; background: linear-gradient(90deg, #fff, #f7fbff); box-shadow: 0 -10px 26px rgb(23 49 92 / 5%); }
.answer-box > div { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; }
.answer-box small { color: var(--muted); }
.pending-message { opacity: .82; }
.thinking-dots { display: flex; gap: 5px; min-width: 42px; }
.thinking-dots i { width: 6px; height: 6px; border-radius: 50%; background: var(--primary); animation: thinking 1.2s infinite ease-in-out; }
.thinking-dots i:nth-child(2) { animation-delay: .15s; }
.thinking-dots i:nth-child(3) { animation-delay: .3s; }
@keyframes thinking { 0%, 60%, 100% { opacity: .3; transform: translateY(0); } 30% { opacity: 1; transform: translateY(-4px); } }
@media (max-width: 640px) {
  .interview-room { min-height: calc(100dvh - 96px); }
  .interview-bar { grid-template-columns: minmax(0, 1fr) auto; gap: 12px; padding: 14px; }
  .interview-bar > div:first-child { grid-column: 1 / -1; min-width: 0; }
  .interview-bar > div:first-child > div { min-width: 0; }
  .interview-bar > div:first-child strong { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .interview-progress { text-align: left; }
  .message-area { padding: 20px 14px; }
  .message { width: 100%; max-width: 100%; gap: 9px; }
  .message > div { min-width: 0; flex: 1; padding: 13px 14px; }
  .message.user { width: auto; max-width: 94%; }
  .answer-box { padding: 14px; }
  .answer-box > div { align-items: flex-end; gap: 12px; }
  .answer-box small { line-height: 1.5; }
}
</style>
