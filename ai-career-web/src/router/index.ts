import { createRouter, createWebHistory } from 'vue-router'
import { tokenStorage } from '../utils/storage'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/login/index.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: () => import('../layouts/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/dashboard' },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/dashboard/index.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('../views/profile/index.vue'),
          meta: { title: '我的职业画像' },
        },
        {
          path: 'career/chat',
          name: 'career-chat',
          component: () => import('../views/career/chat.vue'),
          meta: { title: 'AI职业规划师' },
        },
        {
          path: 'career/plan',
          name: 'career-plan',
          component: () => import('../views/career/plan.vue'),
          meta: { title: '职业规划报告' },
        },
        {
          path: 'tasks',
          name: 'tasks',
          component: () => import('../views/task/index.vue'),
          meta: { title: '我的学习计划' },
        },
        {
          path: 'interview/setup',
          name: 'interview-setup',
          component: () => import('../views/interview/setup.vue'),
          meta: { title: '模拟面试配置' },
        },
        {
          path: 'interview/session/:id?',
          name: 'interview-session',
          component: () => import('../views/interview/chat.vue'),
          meta: { title: 'AI模拟面试' },
        },
        {
          path: 'interviews',
          name: 'interview-history',
          component: () => import('../views/interview/history.vue'),
          meta: { title: '面试记录' },
        },
        {
          path: 'interview/:id/report',
          name: 'interview-report',
          component: () => import('../views/interview/report.vue'),
          meta: { title: '面试报告' },
        },
        {
          path: 'ability',
          name: 'ability',
          component: () => import('../views/ability/index.vue'),
          meta: { title: '能力画像' },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

router.beforeEach((to) => {
  document.title = `${String(to.meta.title || 'AI职途')} · AI职途`
  if (to.meta.requiresAuth && !tokenStorage.get()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && tokenStorage.get()) return { name: 'dashboard' }
  return true
})

export default router
