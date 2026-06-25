<template>
  <div class="admin-container">
    <div class="header">
      <h2>资源管理</h2>
      <el-button type="success" @click="openBulkDialog">
        <el-icon><Upload /></el-icon>
        配置套餐
      </el-button>
    </div>
    <el-divider />

    <!-- 套餐配置列表 -->
    <div class="package-section">
      <h3>商业套餐配置</h3>
      <el-table :data="packageList" border stripe style="width: 100%; margin-bottom: 24px;">
        <el-table-column label="套餐名称" prop="packageName" />
        <el-table-column label="对应角色">
          <template #default="scope">
            <el-tag :type="getRoleTagType(scope.row.role)">
              {{ getRoleText(scope.row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="存储限额">
          <template #default="scope">
            {{ formatBytes(scope.row.storageQuota) }}
          </template>
        </el-table-column>
        <el-table-column label="下载限速">
          <template #default="scope">
            {{ formatSpeed(scope.row.downloadSpeedLimit) }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间">
          <template #default="scope">
            {{ scope.row.modifyTime }}
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 用户列表 -->
    <el-table :data="userList" border stripe style="width: 100%">
      <el-table-column label="用户ID" prop="userId" />
      <el-table-column label="用户名" prop="username" />
      <el-table-column label="邮箱" prop="email" />
      <el-table-column label="角色">
        <template #default="scope">
          <el-tag :type="getRoleTagType(scope.row.role)">
            {{ getRoleText(scope.row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="存储配额">
        <template #default="scope">
          {{ formatBytes(scope.row.storageQuota) }}
        </template>
      </el-table-column>
      <el-table-column label="已用空间">
        <template #default="scope">
          {{ formatBytes(scope.row.usedStorage) }}
        </template>
      </el-table-column>

    </el-table>

    <!-- 批量配置弹窗 -->
    <el-dialog v-model="bulkDialogVisible" title="统一配置商业套餐" width="550px">
      <div style="margin-bottom: 16px; padding: 12px; background: #fffbe6; border-radius: 8px;">
        <el-icon class="warning-icon" style="color: #faad14; margin-right: 8px;">
          <Warning />
        </el-icon>
        <span style="color: #d48806;">商业套餐优先级最高，配置后将影响所有该角色用户（包括未来新注册用户）</span>
      </div>
      <el-form :model="bulkForm" label-width="120px">
        <el-form-item label="套餐类型">
          <el-select v-model="bulkForm.role" placeholder="请选择套餐类型">
            <el-option label="免费套餐" :value="0" />
            <el-option label="VIP套餐" :value="1" />
            <el-option label="管理员" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="存储限额">
          <el-input-number
            v-model="bulkQuotaGB"
            :min="1"
            :max="10240"
            style="width: 200px"
          />
          <span style="margin-left: 8px">GB</span>
        </el-form-item>
        <el-form-item label="下载限速">
          <el-input-number
            v-model="bulkSpeedLimitKB"
            :min="0"
            :max="102400"
            style="width: 200px"
          />
          <span style="margin-left: 8px">KB/s（设为0表示不限速）</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bulkDialogVisible = false">取消</el-button>
        <el-button type="success" @click="submitBulkQuota">确认配置套餐</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Warning } from '@element-plus/icons-vue'
import api from '@/utils/api'

const userList = ref([])
const packageList = ref([])
const bulkDialogVisible = ref(false)
const bulkQuotaGB = ref(10)
const bulkSpeedLimitKB = ref(1024)

const bulkForm = reactive({
  role: null
})

const loadUsers = async () => {
  const res = await api.get('/admin/storage/users')
  if (res.code === 200) {
    userList.value = res.data
  }
}

const loadPackages = async () => {
  const res = await api.get('/admin/storage/packages')
  if (res.code === 200) {
    packageList.value = res.data
  }
}

const openBulkDialog = () => {
  bulkForm.role = null
  bulkQuotaGB.value = 10
  bulkSpeedLimitKB.value = 1024
  bulkDialogVisible.value = true
}

const submitBulkQuota = async () => {
  if (bulkForm.role === null) {
    ElMessage.warning('请选择套餐类型')
    return
  }
  const storageQuotaBytes = bulkQuotaGB.value * 1024 * 1024 * 1024

  let speedLimitBytes = bulkSpeedLimitKB.value * 1024
  if (bulkSpeedLimitKB.value === 0) {
    speedLimitBytes = 1024 * 1024 * 1024 // 设为0表示不限速，后端用1GB/s表示不限速
  }

  try {
    const res = await api.post('/admin/storage/quota/bulk', {
      role: bulkForm.role,
      storageQuota: storageQuotaBytes,
      downloadSpeedLimit: speedLimitBytes
    })
    ElMessage.success(`套餐配置成功，共更新 ${res.data.updatedCount} 个用户`)
    bulkDialogVisible.value = false
    loadUsers()
    loadPackages()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '套餐配置失败')
  }
}

const formatBytes = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatSpeed = (bytesPerSecond) => {
  if (!bytesPerSecond || bytesPerSecond === 0) return '不限速'
  if (bytesPerSecond >= 1024 * 1024 * 100) return '不限速'

  const k = 1024
  const sizes = ['B/s', 'KB/s', 'MB/s', 'GB/s']
  const i = Math.floor(Math.log(bytesPerSecond) / Math.log(k))
  return parseFloat((bytesPerSecond / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

const getRoleText = (role) => {
  const map = { 0: '免费用户', 1: '付费用户', 2: '管理员' }
  return map[role] || '未知'
}
const getRoleTagType = (role) => {
  const map = { 0: 'primary', 1: 'success', 2: 'danger' }
  return map[role] || 'info'
}

onMounted(() => {
  loadUsers()
  loadPackages()
})
</script>

<style scoped>
.admin-container {
  padding: 48px;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.admin-container::before {
  content: '';
  position: absolute;
  width: 800px;
  height: 800px;
  background: radial-gradient(circle, rgba(255,255,255,0.05) 0%, transparent 70%);
  border-radius: 50%;
  top: -200px;
  right: -200px;
  animation: float 10s ease-in-out infinite;
}

.admin-container::after {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(255,255,255,0.05) 0%, transparent 70%);
  border-radius: 50%;
  bottom: -150px;
  left: -150px;
  animation: float 12s ease-in-out infinite reverse;
}

.admin-container > * {
  position: relative;
  z-index: 1;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  padding: 24px 32px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  animation: fadeInUp 0.5s ease-out;
}

.header h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.package-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  padding: 32px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  margin-bottom: 24px;
  animation: fadeInUp 0.5s ease-out 0.1s both;
}

.package-section h3 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 20px;
}

:deep(.el-table) {
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}

:deep(.el-table th) {
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-weight: 600;
}

:deep(.el-table tr:hover > td) {
  background: var(--primary-50);
}

:deep(.el-divider) {
  margin: 32px 0;
  background: var(--border-light);
}

:deep(.el-button) {
  border-radius: var(--radius-md);
  font-weight: 600;
  transition: all var(--transition-normal);
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

:deep(.el-button--success:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4);
}

:deep(.el-dialog) {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-2xl);
}

:deep(.el-dialog__header) {
  padding: 28px 32px 24px;
}

:deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

:deep(.el-dialog__body) {
  padding: 0 32px 32px;
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

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--primary-300);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--primary-400);
  background: var(--bg-primary);
}

:deep(.el-select .el-input__wrapper) {
  background: var(--bg-secondary);
}

:deep(.el-input-number) {
  border-radius: var(--radius-md);
}

:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  border: none;
  background: var(--bg-secondary);
  transition: all var(--transition-fast);
}

:deep(.el-input-number__decrease:hover),
:deep(.el-input-number__increase:hover) {
  background: var(--primary-100);
  color: var(--primary-600);
}
</style>
