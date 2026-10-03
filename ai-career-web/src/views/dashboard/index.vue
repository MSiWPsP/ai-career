<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ArrowRight,
  Calendar,
  ChatDotRound,
  CircleCheckFilled,
  Coin,
  Compass,
  Document,
  List,
  Promotion,
  TrendCharts,
} from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { getAbilityRadar } from '../../api/ability'
import { getCurrentPlan } from '../../api/career'
import { getInterviewHistory } from '../../api/interview'
import { getProfile } from '../../api/profile'
import { getTaskStatistics, getTasks, updateTaskStatus } from '../../api/task'
import { getCurrentUser } from '../../api/user'
import type {
  AbilityRadar,
  CareerPlan,
  CareerTask,
  InterviewHistoryRecord,
  RoadmapStage,
  TaskStatistics,
  UserInfo,
  UserProfile,
} from '../../types/api'
import {
  formatDate,
  interviewTypeLabel,
  parseJsonField,
} from '../../utils/data'
const router = useRouter()
const loading = ref(true)
const updatingTaskId = ref<string>()
const failures = ref<string[]>([])
const user = ref<UserInfo>()
const profile = ref<UserProfile>()
const plan = ref<CareerPlan>()
const tasks = ref<CareerTask[]>([])
const statistics = ref<TaskStatistics>({
  total: 0,
  completed: 0,
  processing: 0,
  waiting: 0,
  completionRate: 0,
})
const radar = ref<AbilityRadar>({ indicators: [], values: [] })
const latestInterview = ref<InterviewHistoryRecord>()
const greeting = computed(() => {
  const hour = new Date().getHours()
  return hour < 6
    ? '夜深了'
    : hour < 11
      ? '早上好'
      : hour < 14
        ? '中午好'
        : hour < 18
          ? '下午好'
          : '晚上好'
})
const roadmap = computed(() =>
  parseJsonField<RoadmapStage[]>(plan.value?.roadmap, []),
)
const nextTask = computed(
  () =>
    tasks.value.find((task) => task.status === 1) ??
    tasks.value.find((task) => task.status === 0),
)
const visibleTasks = computed(() =>
  [...tasks.value]
    .sort((a, b) => (a.status === 2 ? 1 : 0) - (b.status === 2 ? 1 : 0))
    .slice(0, 3),
)
const abilityPriorities = computed(() =>
  radar.value.indicators
    .map((item, index) => ({
      name: item.name,
      score: radar.value.values[index] ?? 0,
      max: item.max || 100,
    }))
    .sort((a, b) => a.score / a.max - b.score / b.max)
    .slice(0, 3),
)
// 用本地日历日比较截止日期，避免凌晨因 UTC 日期产生提醒偏移。
const overdue = computed(() => {
  const now = new Date()
  const today = [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0'),
  ].join('-')
  return tasks.value.filter(
    (task) =>
      task.status < 2 && task.deadline && task.deadline.slice(0, 10) < today,
  ).length
})
const activeStep = computed(() =>
  !profile.value?.targetPosition
    ? 0
    : !plan.value
      ? 1
      : nextTask.value
        ? 2
        : latestInterview.value
          ? 3
          : 2,
)
const steps = [
  { name: '定位', path: '/profile' },
  { name: '规划', path: '/career/plan' },
  { name: '执行', path: '/tasks' },
  { name: '反馈', path: '/ability' },
]
const fallbackAction = computed(() => {
  if (!profile.value?.targetPosition)
    return {
      title: '明确你的职业目标',
      description: '补充目标岗位和学习时间，让建议更贴合你的情况。',
      label: '完善画像',
      path: '/profile',
    }
  if (!plan.value)
    return {
      title: '生成你的专属成长路线',
      description: '结合职业画像和技能记录，规划接下来的学习方向。',
      label: '生成规划',
      path: '/career/plan',
    }
  if (!tasks.value.length)
    return {
      title: '把职业规划变成行动',
      description: '前往职业规划页生成成长任务，从一个小目标开始。',
      label: '查看规划',
      path: '/career/plan',
    }
  return {
    title: '用一次模拟面试检验成长',
    description: '当前任务已完成，试试模拟面试，发现下一阶段的提升方向。',
    label: '开始面试',
    path: '/interview/setup',
  }
})
const checklist = computed(() => [
  { name: '职业画像', done: !!profile.value?.targetPosition, path: '/profile' },
  { name: '成长路线', done: !!plan.value, path: '/career/plan' },
  { name: '学习实践', done: statistics.value.completed > 0, path: '/tasks' },
  {
    name: '模拟面试',
    done: latestInterview.value?.status === 2,
    path: '/interviews',
  },
])
const completedChecks = computed(
  () => checklist.value.filter((item) => item.done).length,
)
onMounted(loadDashboard)
async function loadDashboard() {
  loading.value = true
  failures.value = []
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
  const names = [
    '用户信息',
    '职业画像',
    '职业规划',
    '学习任务',
    '任务统计',
    '能力数据',
    '面试记录',
  ]
  results.forEach((result, index) => {
    if (result.status === 'rejected') failures.value.push(names[index]!)
  })
  if (results[0].status === 'fulfilled') user.value = results[0].value
  if (results[1].status === 'fulfilled') profile.value = results[1].value
  if (results[2].status === 'fulfilled') plan.value = results[2].value
  if (results[3].status === 'fulfilled') tasks.value = results[3].value ?? []
  if (results[4].status === 'fulfilled' && results[4].value)
    statistics.value = results[4].value
  if (results[5].status === 'fulfilled' && results[5].value)
    radar.value = results[5].value
  if (results[6].status === 'fulfilled')
    latestInterview.value = results[6].value?.records[0]
  loading.value = false
}
async function changeTaskStatus(task: CareerTask, status: number) {
  if (updatingTaskId.value) return
  updatingTaskId.value = task.id
  try {
    const updated = await updateTaskStatus(task.id, status)
    const index = tasks.value.findIndex((item) => item.id === task.id)
    if (index >= 0) tasks.value[index] = updated
    statistics.value = await getTaskStatistics()
  } catch {
    // 请求层显示错误；保留成功保存的状态，不伪造完成统计。
  } finally {
    updatingTaskId.value = undefined
  }
}
function openLatestInterview() {
  if (!latestInterview.value) return
  void router.push(
    latestInterview.value.status === 2
      ? '/interview/' + latestInterview.value.id + '/report'
      : '/interview/session/' + latestInterview.value.id,
  )
}
</script>
<template>
  <div v-loading="loading" class="growth-home">
    <header class="growth-heading">
      <div>
        <h1>{{ greeting }}，{{ user?.nickname || '同学' }}</h1>
        <p>今天完成一小步，让成长看得见。</p>
      </div>
      <span class="stage-pill"
        ><el-icon><TrendCharts /></el-icon>职业成长进行中</span
      >
    </header>
    <el-alert v-if="failures.length" type="warning" :closable="false" show-icon
      ><template #title
        >{{ failures.join('、') }}暂时加载失败，相关区域可能不完整。</template
      ><el-button text type="primary" @click="loadDashboard"
        >重新加载</el-button
      ></el-alert
    >
    <section class="goal-strip">
      <div class="goal-label">
        <strong>目标岗位：</strong
        ><span>{{
          profile?.targetPosition || plan?.targetPosition || '尚未设置'
        }}</span
        ><button class="text-link" @click="router.push('/profile')">
          调整目标 <el-icon><ArrowRight /></el-icon>
        </button>
      </div>
      <nav class="growth-steps" aria-label="职业成长流程">
        <button
          v-for="(step, index) in steps"
          :key="step.name"
          :class="{ current: index === activeStep }"
          :aria-current="index === activeStep ? 'step' : undefined"
          @click="router.push(step.path)"
        >
          <b>{{ index + 1 }}</b
          >{{ step.name }}
        </button>
      </nav>
    </section>
    <section class="action-grid">
      <article class="next-action">
        <el-icon class="hero-art" aria-hidden="true"><Coin /></el-icon
        ><span class="action-caption"
          ><el-icon><Promotion /></el-icon>下一步行动</span
        >
        <h2>{{ nextTask?.taskName || fallbackAction.title }}</h2>
        <p>
          {{
            nextTask?.taskDescription ||
            (nextTask
              ? '沿着当前成长路线，专注完成这项学习任务。'
              : fallbackAction.description)
          }}
        </p>
        <div class="action-meta">
          <el-icon><Calendar /></el-icon
          >{{
            nextTask
              ? nextTask.deadline
                ? '截止 ' + formatDate(nextTask.deadline)
                : '按你的学习节奏推进'
              : '从一个小目标开始'
          }}<span v-if="nextTask">{{
            nextTask.status === 1 ? '进行中' : '待开始'
          }}</span>
        </div>
        <div class="hero-buttons">
          <el-button
            v-if="nextTask"
            :loading="updatingTaskId === nextTask.id"
            :disabled="!!updatingTaskId && updatingTaskId !== nextTask.id"
            @click="changeTaskStatus(nextTask, nextTask.status === 1 ? 2 : 1)"
            >{{ nextTask.status === 1 ? '标记完成' : '开始任务' }}</el-button
          ><el-button v-else @click="router.push(fallbackAction.path)">{{
            fallbackAction.label
          }}</el-button
          ><el-button class="hero-secondary" @click="router.push('/tasks')"
            >查看学习任务</el-button
          >
        </div>
        <span class="hero-process">明确目标 → 学习实践 → 面试复盘</span>
      </article>
      <article class="home-card todo-card">
        <div class="home-card-heading">
          <h2>
            <el-icon><List /></el-icon>成长待办
          </h2>
          <button
            class="text-link"
            aria-label="查看成长待办"
            @click="router.push('/tasks')"
          >
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
        <button class="todo-row" @click="router.push('/tasks')">
          <i class="dot danger" /><span
            ><b>{{ overdue }}</b> 项任务已超过截止日期</span
          ></button
        ><button class="todo-row" @click="router.push('/tasks')">
          <i class="dot warning" /><span
            ><b>{{ statistics.waiting }}</b> 项学习任务等待开始</span
          ></button
        ><button class="todo-row" @click="router.push('/tasks')">
          <i class="dot primary" /><span
            ><b>{{ statistics.processing }}</b> 项学习任务正在进行</span
          >
        </button>
      </article>
    </section>
    <section class="home-grid">
      <article class="home-card">
        <div class="home-card-heading">
          <div>
            <h2>
              <el-icon><Calendar /></el-icon>当前行动计划
            </h2>
            <p>已完成 {{ statistics.completed }} / {{ statistics.total }} 项</p>
          </div>
          <div class="completion">
            <el-progress
              :percentage="statistics.completionRate"
              :stroke-width="8"
              :show-text="false"
            /><span>{{ statistics.completionRate }}%</span>
          </div>
        </div>
        <div v-if="visibleTasks.length" class="compact-tasks">
          <div v-for="task in visibleTasks" :key="task.id" class="compact-task">
            <el-icon v-if="task.status === 2" class="task-check"
              ><CircleCheckFilled /></el-icon
            ><i
              v-else
              class="task-circle"
              :class="{ running: task.status === 1 }"
            /><strong :title="task.taskName">{{ task.taskName }}</strong
            ><small>{{
              task.deadline ? formatDate(task.deadline) : '未设截止日'
            }}</small
            ><el-button
              size="small"
              :type="task.status === 0 ? 'primary' : undefined"
              :disabled="
                task.status === 2 ||
                (!!updatingTaskId && updatingTaskId !== task.id)
              "
              :loading="updatingTaskId === task.id"
              @click="
                task.status === 0
                  ? changeTaskStatus(task, 1)
                  : router.push('/tasks')
              "
              >{{
                task.status === 2
                  ? '已完成'
                  : task.status === 1
                    ? '继续'
                    : '开始'
              }}</el-button
            >
          </div>
        </div>
        <p v-else class="home-empty">
          {{
            failures.includes('学习任务')
              ? '任务加载失败，请重试。'
              : '还没有学习任务，先从职业规划生成你的行动计划。'
          }}
        </p>
        <footer>
          <button class="text-link" @click="router.push('/tasks')">
            查看完整学习计划 <el-icon><ArrowRight /></el-icon>
          </button>
        </footer>
      </article>
      <article class="home-card">
        <div class="home-card-heading">
          <h2>
            <el-icon><TrendCharts /></el-icon>能力提升重点
          </h2>
          <span class="card-note">基于技能与面试记录</span>
        </div>
        <div v-if="abilityPriorities.length" class="ability-list">
          <div
            v-for="item in abilityPriorities"
            :key="item.name"
            class="ability-row"
          >
            <strong>{{ item.name }}</strong
            ><el-progress
              :percentage="
                Math.max(
                  0,
                  Math.min(100, Math.round((item.score / item.max) * 100)),
                )
              "
              :show-text="false"
              :stroke-width="10"
            /><span>当前 {{ item.score }} / {{ item.max }}</span>
          </div>
        </div>
        <p v-else class="home-empty">
          {{
            failures.includes('能力数据')
              ? '能力数据加载失败，请重试。'
              : '补充技能或完成模拟面试，逐步点亮你的能力画像。'
          }}
        </p>
        <footer>
          <button class="text-link" @click="router.push('/ability')">
            查看能力画像与趋势 <el-icon><ArrowRight /></el-icon>
          </button>
        </footer>
      </article>
      <article class="home-card">
        <div class="home-card-heading">
          <h2>
            <el-icon><ChatDotRound /></el-icon>面试反馈与复盘
          </h2>
          <span v-if="latestInterview" class="soft-label">{{
            latestInterview.totalScore == null
              ? '尚未评分'
              : latestInterview.totalScore + ' 分'
          }}</span>
        </div>
        <template v-if="latestInterview"
          ><span class="card-note"
            >最近一次{{
              interviewTypeLabel[latestInterview.interviewType] || '模拟面试'
            }}
            · {{ formatDate(latestInterview.createTime) }}</span
          >
          <h3>{{ latestInterview.targetPosition }}</h3>
          <p class="body-note">
            回顾本次回答与面试反馈，把薄弱点转化为下一步学习方向。
          </p></template
        >
        <p v-else class="home-empty">
          {{
            failures.includes('面试记录')
              ? '面试记录加载失败，请重试。'
              : '还没有面试记录。试一次模拟面试，让练习有反馈。'
          }}
        </p>
        <footer>
          <el-button
            v-if="latestInterview"
            plain
            type="primary"
            @click="openLatestInterview"
            >{{
              latestInterview.status === 2 ? '查看面试报告' : '继续面试'
            }}</el-button
          ><el-button type="primary" @click="router.push('/interview/setup')"
            >开始模拟面试</el-button
          >
        </footer>
      </article>
      <article class="home-card">
        <div class="home-card-heading">
          <h2>
            <el-icon><Document /></el-icon>职业成长路线
          </h2>
          <span class="card-note">{{
            plan ? '规划 V' + plan.version : '尚未生成'
          }}</span>
        </div>
        <div v-if="roadmap.length" class="roadmap-list">
          <div v-for="(stage, index) in roadmap.slice(0, 3)" :key="index">
            <b>{{ index + 1 }}</b
            ><strong>{{ stage.name }}</strong
            ><span>{{ stage.duration || '持续推进' }}</span>
          </div>
        </div>
        <p v-else class="home-empty">
          {{
            failures.includes('职业规划')
              ? '职业规划加载失败，请重试。'
              : '结合你的目标与技能，生成可执行的阶段成长路线。'
          }}
        </p>
        <footer>
          <button class="text-link" @click="router.push('/career/plan')">
            {{ plan ? '查看完整职业规划' : '生成职业规划' }}
            <el-icon><ArrowRight /></el-icon>
          </button>
        </footer>
      </article>
      <article class="home-card">
        <div class="home-card-heading">
          <h2>
            <el-icon><Compass /></el-icon>职业方向探索
          </h2>
        </div>
        <p class="body-note">不确定下一步？和 AI 职业规划师聊聊你的目标。</p>
        <div class="explore-actions">
          <button @click="router.push('/career/chat')">
            <el-icon><ChatDotRound /></el-icon><span>咨询职业方向</span
            ><el-icon><ArrowRight /></el-icon></button
          ><button @click="router.push('/profile')">
            <el-icon><Compass /></el-icon><span>完善职业画像</span
            ><el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </article>
      <article class="home-card">
        <div class="home-card-heading">
          <h2>
            <el-icon><List /></el-icon>成长准备清单
          </h2>
          <span class="card-note">已点亮 {{ completedChecks }} / 4 项</span>
        </div>
        <div class="readiness-list">
          <button
            v-for="item in checklist"
            :key="item.name"
            @click="router.push(item.path)"
          >
            <el-icon :class="{ ready: item.done }"
              ><CircleCheckFilled v-if="item.done" /><Compass v-else /></el-icon
            ><strong>{{ item.name }}</strong
            ><small :class="{ ready: item.done }">{{
              item.done ? '已有记录' : '待完善'
            }}</small>
          </button>
        </div>
      </article>
    </section>
  </div>
