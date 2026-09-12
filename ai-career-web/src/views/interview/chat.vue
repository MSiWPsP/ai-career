<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ChatDotRound, RefreshRight, Service } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { answerInterview, finishInterview, getInterview, getInterviewMessages } from '../../api/interview'
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
    if (result.finished) ElMessage.success('本次模拟面试已完成，完整记录已保存')
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
    await finishInterview(interviewId.value)
    await loadInterview()
    ElMessage.success('面试已结束，完整记录已保存')
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
      <div><el-icon class="ai-avatar"><Service /></el-icon><div><strong>{{ detail?.targetPosition || 'AI 模拟面试' }}</strong><p>{{ interviewTypeLabel[detail?.interviewType || 'TECHNICAL'] }} · {{ difficultyLabel[detail?.difficulty || 'MEDIUM'] }}</p></div></div>
      <div class="interview-progress"><span>{{ statusText }}</span><strong>{{ detail?.questionCount || 0 }} / {{ detail?.maxQuestions || 0 }} 题</strong></div>
      <el-button type="danger" plain :loading="finishing" :disabled="!isActive" @click="endInterview">结束面试</el-button>
    </header>

    <div ref="messageArea" class="message-area">
      <el-alert v-if="loadError" type="error" :closable="false" show-icon title="面试会话加载失败">
        <el-button link type="danger" :icon="RefreshRight" @click="loadInterview">重新加载</el-button>
      </el-alert>
      <template v-if="messages.length">
        <article v-for="item in messages" :key="item.id" class="message" :class="item.role">
          <span><template v-if="item.role === 'user'">我</template><el-icon v-else><Service /></el-icon></span>
          <div><small>{{ item.role === 'user' ? '你的回答' : 'AI 面试官' }}</small><p>{{ item.content }}</p></div>
        </article>
      </template>
      <article v-if="pendingAnswer" class="message user pending-message">
        <span>我</span>
        <div><small>你的回答</small><p>{{ pendingAnswer }}</p></div>
      </article>
      <article v-if="submitting" class="message assistant thinking-message">
        <span><el-icon><Service /></el-icon></span>
        <div><small>AI 面试官正在分析你的回答</small><p class="thinking-dots"><i></i><i></i><i></i></p></div>
      </article>
      <div v-else-if="!messages.length && !loadError" class="empty-panel">
        <div><el-icon class="empty-icon"><ChatDotRound /></el-icon><strong>暂时没有面试消息</strong><span>返回配置页重新开始一场模拟面试。</span></div>
      </div>
    </div>

    <footer class="answer-box">
      <el-input v-model="answer" type="textarea" :rows="3" resize="none" :disabled="!isActive || submitting" :placeholder="isActive ? '请输入你的回答……' : '本次面试已结束'" @keydown.ctrl.enter.prevent="submitAnswer" />
      <div>
        <small>{{ isActive ? 'Ctrl + Enter 提交 · 面试过程中不会显示单题评分' : '完整面试记录已保存，报告与能力回写将在后续开放' }}</small>
        <el-button v-if="isActive" type="primary" :loading="submitting" @click="submitAnswer">提交回答</el-button>
        <el-button v-else type="primary" plain @click="router.push({ name: 'interview-setup' })">开始新面试</el-button>
      </div>
    </footer>
  </section>
</template>

<style scoped>
.interview-room { display: flex; min-height: calc(100vh - 128px); flex-direction: column; overflow: hidden; }
.interview-bar { display: grid; grid-template-columns: 1fr auto auto; align-items: center; gap: 25px; padding: 17px 22px; border-bottom: 1px solid var(--line); }
.interview-bar > div:first-child { display: flex; align-items: center; gap: 11px; }
.ai-avatar { display: grid; width: 40px; height: 40px; place-items: center; border-radius: 12px; color: #fff; background: linear-gradient(135deg, #5559d8, #806ce4); font-size: 12px; font-weight: 800; }
.interview-bar strong { font-size: 14px; }
.interview-bar p { margin: 4px 0 0; color: var(--muted); font-size: 11px; }
.interview-progress { text-align: right; }
.interview-progress span,
.interview-progress strong { display: block; }
.interview-progress span { color: var(--muted); font-size: 10px; }
.interview-progress strong { margin-top: 4px; font-size: 13px; }
.message-area { flex: 1; overflow-y: auto; padding: 28px; background: #fafafe; }
.message { display: flex; gap: 12px; max-width: 78%; margin-bottom: 24px; }
.message > span { display: grid; width: 34px; height: 34px; flex: 0 0 34px; place-items: center; border-radius: 10px; color: #fff; background: var(--primary); font-size: 11px; font-weight: 700; }
.message > div { padding: 14px 16px; border: 1px solid var(--line); border-radius: 4px 14px 14px; background: #fff; }
.message small { color: var(--muted); }
.message p { margin: 6px 0 0; line-height: 1.75; }
.message.user { margin-left: auto; flex-direction: row-reverse; }
.message.user > span { background: #2e9c77; }
.message.user > div { border-radius: 14px 4px 14px 14px; background: #eff9f5; }
.answer-box { padding: 16px 20px; border-top: 1px solid var(--line); background: #fff; }
.answer-box > div { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; }
.answer-box small { color: var(--muted); }
.pending-message { opacity: .82; }
.thinking-dots { display: flex; gap: 5px; min-width: 42px; }
.thinking-dots i { width: 6px; height: 6px; border-radius: 50%; background: var(--primary); animation: thinking 1.2s infinite ease-in-out; }
.thinking-dots i:nth-child(2) { animation-delay: .15s; }
.thinking-dots i:nth-child(3) { animation-delay: .3s; }
@keyframes thinking { 0%, 60%, 100% { opacity: .3; transform: translateY(0); } 30% { opacity: 1; transform: translateY(-4px); } }
</style>
