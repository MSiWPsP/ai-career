<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ChatDotRound, CircleCheck, MagicStick, RefreshRight, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { generateInterviewReport, getInterview, getInterviewReport } from '../../api/interview'
import { getCurrentPlan, regenerateCareerPlan } from '../../api/career'
import AbilityRadar from '../../components/AbilityRadar.vue'
import type { AbilityRadar as AbilityRadarData, InterviewDetail, InterviewReport } from '../../types/api'
import { difficultyLabel, formatDate, interviewTypeLabel } from '../../utils/data'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const detail = ref<InterviewDetail>()
const report = ref<InterviewReport>()
const loadError = ref(false)
const generating = ref(false)
const replanning = ref(false)
const hasPlan = ref(false)

const radar = computed<AbilityRadarData>(() => {
  const scores = report.value?.scores || {}
  return {
    indicators: Object.keys(scores).map((name) => ({ name, max: 100 })),
    values: Object.values(scores),
  }
})

const level = computed(() => {
  const score = report.value?.totalScore || 0
  if (score >= 90) return '优秀'
  if (score >= 80) return '表现良好'
  if (score >= 70) return '具备一定基础'
  if (score >= 60) return '需要加强'
  return '建议系统复习'
})

async function loadReport() {
  const id = String(route.params.id || '')
  if (!id) return
  loading.value = true
  loadError.value = false
  try {
    const [interview, currentPlan] = await Promise.all([
      getInterview(id), getCurrentPlan({ silent: true }).catch(() => undefined),
    ])
    detail.value = interview
    hasPlan.value = Boolean(currentPlan)
    try {
      report.value = await getInterviewReport(id, { silent: true })
    } catch {
      // 已结束但报告尚未生成时保留详情，供用户显式重试。
      report.value = undefined
    }
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(loadReport)

async function createReport() {
  const id = String(route.params.id || '')
  if (!id || generating.value) return
  generating.value = true
  try {
    report.value = await generateInterviewReport(id)
    ElMessage.success('面试报告已生成')
  } catch {
    // 请求层已提示具体错误，保留当前页面供再次重试。
  } finally {
    generating.value = false
  }
}

async function replan() {
  const id = String(route.params.id || '')
  if (!id || replanning.value) return
  replanning.value = true
  try {
    await regenerateCareerPlan(id)
    ElMessage.success('职业规划已根据面试反馈更新')
    await router.push('/career/plan')
  } catch {
    // 模型或并发校验失败时不离开报告页，用户可以查看提示后重试。
  } finally {
    replanning.value = false
  }
}
</script>

<template>
  <div v-loading="loading">
    <header class="page-heading">
      <div>
        <p class="eyebrow">INTERVIEW REPORT</p>
        <h1>模拟面试报告</h1>
        <p>{{ detail ? formatDate(detail.endTime || detail.createTime, true) : '正在读取报告' }}</p>
      </div>
      <el-button @click="router.push('/interviews')">返回面试记录</el-button>
    </header>

    <el-alert v-if="loadError" type="error" :closable="false" show-icon title="面试详情加载失败">
      <el-button link type="danger" :icon="RefreshRight" @click="loadReport">重新加载</el-button>
    </el-alert>

    <template v-if="report">
      <section class="surface-card report-hero">
        <div>
          <span class="soft-label">{{ interviewTypeLabel[detail?.interviewType || ''] || detail?.interviewType }} · {{ difficultyLabel[detail?.difficulty || ''] || detail?.difficulty }}</span>
          <h2>{{ detail?.targetPosition }}模拟面试报告</h2>
          <p>{{ report.summary || '本次面试已完成，请结合各项能力评分持续复盘。' }}</p>
        </div>
        <div class="total-score">
          <div><strong>{{ report.totalScore }}</strong><span>分</span></div>
          <p>{{ level }}</p>
        </div>
      </section>

      <section class="report-grid">
        <article class="surface-card radar-card">
          <div class="card-header"><div><h2>面试能力雷达</h2><p>各项能力表现一览</p></div></div>
          <AbilityRadar :data="radar" :height="330" name="本次面试" />
        </article>
        <article class="surface-card scores-card">
          <div class="card-header"><div><h2>各项能力评分</h2><p>找到最值得投入的提升方向</p></div></div>
          <div class="score-list">
            <div v-for="(score, name) in report.scores" :key="name">
              <span>{{ name }}</span>
              <el-progress :percentage="score" :show-text="false" />
              <strong>{{ score }}</strong>
            </div>
          </div>
        </article>
      </section>

      <section class="analysis-grid">
        <article class="surface-card analysis-card advantages">
          <div class="analysis-title"><el-icon><CircleCheck /></el-icon><div><h2>本次表现较好的部分</h2><p>继续保持并迁移到更多场景</p></div></div>
          <ul><li v-for="item in report.advantages" :key="item">{{ item }}</li></ul>
          <p v-if="!report.advantages.length" class="empty-copy">暂无优势分析</p>
        </article>
        <article class="surface-card analysis-card weaknesses">
          <div class="analysis-title"><el-icon><Warning /></el-icon><div><h2>需要重点提升</h2><p>下一阶段优先突破这些短板</p></div></div>
          <ul><li v-for="item in report.weaknesses" :key="item">{{ item }}</li></ul>
          <p v-if="!report.weaknesses.length" class="empty-copy">暂无薄弱项分析</p>
        </article>
      </section>

      <section class="surface-card suggestion-card">
        <div class="suggestion-title"><el-icon><MagicStick /></el-icon><div><p>AI 学习建议</p><h2>把复盘转化为下一步行动</h2></div></div>
        <div v-if="report.suggestions.length" class="suggestion-list">
          <article v-for="item in report.suggestions" :key="item.topic + item.content">
            <div><strong>{{ item.topic }}</strong><el-tag :type="item.priority === 'HIGH' ? 'danger' : 'warning'" size="small">{{ item.priority }}</el-tag></div>
            <p>{{ item.content }}</p>
          </article>
        </div>
        <p v-else class="empty-copy">暂无改进建议</p>
      </section>

      <section class="replan-banner">
        <div><span>形成成长闭环</span><strong>根据本次面试表现，动态调整职业规划</strong></div>
        <el-button v-if="hasPlan" type="primary" size="large" :icon="MagicStick" :loading="replanning" @click="replan">根据本次面试重新规划</el-button>
        <el-button v-else type="primary" size="large" :icon="MagicStick" @click="router.push('/career/plan')">先生成首版职业规划</el-button>
      </section>
    </template>

    <section v-else-if="!loading && !loadError" class="surface-card empty-panel report-empty">
      <div><el-icon class="empty-icon"><ChatDotRound /></el-icon><strong>面试报告尚未生成</strong><p>面试记录已保存。报告生成失败或尚未执行时，可在这里重试；未回答任何题目无法生成报告。</p><el-button type="primary" :loading="generating" :disabled="detail?.status !== 2 && detail?.status !== 3" @click="createReport">生成面试报告</el-button></div>
    </section>
  </div>
</template>

<style scoped>
.report-hero { position: relative; display: flex; align-items: center; justify-content: space-between; gap: 30px; overflow: hidden; padding: 30px 36px; border-color: #2563eb; color: #fff; background: radial-gradient(circle at 82% 12%, rgb(103 232 249 / 27%), transparent 34%), linear-gradient(120deg, #102f68, #1d4ed8 60%, #2686e7); box-shadow: 0 20px 44px rgb(29 78 216 / 18%); }
.report-hero::after { position: absolute; right: -72px; bottom: -132px; width: 300px; height: 300px; border: 1px solid rgb(255 255 255 / 18%); border-radius: 50%; box-shadow: 0 0 0 42px rgb(255 255 255 / 5%); content: ''; pointer-events: none; }
.report-hero > * { position: relative; z-index: 1; }
.report-hero > div:first-child { max-width: 780px; }
.report-hero h2 { margin: 16px 0 9px; color: #fff; font-size: 26px; }
.report-hero p { margin: 0; color: #dbeafe; line-height: 1.75; }
.report-hero .soft-label { border: 1px solid rgb(255 255 255 / 20%); color: #e0f2fe; background: rgb(255 255 255 / 12%); }
.total-score { min-width: 150px; text-align: center; }
.total-score > div { display: inline-flex; align-items: baseline; color: #67e8f9; text-shadow: 0 0 24px rgb(103 232 249 / 35%); }
.total-score strong { font-size: 58px; line-height: 1; }
.total-score span { font-size: 15px; font-weight: 700; }
.total-score p { margin-top: 8px; color: #fff; font-size: 13px; font-weight: 700; }
.report-grid { display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 20px; margin: 20px 0; }
.radar-card,
.scores-card,
.analysis-card,
.suggestion-card { position: relative; overflow: hidden; padding: 23px 26px; box-shadow: 0 14px 34px rgb(23 49 92 / 8%); }
.radar-card::before,
.scores-card::before { position: absolute; inset: 0 0 auto; height: 3px; background: linear-gradient(90deg, #2563eb, #38bdf8, transparent); content: ''; }
.analysis-card { transition: box-shadow var(--motion-normal) ease, transform var(--motion-normal) var(--ease-standard); }
.analysis-card:hover { box-shadow: 0 20px 40px rgb(29 78 216 / 12%); transform: translateY(-4px); }
.score-list { display: grid; gap: 20px; padding-top: 10px; }
.score-list > div { display: grid; grid-template-columns: 105px 1fr 30px; align-items: center; gap: 12px; }
.score-list span { font-size: 12px; font-weight: 600; }
.score-list strong { color: var(--primary); text-align: right; }
.analysis-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.analysis-title { display: flex; align-items: center; gap: 13px; margin-bottom: 18px; }
.analysis-title > .el-icon { display: grid; width: 38px; height: 38px; place-items: center; border-radius: 11px; }
.advantages .analysis-title > .el-icon { color: var(--success); background: var(--success-soft); }
.weaknesses .analysis-title > .el-icon { color: var(--warning); background: var(--warning-soft); }
.analysis-title h2 { margin: 0; font-size: 16px; }
.analysis-title p { margin: 4px 0 0; color: var(--muted); font-size: 11px; }
.analysis-card ul { display: grid; gap: 10px; margin: 0; padding: 0; list-style: none; }
.analysis-card li { padding: 10px 13px; border: 1px solid #e4eaf1; border-radius: var(--radius-sm); background: var(--surface-subtle); color: var(--muted); font-size: 12px; }
.suggestion-card { margin-top: 20px; background: radial-gradient(circle at 100% 0%, rgb(59 130 246 / 10%), transparent 36%), linear-gradient(145deg, #fff, #f5f9ff); }
.suggestion-title { display: flex; align-items: center; gap: 13px; }
.suggestion-title > .el-icon { display: grid; width: 42px; height: 42px; place-items: center; border-radius: var(--radius-md); color: #fff; background: var(--primary); }
.suggestion-title p { margin: 0 0 3px; color: var(--primary); font-size: 11px; font-weight: 800; }
.suggestion-title h2 { margin: 0; font-size: 17px; }
.suggestion-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 12px; margin-top: 20px; }
.suggestion-list article { padding: 15px 17px; border: 1px solid #d3e2f4; border-radius: var(--radius-md); background: rgb(255 255 255 / 84%); box-shadow: 0 7px 18px rgb(23 49 92 / 5%); transition: border-color var(--motion-fast) ease, transform var(--motion-normal) var(--ease-standard); }
.suggestion-list article:hover { border-color: #9fc3f2; transform: translateY(-3px); }
.suggestion-list article > div { display: flex; align-items: center; justify-content: space-between; }
.suggestion-list p { margin: 9px 0 0; color: var(--muted); font-size: 12px; line-height: 1.6; }
.empty-copy { color: var(--muted); font-size: 12px; }
.replan-banner { position: relative; display: flex; align-items: center; justify-content: space-between; gap: 20px; overflow: hidden; margin-top: 20px; padding: 24px 28px; border: 1px solid #2563eb; border-radius: var(--radius-lg); color: #fff; background: linear-gradient(110deg, #123b82, #1d4ed8 68%, #2581df); box-shadow: 0 16px 36px rgb(29 78 216 / 16%); }
.replan-banner span,
.replan-banner strong { display: block; }
.replan-banner span { margin-bottom: 5px; color: #93c5fd; font-size: 11px; font-weight: 800; }
.replan-banner strong { font-size: 15px; }
.replan-banner .el-button { border-color: #fff; color: #1746a2; background: #fff; }
.report-empty { min-height: 440px; }
@media (max-width: 900px) { .report-grid, .analysis-grid { grid-template-columns: 1fr; } .report-hero, .replan-banner { align-items: flex-start; flex-direction: column; } }
</style>
