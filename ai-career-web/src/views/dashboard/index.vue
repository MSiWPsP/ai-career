<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  Aim,
  ArrowRight,
  ChatDotRound,
  CircleCheck,
  MagicStick,
  Sunny,
  TrendCharts,
} from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { getAbilityRadar } from '../../api/ability'
import { getCurrentPlan } from '../../api/career'
import { getInterviewHistory } from '../../api/interview'
import { getProfile } from '../../api/profile'
import { getTaskStatistics, getTasks, updateTaskStatus } from '../../api/task'
import { getCurrentUser } from '../../api/user'
import AbilityRadar from '../../components/AbilityRadar.vue'
import TaskCard from '../../components/TaskCard.vue'
import type {
  AbilityRadar as AbilityRadarData,
  CareerPlan,
  CareerTask,
  InterviewHistoryRecord,
  RoadmapStage,
  TaskStatistics,
  UserInfo,
  UserProfile,
} from '../../types/api'
import { formatDate, interviewTypeLabel, parseJsonField } from '../../utils/data'

const router = useRouter()
const loading = ref(true)
const updatingTaskId = ref<number>()
const user = ref<UserInfo>()
const profile = ref<UserProfile>()
const plan = ref<CareerPlan>()
const tasks = ref<CareerTask[]>([])
const statistics = ref<TaskStatistics>({ total: 0, completed: 0, processing: 0, waiting: 0, completionRate: 0 })
const radar = ref<AbilityRadarData>({ indicators: [], values: [] })
const latestInterview = ref<InterviewHistoryRecord>()

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const roadmap = computed(() => parseJsonField<RoadmapStage[]>(plan.value?.roadmap, []))

const advice = computed(() => {
  if (!profile.value?.targetPosition) {
    return '先完善职业画像，明确目标岗位和可投入的学习时间，我会据此为你组织接下来的成长路线。'
  }
  if (!radar.value.values.length) {
    return '你的职业目标已经明确。下一步补充技能画像，能力雷达图会帮助你快速识别优先提升方向。'
  }
  const minScore = Math.min(...radar.value.values)
  const index = radar.value.values.indexOf(minScore)
  const ability = radar.value.indicators[index]?.name
  return ability + ' 是当前能力画像中相对薄弱的一项（' + minScore + ' 分）。建议今天优先安排一个可完成的小任务进行强化。'
})

onMounted(loadDashboard)

async function loadDashboard() {
  loading.value = true
  const silent = { silent: true }
  const results = await Promise.allSettled([
    getCurrentUser(silent),
    getProfile(silent),
    getCurrentPlan(silent),
    getTasks(undefined, silent),
    getTaskStatistics(silent),
    getAbilityRadar(silent),
    getInterviewHistory(1, 1, silent),
  ])
  if (results[0].status === 'fulfilled') user.value = results[0].value
  if (results[1].status === 'fulfilled') profile.value = results[1].value
  if (results[2].status === 'fulfilled') plan.value = results[2].value
  if (results[3].status === 'fulfilled') tasks.value = results[3].value
  if (results[4].status === 'fulfilled') statistics.value = results[4].value
  if (results[5].status === 'fulfilled') radar.value = results[5].value
  if (results[6].status === 'fulfilled') latestInterview.value = results[6].value.records[0]
  loading.value = false
}

async function changeTaskStatus(task: CareerTask, status: number) {
  updatingTaskId.value = task.id
  try {
    const updated = await updateTaskStatus(task.id, status)
    const index = tasks.value.findIndex((item) => item.id === task.id)
    if (index >= 0) tasks.value[index] = updated
    statistics.value = await getTaskStatistics()
  } finally {
    updatingTaskId.value = undefined
  }
}
</script>

