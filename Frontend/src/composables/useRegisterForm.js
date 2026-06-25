import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useRegisterStore } from '@/stores/register'
import { ElMessage } from 'element-plus'

export function useRegisterForm() {
  const router = useRouter()
  const registerStore = useRegisterStore()
  const formRef = ref(null)

  // ---------- 表单数据 ----------
  const form = reactive({
    username: '',
    email: '',
    verifyCode: '',
    password: '',
    confirmPassword: '',
  })

  // ---------- 验证码倒计时 ----------
  const countdown = ref(0)
  const sending = ref(false)

  const codeBtnDisabled = computed(() => sending.value || countdown.value > 0)
  const codeBtnText = computed(() => {
    if (sending.value) return '发送中...'
    if (countdown.value > 0) return `${countdown.value}秒后重发`
    return '发送验证码'
  })

  function startCountdown() {
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  }

  // ---------- 发送验证码 ----------
  async function sendVerifyCode() {
    const email = form.email
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      ElMessage.warning('请先输入正确的邮箱地址')
      return
    }
    sending.value = true
    try {
      await registerStore.sendEmailCode(email)
      ElMessage.success('验证码已发送至邮箱')
      startCountdown()
    } catch {
      // 错误在拦截器中统一提示
    } finally {
      sending.value = false
    }
  }

  // ---------- 表单校验规则 ----------
  const validateConfirmPassword = (rule, value, callback) => {
    if (value !== form.password) {
      callback(new Error('两次输入的密码不一致'))
    } else {
      callback()
    }
  }

  const rules = {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
      { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' },
    ],
    email: [
      { required: true, message: '请输入邮箱', trigger: 'blur' },
      { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' },
    ],
    verifyCode: [
      { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
      { len: 6, message: '验证码为6位数字', trigger: 'blur' },
      { pattern: /^\d{6}$/, message: '验证码格式错误', trigger: 'blur' },
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' },
    ],
    confirmPassword: [
      { required: true, message: '请再次输入密码', trigger: 'blur' },
      { validator: validateConfirmPassword, trigger: 'blur' },
    ],
  }

  // ---------- 注册提交 ----------
  async function handleRegister() {
    if (!formRef.value) return
    try {
      await formRef.value.validate()
    } catch {
      return
    }
    const success = await registerStore.register({
      username: form.username,
      email: form.email,
      verifyCode: form.verifyCode,
      password: form.password,
    })
    if (success) {
      ElMessage.success('注册成功，即将跳转到登录页')
      setTimeout(() => router.push('/login'), 1500)
    }
  }

  // 导出模板所需的一切
  return {
    formRef,
    form,
    rules,
    codeBtnDisabled,
    codeBtnText,
    sending,
    sendVerifyCode,
    handleRegister,
    registerStore,
  }
}
