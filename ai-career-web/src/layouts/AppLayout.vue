<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCurrentUser } from '../api/user'
import { useAuthStore } from '../stores/auth'
import type { UserInfo } from '../types/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const user = ref<UserInfo | null>(null)
const sidebarOpen = ref(false)

const pageTitle = computed(() => String(route.meta.title || '首页'))
const nickname = computed(() => user.value?.nickname || authStore.session?.nickname || '同学')

onMounted(async () => {
  try {
    user.value = await getCurrentUser()
  } catch {
    // The global request handler has already surfaced the error.
  }
})

function navigate(path: string) {
  sidebarOpen.value = false
  void router.push(path)
}

function logout() {
  authStore.logout()
  void router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <div v-if="sidebarOpen" class="sidebar-mask" @click="sidebarOpen = false" />
    <aside class="sidebar" :class="{ 'is-open': sidebarOpen }">
      <div class="brand" @click="navigate('/dashboard')">
        <span class="brand-mark">途</span>
        <span>
          <strong>AI职途</strong>
          <small>智能职业成长平台</small>
        </span>
      </div>

      <nav class="nav-list">
        <button class="nav-item" :class="{ active: route.path === '/dashboard' }" @click="navigate('/dashboard')">
          <span>⌂</span> 首页
        </button>
        <button class="nav-item" :class="{ active: route.path === '/profile' }" @click="navigate('/profile')">
          <span>◉</span> 我的职业画像
        </button>

        <p class="nav-group">AI 助手</p>
        <button class="nav-item" :class="{ active: route.path.startsWith('/career') }" @click="navigate('/career/chat')">
          <span>✦</span> AI职业规划师
        </button>
        <button class="nav-item" :class="{ active: route.path.startsWith('/interview/setup') || route.path.startsWith('/interview/session') }" @click="navigate('/interview/setup')">
          <span>◌</span> AI模拟面试官
        </button>

        <p class="nav-group">成长中心</p>
        <button class="nav-item" :class="{ active: route.path === '/tasks' }" @click="navigate('/tasks')">
          <span>✓</span> 我的学习计划
        </button>
        <button class="nav-item" :class="{ active: route.path === '/ability' }" @click="navigate('/ability')">
          <span>⌁</span> 能力画像
        </button>
        <button class="nav-item" :class="{ active: route.path === '/interviews' || route.path.includes('/report') }" @click="navigate('/interviews')">
          <span>▤</span> 面试记录
        </button>
      </nav>

      <div class="sidebar-user">
        <el-avatar :size="38" :src="user?.avatar">{{ nickname.slice(0, 1) }}</el-avatar>
        <div>
          <strong>{{ nickname }}</strong>
          <span>持续成长中</span>
        </div>
        <button class="logout-button" title="退出登录" @click="logout">↗</button>
      </div>
    </aside>

    <section class="shell-content">
      <header class="topbar">
        <button class="mobile-menu" @click="sidebarOpen = true">☰</button>
        <div>
          <span class="breadcrumb">AI职途 /</span>
          <strong>{{ pageTitle }}</strong>
        </div>
        <div class="topbar-actions">
          <button class="icon-button" title="通知">○</button>
          <el-avatar :size="34" :src="user?.avatar">{{ nickname.slice(0, 1) }}</el-avatar>
          <span>{{ nickname }}</span>
        </div>
      </header>
      <main class="main-content">
        <router-view />
      </main>
    </section>
  </div>
</template>
