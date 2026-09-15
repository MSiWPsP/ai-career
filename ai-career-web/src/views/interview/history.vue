<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ChatDotRound } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { getInterviewHistory } from '../../api/interview'
import type { InterviewHistoryRecord } from '../../types/api'
import { difficultyLabel, formatDate, interviewTypeLabel } from '../../utils/data'

const router = useRouter()
const loading = ref(true)
const records = ref<InterviewHistoryRecord[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 8

onMounted(load)

async function load() {
  loading.value = true
  try {
    const result = await getInterviewHistory(page.value, pageSize)
    records.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function statusInfo(status: number) {
  const map: Record<number, { label: string; type: 'info' | 'primary' | 'success' | 'warning' | 'danger' }> = {
    0: { label: '未开始', type: 'info' },
    1: { label: '进行中', type: 'primary' },
    2: { label: '已完成', type: 'success' },
    3: { label: '主动终止', type: 'warning' },
    4: { label: '异常结束', type: 'danger' },
  }
  return map[status] || map[0]
}
</script>

<template>
  <div>
    <header class="page-heading">
      <div>
        <p class="eyebrow">INTERVIEW HISTORY</p>
        <h1>面试记录</h1>
        <p>每一次练习都有记录，每一次复盘都让下一次表现更好。</p>
      </div>
      <el-button type="primary" @click="router.push('/interview/setup')">开始新的模拟面试</el-button>
    </header>

    <section class="history-summary">
      <div><span>累计面试</span><strong>{{ total }}</strong><small>次练习</small></div>
      <div><span>当前页面最高分</span><strong>{{ records.length ? Math.max(...records.map((item) => item.totalScore || 0)) : '—' }}</strong><small>继续突破</small></div>
      <div><span>已完成</span><strong>{{ records.filter((item) => item.status === 2).length }}</strong><small>当前页记录</small></div>
    </section>

    <section class="surface-card history-list" v-loading="loading">
      <div class="list-head">
        <div><h2>历史模拟面试</h2><p>按时间从近到远排列</p></div>
        <span>共 {{ total }} 条记录</span>
      </div>
      <template v-if="records.length">
        <article v-for="item in records" :key="item.id" class="interview-item">
          <div class="date-block"><strong>{{ formatDate(item.createTime).slice(5) }}</strong><span>{{ formatDate(item.createTime).slice(0, 4) }}</span></div>
          <el-icon class="interview-icon"><ChatDotRound /></el-icon>
          <div class="interview-info">
            <h3>{{ item.targetPosition }}</h3>
            <p>
              <span>{{ interviewTypeLabel[item.interviewType] || item.interviewType }}</span>
              <i>·</i>
              <span>{{ difficultyLabel[item.difficulty] || item.difficulty }}</span>
            </p>
          </div>
          <el-tag :type="statusInfo(item.status).type" effect="light">{{ statusInfo(item.status).label }}</el-tag>
          <div class="item-score"><strong>{{ item.totalScore ?? '—' }}</strong><span>综合评分</span></div>
          <div class="item-actions">
            <el-button @click="router.push('/interview/session/' + item.id)">查看记录</el-button>
            <el-button type="primary" plain :disabled="item.status !== 2 && item.status !== 3" @click="router.push('/interview/' + item.id + '/report')">{{ item.totalScore == null ? '生成报告' : '查看报告' }}</el-button>
          </div>
        </article>
      </template>
      <div v-else class="empty-panel">
        <div><el-icon class="empty-icon"><ChatDotRound /></el-icon><strong>还没有模拟面试记录</strong><p>完成一次 AI 模拟面试，系统会为你沉淀完整对话和能力报告。</p><el-button type="primary" @click="router.push('/interview/setup')">准备第一次面试</el-button></div>
      </div>
      <footer v-if="total > pageSize" class="pagination">
        <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="load" />
      </footer>
    </section>
  </div>
</template>

<style scoped>
.history-summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; margin-bottom: 20px; }
.history-summary > div { position: relative; display: grid; min-height: 88px; grid-template-columns: 1fr auto; align-items: center; overflow: hidden; padding: 18px 20px; border: 1px solid #bfd5f3; border-radius: 13px; background: linear-gradient(145deg, #fff, #f2f7ff); box-shadow: 0 10px 25px rgb(29 78 216 / 8%); transition: box-shadow var(--motion-normal) ease, transform var(--motion-normal) var(--ease-standard); }
.history-summary > div:first-child { border-color: #2563eb; color: #fff; background: linear-gradient(145deg, #123b82, #2563eb); }
.history-summary > div::after { position: absolute; right: -28px; bottom: -42px; width: 100px; height: 100px; border: 15px solid rgb(59 130 246 / 9%); border-radius: 50%; content: ''; }
.history-summary > div:hover { box-shadow: 0 17px 34px rgb(29 78 216 / 14%); transform: translateY(-4px); }
.history-summary span { color: var(--muted); font-size: 12px; }
.history-summary > div:first-child span,
.history-summary > div:first-child small { color: #dbeafe; }
.history-summary strong { grid-row: 1 / 3; grid-column: 2; font-size: 25px; }
.history-summary small { margin-top: 5px; color: #9a9eb0; }
.history-list { position: relative; overflow: hidden; box-shadow: 0 16px 38px rgb(23 49 92 / 9%); }
.history-list::before { position: absolute; z-index: 1; inset: 0 0 auto; height: 4px; background: linear-gradient(90deg, #1d4ed8, #38bdf8 62%, transparent); content: ''; }
.list-head { display: flex; align-items: center; justify-content: space-between; padding: 23px 25px; border-bottom: 1px solid #d5e3f4; background: linear-gradient(90deg, #eff6ff, #fff); }
.list-head h2 { margin: 0; font-size: 17px; }
.list-head p { margin: 5px 0 0; color: var(--muted); font-size: 11px; }
.list-head > span { color: var(--muted); font-size: 12px; }
.interview-item { display: grid; grid-template-columns: 78px 42px minmax(180px, 1fr) auto 90px auto; align-items: center; gap: 17px; padding: 18px 25px; border-bottom: 1px solid var(--line); transition: background var(--motion-fast) ease, transform var(--motion-normal) var(--ease-standard); }
.interview-item:hover { background: linear-gradient(90deg, #f1f7ff, #fff); transform: translateX(4px); }
.date-block strong,
.date-block span { display: block; }
.date-block strong { font-size: 13px; }
.date-block span { margin-top: 4px; color: var(--muted); font-size: 11px; }
.interview-icon { display: grid; width: 40px; height: 40px; place-items: center; border-radius: 12px; color: #fff; background: linear-gradient(145deg, #1d4ed8, #38bdf8); box-shadow: 0 8px 18px rgb(29 78 216 / 20%); font-size: 20px; }
.interview-info h3 { margin: 0 0 7px; font-size: 14px; }
.interview-info p { display: flex; gap: 7px; margin: 0; color: var(--muted); font-size: 11px; }
.item-score { text-align: center; }
.item-score strong,
.item-score span { display: block; }
.item-score strong { color: var(--primary); font-size: 21px; }
.item-score span { margin-top: 3px; color: var(--muted); font-size: 9px; }
.item-actions { display: flex; gap: 7px; }
.pagination { display: flex; justify-content: flex-end; padding: 16px 24px; }
.empty-panel p { max-width: 460px; margin: 8px auto 18px; line-height: 1.6; }
@media (max-width: 1050px) {
  .interview-item { grid-template-columns: 60px 40px 1fr auto; }
  .interview-item > .el-tag { display: none; }
  .item-score { grid-column: 4; grid-row: 1; }
  .item-actions { grid-column: 3 / 5; }
}
@media (max-width: 700px) { .history-summary { grid-template-columns: 1fr; } .interview-item { grid-template-columns: 45px 1fr; } .date-block { display: none; } .item-score { grid-column: 2; grid-row: 2; text-align: left; } .item-actions { grid-column: 2; } }
</style>
