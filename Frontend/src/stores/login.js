import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi } from '@/api/login'

export const useAuthStore = defineStore('auth', () => {
  const loading = ref(false)
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  // 登录
  async function login({ account, password }) {
    loading.value = true
    try {
      const res = await loginApi({ account, password })
      // 响应拦截器已经提取了 response.data（Result对象）
      // 所以 res 就是 Result 对象，res.data 才是实际数据
      token.value = res.data.token
      userInfo.value = res.data.user
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(res.data.user))
      return true
    } catch {
      return false
    } finally {
      loading.value = false
    }
  }

  // 退出
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { loading, token, userInfo, login, logout }
})
