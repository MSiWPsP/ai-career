<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { CircleCheck } from '@element-plus/icons-vue'
import { getTasks, getTaskStatistics, updateTaskStatus } from '../../api/task'
import TaskCard from '../../components/TaskCard.vue'
import type { CareerTask, TaskStatistics } from '../../types/api'

const loading = ref(true)
const updatingTaskId = ref<string>()
const activeStatus = ref<number | undefined>()
const tasks = ref<CareerTask[]>([])
const statistics = ref<TaskStatistics>({ total: 0, completed: 0, processing: 0, waiting: 0, completionRate: 0 })

const filters = [
  { label: '全部', value: undefined },
  { label: '待开始', value: 0 },
  { label: '进行中', value: 1 },
  { label: '已完成', value: 2 },
  { label: '已跳过', value: 3 },
]

const groupedTasks = computed(() => {
  const groups = new Map<string, CareerTask[]>()
  tasks.value.forEach((task) => {
    const stage = task.stageName || '其他任务'
    groups.set(stage, [...(groups.get(stage) || []), task])
  })
  return Array.from(groups.entries())
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [taskData, statsData] = await Promise.all([
      getTasks(activeStatus.value === undefined ? undefined : { status: activeStatus.value }),
      getTaskStatistics(),
    ])
    tasks.value = taskData
    statistics.value = statsData
  } finally {
    loading.value = false
  }
}

async function selectStatus(status?: number) {
  activeStatus.value = status
  await load()
}

async function changeTaskStatus(task: CareerTask, status: number) {
  updatingTaskId.value = task.id
  try {
    await updateTaskStatus(task.id, status)
    await load()
  } catch {
    // 请求层已经展示错误信息；组件内消费异常，避免 Vue 报告未处理的事件错误。
  } finally {
    updatingTaskId.value = undefined
  }
}
</script>