</template>
<style scoped>
.growth-home {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
}
.growth-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 4px 6px;
}
.growth-heading h1 {
  margin: 0 0 7px;
  font-size: 30px;
  letter-spacing: -0.8px;
}
.growth-heading p {
  margin: 0;
  color: var(--muted);
  font-size: 16px;
}
.stage-pill {
  display: inline-flex;
  gap: 7px;
  align-items: center;
  padding: 10px 15px;
  border-radius: 24px;
  color: var(--primary);
  background: #e9ecff;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}
.goal-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 13px 18px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: white;
}
.goal-label {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 14px;
}
.goal-label .text-link {
  margin-left: 12px;
}
.growth-steps {
  display: flex;
  gap: 16px;
}
.growth-steps button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  border: 0;
  color: var(--muted);
  background: transparent;
  cursor: pointer;
  white-space: nowrap;
  font-size: 12px;
}
.growth-steps button:not(:last-child)::after {
  width: 15px;
  height: 1px;
  margin-left: 6px;
  background: #c8cce3;
  content: '';
}
.growth-steps b {
  display: grid;
  place-items: center;
  width: 27px;
  height: 27px;
  border-radius: 50%;
  background: #eef0f7;
  color: #69718f;
}
.growth-steps .current {
  color: var(--primary);
  font-weight: 700;
}
.growth-steps .current b {
  background: var(--primary);
  color: white;
  box-shadow: 0 2px 5px #4c5cf133;
}
.action-grid {
  display: grid;
  grid-template-columns: 1.7fr 1fr;
  gap: 16px;
}
.next-action {
  position: relative;
  overflow: hidden;
  padding: 24px 30px;
  border-radius: 14px;
  background: linear-gradient(115deg, #4e5de9, #8094f5);
  color: white;
  min-height: 236px;
  box-shadow: 0 5px 16px #5465dd14;
}
.next-action::after {
  position: absolute;
  width: 360px;
  height: 360px;
  border-radius: 50%;
  background: #ffffff09;
  top: -120px;
  right: -95px;
  content: '';
  pointer-events: none;
}
.action-caption {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 15px;
  opacity: 0.92;
}
.next-action h2 {
  position: relative;
  max-width: 78%;
  margin: 16px 0 8px;
  font-size: 25px;
  line-height: 1.3;
}
.next-action p {
  position: relative;
  max-width: 76%;
  margin: 0 0 9px;
  font-size: 13px;
  color: #eef0ff;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.action-meta {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: #ecedff;
}
.action-meta span {
  padding-left: 8px;
  border-left: 1px solid #ffffff55;
  margin-left: 5px;
}
.hero-buttons {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}
.hero-buttons .el-button {
  height: 38px;
  min-width: 132px;
  margin: 0;
  color: var(--primary);
  border: 1px solid #eff0ff;
  background: white;
  font-weight: 600;
}
.hero-buttons .hero-secondary {
  background: #ffffff08;
  color: white;
  border-color: #c6ceff;
}
.hero-art {
  position: absolute;
  right: 36px;
  top: 28px;
  font-size: 94px;
  color: #c5d0ff;
  opacity: 0.6;
  transform: rotate(-8deg);
}
.hero-process {
  position: absolute;
  bottom: 24px;
  right: 24px;
  padding: 12px 16px;
  border-radius: 11px;
  background: #e3e8ffb3;
  color: #324386;
  font-size: 11px;
}
.home-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 19px 22px 15px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 2px 8px #555d9b03;
}
.home-card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 15px;
}
.home-card h2 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 18px;
  white-space: nowrap;
}
.home-card h2 > .el-icon {
  color: var(--primary);
  font-size: 24px;
}
.home-card-heading p {
  margin: 6px 0 0 34px;
  font-size: 12px;
  color: var(--muted);
}
.home-card h3 {
  margin: 9px 0;
  font-size: 19px;
}
.home-card footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 9px;
  margin-top: auto;
  padding-top: 12px;
}
.home-card footer .el-button + .el-button {
  margin: 0;
}
.card-note,
.body-note {
  color: var(--muted);
  font-size: 12px;
}
.body-note {
  line-height: 1.7;
  margin: 0 0 12px;
}
.home-empty {
  margin: 10px 0 16px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.8;
}
.todo-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  border: 0;
  background: transparent;
  padding: 12px 0;
  color: var(--text);
  text-align: left;
  cursor: pointer;
  font-size: 14px;
}
.todo-row:hover {
  color: var(--primary);
}
.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex: none;
}
.dot.danger {
  background: #df5657;
}
.dot.warning {
  background: #e7ad4b;
}
.dot.primary {
  background: #6678f7;
}
.home-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.completion {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 12px;
  color: var(--muted);
  width: 35%;
}
.completion .el-progress {
  flex: 1;
}
.compact-task {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) auto 64px;
  align-items: center;
  gap: 10px;
  min-height: 38px;
  border-bottom: 1px solid #edf0f8;
  font-size: 12px;
}
.compact-task strong {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-weight: 600;
}
.compact-task small {
  color: var(--muted);
  font-size: 11px;
}
.task-check {
  color: var(--success);
  font-size: 19px;
}
.task-circle {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid #b6bcd1;
}
.task-circle.running {
  border-color: var(--primary);
  background: var(--primary-soft);
}
.ability-list {
  display: grid;
  gap: 20px;
  padding: 8px 0;
}
.ability-row {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr) 102px;
  gap: 14px;
  align-items: center;
  font-size: 12px;
}
.ability-row strong {
  font-weight: 600;
  overflow-wrap: anywhere;
}
.ability-row > span {
  color: var(--muted);
  font-size: 11px;
  text-align: right;
}
.roadmap-list > div {
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #edf0f8;
  padding: 8px 0;
  font-size: 12px;
}
.roadmap-list b {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 7px;
  color: var(--primary);
  background: var(--primary-soft);
  flex: none;
}
.roadmap-list strong {
  flex: 1;
  font-weight: 600;
}
.roadmap-list span {
  color: var(--muted);
  font-size: 11px;
}
.explore-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.explore-actions button {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #e0e5ff;
  border-radius: 9px;
  color: var(--text);
  background: white;
  cursor: pointer;
  font-size: 12px;
}
.explore-actions button:hover {
  background: var(--primary-soft);
}
.explore-actions .el-icon {
  color: var(--primary);
  font-size: 19px;
}
.explore-actions span {
  flex: 1;
}
.readiness-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: auto;
}
.readiness-list button {
  display: grid;
  grid-template-columns: 20px 1fr;
  align-items: center;
  gap: 5px;
  min-width: 0;
  padding: 12px 8px;
  border: 1px solid var(--line);
  border-radius: 10px;
  color: var(--text);
  background: white;
  text-align: left;
  cursor: pointer;
}
.readiness-list button:hover {
  border-color: var(--primary);
}
.readiness-list .el-icon {
  grid-row: span 2;
  font-size: 20px;
  color: var(--primary);
}
.readiness-list strong {
  font-size: 11px;
  white-space: nowrap;
}
.readiness-list small {
  color: var(--muted);
  font-size: 10px;
}
.readiness-list .ready {
  color: var(--success);
}
@media (min-width: 1600px) {
  .home-card {
    padding: 23px 26px 18px;
  }
  .home-card h2 {
    font-size: 20px;
  }
  .next-action {
    min-height: 248px;
  }
  .compact-task {
    min-height: 44px;
    font-size: 14px;
  }
  .ability-row {
    font-size: 14px;
  }
}
@media (max-width: 1200px) {
  .hero-process {
    display: none;
  }
  .growth-steps {
    gap: 10px;
  }
  .growth-steps button:not(:last-child)::after {
    display: none;
  }
  .readiness-list {
    grid-template-columns: repeat(2, 1fr);
  }
  .home-card-heading {
    flex-wrap: wrap;
  }
}
@media (max-width: 760px) {
  .action-grid,
  .home-grid {
    grid-template-columns: 1fr;
  }
  .goal-strip {
    flex-direction: column;
    align-items: flex-start;
  }
  .growth-steps {
    width: 100%;
    justify-content: space-between;
  }
  .stage-pill {
    display: none;
  }
  .growth-heading h1 {
    font-size: 25px;
  }
  .next-action {
    padding: 22px;
  }
  .home-card {
    padding: 18px;
  }
}
@media (max-width: 420px) {
  .growth-heading p {
    font-size: 13px;
  }
  .goal-label {
    font-size: 12px;
  }
  .goal-label .text-link {
    margin-left: 0;
  }
  .hero-art {
    font-size: 62px;
    right: 15px;
  }
  .next-action h2 {
    font-size: 22px;
    max-width: 85%;
  }
  .hero-buttons .el-button {
    min-width: 0;
    flex: 1;
  }
  .ability-row {
    grid-template-columns: 70px minmax(0, 1fr) 85px;
    gap: 8px;
  }
  .compact-task {
    grid-template-columns: 16px minmax(0, 1fr) 54px;
  }
  .compact-task small {
    display: none;
  }
  .explore-actions {
    grid-template-columns: 1fr;
  }
}
</style>
