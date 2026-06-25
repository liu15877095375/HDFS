<template>
  <div class="admin-home">
    <div class="admin-card">
      <div class="admin-header">
        <h1>超级管理员控制台</h1>
        <el-button type="danger" size="small" @click="handleLogout">退出登录</el-button>
      </div>

      <el-divider />

      <div class="admin-info">
        <h3>管理员信息</h3>
        <el-form :model="adminInfo" label-width="120px" class="info-form">
          <!-- <el-form-item label="用户ID">
            <el-input :value="adminInfo.userId" disabled />
          </el-form-item> -->
          <el-form-item label="用户名">
            <el-input :value="adminInfo.username" disabled />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input :value="adminInfo.email" disabled />
          </el-form-item>
          <el-form-item label="角色">
            <el-tag type="danger">超级管理员</el-tag>
          </el-form-item>
          <!-- <el-form-item label="存储配额">
            <el-input :value="formatSize(adminInfo.storageQuota)" disabled />
          </el-form-item>
          <el-form-item label="已用空间">
            <el-input :value="formatSize(adminInfo.usedStorage)" disabled />
          </el-form-item> -->
        </el-form>
      </div>

      <el-divider />

      <div class="admin-actions">
        <h3>管理功能</h3>
        <el-button
          type="primary"
          size="large"
          @click="goToStorage"
          class="action-btn"
        >
          <span class="btn-icon">📦</span>
          资源管理
        </el-button>

        <el-button
          type="success"
          style="margin-left: 0;"
          size="large"
          @click="goToStatistics"
          class="action-btn"
        >
          <span class="btn-icon">📊</span>
          用户行为数据统计
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfoApi } from '@/api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()

const adminInfo = reactive({
  userId: null,
  username: '',
  email: '',
  role: 2,
  storageQuota: 0,
  usedStorage: 0
})

const formatSize = (bytes) => {
  if (!bytes && bytes !== 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(1) + ' ' + units[i]
}

const loadAdminInfo = async () => {
  try {
    const res = await getUserInfoApi()
    Object.assign(adminInfo, res.data)
  } catch {
    ElMessage.error('获取管理员信息失败')
  }
}

const goToStorage = () => {
  router.push('/admin/storage')
}

const goToStatistics = () => {
  router.push('/admin/statistics')
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}

onMounted(() => {
  loadAdminInfo()
})
</script>

<style scoped>
.admin-home {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 48px 24px;
  position: relative;
  overflow: hidden;
}

.admin-home::before {
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

.admin-home::after {
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

.admin-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-radius: var(--radius-xl);
  box-shadow:
    0 50px 100px -20px rgba(0, 0, 0, 0.25),
    0 30px 60px -30px rgba(0, 0, 0, 0.3);
  padding: 48px;
  width: 550px;
  position: relative;
  z-index: 1;
  animation: fadeInUp 0.6s ease-out;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.admin-header h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

:deep(.el-divider) {
  margin: 24px 0;
  background: var(--border-light);
}

.info-form {
  margin-top: 20px;
}

.admin-info {
  margin-bottom: 32px;
}

.admin-info h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.admin-actions {
  margin-top: 20px;
}

.admin-actions h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 14px;
}

:deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  padding: 8px 12px;
  box-shadow: 0 0 0 1px var(--border-light);
  transition: all var(--transition-fast);
  background: var(--bg-secondary);
}

:deep(.el-input__wrapper.is-disabled) {
  background: var(--bg-secondary);
  opacity: 0.8;
}

:deep(.el-button) {
  border-radius: var(--radius-md);
  font-weight: 600;
  transition: all var(--transition-normal);
}

:deep(.el-button--danger) {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}

:deep(.el-button--danger:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
}

.action-btn {
  width: 100%;
  height: 64px;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}

:deep(.el-button--primary.action-btn) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

:deep(.el-button--primary.action-btn:hover) {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.5);
}

:deep(.el-button--success.action-btn) {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(16, 185, 129, 0.4);
}

:deep(.el-button--success.action-btn:hover) {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(16, 185, 129, 0.5);
}

.btn-icon {
  font-size: 24px;
}
</style>
