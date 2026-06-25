import api from '@/utils/api'

/**
 * 用户登录
 */
export function loginApi(data) {
  return api.post('/api/login', data)
}
