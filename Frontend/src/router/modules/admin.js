// src/router/modules/admin.js
export default [
  {
    path: '/admin',
    name: 'AdminHome',
    component: () => import('@/views/AdminHome.vue'),
    meta: { requiresAuth: true, role: 2 }
  },
  {
    path: '/admin/storage',
    name: 'AdminStorageQuota',
    component: () => import('@/views/AdminStorageQuota.vue'),
    meta: { requiresAuth: true, role: 2 }
  },
  {
    path: '/admin/statistics',
    name: 'AdminStatistics',
    component: () => import('@/views/AdminStatistics.vue'),
    meta: { requiresAuth: true, role: 2 }
  }
]
