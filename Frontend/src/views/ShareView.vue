<template>
  <div class="share-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading">
      <el-spin size="large" />
    </div>

    <!-- 分享内容 -->
    <div v-else-if="shareData && !shareData.needExtractCode" class="share-card">
      <h2 class="title">📤 分享文件</h2>

      <div class="info-section">
        <div class="info-row">
          <span class="label">分享者</span>
          <span class="value">{{ shareData.sharer?.name || '未知' }}</span>
        </div>
        <div class="info-row">
          <span class="label">文件名称</span>
          <span class="value">{{ shareData.target?.fileName || shareData.target?.dirName }}</span>
        </div>
        <div class="info-row">
          <span class="label">文件大小</span>
          <span class="value">{{ shareData.target?.fileSize || '-' }}</span>
        </div>
        <div class="info-row">
          <span class="label">权限</span>
          <span class="value">{{ shareData.permissionText }}</span>
        </div>
        <div class="info-row">
          <span class="label">有效期至</span>
          <span class="value">{{ formatDateTime(shareData.expireTime) }}</span>
        </div>
      </div>

      <div class="action-section">
        <el-button
          v-if="shareData.permission >= 2"
          type="primary"
          size="large"
          @click="handleDownload"
          :loading="downloading"
          :disabled="downloading"
        >
          📥 下载文件
        </el-button>

        <!-- 下载进度显示 -->
        <div v-if="downloading" class="download-progress">
          <el-progress
            :percentage="downloadProgress"
            :status="downloadProgress === 100 ? 'success' : ''"
          />
          <p class="download-status">{{ downloadStatus }}</p>
        </div>

        <el-button
          v-if="shareData.permission >= 3"
          type="success"
          size="large"
          @click="handleSaveToDrive"
          :loading="saving"
          style="margin-left: 10px"
        >
          📂 转存到我的网盘
        </el-button>
        <el-button v-if="shareData.permission === 1" type="default" size="large" disabled>
          仅查看权限，无法下载或转存
        </el-button>
      </div>
    </div>

    <!-- 需要提取码 -->
    <div v-else-if="shareData?.needExtractCode" class="share-card">
      <h2 class="title">🔐 需要提取码</h2>

      <el-form :model="extractForm" class="extract-form">
        <el-form-item label="提取码" label-width="80px">
          <el-input
            v-model="extractForm.code"
            placeholder="请输入提取码"
            maxlength="10"
            @keyup.enter="verifyExtractCode"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="verifyExtractCode" :loading="verifying">
            确认
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 错误提示 -->
    <div v-else-if="error" class="error-card">
      <el-alert type="error" :title="error" :closable="false" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getShareDetailApi } from '@/api/share'
import api from '@/utils/api'

const route = useRoute()
const loading = ref(true)
const verifying = ref(false)
const downloading = ref(false)
const saving = ref(false)
const shareData = ref(null)
const error = ref('')
const downloadProgress = ref(0)
const downloadStatus = ref('准备下载...')

const extractForm = ref({
  code: ''
})

