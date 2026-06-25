const MainView = () => import('@/views/MainView.vue')

export default [
  {
    path: '/main',
    name: 'Main',
    component: MainView,
    meta: { requiresAuth: true },
  },
]
