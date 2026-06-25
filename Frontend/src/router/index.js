import { createRouter, createWebHistory } from 'vue-router'
import registerRoutes from './modules/register'
import loginRoutes from './modules/login'
import mainRoutes from './modules/main'
import adminRoutes from './modules/admin'

const ShareView = () => import('@/views/ShareView.vue')
const AccountRestoreView = () => import('@/views/AccountRestoreView.vue')
const ResetPasswordView = () => import('@/views/ResetPasswordView.vue')
const AiAnalysisView = () => import('@/views/ai/AiAnalysis.vue')


const routes = [
  ...registerRoutes,
  ...loginRoutes,
  ...mainRoutes, // 必须
  ...adminRoutes, // 管理员路由

  {
    path: '/pay',
    name: 'Pay',
    component: () => import('@/views/PayView.vue'),//LYL新增
    meta: { requiresAuth: true }
  },
  {
    path: '/pay/result',
    name: 'PayResult',
    component: () => import('@/views/PayView.vue'),  // 复用同一个页面组件
    meta: { requiresAuth: true }
  },
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/change-password',      // LYL增
    name: 'ChangePassword',
    component: () => import('@/views/ChangePasswordView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: ResetPasswordView,
    meta: { requiresAuth: false }
  },
  {
    path: '/account-restore',
    name: 'AccountRestore',
    component: AccountRestoreView,
    meta: { requiresAuth: false }
  },
  {
    path: '/share/:link',
    name: 'Share',
    component: ShareView,
    meta: { requiresAuth: false }
  },
  {
    path: '/ai/analysis',
    name: 'AiAnalysis',
    component: AiAnalysisView,
    meta: { requiresAuth: true }
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth) {
    if (!token) {
      next('/login')
      return
    }

    if (to.meta.role !== undefined) {
      const userStr = localStorage.getItem('user')
      if (!userStr) {
        next('/login')
        return
      }
      const user = JSON.parse(userStr)
      if (user.role !== to.meta.role) {
        next('/')
        return
      }
    }
  }

  next()
})

export default router
