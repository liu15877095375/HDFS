<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="title">欢迎回来</h2>
      <p class="subtitle">登录你的网盘账号</p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名或邮箱" prop="account">
          <el-input
            v-model="form.account"
            placeholder="请输入用户名或邮箱"
            prefix-icon="User"
            clearable
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            :loading="authStore.loading"
            class="submit-btn"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="footer-link">还没有账号？<router-link to="/register">立即注册</router-link></div>
      <div class="forgot-link"><router-link to="/reset-password">忘记密码？</router-link></div>
      <div class="restore-link"><router-link to="/account-restore">恢复已注销账户</router-link></div>
    </div>
  </div>
</template>

<script setup>
import { useLoginForm } from '@/composables/useLoginForm'

const { formRef, form, rules, handleLogin, authStore } = useLoginForm()
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

/* 装饰背景元素 */
.login-container::before {
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

.login-container::after {
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

.login-card {
  width: 460px;
  padding: 56px 48px;
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
  background: var(--bg-gradient);
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

/* 表单样式增强 */
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

:deep(.el-input__inner) {
  font-size: 15px;
  color: var(--text-primary);
}

.submit-btn {
  width: 100%;
  font-size: 16px;
  font-weight: 600;
  padding: 14px 0;
  border-radius: var(--radius-md);
  background: var(--bg-gradient);
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

.forgot-link {
  text-align: center;
  margin-top: 14px;
  font-size: 13px;
}

.forgot-link a {
  color: var(--text-secondary);
  text-decoration: none;
  transition: color var(--transition-fast);
}

.forgot-link a:hover {
  color: var(--primary-600);
}

.restore-link {
  text-align: center;
  margin-top: 14px;
  font-size: 13px;
}

.restore-link a {
  color: var(--text-muted);
  text-decoration: none;
  transition: color var(--transition-fast);
}

.restore-link a:hover {
  color: var(--primary-600);
}
</style>
