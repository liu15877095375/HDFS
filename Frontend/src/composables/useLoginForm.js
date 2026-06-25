import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/login'
import { ElMessage } from 'element-plus'

export function useLoginForm() {
  const router = useRouter()
  const authStore = useAuthStore()
  const formRef = ref(null)

  const form = reactive({
    account: '', // 用户名或邮箱
    password: '',
  })

  const rules = {
    account: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  }

  async function handleLogin() {
    if (!formRef.value) return
    try {
      await formRef.value.validate()
    } catch {
      return
    }
    const success = await authStore.login({
      account: form.account,
      password: form.password,
    })
    if (success) {
        ElMessage.success('登录成功')
        // 检查是否有待转存的分享
        const pendingShareSave = localStorage.getItem('pendingShareSave')
        if (pendingShareSave) {
          try {
            const { shareLink, extractCode } = JSON.parse(pendingShareSave)
            // 跳转到分享页面，自动转存
            const query = extractCode ? { extractCode, autoSave: 'true' } : { autoSave: 'true' }
            router.push({ path: `/share/${shareLink}`, query })
            return
          } catch {
            // 解析失败，继续正常跳转
          }
        }
        
        // 直接从状态管理获取用户信息，更可靠
        const user = authStore.userInfo
        if (user && user.role === 2) {
          router.push('/admin')
        } else {
          router.push('/main')
        }
      }
  }

  return {
    formRef,
    form,
    rules,
    handleLogin,
    authStore,
  }
}
