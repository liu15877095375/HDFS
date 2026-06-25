const LoginView = () => import('@/views/LoginView.vue')

export default [
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
  },
]
