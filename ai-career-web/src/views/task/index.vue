<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { CircleCheck } from '@element-plus/icons-vue'
import { getTasks, getTaskStatistics, updateTaskStatus } from '../../api/task'
import TaskCard from '../../components/TaskCard.vue'
import type { CareerTask, TaskStatistics } from '../../types/api'

const loading = ref(true)
const updatingTaskId = ref<number>()
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
  border: 1px solid var(--line);
  border-radius: 13px;
  background: #fff;
}
.weekly-progress > div { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.weekly-progress span { color: var(--muted); font-size: 12px; }
.weekly-progress strong { color: var(--primary); font-size: 16px; }
.task-summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 20px; }
.task-summary article { display: flex; align-items: center; justify-content: space-between; padding: 17px 20px; border: 1px solid var(--line); border-radius: 13px; background: #fff; }
.task-summary span { color: var(--muted); font-size: 12px; }
.task-summary strong { font-size: 21px; }
.task-workspace { overflow: hidden; }
.task-toolbar { display: flex; align-items: center; justify-content: space-between; padding: 15px 24px; border-bottom: 1px solid var(--line); }
.task-toolbar > span { color: var(--muted); font-size: 12px; }
.filter-tabs { display: flex; gap: 5px; }
.filter-tabs button { padding: 8px 15px; border: 0; border-radius: 9px; color: #73778b; background: transparent; cursor: pointer; font-size: 12px; }
.filter-tabs button.active { color: var(--primary-dark); background: var(--primary-soft); font-weight: 700; }
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
