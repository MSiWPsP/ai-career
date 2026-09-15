<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ArrowDown,
  Bell,
  Camera,
  ChatDotRound,
  Check,
  Compass,
  DataAnalysis,
  House,
  List,
  Operation,
  SwitchButton,
  Tickets,
  UserFilled,
} from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { getProfileCompletion } from '../api/profile'
import { getTasks } from '../api/task'
import { getCurrentUser } from '../api/user'
import AvatarUploadDialog from '../components/AvatarUploadDialog.vue'
import { useAuthStore } from '../stores/auth'
import type { CareerTask, UserInfo } from '../types/api'

interface ShellNotification {
  id: string
  title: string
  description: string
  path: string
  read: boolean
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const user = ref<UserInfo | null>(null)
const sidebarOpen = ref(false)
const notificationVisible = ref(false)
const notifications = ref<ShellNotification[]>([])
const avatarDialog = ref<InstanceType<typeof AvatarUploadDialog>>()

const pageTitle = computed(() => String(route.meta.title || '首页'))
const nickname = computed(() => user.value?.nickname || authStore.session?.nickname || '同学')
const unreadCount = computed(() => notifications.value.filter((item) => !item.read).length)

onMounted(loadShellData)

async function loadShellData() {
  const [userResult, completionResult, taskResult] = await Promise.allSettled([
    getCurrentUser(),
    getProfileCompletion({ silent: true }),
    getTasks(undefined, { silent: true }),
  ])

  if (userResult.status === 'fulfilled') {
    user.value = userResult.value
    authStore.syncCurrentUser(userResult.value)
  }

  const items: Omit<ShellNotification, 'read'>[] = []
  if (completionResult.status === 'fulfilled' && completionResult.value.score < 100) {
    items.push({
      id: 'profile-incomplete',
      title: '职业画像待完善',
      description: `当前完整度 ${completionResult.value.score}%，补充信息后可获得更准确的职业建议。`,
      path: '/profile',
    })
  }
  if (taskResult.status === 'fulfilled') {
    appendTaskNotifications(items, taskResult.value)
  }

  const readIds = readNotificationIds()
  notifications.value = items.map((item) => ({ ...item, read: readIds.has(item.id) }))
}

function appendTaskNotifications(items: Omit<ShellNotification, 'read'>[], tasks: CareerTask[]) {
  const today = new Date().toISOString().slice(0, 10)
  const overdueCount = tasks.filter((task) => task.status < 2 && task.deadline && task.deadline < today).length
  const activeCount = tasks.filter((task) => task.status === 1).length
  if (overdueCount > 0) {
    items.push({
      id: `overdue-tasks-${overdueCount}`,
      title: `${overdueCount} 项任务已超过截止日期`,
      description: '建议重新安排优先级，先完成最关键的成长任务。',
      path: '/tasks',
    })
  } else if (activeCount > 0) {
    items.push({
      id: `active-tasks-${activeCount}`,
      title: `${activeCount} 项任务正在进行`,
      description: '保持当前节奏，完成后记得更新任务状态。',
      path: '/tasks',
    })
  }
}

function notificationStorageKey() {
  const identity = user.value?.username || String(authStore.session?.userId || 'anonymous')
  return `ai-career:read-notifications:${identity}`
}

function readNotificationIds() {
  try {
    const stored = localStorage.getItem(notificationStorageKey())
    return new Set<string>(stored ? JSON.parse(stored) : [])
  } catch {
    return new Set<string>()
  }
}

function persistNotificationState() {
  const ids = notifications.value.filter((item) => item.read).map((item) => item.id)
  localStorage.setItem(notificationStorageKey(), JSON.stringify(ids))
}

function openNotification(item: ShellNotification) {
  item.read = true
  persistNotificationState()
  notificationVisible.value = false
  navigate(item.path)
}

function markAllNotificationsRead() {
  notifications.value.forEach((item) => { item.read = true })
  persistNotificationState()
}

function navigate(path: string) {
  sidebarOpen.value = false
  void router.push(path)
}

function handleUserCommand(command: string | number | object) {
  if (command === 'avatar') avatarDialog.value?.open()
  if (command === 'profile') navigate('/profile')
  if (command === 'logout') logout()
}

function handleAvatarUploaded(updatedUser: UserInfo) {
  user.value = updatedUser
  authStore.syncCurrentUser(updatedUser)
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
      <button class="brand" type="button" aria-label="返回首页" @click="navigate('/dashboard')">
        <span class="brand-mark"><img src="/ai-career-mark.png" alt="" /></span>
        <span>
          <strong>AI职途</strong>
          <small>智能职业成长平台</small>
        </span>
      </button>

      <nav class="nav-list">
        <button class="nav-item" :class="{ active: route.path === '/dashboard' }" :aria-current="route.path === '/dashboard' ? 'page' : undefined" @click="navigate('/dashboard')">
          <el-icon><House /></el-icon> 首页
        </button>
        <button class="nav-item" :class="{ active: route.path === '/profile' }" :aria-current="route.path === '/profile' ? 'page' : undefined" @click="navigate('/profile')">
          <el-icon><UserFilled /></el-icon> 我的职业画像
        </button>

        <p class="nav-group">AI 助手</p>
        <button class="nav-item" :class="{ active: route.path.startsWith('/career') }" :aria-current="route.path.startsWith('/career') ? 'page' : undefined" @click="navigate('/career/chat')">
          <el-icon><Compass /></el-icon> AI职业规划师
        </button>
        <button class="nav-item" :class="{ active: route.path.startsWith('/interview/setup') || route.path.startsWith('/interview/session') }" :aria-current="route.path.startsWith('/interview/setup') || route.path.startsWith('/interview/session') ? 'page' : undefined" @click="navigate('/interview/setup')">
          <el-icon><ChatDotRound /></el-icon> AI模拟面试官
        </button>

        <p class="nav-group">成长中心</p>
        <button class="nav-item" :class="{ active: route.path === '/tasks' }" :aria-current="route.path === '/tasks' ? 'page' : undefined" @click="navigate('/tasks')">
          <el-icon><List /></el-icon> 我的学习计划
        </button>
        <button class="nav-item" :class="{ active: route.path === '/ability' }" :aria-current="route.path === '/ability' ? 'page' : undefined" @click="navigate('/ability')">
          <el-icon><DataAnalysis /></el-icon> 能力画像
        </button>
        <button class="nav-item" :class="{ active: route.path === '/interviews' || route.path.includes('/report') }" :aria-current="route.path === '/interviews' || route.path.includes('/report') ? 'page' : undefined" @click="navigate('/interviews')">
          <el-icon><Tickets /></el-icon> 面试记录
        </button>
      </nav>

      <div class="sidebar-user">
        <button class="sidebar-avatar" title="更换头像" @click="avatarDialog?.open()">
          <el-avatar :size="38" :src="user?.avatar">{{ nickname.slice(0, 1) }}</el-avatar>
          <span class="sidebar-avatar-edit"><el-icon><Camera /></el-icon></span>
        </button>
        <div>
          <strong>{{ nickname }}</strong>
          <span>持续成长中</span>
        </div>
        <button class="logout-button" title="退出登录" @click="logout">
          <el-icon><SwitchButton /></el-icon>
        </button>
      </div>
    </aside>

    <section class="shell-content">
      <header class="topbar">
        <button class="mobile-menu" title="打开菜单" @click="sidebarOpen = true">
          <el-icon><Operation /></el-icon>
        </button>
        <div>
          <span class="breadcrumb">AI职途 /</span>
          <strong>{{ pageTitle }}</strong>
        </div>
        <div class="topbar-actions">
          <el-popover
            v-model:visible="notificationVisible"
            placement="bottom-end"
            :width="360"
            trigger="click"
            popper-class="notification-popper"
          >
            <template #reference>
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="9">
                <button class="icon-button" title="通知">
                  <el-icon><Bell /></el-icon>
                </button>
              </el-badge>
            </template>
            <div class="notification-panel">
              <header>
                <div><strong>消息通知</strong><span>{{ unreadCount }} 条未读</span></div>
                <button v-if="unreadCount" @click="markAllNotificationsRead">全部已读</button>
              </header>
              <div v-if="notifications.length" class="notification-list">
                <button
                  v-for="item in notifications"
                  :key="item.id"
                  :class="{ read: item.read }"
                  @click="openNotification(item)"
                >
                  <span class="notification-dot" />
                  <span><strong>{{ item.title }}</strong><small>{{ item.description }}</small></span>
                </button>
              </div>
              <div v-else class="notification-empty">
                <el-icon><Check /></el-icon>
                <strong>暂时没有新提醒</strong>
                <span>新的成长任务和系统提醒会显示在这里。</span>
              </div>
            </div>
          </el-popover>

          <el-dropdown trigger="click" @command="handleUserCommand">
            <button class="avatar-menu-trigger">
              <el-avatar :size="34" :src="user?.avatar">{{ nickname.slice(0, 1) }}</el-avatar>
              <span>{{ nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="avatar" :icon="Camera">更换头像</el-dropdown-item>
                <el-dropdown-item command="profile" :icon="UserFilled">职业画像</el-dropdown-item>
                <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="main-content">
        <router-view />
      </main>
    </section>

    <AvatarUploadDialog
      ref="avatarDialog"
      :avatar="user?.avatar"
      :nickname="nickname"
      @uploaded="handleAvatarUploaded"
    />
  </div>
</template>
