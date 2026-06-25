import api from '@/utils/api'

/** 获取当前登录用户信息 */
export function getUserInfoApi() {
  return api.get('/api/user/info')
}

/** 修改密码（需要原密码） */
export function changePassword(data) {
  return api.post('/api/user/change-password', data)
}

/** 发送重置密码验证码 */
export function sendResetCode(data) {
  return api.post('/api/send-reset-code', data)
}

/** 使用邮箱验证码重置密码（无需原密码） */
export function resetPassword(data) {
  return api.post('/api/reset-password', data)
}
