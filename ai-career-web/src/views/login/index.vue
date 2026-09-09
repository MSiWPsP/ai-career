<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const isRegister = ref(false)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  nickname: '',
})

const rules = computed<FormRules>(() => ({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    ...(isRegister.value ? [{ min: 3, max: 50, message: '用户名长度为 3～50 个字符', trigger: 'blur' }] : []),
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    ...(isRegister.value ? [{ min: 6, max: 20, message: '密码长度为 6～20 个字符', trigger: 'blur' }] : []),
  ],
  nickname: isRegister.value ? [{ required: true, message: '请输入昵称', trigger: 'blur' }] : [],
}))

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    if (isRegister.value) {
      await authStore.createAccount(form)
      ElMessage.success('注册成功，请登录')
      isRegister.value = false
      form.password = ''
      await nextTick()
      formRef.value?.clearValidate()
      return
    }
    await authStore.authenticate(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } finally {
    loading.value = false
  }
}

async function switchMode() {
  isRegister.value = !isRegister.value
  form.password = ''
  await nextTick()
  formRef.value?.clearValidate()
}
</script>

<template>
  <main class="login-page">
    <section class="login-hero">
      <div class="hero-grid" />
      <div class="hero-orbit orbit-one" />
      <div class="hero-orbit orbit-two" />
      <div class="hero-content">
        <div class="login-brand">
          <span>途</span>
          <strong>AI职途</strong>
        </div>
        <p class="hero-kicker">AI CAREER GROWTH PLATFORM</p>
        <h1>让每一步成长，<br />都更接近理想职业。</h1>
        <p class="hero-desc">
          从职业画像、成长规划到模拟面试，用 AI 陪你看清方向，拆解目标，持续进步。
        </p>
        <div class="journey">
          <span class="active">职业认知</span><i />
          <span>能力分析</span><i />
          <span>成长规划</span><i />
          <span>模拟面试</span>
        </div>
      </div>
      <div class="hero-quote">
        <span>“</span>
        <p>你不需要一次想清楚整个未来，<br />只需要走好下一步。</p>
      </div>
    </section>

    <section class="login-panel">
      <div class="form-wrap">
        <p class="eyebrow">{{ isRegister ? 'CREATE ACCOUNT' : 'WELCOME BACK' }}</p>
        <h2>{{ isRegister ? '创建你的成长档案' : '欢迎回到 AI职途' }}</h2>
        <p class="form-subtitle">
          {{ isRegister ? '注册后，从一份职业画像开始你的成长旅程。' : '登录后继续查看你的职业路线与成长任务。' }}
        </p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          :validate-on-rule-change="false"
          label-position="top"
          size="large"
          @submit.prevent="submit"
        >
          <el-form-item v-if="isRegister" label="昵称" prop="nickname">
            <el-input v-model="form.nickname" placeholder="怎么称呼你" maxlength="50" />
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              show-password
              @keyup.enter="submit"
            />
          </el-form-item>
          <div v-if="!isRegister" class="form-assist">
            <el-checkbox>记住登录状态</el-checkbox>
            <span>遇到问题？</span>
          </div>
          <el-button class="submit-button" type="primary" :loading="loading" @click="submit">
            {{ isRegister ? '注册并开始' : '进入成长中心' }}
          </el-button>
        </el-form>

        <p class="switch-mode">
          {{ isRegister ? '已经有账号？' : '还没有账号？' }}
          <button @click="switchMode">{{ isRegister ? '直接登录' : '免费注册' }}</button>
        </p>
      </div>
      <p class="copyright">AI职途 · 让职业成长有迹可循</p>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(460px, 1.18fr) minmax(420px, 0.82fr);
  background: #fff;
}

.login-hero {
  position: relative;
  display: flex;
  overflow: hidden;
  align-items: center;
  padding: 9vw;
  color: #fff;
  background:
    radial-gradient(circle at 80% 20%, rgba(153, 141, 255, 0.34), transparent 28%),
    radial-gradient(circle at 18% 80%, rgba(80, 154, 234, 0.25), transparent 30%),
    linear-gradient(145deg, #292d6a 0%, #4b4dc2 58%, #6d66dc 100%);
}

.hero-grid {
  position: absolute;
  inset: 0;
  opacity: 0.14;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.18) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.18) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: linear-gradient(120deg, #000, transparent 82%);
}

.hero-orbit {
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 50%;
}

.orbit-one {
  width: 420px;
  height: 420px;
  right: -160px;
  top: -130px;
}

.orbit-two {
  width: 260px;
  height: 260px;
  right: 90px;
  bottom: -150px;
}

.hero-content {
  position: relative;
  z-index: 2;
  max-width: 650px;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 76px;
}

.login-brand span {
  display: grid;
  width: 45px;
  height: 45px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 20px;
  font-weight: 800;
  backdrop-filter: blur(8px);
}

.login-brand strong {
  font-size: 23px;
  letter-spacing: 1px;
}

.hero-kicker {
  margin: 0 0 15px;
  color: #c7c9ff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 2.2px;
}

.hero-content h1 {
  margin: 0;
  font-size: clamp(38px, 4vw, 60px);
  line-height: 1.24;
  letter-spacing: -1.5px;
}

.hero-desc {
  max-width: 560px;
  margin: 25px 0 38px;
  color: #dfe1ff;
  font-size: 16px;
  line-height: 1.9;
}

.journey {
  display: flex;
  align-items: center;
  max-width: 580px;
  color: #d8daf9;
  font-size: 12px;
}

.journey span {
  white-space: nowrap;
}

.journey span.active {
  color: #fff;
  font-weight: 700;
}

.journey i {
  height: 1px;
  flex: 1;
  margin: 0 10px;
  background: rgba(255, 255, 255, 0.28);
}

.hero-quote {
  position: absolute;
  z-index: 2;
  right: 8%;
  bottom: 7%;
  display: flex;
  gap: 9px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 12px;
  line-height: 1.7;
}

.hero-quote span {
  font-family: Georgia, serif;
  font-size: 36px;
}

.hero-quote p {
  margin: 7px 0 0;
}

.login-panel {
  position: relative;
  display: grid;
  place-items: center;
  padding: 60px;
}

.form-wrap {
  width: min(100%, 400px);
}

.form-wrap h2 {
  margin: 0;
  color: #24273d;
  font-size: 30px;
}

.form-subtitle {
  margin: 10px 0 34px;
  color: #898da0;
  font-size: 14px;
  line-height: 1.7;
}

.form-assist {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 20px;
  color: #8c90a4;
  font-size: 12px;
}

.submit-button {
  width: 100%;
  height: 46px;
  border-radius: 10px;
  box-shadow: 0 10px 22px rgba(85, 88, 217, 0.2);
}

.switch-mode {
  margin-top: 24px;
  color: #8b8fa2;
  font-size: 13px;
  text-align: center;
}

.switch-mode button {
  border: 0;
  color: var(--primary);
  background: transparent;
  cursor: pointer;
  font-weight: 700;
}

.copyright {
  position: absolute;
  bottom: 25px;
  color: #b0b3c0;
  font-size: 11px;
}

:deep(.el-form-item__label) {
  color: #4f5367;
  font-weight: 600;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e0e2eb inset;
}

@media (max-width: 880px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-hero {
    min-height: 350px;
    padding: 50px 9vw;
  }

  .login-brand {
    margin-bottom: 35px;
  }

  .hero-quote {
    display: none;
  }

  .login-panel {
    min-height: 610px;
    padding: 55px 24px 80px;
  }
}
</style>
