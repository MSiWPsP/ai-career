<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { DataAnalysis, TrendCharts, Trophy } from '@element-plus/icons-vue'
import { getAbilityHistory, getAbilityRadar, getAbilityTrend, getCurrentAbilities } from '../../api/ability'
import AbilityRadar from '../../components/AbilityRadar.vue'
import AbilityTrendChart from '../../components/AbilityTrendChart.vue'
import type { AbilityRadar as AbilityRadarData, AbilityScore, AbilityTrend } from '../../types/api'
import { formatDate } from '../../utils/data'

const loading = ref(true)
const current = ref<Record<string, number>>({})
const radar = ref<AbilityRadarData>({ indicators: [], values: [] })
const trend = ref<AbilityTrend>({ records: [] })
const history = ref<AbilityScore[]>([])
const historyTotal = ref(0)
const selectedAbility = ref('')

const averageScore = computed(() => {
  const values = Object.values(current.value)
  return values.length ? Math.round(values.reduce((sum, score) => sum + score, 0) / values.length) : 0
})

const strongest = computed(() => {
  const entries = Object.entries(current.value)
  return entries.sort((a, b) => b[1] - a[1])[0]
})

const abilityChartTitle = computed(() => radar.value.indicators.length > 0 && radar.value.indicators.length < 3
  ? '当前能力概览'
  : '当前能力雷达')

onMounted(async () => {
  const [currentData, radarData, historyData] = await Promise.all([
    getCurrentAbilities(),
    getAbilityRadar(),
    getAbilityHistory({ page: 1, pageSize: 10 }),
  ])
  current.value = currentData
  radar.value = radarData
  history.value = historyData.records
  historyTotal.value = historyData.total
  selectedAbility.value = Object.keys(currentData)[0] || ''
  trend.value = await getAbilityTrend(selectedAbility.value || undefined)
  if (!selectedAbility.value && trend.value.abilityName) selectedAbility.value = trend.value.abilityName
  loading.value = false
})

async function switchTrend() {
  trend.value = await getAbilityTrend(selectedAbility.value || undefined)
}

function sourceLabel(source: string) {
  return { SELF: '自我评估', INTERVIEW: '模拟面试', AGENT: 'AI 分析' }[source] || source
}
</script>

<template>
  <div v-loading="loading">
    <header class="page-heading">
      <div>
        <p class="eyebrow">ABILITY GROWTH</p>
        <h1>能力画像</h1>
        <p>关注分数背后的变化，找到下一阶段最值得投入的方向。</p>
      </div>
      <span class="soft-label">已记录 {{ historyTotal }} 次能力评估</span>
    </header>

    <section class="ability-summary">
      <article><el-icon class="summary-icon"><DataAnalysis /></el-icon><div><p>能力维度</p><strong>{{ Object.keys(current).length }}<small>项</small></strong></div></article>
      <article><el-icon class="summary-icon green"><TrendCharts /></el-icon><div><p>当前平均分</p><strong>{{ averageScore }}<small>分</small></strong></div></article>
      <article><el-icon class="summary-icon orange"><Trophy /></el-icon><div><p>当前优势能力</p><strong class="ability-name">{{ strongest?.[0] || '待评估' }}</strong><small v-if="strongest">{{ strongest[1] }} 分</small></div></article>
    </section>

    <section class="ability-grid">
      <article class="surface-card chart-card">
        <div class="card-header"><div><h2>{{ abilityChartTitle }}</h2><p>最近一次有效评分</p></div></div>
        <AbilityRadar :data="radar" :height="320" />
      </article>
      <article class="surface-card chart-card">
        <div class="card-header">
          <div><h2>能力成长趋势</h2><p>观察同一项能力的长期变化</p></div>
          <el-select v-model="selectedAbility" placeholder="选择能力" size="small" style="width: 130px" @change="switchTrend">
            <el-option v-for="name in Object.keys(current)" :key="name" :label="name" :value="name" />
          </el-select>
        </div>
        <AbilityTrendChart :data="trend" :height="300" />
      </article>
    </section>

    <section class="surface-card history-card">
      <div class="card-header">
        <div><h2>能力评分记录</h2><p>能力评分来自自我评估、模拟面试和后续 Agent 分析</p></div>
      </div>
      <el-table v-if="history.length" :data="history" style="width: 100%">
        <el-table-column prop="abilityName" label="能力名称" min-width="150" />
        <el-table-column label="评分" min-width="210">
          <template #default="{ row }">
            <div class="table-score"><el-progress :percentage="row.score" :show-text="false" /><strong>{{ row.score }}</strong></div>
          </template>
        </el-table-column>
        <el-table-column label="数据来源" min-width="130">
          <template #default="{ row }"><el-tag effect="light">{{ sourceLabel(row.sourceType) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="评估时间" min-width="160">
          <template #default="{ row }">{{ formatDate(row.createTime, true) }}</template>
        </el-table-column>
      </el-table>
      <div v-else class="empty-panel">
        <div><el-icon class="empty-icon"><DataAnalysis /></el-icon><strong>暂无能力评分记录</strong><span>完成技能画像或模拟面试后，这里会持续沉淀能力变化。</span></div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.ability-summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; }