<template>
  <div>
    <header class="page-heading">
      <div>
        <p class="eyebrow">GROWTH PLAN</p>
        <h1>我的成长计划</h1>
        <p>把规划变成每天都能完成的小行动。</p>
      </div>
      <div class="weekly-progress">
        <div><span>当前完成度</span><strong>{{ statistics.completionRate }}%</strong></div>
        <el-progress :percentage="statistics.completionRate" :show-text="false" />
      </div>
    </header>

    <section class="task-summary">
      <article><span>全部任务</span><strong>{{ statistics.total }}</strong></article>
      <article><span>待开始</span><strong>{{ statistics.waiting }}</strong></article>
      <article><span>进行中</span><strong>{{ statistics.processing }}</strong></article>
      <article><span>已完成</span><strong>{{ statistics.completed }}</strong></article>
    </section>

    <section class="surface-card task-workspace">
      <div class="task-toolbar">
        <div class="filter-tabs">
          <button
            v-for="filter in filters"
            :key="String(filter.value)"
            :class="{ active: activeStatus === filter.value }"
            @click="selectStatus(filter.value)"
          >
            {{ filter.label }}
          </button>
        </div>
        <span>{{ tasks.length }} 项任务</span>
      </div>

      <div v-loading="loading" class="task-content">
        <template v-if="groupedTasks.length">
          <section v-for="[stage, items] in groupedTasks" :key="stage" class="task-stage">
            <div class="stage-heading">
              <span>{{ stage }}</span>
              <i />
              <small>{{ items.length }} 项</small>
            </div>
            <div class="stage-list">
              <TaskCard
                v-for="task in items"
                :key="task.id"
                :task="task"
                :updating="updatingTaskId === task.id"
                @change-status="changeTaskStatus"
              />
            </div>
          </section>
        </template>
        <div v-else class="empty-panel">
          <div><el-icon class="empty-icon"><CircleCheck /></el-icon><strong>当前筛选下没有任务</strong><span>职业规划生成后，会自动拆解为可以执行的成长任务。</span></div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.weekly-progress {
  width: 280px;
  padding: 14px 18px;
  border: 1px solid rgb(255 255 255 / 22%);
  border-radius: 13px;
  background: rgb(255 255 255 / 12%);
  backdrop-filter: blur(8px);
}
.weekly-progress > div { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.weekly-progress span { color: #dbeafe; font-size: 12px; }
.weekly-progress strong { color: #fff; font-size: 16px; }
.weekly-progress :deep(.el-progress-bar__outer) { background: rgb(255 255 255 / 22%); }
.weekly-progress :deep(.el-progress-bar__inner) { background: linear-gradient(90deg, #fff, #67e8f9); }
.task-summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 20px; }
.task-summary article { position: relative; display: flex; min-height: 78px; align-items: center; justify-content: space-between; overflow: hidden; padding: 17px 20px; border: 1px solid #c6d9f3; border-radius: 13px; background: linear-gradient(145deg, #fff, #f1f7ff); box-shadow: 0 9px 24px rgb(29 78 216 / 7%); transition: box-shadow var(--motion-normal) ease, transform var(--motion-normal) var(--ease-standard); }
.task-summary article:first-child { border-color: #2563eb; color: #fff; background: linear-gradient(145deg, #123b82, #2563eb); }
.task-summary article:first-child span { color: #dbeafe; }
.task-summary article::after { position: absolute; right: -18px; bottom: -34px; width: 78px; height: 78px; border: 12px solid rgb(59 130 246 / 9%); border-radius: 50%; content: ''; }
.task-summary article:hover { box-shadow: 0 16px 32px rgb(29 78 216 / 13%); transform: translateY(-4px); }
.task-summary span { color: var(--muted); font-size: 12px; }
.task-summary strong { font-size: 21px; }
.task-workspace { position: relative; overflow: hidden; box-shadow: 0 16px 38px rgb(23 49 92 / 9%); }
.task-workspace::before { position: absolute; z-index: 1; inset: 0 0 auto; height: 4px; background: linear-gradient(90deg, #1d4ed8, #38bdf8 62%, transparent); content: ''; }
.task-toolbar { display: flex; align-items: center; justify-content: space-between; padding: 18px 24px; border-bottom: 1px solid #d5e3f4; background: linear-gradient(90deg, #eff6ff, #fff); }
.task-toolbar > span { color: var(--muted); font-size: 12px; }
.filter-tabs { display: flex; gap: 5px; }
.filter-tabs button { padding: 8px 15px; border: 0; border-radius: 9px; color: #73778b; background: transparent; cursor: pointer; font-size: 12px; }
.filter-tabs button { transition: color var(--motion-fast) ease, background var(--motion-fast) ease, transform var(--motion-fast) ease; }
.filter-tabs button:hover { color: var(--primary); transform: translateY(-1px); }
.filter-tabs button.active { color: #fff; background: linear-gradient(135deg, #1d4ed8, #3b82f6); box-shadow: 0 6px 15px rgb(29 78 216 / 18%); font-weight: 700; }
.task-content { min-height: 360px; padding: 24px; }
.task-stage { display: grid; grid-template-columns: 160px 1fr; gap: 24px; }
.task-stage + .task-stage { margin-top: 18px; padding-top: 18px; border-top: 1px solid var(--line); }
.stage-heading { display: flex; align-items: center; align-self: start; padding-top: 18px; }
.stage-heading span { font-size: 13px; font-weight: 700; }
.stage-heading i { width: 18px; height: 1px; margin: 0 8px; background: #d3d5e2; }
.stage-heading small { color: var(--muted); }
@media (max-width: 800px) {
  .page-heading { flex-direction: column; }
  .weekly-progress { width: 100%; }
  .task-summary { grid-template-columns: repeat(2, 1fr); }
  .task-stage { grid-template-columns: 1fr; gap: 0; }
  .filter-tabs { overflow-x: auto; }
  .task-toolbar { align-items: flex-start; gap: 12px; flex-direction: column; }
}
</style>
