<template>
  <div class="reset-container">
    <div class="reset-card">
      <h2 class="title">忘记密码</h2>
      <p class="subtitle">通过邮箱验证码重置密码</p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleReset"
      >
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入注册时的邮箱"
            prefix-icon="Mail"
            clearable
          />
        </el-form-item>
        <el-form-item label="验证码" prop="verifyCode">
          <div class="verify-code-wrapper">
            <el-input
              v-model="form.verifyCode"
              placeholder="请输入验证码"
              prefix-icon="Key"
              clearable
            />
            <el-button
              type="primary"
              :disabled="!form.email || countdown > 0"
              @click="sendCode"
              class="send-code-btn"
            >
              {{ countdown > 0 ? countdown + 's' : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="请输入新密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            class="submit-btn"
          >
            重置密码
          </el-button>
        </el-form-item>
      </el-form>
      <div class="footer-link">
        记起来密码了？<router-link to="/login">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendResetCode, resetPassword } from '@/api/user'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const countdown = ref(0)

const form = reactive({
  email: '',
  verifyCode: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { length: 6, message: '验证码必须是6位数字', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

function validateConfirmPassword(rule, value, callback) {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

async function sendCode() {
  if (!form.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  try {
    await sendResetCode({ email: form.email })
    ElMessage.success('验证码已发送，请注意查收')
    startCountdown()
  } catch (error) {
    ElMessage.error('发送失败，请稍后重试')
  }
}

function startCountdown() {
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

async function handleReset() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await resetPassword({
      email: form.email,
      verifyCode: form.verifyCode,
      newPassword: form.newPassword
    })
    // 清空登录状态，确保安全
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    ElMessage.success('密码重置成功，请使用新密码登录')
    router.push('/login')
  } catch (error) {
    ElMessage.error('重置失败，请检查验证码是否正确')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.reset-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.reset-container::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
  border-radius: 50%;
  top: -150px;
  right: -150px;
  animation: float 8s ease-in-out infinite;
}

.reset-container::after {
  content: '';
  position: absolute;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(255,255,255,0.08) 0%, transparent 70%);
  border-radius: 50%;
  bottom: -100px;
  left: -100px;
  animation: float 10s ease-in-out infinite reverse;
}

.reset-card {
  width: 460px;
  padding: 52px 48px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-radius: var(--radius-xl);
  box-shadow:
    0 50px 100px -20px rgba(0, 0, 0, 0.25),
    0 30px 60px -30px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
  animation: fadeInUp 0.6s ease-out;
}

.title {
  font-size: 36px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-align: center;
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.subtitle {
  font-size: 15px;
  color: var(--text-secondary);
  text-align: center;
  margin-bottom: 40px;
  font-weight: 400;
}

.verify-code-wrapper {
  display: flex;
  gap: 12px;
}

.send-code-btn {
  flex-shrink: 0;
  width: 120px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  font-weight: 600;
  transition: all var(--transition-normal);
}

.send-code-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.send-code-btn:disabled {
  opacity: 0.6;
  transform: none;
  box-shadow: none;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 14px;
  margin-bottom: 8px;
}

:deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  padding: 8px 12px;
  box-shadow: 0 0 0 1px var(--border-light);
  transition: all var(--transition-fast);
  background: var(--bg-secondary);
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--primary-300);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--primary-400);
  background: var(--bg-primary);
}

.submit-btn {
  width: 100%;
  font-size: 16px;
  font-weight: 600;
  padding: 14px 0;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all var(--transition-normal);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  margin-top: 8px;
}

.submit-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.5);
}

.submit-btn:active {
  transform: translateY(-1px);
}

.footer-link {
  text-align: center;
  margin-top: 28px;
  font-size: 14px;
  color: var(--text-secondary);
}

.footer-link a {
  color: var(--primary-600);
  text-decoration: none;
  font-weight: 600;
  transition: color var(--transition-fast);
}

.footer-link a:hover {
  color: var(--primary-700);
}
</style>
