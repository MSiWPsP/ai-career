<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Microphone, RefreshRight, Service } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { startInterview as startInterviewRequest } from '../../api/interview'
import { getProfile } from '../../api/profile'
import { getSkills } from '../../api/skill'
import type { UserProfile, UserSkill } from '../../types/api'

const loading = ref(true)
const starting = ref(false)
const loadError = ref(false)
const profile = ref<UserProfile>()
const skills = ref<UserSkill[]>([])
const config = reactive({
  targetPosition: '',
  interviewType: 'TECHNICAL',
  difficulty: 'MEDIUM',
  maxQuestions: 5,
})
const router = useRouter()

const strongestSkills = computed(() => [...skills.value].sort((a, b) => b.score - a.score).slice(0, 5))

async function loadContext() {
  loading.value = true
  loadError.value = false
  const [profileResult, skillResult] = await Promise.allSettled([
    getProfile({ silent: true }),
    getSkills(),
  ])
  if (profileResult.status === 'fulfilled') {
    profile.value = profileResult.value
    config.targetPosition = profileResult.value.targetPosition || ''
  }
  if (skillResult.status === 'fulfilled') skills.value = skillResult.value
  loadError.value = profileResult.status === 'rejected' || skillResult.status === 'rejected'
  loading.value = false
}

onMounted(loadContext)

async function startInterview() {
  const targetPosition = config.targetPosition.trim()
  if (!targetPosition) {
    ElMessage.warning('请先填写目标岗位')
    return
  }
  starting.value = true
  try {
    const result = await startInterviewRequest({ ...config, targetPosition })
    await router.push({ name: 'interview-session', params: { id: result.interviewId } })
  } finally {
    starting.value = false
  }
}
</script>

<template>
  <div v-loading="loading">
    <header class="page-heading">
      <div>
        <p class="eyebrow">MOCK INTERVIEW</p>
        <h1>准备一次模拟面试</h1>
        <p>选择岗位、类型和难度，进行一场贴近真实招聘过程的练习。</p>
      </div>
      <span class="ready-tag">个性化动态提问</span>
    </header>

    <el-alert v-if="loadError" class="context-alert" type="warning" :closable="false" show-icon>
      <template #title>部分职业画像加载失败，建议刷新后再开始面试</template>
      <el-button link type="warning" :icon="RefreshRight" @click="loadContext">重新加载</el-button>
    </el-alert>

    <section class="setup-grid">
      <article class="surface-card config-card">
        <div class="config-head">
          <el-icon class="interviewer-avatar"><Service /></el-icon>
          <div><h2>AI 模拟面试</h2><p>面试过程中不会实时展示评分，问题会根据你的回答动态调整。</p></div>
        </div>

        <el-form :model="config" label-position="top" size="large">
          <el-form-item label="目标岗位">
            <el-input v-model="config.targetPosition" placeholder="例如：Java后端开发工程师" />
          </el-form-item>
          <el-form-item label="面试类型">
            <div class="choice-grid">
              <button v-for="item in [{ label: '技术面', value: 'TECHNICAL', desc: '基础知识与技术深度' }, { label: '项目面', value: 'PROJECT', desc: '项目经验与问题解决' }]" :key="item.value" type="button" :class="{ active: config.interviewType === item.value }" @click="config.interviewType = item.value">
                <strong>{{ item.label }}</strong><small>{{ item.desc }}</small>
              </button>
            </div>
          </el-form-item>
          <el-form-item label="面试难度">
            <el-segmented v-model="config.difficulty" :options="[{ label: '初级', value: 'EASY' }, { label: '中级', value: 'MEDIUM' }, { label: '高级', value: 'HARD' }]" block />
          </el-form-item>
          <el-form-item label="问题数量">
            <el-radio-group v-model="config.maxQuestions">
              <el-radio-button :value="5">5 题</el-radio-button>
              <el-radio-button :value="10">10 题</el-radio-button>
              <el-radio-button :value="15">15 题</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <div class="start-area">
            <p>预计用时 {{ config.maxQuestions * 2 }}～{{ config.maxQuestions * 3 }} 分钟</p>
            <el-button type="primary" size="large" :icon="Microphone" :loading="starting" :disabled="loading" @click="startInterview">开始模拟面试</el-button>
          </div>
        </el-form>
      </article>

      <aside class="surface-card readiness-card">
        <p class="eyebrow">YOUR CONTEXT</p>
        <h2>本次面试会参考</h2>
        <p>面试官将结合你的目标与技能画像动态组织问题。</p>
        <div class="target-context">
          <span>目标岗位</span>
          <strong>{{ config.targetPosition || '尚未设置' }}</strong>
        </div>
        <div class="skill-preview">
          <div v-for="skill in strongestSkills" :key="skill.id">
            <span>{{ skill.skillName }}</span>
            <el-progress :percentage="skill.score" :show-text="false" />
            <strong>{{ skill.score }}</strong>
          </div>
          <div v-if="!strongestSkills.length" class="skills-empty">暂无技能画像，建议先完善职业画像。</div>
        </div>
        <div class="tips">
          <strong>面试小提示</strong>
          <p>尽量完整表达思路。遇到不确定的问题，也可以先说明你的理解和分析过程。</p>
        </div>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.ready-tag { padding: 8px 13px; border-radius: 999px; color: var(--primary); background: var(--primary-soft); font-size: 12px; font-weight: 700; }
