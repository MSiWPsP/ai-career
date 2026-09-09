<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Cpu, MagicStick, Monitor } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createProfile, getProfile, getProfileCompletion, updateProfile } from '../../api/profile'
import { getSkills, replaceSkills } from '../../api/skill'
import type { UserProfile } from '../../types/api'

interface SkillCatalogItem {
  name: string
  category: string
}

const skillCatalog: Record<string, SkillCatalogItem[]> = {
  编程语言: [
    { name: 'Java', category: 'PROGRAMMING' },
    { name: 'Python', category: 'PROGRAMMING' },
    { name: 'C', category: 'PROGRAMMING' },
    { name: 'JavaScript', category: 'PROGRAMMING' },
  ],
  框架: [
    { name: 'Spring', category: 'FRAMEWORK' },
    { name: 'SpringBoot', category: 'FRAMEWORK' },
    { name: 'Vue', category: 'FRAMEWORK' },
  ],
  数据与中间件: [
    { name: 'MySQL', category: 'DATABASE' },
    { name: 'Redis', category: 'MIDDLEWARE' },
  ],
  开发工具: [
    { name: 'Git', category: 'TOOL' },
    { name: 'Docker', category: 'TOOL' },
    { name: 'Linux', category: 'TOOL' },
  ],
}

const formRef = ref<FormInstance>()
const currentStep = ref(0)
const loading = ref(true)
const saving = ref(false)
const profileExists = ref(false)
const completion = ref(0)
const skillLevels = reactive<Record<string, number>>({})
const form = reactive<UserProfile>({
  education: '',
  major: '',
  grade: '',
  graduationYear: new Date().getFullYear() + 2,
  dailyStudyHours: 2,
  interestDescription: '',
  targetPosition: '',
  targetCity: '',
  careerStage: 'LEARNING',
  targetTime: '6个月',
  careerGoal: '',
})

const rules: FormRules = {
  graduationYear: [{ type: 'number', min: 2000, max: 2100, message: '请输入有效毕业年份', trigger: 'change' }],
  dailyStudyHours: [{ type: 'number', min: 0, max: 24, message: '每日学习时间应为 0～24 小时', trigger: 'change' }],
}

const interests = [
  { icon: Cpu, title: 'Java后端开发', desc: 'Web 服务、业务系统与中间件' },
  { icon: Monitor, title: 'Web前端开发', desc: 'Vue、React 与交互开发' },
  { icon: MagicStick, title: 'AI应用开发', desc: 'Agent、RAG 与大模型应用' },
]

const selectedSkillCount = computed(() => Object.values(skillLevels).filter((level) => level > 0).length)

onMounted(async () => {
  const [profileResult, skillsResult, completionResult] = await Promise.allSettled([
    getProfile({ silent: true }),
    getSkills(),
    getProfileCompletion({ silent: true }),
  ])
  if (profileResult.status === 'fulfilled') {
    Object.assign(form, profileResult.value)
    profileExists.value = true
  }
  if (skillsResult.status === 'fulfilled') {
    skillsResult.value.forEach((skill) => {
      skillLevels[skill.skillName] = skill.level
    })
  }
  if (completionResult.status === 'fulfilled') completion.value = completionResult.value.score
  loading.value = false
})

function chooseInterest(title: string) {
  form.interestDescription = form.interestDescription === title ? '' : title
  if (!form.targetPosition && title === 'Java后端开发') form.targetPosition = 'Java后端开发工程师'
}

