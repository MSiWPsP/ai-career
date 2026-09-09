<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentPlan, getPlanById, getPlanHistory } from '../../api/career'
import RoadmapTimeline from '../../components/RoadmapTimeline.vue'
import type { CareerPlan, RoadmapStage } from '../../types/api'
import { formatDate, parseJsonField } from '../../utils/data'

const router = useRouter()
const loading = ref(true)
const plan = ref<CareerPlan>()
const history = ref<CareerPlan[]>([])

const advantages = computed(() => parseJsonField<string[]>(plan.value?.advantages, []))
const weaknesses = computed(() => parseJsonField<string[]>(plan.value?.weaknesses, []))
const roadmap = computed(() => parseJsonField<RoadmapStage[]>(plan.value?.roadmap, []))

onMounted(async () => {
  const [currentResult, historyResult] = await Promise.allSettled([
    getCurrentPlan({ silent: true }),
    getPlanHistory({ silent: true }),
  ])
  if (currentResult.status === 'fulfilled') plan.value = currentResult.value
  if (historyResult.status === 'fulfilled') history.value = historyResult.value
  loading.value = false
})

async function switchPlan(id: number) {
  loading.value = true
  try {
    plan.value = await getPlanById(id)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div v-loading="loading">
    <header class="page-heading">
      <div>
        <p class="eyebrow">CAREER ROADMAP</p>
        <h1>职业规划报告</h1>
        <p>把长期目标拆成清晰、可执行、可复盘的成长路线。</p>
      </div>
      <div class="plan-actions">
        <el-button @click="router.push('/career/chat')">与 AI 讨论规划</el-button>
        <el-button type="primary" disabled>重新生成规划 · 待接入</el-button>
      </div>
    </header>

    <template v-if="plan">
      <section class="plan-hero surface-card">
        <div>
          <span class="soft-label">规划 V{{ plan.version }} {{ plan.status === 1 ? '· 当前' : '· 历史' }}</span>
          <h2>{{ plan.targetPosition }}</h2>
          <p>{{ plan.summary || '围绕目标岗位持续积累技术能力、项目经验与面试表现。' }}</p>
          <small>创建于 {{ formatDate(plan.createTime, true) }}</small>
        </div>
        <div class="match-score">
          <el-progress type="dashboard" :percentage="plan.matchScore || 0" :width="145" :stroke-width="12" />
          <span>综合匹配度</span>
        </div>
      </section>

      <section class="insight-grid">
        <article class="surface-card insight-card advantage">
          <div class="card-header"><h2>当前优势</h2><span>✓</span></div>
          <ul v-if="advantages.length">
            <li v-for="item in advantages" :key="item">{{ item }}</li>
          </ul>
          <p v-else class="muted-copy">规划中暂未记录优势项。</p>
        </article>
        <article class="surface-card insight-card weakness">
          <div class="card-header"><h2>重点短板</h2><span>!</span></div>
          <ul v-if="weaknesses.length">
            <li v-for="item in weaknesses" :key="item">{{ item }}</li>
          </ul>
          <p v-else class="muted-copy">规划中暂未记录待提升项。</p>
        </article>
      </section>

      <section class="surface-card roadmap-card">
        <div class="card-header">
          <div><h2>职业成长路线</h2><p>按阶段聚焦最关键的能力，不被过多目标分散注意力。</p></div>
          <span class="soft-label">{{ roadmap.length }} 个阶段</span>
        </div>
        <RoadmapTimeline :stages="roadmap" />
      </section>

      <section v-if="history.length > 1" class="surface-card history-card">
        <div class="card-header"><div><h2>规划版本</h2><p>查看目标与能力变化带来的路线调整</p></div></div>
        <div class="version-list">
          <button
            v-for="item in history"
            :key="item.id"
            :class="{ active: item.id === plan.id }"
            @click="switchPlan(item.id)"
          >
            <span>V{{ item.version }}</span>
            <strong>{{ item.targetPosition }}</strong>
            <small>{{ formatDate(item.createTime) }}</small>
            <em>{{ item.matchScore ?? '—' }}%</em>
          </button>
        </div>
      </section>
    </template>

    <section v-else class="surface-card empty-panel plan-empty">
      <div>
        <span class="empty-icon">↗</span>
        <strong>你的第一份职业规划还未生成</strong>
        <p>先完善职业画像和技能信息。AI 规划接口完成后，可直接在这里生成结构化成长路线。</p>
        <el-button type="primary" @click="router.push('/profile')">完善职业画像</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.plan-actions { display: flex; gap: 10px; }
.plan-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
  padding: 30px 36px;
  background:
    radial-gradient(circle at 90% 20%, rgba(117, 108, 232, 0.11), transparent 28%),
    #fff;
}

.plan-hero > div:first-child { max-width: 780px; }
.plan-hero h2 { margin: 18px 0 10px; font-size: 28px; }
.plan-hero p { margin: 0 0 18px; color: #656a80; line-height: 1.8; }
.plan-hero small { color: var(--muted); }
.match-score { min-width: 160px; text-align: center; }
.match-score span { display: block; margin-top: -15px; color: var(--muted); font-size: 12px; }
.insight-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 20px 0; }
.insight-card { padding: 23px 26px; }
.insight-card .card-header span {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 9px;
}
.advantage .card-header span { color: var(--success); background: var(--success-soft); }
.weakness .card-header span { color: var(--warning); background: var(--warning-soft); }
.insight-card ul { display: grid; gap: 11px; margin: 0; padding: 0; list-style: none; }
.insight-card li { position: relative; padding-left: 20px; color: #54596d; font-size: 13px; line-height: 1.6; }
.insight-card li::before { position: absolute; left: 0; color: var(--primary); content: '•'; }
.muted-copy { color: var(--muted); font-size: 13px; }
.roadmap-card,
.history-card { padding: 25px 28px; }
.history-card { margin-top: 20px; }
.version-list { display: flex; gap: 10px; overflow-x: auto; padding-bottom: 4px; }
.version-list button {
  position: relative;
  display: grid;
  min-width: 210px;
  grid-template-columns: 42px 1fr;
  gap: 2px 10px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}
.version-list button.active { border-color: #9698eb; background: #f8f8ff; }
.version-list span { grid-row: 1 / 3; color: var(--primary); font-size: 17px; font-weight: 800; }
.version-list strong { overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.version-list small { color: var(--muted); }
.version-list em { position: absolute; right: 12px; bottom: 12px; color: var(--primary); font-style: normal; font-weight: 800; }
.plan-empty { min-height: 460px; }
.plan-empty p { max-width: 460px; margin: 8px auto 20px; line-height: 1.7; }

@media (max-width: 800px) {
  .plan-hero { align-items: flex-start; flex-direction: column; }
  .insight-grid { grid-template-columns: 1fr; }
  .page-heading { flex-direction: column; }
}
</style>
