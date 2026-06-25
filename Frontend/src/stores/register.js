import { defineStore } from 'pinia'
import { ref } from 'vue'
import { sendEmailCodeApi, registerApi } from '@/api/register'

export const useRegisterStore = defineStore('register', () => {
  const loading = ref(false)

  // 发送邮箱验证码
  async function sendEmailCode(email) {
    loading.value = true
    try {
      await sendEmailCodeApi(email)
      return true
    } catch {
      return false
    } finally {
      loading.value = false
    }
  }

  // 注册
  async function register(form) {
    loading.value = true
    try {
      await registerApi(form)
      return true
    } catch {
      return false
    } finally {
      loading.value = false
    }
  }

  return { loading, sendEmailCode, register }
})
