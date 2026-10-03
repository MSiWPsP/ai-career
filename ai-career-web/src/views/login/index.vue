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
      <div class="hero-content">
        <div class="login-brand">
          <img src="/ai-career-mark.png" alt="智职星" />
          <span class="login-brand-copy">
            <strong>智职星</strong>
            <small>智能职业成长平台</small>
          </span>
        </div>
        <h1>让每一步成长，<br />都更接近理想职业。</h1>
        <p class="hero-desc">
          从职业画像、成长规划到模拟面试，清晰了解自己，拆解目标，持续积累职业能力。
        </p>
        <div class="journey">
          <span class="active">职业认知</span><i />
          <span>能力分析</span><i />
          <span>成长规划</span><i />
          <span>模拟面试</span>
        </div>
      </div>
    </section>

    <section class="login-panel">
      <div class="form-wrap">
        <p class="eyebrow">{{ isRegister ? '新用户注册' : '账号登录' }}</p>
        <h2>{{ isRegister ? '创建你的成长档案' : '欢迎回到智职星' }}</h2>
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
      <p class="copyright">智职星 · 让职业成长有迹可循</p>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  position: relative;
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(460px, 1.05fr) minmax(420px, 0.95fr);
  overflow: hidden;
  background-color: var(--primary-soft);
  background-image: url('../../assets/login-background.jpg');
  background-position: left center;
  background-size: cover;
}

.login-page::before {
  position: absolute;
  z-index: 0;
  inset: 0;
  background: linear-gradient(90deg, rgb(5 16 35 / 38%) 0%, rgb(8 20 42 / 55%) 48%, #080f1f 100%);
  content: '';
  pointer-events: none;
}

.login-hero {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  padding: clamp(56px, 8vw, 120px);
  color: #fff;
  background: linear-gradient(90deg, rgb(6 38 92 / 60%) 0%, rgb(11 65 139 / 22%) 66%, transparent 100%);
}

.hero-content {
  max-width: 590px;
}

.login-brand {
  display: flex;
  width: fit-content;
  align-items: center;
  gap: 14px;
  margin-bottom: 40px;
  padding: 12px 18px 12px 12px;
  border-radius: 8px;
  background: var(--surface);
  box-shadow: 0 12px 30px rgb(5 31 75 / 16%);
}

.login-brand img {
  display: block;
  width: 62px;
  height: 62px;
  object-fit: contain;
}

.login-brand-copy {
  display: grid;
  gap: 3px;
}

.login-brand-copy strong {
  color: var(--muted);
  font-size: 28px;
  line-height: 1.1;
  letter-spacing: 4px;
}

.login-brand-copy small {
  color: var(--muted);
  font-size: 11px;
  letter-spacing: 2px;
}

.hero-content h1 {
  margin: 0;
  font-size: clamp(36px, 3.7vw, 54px);
  line-height: 1.3;
  letter-spacing: -1px;
  text-shadow: 0 2px 18px rgb(5 31 75 / 24%);
}

.hero-desc {
  max-width: 520px;
  margin: 24px 0 46px;
  color: #d8e6f7;
  font-size: 16px;
  line-height: 1.85;
}

.journey {
  display: flex;
  align-items: center;
  max-width: 580px;
  color: #b8cee8;
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
  background: #7599c3;
}

.login-panel {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  padding: 64px;
}

.form-wrap {
  width: min(100%, 420px);
  padding: 40px;
  border: 1px solid rgb(191 219 254 / 78%);
  border-radius: 16px;
  background: var(--surface);
  box-shadow: 0 28px 72px rgb(23 49 92 / 15%);
  backdrop-filter: blur(18px);
}

.form-wrap h2 {
  margin: 0;
  color: var(--text);
  font-size: 30px;
}

.form-subtitle {
  margin: 10px 0 34px;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.7;
}

.form-assist {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 20px;
  color: var(--muted);
  font-size: 12px;
}

.submit-button {
  width: 100%;
  height: 46px;
  border-radius: 7px;
}

.switch-mode {
  margin-top: 24px;
  color: var(--muted);
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
  color: var(--muted);
  font-size: 11px;
  text-shadow: none;
}

:deep(.el-form-item__label) {
  color: var(--text);
  font-weight: 600;
}

:deep(.el-input__wrapper) {
  border-radius: 7px;
  box-shadow: 0 0 0 1px var(--line) inset;
}

@media (max-width: 880px) {
  .login-page {
    grid-template-columns: 1fr;
    overflow: visible;
    background: var(--page);
  }

  .login-page::before {
    display: none;
  }

  .login-hero {
    min-height: 350px;
    padding: 50px 9vw;
    background-color: var(--primary);
    background-image:
      linear-gradient(90deg, rgb(5 38 91 / 68%) 0%, rgb(14 72 150 / 25%) 72%, rgb(239 246 255 / 8%) 100%),
      url('../../assets/login-background.jpg');
    background-position: left center;
    background-size: cover;
  }

  .login-brand { margin-bottom: 22px; }

  .login-panel {
    min-height: 590px;
    padding: 55px 24px 80px;
    background: var(--surface);
  }

  .form-wrap {
    padding: 0;
    border: 0;
    border-radius: 0;
    background: transparent;
    box-shadow: none;
    backdrop-filter: none;
  }
}

@media (max-width: 640px) {
  .login-hero {
    min-height: 0;
    padding: 26px 24px;
  }

  .hero-content {
    display: grid;
    grid-template-columns: 74px minmax(0, 1fr);
    align-items: center;
    gap: 0 16px;
  }

  .login-brand {
    width: 74px;
    height: 70px;
    grid-row: span 2;
    gap: 0;
    margin: 0;
    padding: 5px;
  }

  .login-brand img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }

  .login-brand-copy { display: none; }

  .hero-content h1 {
    font-size: 22px;
    line-height: 1.4;
    letter-spacing: 0;
  }

  .hero-desc {
    grid-column: 1 / -1;
    margin: 14px 0 0;
    font-size: 13px;
    line-height: 1.7;
  }

  .journey {
    display: none;
  }

  .login-panel {
    min-height: 0;
    padding-top: 36px;
  }
}
</style>