<template>
  <div v-loading="loading">
    <header class="dashboard-hero">
      <div>
        <p class="eyebrow">TODAY'S GROWTH</p>
        <h1>{{ greeting }}，{{ user?.nickname || '同学' }} <el-icon><Sunny /></el-icon></h1>
        <p>今天也是向目标前进一步的一天。</p>
      </div>
      <div class="goal-card">
        <span>当前职业目标</span>
        <strong>{{ profile?.targetPosition || plan?.targetPosition || '尚未设置' }}</strong>
        <button @click="router.push('/profile')">编辑目标 <el-icon><ArrowRight /></el-icon></button>
      </div>
    </header>

    <section class="metrics-grid">
      <article class="metric-card">
        <el-icon class="metric-icon purple"><Aim /></el-icon>
        <div><p>职业匹配度</p><strong>{{ plan?.matchScore ?? '—' }}<small v-if="plan?.matchScore">%</small></strong></div>
        <em>目标契合情况</em>
      </article>
      <article class="metric-card">
        <el-icon class="metric-icon green"><CircleCheck /></el-icon>
        <div><p>任务完成率</p><strong>{{ statistics.completionRate }}<small>%</small></strong></div>
        <em>{{ statistics.completed }}/{{ statistics.total }} 项已完成</em>
      </article>
      <article class="metric-card">
        <el-icon class="metric-icon orange"><ChatDotRound /></el-icon>
        <div><p>最近面试成绩</p><strong>{{ latestInterview?.totalScore ?? '—' }}<small v-if="latestInterview?.totalScore">分</small></strong></div>
        <em>{{ latestInterview ? formatDate(latestInterview.createTime) : '等待首次面试' }}</em>
      </article>
      <article class="metric-card">
        <el-icon class="metric-icon blue"><TrendCharts /></el-icon>
        <div><p>成长任务</p><strong>{{ statistics.processing }}<small>项</small></strong></div>
        <em>正在进行中</em>
      </article>
    </section>

    <section class="section-grid two-column dashboard-main">
      <article class="surface-card radar-panel">
        <div class="card-header">
          <div><h2>我的能力画像</h2><p>基于最近的技能与能力评分</p></div>
          <button class="text-link" @click="router.push('/ability')">查看详情 <el-icon><ArrowRight /></el-icon></button>
        </div>
        <AbilityRadar :data="radar" :height="310" />
      </article>

      <article class="surface-card tasks-panel">
        <div class="card-header">
          <div><h2>当前成长任务</h2><p>从最重要的一件事开始</p></div>
          <span class="soft-label">{{ statistics.waiting }} 项待开始</span>
        </div>
        <div v-if="tasks.length" class="dashboard-tasks">
          <TaskCard
            v-for="task in tasks.slice(0, 3)"
            :key="task.id"
            :task="task"
            :updating="updatingTaskId === task.id"
            @change-status="changeTaskStatus"
          />
        </div>
        <div v-else class="empty-panel compact">
          <div><el-icon class="empty-icon"><CircleCheck /></el-icon><strong>暂无成长任务</strong><span>生成职业规划后，任务会出现在这里。</span></div>
        </div>
        <button class="panel-footer-link" @click="router.push('/tasks')">查看全部成长任务 <el-icon><ArrowRight /></el-icon></button>
      </article>
    </section>

    <article class="advice-card">
      <el-icon class="advice-spark"><MagicStick /></el-icon>
      <div><p>AI 今日建议</p><strong>{{ advice }}</strong></div>
      <button @click="router.push('/career/chat')">和 AI 规划师聊聊 <el-icon><ArrowRight /></el-icon></button>
    </article>

    <section class="section-grid two-column dashboard-bottom">
      <article class="surface-card recent-panel">
        <div class="card-header"><div><h2>最近一次模拟面试</h2><p>持续练习，也持续复盘</p></div></div>
        <div v-if="latestInterview" class="recent-interview">
          <div class="score-ring"><strong>{{ latestInterview.totalScore ?? '—' }}</strong><span>综合评分</span></div>
          <div class="interview-summary">
            <span class="soft-label">{{ interviewTypeLabel[latestInterview.interviewType] || latestInterview.interviewType }}</span>
            <h3>{{ latestInterview.targetPosition }}</h3>
            <p>{{ formatDate(latestInterview.createTime) }} · {{ latestInterview.difficulty }}</p>
          </div>
          <el-button type="primary" plain @click="router.push('/interview/' + latestInterview.id + '/report')">查看报告</el-button>
        </div>
        <div v-else class="empty-panel compact">
          <div><el-icon class="empty-icon"><ChatDotRound /></el-icon><strong>还没有模拟面试记录</strong><span>准备好后，完成你的第一次模拟面试。</span></div>
        </div>
      </article>

      <article class="surface-card roadmap-panel">
        <div class="card-header">
          <div><h2>职业成长路线</h2><p>{{ plan ? '规划 V' + plan.version : '等待生成规划' }}</p></div>
          <button class="text-link" @click="router.push('/career/plan')">查看规划 <el-icon><ArrowRight /></el-icon></button>
        </div>
        <div v-if="roadmap.length" class="mini-roadmap">
          <div v-for="(stage, index) in roadmap.slice(0, 4)" :key="stage.stage || index" class="mini-stage">
            <span>{{ stage.stage || index + 1 }}</span>
            <div><strong :title="stage.name">{{ stage.name }}</strong><small>{{ stage.duration || '持续推进' }}</small></div>
          </div>
        </div>
        <div v-else class="empty-panel compact">
          <div><el-icon class="empty-icon"><TrendCharts /></el-icon><strong>职业路线等待生成</strong><span>完善画像后即可开启个性化规划。</span></div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.dashboard-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
  margin-bottom: 24px;
}