.ability-summary article { position: relative; display: flex; min-height: 96px; align-items: center; gap: 13px; overflow: hidden; padding: 18px 20px; border: 1px solid #bfd6f5; border-radius: 13px; background: linear-gradient(145deg, #fff, #f3f8ff); box-shadow: 0 10px 25px rgb(29 78 216 / 8%); transition: box-shadow var(--motion-normal) ease, transform var(--motion-normal) var(--ease-standard); }
.ability-summary article::after { position: absolute; right: -32px; bottom: -48px; width: 110px; height: 110px; border: 16px solid rgb(59 130 246 / 8%); border-radius: 50%; content: ''; }
.ability-summary article:hover { box-shadow: 0 18px 34px rgb(29 78 216 / 14%); transform: translateY(-4px); }
.ability-summary article:first-child { border-color: #2563eb; color: #fff; background: linear-gradient(145deg, #123b82, #2563eb); }
.ability-summary article:nth-child(2) { border-color: #3b82f6; color: #fff; background: linear-gradient(145deg, #1d4ed8, #2297e6); }
.ability-summary article:first-child p,
.ability-summary article:nth-child(2) p,
.ability-summary article:first-child small,
.ability-summary article:nth-child(2) small { color: #dbeafe; }
.ability-summary article:first-child .summary-icon,
.ability-summary article:nth-child(2) .summary-icon { color: #fff; background: rgb(255 255 255 / 14%); box-shadow: inset 0 0 0 1px rgb(255 255 255 / 18%); }
.summary-icon { display: grid; width: 44px; height: 44px; place-items: center; border-radius: 13px; color: var(--primary); background: var(--primary-soft); font-size: 20px; }
.summary-icon.green { color: var(--success); background: var(--success-soft); }
.summary-icon.orange { color: var(--warning); background: var(--warning-soft); }
.ability-summary p { margin: 0 0 4px; color: var(--muted); font-size: 11px; }
.ability-summary strong { font-size: 22px; }
.ability-summary small { margin-left: 4px; color: var(--muted); font-size: 11px; }
.ability-summary .ability-name { font-size: 16px; }
.ability-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.chart-card,
.history-card { position: relative; overflow: hidden; padding: 23px 25px; background: linear-gradient(145deg, #fff, #f8fbff); box-shadow: 0 14px 34px rgb(23 49 92 / 8%); }
.chart-card::before { position: absolute; inset: 0 0 auto; height: 3px; background: linear-gradient(90deg, #2563eb, #38bdf8, transparent 85%); content: ''; }
.chart-card { transition: box-shadow var(--motion-normal) ease, transform var(--motion-normal) var(--ease-standard); }
.chart-card:hover { box-shadow: 0 20px 42px rgb(29 78 216 / 12%); transform: translateY(-3px); }
.history-card { margin-top: 20px; }
.history-card .card-header { margin: -23px -25px 20px; padding: 20px 25px; border-bottom: 1px solid #d8e6f6; background: linear-gradient(90deg, #eff6ff, #fff); }
.table-score { display: grid; grid-template-columns: minmax(100px, 1fr) 35px; align-items: center; gap: 12px; }
.table-score strong { color: var(--primary); }
@media (max-width: 900px) { .ability-grid { grid-template-columns: 1fr; } }
@media (max-width: 700px) { .ability-summary { grid-template-columns: 1fr; } }
</style>
