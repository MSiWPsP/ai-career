<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { getInterview, getInterviewMessages } from '../../api/interview'
import type { InterviewDetail, InterviewMessage } from '../../types/api'
import { difficultyLabel, interviewTypeLabel } from '../../utils/data'

const route = useRoute()
const loading = ref(false)
const detail = ref<InterviewDetail>()
const messages = ref<InterviewMessage[]>([])
const answer = ref('')

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    ;[detail.value, messages.value] = await Promise.all([getInterview(id), getInterviewMessages(id)])
  } finally {
    loading.value = false
  }
})

function submitAnswer() {
  ElMessage.info('回答接口将在 Interviewer Agent 阶段接入')
}
</script>

<template>
  <section class="surface-card interview-room" v-loading="loading">
    <header class="interview-bar">
      <div><span class="ai-avatar">AI</span><div><strong>{{ detail?.targetPosition || 'AI 模拟面试' }}</strong><p>{{ interviewTypeLabel[detail?.interviewType || 'TECHNICAL'] }} · {{ difficultyLabel[detail?.difficulty || 'MEDIUM'] }}</p></div></div>
      <div class="interview-progress"><span>当前进度</span><strong>{{ detail?.questionCount || 0 }} 题</strong></div>
      <el-button type="danger" plain disabled>结束面试</el-button>
    </header>

    <div class="message-area">
      <template v-if="messages.length">
        <article v-for="item in messages" :key="item.id" class="message" :class="item.role">
          <span>{{ item.role === 'user' ? '我' : 'AI' }}</span>
          <div><small>{{ item.role === 'user' ? '你的回答' : 'AI 面试官' }}</small><p>{{ item.content }}</p></div>
        </article>
      </template>
      <div v-else class="empty-panel">
        <div><span class="empty-icon">◇</span><strong>面试会话等待开始</strong><span>启动与回答接口完成后，这里将呈现真实的多轮面试过程。</span></div>
      </div>
    </div>

    <footer class="answer-box">
      <el-input v-model="answer" type="textarea" :rows="3" resize="none" placeholder="请输入你的回答……" @keydown.ctrl.enter="submitAnswer" />
      <div><small>面试过程中不会显示单题评分</small><el-button type="primary" @click="submitAnswer">提交回答</el-button></div>
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
</style>