async function nextStep() {
  if (currentStep.value < 3) currentStep.value += 1
  else await save()
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (profileExists.value) await updateProfile(form)
    else {
      await createProfile(form)
      profileExists.value = true
    }
    const selectedSkills = Object.entries(skillLevels)
      .filter(([, level]) => level > 0)
      .map(([skillName, level]) => {
        const item = Object.values(skillCatalog).flat().find((skill) => skill.name === skillName)
        return { skillName, skillCategory: item?.category || 'OTHER', level }
      })
    if (selectedSkills.length) await replaceSkills(selectedSkills)
    const latestCompletion = await getProfileCompletion()
    completion.value = latestCompletion.score
    ElMessage.success('职业画像已保存')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div v-loading="loading">
    <header class="page-heading">
      <div>
        <p class="eyebrow">CAREER PORTRAIT</p>
        <h1>我的职业画像</h1>
        <p>让 AI 了解你的起点、兴趣与目标，后续建议才会真正适合你。</p>
      </div>
      <div class="completion-badge">
        <el-progress type="circle" :percentage="completion" :width="62" :stroke-width="6" />
        <div><span>画像完整度</span><strong>{{ completion >= 80 ? '信息较完整' : '继续完善中' }}</strong></div>
      </div>
    </header>

    <section class="surface-card profile-shell">
      <el-steps :active="currentStep" align-center finish-status="success">
        <el-step title="基础信息" description="你的学习背景" />
        <el-step title="技能能力" description="当前掌握程度" />
        <el-step title="职业兴趣" description="喜欢的方向" />
        <el-step title="职业目标" description="想去的地方" />
      </el-steps>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="profile-form">
        <section v-show="currentStep === 0" class="step-panel">
          <div class="step-intro"><span>01</span><div><h2>先了解你的学习背景</h2><p>这些信息帮助 AI 判断你当前所处的成长阶段。</p></div></div>
          <div class="form-grid">
            <el-form-item label="当前学历">
              <el-select v-model="form.education" placeholder="请选择学历">
                <el-option v-for="item in ['专科', '本科', '硕士', '博士']" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="专业">
              <el-input v-model="form.major" placeholder="例如：软件工程" maxlength="100" />
            </el-form-item>
            <el-form-item label="年级">
              <el-select v-model="form.grade" placeholder="请选择年级">
                <el-option v-for="item in ['大一', '大二', '大三', '大四', '研一', '研二', '研三']" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="预计毕业年份" prop="graduationYear">
              <el-input-number v-model="form.graduationYear" :min="2000" :max="2100" controls-position="right" />
            </el-form-item>
            <el-form-item label="每天可投入学习时间" prop="dailyStudyHours" class="wide-field">
              <el-slider v-model="form.dailyStudyHours" :min="0" :max="12" :step="0.5" show-input />
            </el-form-item>
          </div>
        </section>

        <section v-show="currentStep === 1" class="step-panel">
          <div class="step-intro"><span>02</span><div><h2>标记当前技能水平</h2><p>0 表示暂未学习，5 表示熟练掌握。已选择 {{ selectedSkillCount }} 项技能。</p></div></div>
          <div class="skill-groups">
            <article v-for="(skills, category) in skillCatalog" :key="category" class="skill-group">
              <h3>{{ category }}</h3>
              <div v-for="skill in skills" :key="skill.name" class="skill-row">
                <span>{{ skill.name }}</span>
                <el-rate v-model="skillLevels[skill.name]" :max="5" clearable />
                <small>{{ ['未学习', '了解', '入门', '基础', '熟悉', '掌握'][skillLevels[skill.name] || 0] }}</small>
              </div>
            </article>
          </div>
        </section>

        <section v-show="currentStep === 2" class="step-panel">
          <div class="step-intro"><span>03</span><div><h2>哪类工作更吸引你？</h2><p>选择一个主要方向，也可以在下方补充你的真实想法。</p></div></div>
          <div class="interest-grid">
            <button
              v-for="item in interests"
              :key="item.title"
              type="button"
              class="interest-card"
              :class="{ selected: form.interestDescription === item.title }"
              @click="chooseInterest(item.title)"
            >
              <el-icon class="interest-icon"><component :is="item.icon" /></el-icon><strong>{{ item.title }}</strong><small>{{ item.desc }}</small>
            </button>
          </div>
          <el-form-item label="补充描述">
            <el-input v-model="form.interestDescription" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="例如：我喜欢解决复杂业务问题，也希望参与有真实用户的项目。" />
          </el-form-item>
        </section>

        <section v-show="currentStep === 3" class="step-panel">
          <div class="step-intro"><span>04</span><div><h2>明确下一阶段的目标</h2><p>目标不必完美，先设定一个可行动、可调整的方向。</p></div></div>
          <div class="form-grid">
            <el-form-item label="目标岗位">
              <el-input v-model="form.targetPosition" placeholder="例如：Java后端开发工程师" maxlength="100" />
            </el-form-item>
            <el-form-item label="目标城市">
              <el-input v-model="form.targetCity" placeholder="例如：南京" maxlength="100" />
            </el-form-item>
            <el-form-item label="当前阶段">
              <el-select v-model="form.careerStage">
                <el-option label="探索方向" value="EXPLORING" />
                <el-option label="学习提升" value="LEARNING" />
                <el-option label="实习准备" value="INTERNSHIP" />
                <el-option label="校招准备" value="JOB_HUNTING" />
              </el-select>
            </el-form-item>
            <el-form-item label="目标时间">
              <el-select v-model="form.targetTime">
                <el-option v-for="item in ['3个月', '6个月', '1年', '毕业前']" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="职业目标描述" class="wide-field">
              <el-input v-model="form.careerGoal" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="例如：半年后寻找 Java 后端实习，能够独立完成中小型业务系统。" />
            </el-form-item>
          </div>
        </section>
      </el-form>

      <footer class="step-actions">
        <el-button :disabled="currentStep === 0" @click="currentStep--">上一步</el-button>
        <div>
          <el-button v-if="currentStep === 3" :loading="saving" @click="save">仅保存画像</el-button>
          <el-button type="primary" :loading="saving" @click="nextStep">
            {{ currentStep === 3 ? '保存职业画像' : '下一步' }}
          </el-button>
        </div>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.completion-badge {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 10px 16px;
  border: 1px solid #e0e2f3;
  border-radius: 14px;
  background: #fff;
}

.completion-badge span,
.completion-badge strong { display: block; }
.completion-badge span { color: var(--muted); font-size: 11px; }
.completion-badge strong { margin-top: 4px; font-size: 13px; }
.profile-shell { padding: 28px 34px 24px; }
.profile-form { min-height: 450px; margin-top: 40px; }
.step-panel { max-width: 980px; margin: 0 auto; }

.step-intro {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 28px;
}

.step-intro > span {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 13px;
  color: var(--primary);
  background: var(--primary-soft);
  font-size: 13px;
  font-weight: 800;
}

.step-intro h2 { margin: 0; font-size: 20px; }
.step-intro p { margin: 6px 0 0; color: var(--muted); font-size: 13px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4px 28px; }
.wide-field { grid-column: 1 / -1; }
.form-grid :deep(.el-select),
.form-grid :deep(.el-input-number) { width: 100%; }
.skill-groups { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

.skill-group {
  padding: 17px 20px;
  border: 1px solid var(--line);
  border-radius: 13px;
  background: #fafafe;
}

.skill-group h3 { margin: 0 0 10px; font-size: 14px; }
.skill-row {
  display: grid;
  grid-template-columns: 95px 1fr 45px;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  border-top: 1px dashed #e3e4ed;
}

.skill-row > span { font-size: 13px; font-weight: 600; }
.skill-row small { color: var(--muted); font-size: 11px; text-align: right; }
.interest-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; margin-bottom: 25px; }

.interest-card {
  padding: 24px 18px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: 0.2s ease;
}

.interest-card:hover,
.interest-card.selected {
  border-color: #8d8fe7;
  background: #f7f7ff;
  box-shadow: 0 10px 24px rgba(80, 82, 185, 0.09);
  transform: translateY(-2px);
}

.interest-card .interest-icon,
.interest-card strong,
.interest-card small { display: block; }
.interest-card .interest-icon { color: var(--primary); font-size: 27px; }
.interest-card strong { margin: 16px 0 7px; font-size: 15px; }
.interest-card small { color: var(--muted); line-height: 1.5; }

.step-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 25px;
  padding-top: 20px;
  border-top: 1px solid var(--line);
}

@media (max-width: 850px) {
  .form-grid,
  .skill-groups,
  .interest-grid { grid-template-columns: 1fr; }
  .profile-shell { padding: 22px 18px; }
  .profile-shell :deep(.el-step__description) { display: none; }
}
</style>
