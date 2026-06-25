import api from '@/utils/api'

/**
 * 发送邮箱验证码
 */
export function sendEmailCodeApi(email) {
  return api.post('/api/send-verify-code', { email })
}

/**
 * 用户注册
 */
export function registerApi(data) {
  return api.post('/api/register', data)
}