function formatDateTime(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function fetchShareDetail(extractCode = null) {
  loading.value = true
  try {
    const params = extractCode ? { extractCode } : {}
    const res = await getShareDetailApi(route.params.link, extractCode)
    shareData.value = res.data
    error.value = ''
  } catch (e) {
    error.value = e.response?.data?.message || '获取分享信息失败'
    shareData.value = null
  } finally {
    loading.value = false
  }
}

async function verifyExtractCode() {
  if (!extractForm.value.code.trim()) {
    ElMessage.warning('请输入提取码')
    return
  }
  verifying.value = true
  await fetchShareDetail(extractForm.value.code)
  verifying.value = false
}

async function handleDownload() {
  const fileId = shareData.value?.target?.fileId
  if (!fileId) {
    ElMessage.error('文件ID不存在')
    return
  }

  downloading.value = true
  downloadProgress.value = 0
  downloadStatus.value = '准备下载...'
  try {
    // 使用分享公开下载接口，不需要登录
    const extractCode = extractForm.value.code || null
    const params = extractCode ? `?extractCode=${extractCode}` : ''

    const response = await api.get(`/api/share/download/${route.params.link}${params}`, {
      responseType: 'blob',
      onDownloadProgress: (progressEvent) => {
        if (progressEvent.total > 0) {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          downloadProgress.value = percentCompleted
          if (percentCompleted === 100) {
            downloadStatus.value = '下载完成，正在保存...'
          } else {
            downloadStatus.value = `下载中 ${percentCompleted}%...`
          }
        }
      }
    })
    const url = window.URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', shareData.value.target.fileName)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '下载失败')
  } finally {
    downloading.value = false
    downloadProgress.value = 0
  }
}

async function handleSaveToDrive() {
  saving.value = true
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.warning('请先登录后再转存')
      await ElMessageBox.confirm(
        '需要登录才能转存文件，是否跳转到登录页面？',
        '提示',
        {
          confirmButtonText: '去登录',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      // 保存待转存的分享信息，登录后自动转存
      localStorage.setItem('pendingShareSave', JSON.stringify({
        shareLink: route.params.link,
        extractCode: extractForm.value.code || null
      }))
      window.location.href = '/login'
      return
    }

    const extractCode = extractForm.value.code || null
    await api.post('/api/share/save-to-drive', {
      shareLink: route.params.link,
      extractCode
    })

    ElMessage.success('转存成功')
    // 转存成功后跳转到主页面
    window.location.href = '/main'
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '转存失败')
    }
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await fetchShareDetail()

  // 检查是否需要自动转存
  if (route.query.autoSave === 'true') {
    // 清除待转存标记
    localStorage.removeItem('pendingShareSave')

    // 如果提取码已在URL参数中，先验证提取码
    if (route.query.extractCode && (!shareData.value || shareData.value.needExtractCode)) {
      extractForm.value.code = route.query.extractCode
      await verifyExtractCode()
    }

    // 验证成功后自动转存
    if (shareData.value && !shareData.value.needExtractCode && shareData.value.permission >= 3) {
      await handleSaveToDrive()
    }
  }
})
</script>

<style scoped>
.share-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 48px 24px;
  position: relative;
  overflow: hidden;
}

.share-page::before {
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

.share-page::after {
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

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
}

.share-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-radius: var(--radius-xl);
  padding: 48px;
  width: 500px;
  box-shadow:
    0 50px 100px -20px rgba(0, 0, 0, 0.25),
    0 30px 60px -30px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
  animation: fadeInUp 0.6s ease-out;
}

.error-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-radius: var(--radius-xl);
  padding: 48px;
  width: 500px;
  box-shadow:
    0 50px 100px -20px rgba(0, 0, 0, 0.25),
    0 30px 60px -30px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
  animation: fadeInUp 0.6s ease-out;
}

.title {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 32px 0;
  text-align: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.info-section {
  margin-bottom: 32px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid var(--border-light);
  transition: background var(--transition-fast);
}

.info-row:hover {
  background: var(--primary-50);
  margin: 0 -12px;
  padding: 14px 12px;
  border-radius: var(--radius-md);
}

.info-row:last-child {
  border-bottom: none;
}

.label {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
}

.value {
  color: var(--text-primary);
  font-weight: 600;
  font-size: 14px;
}

.action-section {
  text-align: center;
}

.extract-form {
  margin-top: 24px;
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

:deep(.el-button) {
  border-radius: var(--radius-md);
  font-weight: 600;
  padding: 12px 28px;
  transition: all var(--transition-normal);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.5);
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(16, 185, 129, 0.4);
}

:deep(.el-button--success:hover) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(16, 185, 129, 0.5);
}

.download-progress {
  margin-top: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}

.download-status {
  margin: 14px 0 0 0;
  font-size: 14px;
  color: var(--text-secondary);
  text-align: center;
  font-weight: 500;
}

:deep(.el-alert) {
  border-radius: var(--radius-md);
  border: none;
  box-shadow: var(--shadow-sm);
}
</style>
