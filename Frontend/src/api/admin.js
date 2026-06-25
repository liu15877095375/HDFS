import api from '@/utils/api'

export function listUsers() {
  return api.get('/admin/storage/users')
}

export function configStorageQuota(data) {
  return api.post('/admin/storage/quota', data)
}