.dashboard-hero h1 { margin: 0; font-size: 28px; }
.dashboard-hero h1 .el-icon { margin-left: 5px; color: #f2a93b; font-size: 24px; vertical-align: -3px; }
.dashboard-hero > div > p:last-child { margin: 8px 0 0; color: var(--muted); }

.goal-card {
  min-width: 310px;
  padding: 18px 22px;
  border: 1px solid #e2e4fa;
  border-radius: 14px;
  background: linear-gradient(135deg, #f8f8ff, #eef0ff);
}

.goal-card span,
.goal-card strong { display: block; }
.goal-card span { color: var(--muted); font-size: 12px; }
.goal-card strong { margin: 7px 0; font-size: 17px; }

.goal-card button,
.text-link,
.panel-footer-link,
.advice-card button {
  border: 0;
  color: var(--primary);
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
}

.goal-card button,
.text-link,
.advice-card button {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 17px;
  margin-bottom: 20px;
}

.metric-card {
  display: grid;
  grid-template-columns: 45px 1fr;
  gap: 13px;
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 8px 25px rgba(44, 48, 90, 0.045);
}

.metric-icon {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 13px;
  font-size: 21px;
}

.metric-icon.purple { color: #655bd9; background: #f0efff; }
.metric-icon.green { color: #20966c; background: #eaf8f2; }
.metric-icon.orange { color: #dd8430; background: #fff3e7; }
.metric-icon.blue { color: #3a83d5; background: #eaf4ff; }
.metric-card p { margin: 0 0 5px; color: var(--muted); font-size: 12px; }
.metric-card strong { font-size: 26px; }
.metric-card small { margin-left: 3px; font-size: 13px; }
.metric-card em { grid-column: 1 / -1; color: #9a9eb0; font-size: 11px; font-style: normal; }

.radar-panel,
.tasks-panel,
.recent-panel,
.roadmap-panel { padding: 22px 24px; }

.dashboard-bottom > .surface-card,
.roadmap-panel,
.mini-roadmap,
.mini-stage,
.mini-stage > div {
  min-width: 0;
}

.roadmap-panel { overflow: hidden; }

.dashboard-tasks :deep(.task-card) { grid-template-columns: 22px 1fr; padding: 13px 0; }
.dashboard-tasks :deep(.task-card > .el-button),
.dashboard-tasks :deep(.task-title-row .el-tag) { display: none; }

.panel-footer-link {
  width: calc(100% + 48px);
  margin: 4px -24px -22px;
  padding: 15px 24px;
  border-top: 1px solid var(--line);
  text-align: left;
}

.panel-footer-link .el-icon { float: right; }

.advice-card {
  display: grid;
  grid-template-columns: 46px 1fr auto;
  align-items: center;
  gap: 15px;
  margin: 20px 0;
  padding: 19px 22px;
  border: 1px solid #dadcf8;
  border-radius: 15px;
  color: #343762;
  background: linear-gradient(90deg, #f1f1ff, #fafaff 60%, #f3f6ff);
}

.advice-spark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 14px;
  color: #fff;
  background: linear-gradient(135deg, #5c60dc, #8b75e9);
  box-shadow: 0 8px 18px rgba(89, 88, 207, 0.2);
  font-size: 21px;
}

.advice-card p { margin: 0 0 4px; color: var(--primary); font-size: 12px; font-weight: 800; }
.advice-card strong { font-size: 13px; font-weight: 500; line-height: 1.65; }
.advice-card button { padding: 9px 13px; border: 1px solid #cfd2f6; border-radius: 9px; background: #fff; }

.recent-interview {
  display: grid;
  min-height: 145px;
  grid-template-columns: 88px 1fr auto;
  align-items: center;
  gap: 18px;
}

.score-ring {
  display: grid;
  width: 84px;
  height: 84px;
  place-content: center;
  border: 7px solid #e8e9ff;
  border-top-color: var(--primary);
  border-radius: 50%;
  text-align: center;
}

.score-ring strong,
.score-ring span { display: block; }
.score-ring strong { font-size: 24px; }
.score-ring span { color: var(--muted); font-size: 10px; }
.interview-summary h3 { margin: 10px 0 6px; font-size: 16px; }
.interview-summary p { margin: 0; color: var(--muted); font-size: 12px; }

.mini-roadmap {
  display: grid;
  width: 100%;
  min-height: 145px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  align-items: center;
  gap: 8px;
}

.mini-stage { position: relative; text-align: center; }
.mini-stage:not(:last-child)::after {
  position: absolute;
  width: 35%;
  height: 1px;
  top: 16px;
  right: -20%;
  background: #d9dbec;
  content: '';
}

.mini-stage > span {
  display: grid;
  width: 32px;
  height: 32px;
  margin: 0 auto 9px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: var(--primary);
  font-size: 12px;
}

.mini-stage strong,
.mini-stage small { display: block; }
.mini-stage strong {
  display: -webkit-box;
  min-height: 2.8em;
  overflow: hidden;
  font-size: 12px;
  line-height: 1.4;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.mini-stage small {
  overflow: hidden;
  margin-top: 5px;
  color: var(--muted);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.compact { min-height: 145px; padding: 12px; font-size: 12px; }
.compact .empty-icon { width: 44px; height: 44px; }

@media (max-width: 1160px) {
  .metrics-grid { grid-template-columns: repeat(2, 1fr); }
  .two-column { grid-template-columns: 1fr; }
}

@media (max-width: 720px) {
  .dashboard-hero { align-items: stretch; flex-direction: column; }
  .goal-card { min-width: 0; }
  .metrics-grid { grid-template-columns: 1fr; }
  .advice-card { grid-template-columns: 42px 1fr; }
  .advice-card button { grid-column: 2; justify-self: start; }
  .recent-interview { grid-template-columns: 80px 1fr; }
  .recent-interview > .el-button { grid-column: 2; justify-self: start; }

  .mini-roadmap {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px 12px;
  }

  .mini-stage:nth-child(2)::after { display: none; }
}
</style>