.context-alert { margin-bottom: 18px; }
.setup-grid { display: grid; grid-template-columns: minmax(0, 1fr) 350px; gap: 20px; }
.config-card { padding: 28px 32px; }
.config-head { display: flex; gap: 15px; margin-bottom: 28px; padding-bottom: 22px; border-bottom: 1px solid var(--line); }
.interviewer-avatar { display: grid; width: 48px; height: 48px; flex: 0 0 48px; place-items: center; border-radius: 15px; color: #fff; background: linear-gradient(135deg, #5559d8, #8470e5); font-size: 13px; font-weight: 800; }
.config-head h2 { margin: 1px 0 6px; font-size: 19px; }
.config-head p { margin: 0; color: var(--muted); font-size: 12px; line-height: 1.6; }
.choice-grid { display: grid; width: 100%; grid-template-columns: 1fr 1fr; gap: 12px; }
.choice-grid button { padding: 17px; border: 1px solid var(--line); border-radius: 12px; background: #fff; cursor: pointer; text-align: left; }
.choice-grid button.active { border-color: #9295ea; background: #f6f6ff; box-shadow: inset 0 0 0 1px #9295ea; }
.choice-grid strong,
.choice-grid small { display: block; }
.choice-grid small { margin-top: 5px; color: var(--muted); }
.start-area { display: flex; align-items: center; justify-content: space-between; margin-top: 28px; padding-top: 22px; border-top: 1px solid var(--line); }
.start-area p { color: var(--muted); font-size: 12px; }
.readiness-card { padding: 26px; }
.readiness-card h2 { margin: 0 0 7px; font-size: 19px; }
.readiness-card > p:not(.eyebrow) { margin: 0; color: var(--muted); font-size: 12px; line-height: 1.6; }
.target-context { margin: 24px 0; padding: 17px; border-radius: 12px; background: var(--primary-soft); }
.target-context span,
.target-context strong { display: block; }
.target-context span { color: var(--muted); font-size: 11px; }
.target-context strong { margin-top: 6px; font-size: 14px; }
.skill-preview { display: grid; gap: 14px; }
.skill-preview > div { display: grid; grid-template-columns: 85px 1fr 28px; align-items: center; gap: 10px; font-size: 12px; }
.skill-preview strong { color: var(--primary); text-align: right; }
.skills-empty { display: block !important; padding: 18px; border: 1px dashed var(--line); border-radius: 10px; color: var(--muted); text-align: center; }
.tips { margin-top: 25px; padding: 17px; border-radius: 12px; background: #f8f8fb; }
.tips strong { font-size: 12px; }
.tips p { margin: 7px 0 0; color: var(--muted); font-size: 11px; line-height: 1.7; }
@media (max-width: 980px) { .setup-grid { grid-template-columns: 1fr; } }
</style>
