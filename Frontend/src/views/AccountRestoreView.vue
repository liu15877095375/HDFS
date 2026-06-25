<template>
  <div class="restore-page">
    <div class="restore-card">
      <h2 class="title">🔄 恢复账户</h2>
      <p class="subtitle">如果您误注销了账户，可以在7天内通过邮箱恢复</p>

      <el-form v-if="!success" :model="form" class="restore-form" @submit.prevent="handleRestore">
        <el-form-item label="注册邮箱" label-width="80px">
          <el-input 
            v-model="form.email" 
            type="email"
            placeholder="请输入注册时的邮箱"
            :disabled="loading"
          />
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            size="large"
            class="submit-btn"
            :loading="loading"
            @click="handleRestore"
            :disabled="!form.email"
          >
            提交恢复申请
          </el-button>
        </el-form-item>
      </el-form>

      <div v-else class="success-section">
        <div class="success-icon">✅</div>
        <h3>账户恢复成功！</h3>
        <p>您的账户已成功恢复，可以重新登录了</p>
        <el-button type="primary" @click="goToLogin">返回登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/utils/api'

const router = useRouter()
const loading = ref(false)
const success = ref(false)

const form = ref({
  email: ''
})

async function handleRestore() {
  if (!form.value.email.trim()) {
    ElMessage.warning('请输入邮箱')
    return
  }

  loading.value = true
  try {
    await api.post('/api/account/restore', { email: form.value.email })
    success.value = true
    ElMessage.success('账户恢复成功')
  } catch (e) {
    const msg = e.response?.data?.message || '恢复失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

function goToLogin() {
  router.push('/login')
}
</script>

<style scoped>
.restore-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 48px 24px;
  position: relative;
  overflow: hidden;
}

.restore-page::before {
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

.restore-page::after {
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

.restore-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-radius: var(--radius-xl);
  padding: 52px 48px;
  width: 460px;
  box-shadow:
    0 50px 100px -20px rgba(0, 0, 0, 0.25),
    0 30px 60px -30px rgba(0, 0, 0, 0.3);
  text-align: center;
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
  margin: 0 0 8px 0;
}

.subtitle {
  color: var(--text-secondary);
  font-size: 15px;
  margin: 0 0 40px 0;
  font-weight: 400;
}

.restore-form {
  text-align: left;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 14px;
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
  margin-top: 20px;
  font-size: 16px;
  font-weight: 600;
  padding: 14px 0;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all var(--transition-normal);
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.submit-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.5);
}

.submit-btn:active {
  transform: translateY(-1px);
}

.success-section {
  padding: 32px 0;
}

.success-icon {
  font-size: 80px;
  margin-bottom: 20px;
  animation: float 3s ease-in-out infinite;
}

.success-section h3 {
  margin: 0 0 10px 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.success-section p {
  margin: 0 0 28px 0;
  color: var(--text-secondary);
  font-size: 15px;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: var(--radius-md);
  font-weight: 600;
  padding: 12px 28px;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: all var(--transition-normal);
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.5);
}
</style>