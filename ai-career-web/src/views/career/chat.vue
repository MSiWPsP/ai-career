<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const message = ref('')
const quickQuestions = [
  '我适合做 Java 后端吗？',
  '半年后找实习应该怎么准备？',
  'Redis 和微服务应该先学哪个？',
]

function send() {
  ElMessage.info('AI 职业规划对话接口将在后续 Agent 阶段接入')
}
</script>

<template>
  <div class="chat-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">AI CAREER PLANNER</p>
        <h1>AI 职业规划师</h1>
        <p>结合你的职业画像，讨论方向、能力差距和下一步行动。</p>
      </div>
      <el-button type="primary" disabled>生成完整职业规划 · 待接入</el-button>
    </header>

    <section class="chat-layout">
      <article class="surface-card chat-card">
        <div class="assistant-intro">
          <span>✦</span>
          <div>
            <small>AI 职业规划师</small>
            <h2>你好，我是你的职业成长搭档。</h2>
            <p>我会基于职业画像、技能、成长任务和面试表现，为你提供连续的职业建议。</p>
          </div>
        </div>
        <div class="quick-list">
          <button v-for="question in quickQuestions" :key="question" @click="message = question">{{ question }} <span>→</span></button>
        </div>
        <div class="chat-notice">
          <span>开发中</span>
          <p>页面结构已经就绪。CareerPlanner Agent 接口完成后，将在这里接入真实的多轮对话。</p>
        </div>
        <div class="chat-input">
          <el-input v-model="message" type="textarea" :rows="3" resize="none" placeholder="输入你的职业问题……" @keydown.ctrl.enter="send" />
          <div><small>Ctrl + Enter 发送</small><el-button type="primary" @click="send">发送</el-button></div>
        </div>
      </article>

      <aside class="surface-card portrait-card">
        <p class="eyebrow">CURRENT CONTEXT</p>
        <h2>当前职业画像</h2>
        <div class="context-item"><span>目标岗位</span><strong>从职业画像读取</strong></div>
        <div class="context-item"><span>当前阶段</span><strong>从职业画像读取</strong></div>
        <div class="context-item"><span>能力短板</span><strong>从能力评分读取</strong></div>
        <el-button class="profile-button" @click="router.push('/profile')">查看并完善画像</el-button>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.chat-layout { display: grid; grid-template-columns: minmax(0, 1fr) 310px; gap: 20px; min-height: 640px; }
.chat-card { display: flex; flex-direction: column; padding: 30px; }
.assistant-intro { display: flex; gap: 17px; max-width: 720px; padding: 24px; border-radius: 16px; background: linear-gradient(135deg, #f0f1ff, #fafaff); }
.assistant-intro > span { display: grid; width: 43px; height: 43px; flex: 0 0 43px; place-items: center; border-radius: 13px; color: #fff; background: linear-gradient(135deg, #595dd8, #8872e7); }
.assistant-intro small { color: var(--primary); font-weight: 700; }
.assistant-intro h2 { margin: 8px 0; font-size: 19px; }
.assistant-intro p { margin: 0; color: var(--muted); font-size: 13px; line-height: 1.7; }
.quick-list { display: flex; flex-wrap: wrap; gap: 9px; margin: 25px 0; }
.quick-list button { padding: 9px 13px; border: 1px solid #dddff1; border-radius: 999px; color: #595e76; background: #fff; cursor: pointer; font-size: 12px; }
.quick-list span { margin-left: 6px; color: var(--primary); }
.chat-notice { display: grid; min-height: 220px; place-content: center; color: var(--muted); text-align: center; }
.chat-notice span { width: fit-content; margin: 0 auto 10px; padding: 6px 11px; border-radius: 999px; color: var(--warning); background: var(--warning-soft); font-size: 11px; font-weight: 700; }
.chat-notice p { max-width: 450px; margin: 0; font-size: 13px; line-height: 1.7; }
.chat-input { margin-top: auto; padding: 15px; border: 1px solid #dfe1ed; border-radius: 14px; }
.chat-input :deep(.el-textarea__inner) { padding: 0; border: 0; box-shadow: none; }
.chat-input > div { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; }
.chat-input small { color: var(--muted); }
.portrait-card { align-self: start; padding: 24px; }
.portrait-card h2 { margin: 0 0 24px; font-size: 18px; }
.context-item { padding: 14px 0; border-bottom: 1px solid var(--line); }
.context-item span,
.context-item strong { display: block; }
.context-item span { color: var(--muted); font-size: 11px; }
.context-item strong { margin-top: 6px; font-size: 13px; }
.profile-button { width: 100%; margin-top: 20px; }
@media (max-width: 960px) { .chat-layout { grid-template-columns: 1fr; } }
</style>
