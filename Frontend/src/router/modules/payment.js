export default {
    path: '/pay',
    name: 'Pay',
    component: () => import('@/views/PayView.vue'),
    meta: { requiresAuth: true }
  }