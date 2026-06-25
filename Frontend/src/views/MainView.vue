<template>
  <div class="main-layout">
    <!-- 侧边栏 -->
    <div class="sidebar">
      <!-- 用户信息区域 -->
      <div class="user-section">
        <div class="user-avatar">{{ user.username?.charAt(0)?.toUpperCase() || 'U' }}</div>
        <div class="user-info">
          <div class="user-name">{{ user.username }}</div>
          <div class="user-role">{{ roleText }}</div>
          <div class="user-email">{{ user.email || '未绑定邮箱' }}</div>
        </div>
      </div>

      <!-- 导航菜单 -->
      <div class="nav-menu">
        <div class="nav-item" :class="{ active: viewMode === 'files' }" @click="viewMode = 'files'">
          <span class="nav-icon">📁</span>
          <span>我的文件</span>
        </div>
        <div class="nav-item" :class="{ active: viewMode === 'recycle' }" @click="viewMode = 'recycle'">
          <span class="nav-icon">🗑️</span>
          <span>回收站</span>
        </div>
      </div>

      <!-- 存储空间 -->
      <div class="storage-section">
        <div class="storage-header">
          <span>存储空间</span>
          <span class="storage-value">{{ formatSize(user.used_storage) }} / {{ formatSize(user.storage_quota) }}</span>
        </div>
        <div class="storage-bar-container">
          <div class="storage-bar" :style="{ width: usagePercent + '%' }">
          </div>
        </div>
        <el-tag v-if="usagePercent > 90" type="danger" size="small">仅剩 {{ formatSize(remainingSpace) }}</el-tag>
        <span v-if="usagePercent > 90" class="storage-warning">空间不足，建议清理</span>
      </div>

      <!-- 快捷操作 -->
      <div class="quick-actions">
        <el-button
          v-if="user.role !== 1"
          class="upgrade-btn"
          @click="router.push('/pay')"
        >
          <span>✨</span>
          <span>升级VIP</span>
        </el-button>
        <div v-else class="vip-badge">
          <span class="vip-icon">👑</span>
          <span class="vip-text">您已是尊贵的VIP用户</span>
        </div>
      </div>

      <!-- 用户操作 -->
      <div class="user-actions">
        <el-link type="primary" @click="openChangePasswordDialog">修改密码</el-link>
        <el-link type="primary" @click="deleteAccountDialogVisible = true">注销账户</el-link>
        <el-link type="info" @click="handleLogout">退出登录</el-link>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 文件视图 -->
      <div class="content-section" v-if="viewMode === 'files'">
        <!-- 顶部操作栏 -->
        <div class="action-bar">
          <div class="action-left">
          <el-button class="upload-btn" @click="uploadDialogVisible = true">
            <span>📤</span>
            <span>上传文件</span>
          </el-button>
          <el-button class="new-folder-btn" @click="showNewFolderDialog = true">
            <span>📁</span>
            <span>新建文件夹</span>
          </el-button>
          </div>
        <div class="breadcrumb-nav">
            <div class="breadcrumb-item" @click="goToRoot">
              <span class="breadcrumb-icon">🏠</span>
              <span>根目录</span>
            </div>
            <span class="breadcrumb-separator">/</span>
            <div v-for="(item, index) in breadcrumb" :key="index">
              <div class="breadcrumb-item" @click="goToDir(item.dirId)">
                {{ item.name }}
              </div>
              <span v-if="index < breadcrumb.length - 1" class="breadcrumb-separator">/</span>
            </div>
          </div>
          <el-button v-if="currentDirId !== rootDirId" @click="goUp" class="back-btn">
            <span>⬆️</span>
            <span>返回上级</span>
          </el-button>
        </div>

        <!-- 下载进度 -->
        <div v-for="task in downloadTasks" :key="task.fileId" class="download-card">
          <div class="download-card-header">
            <span class="download-file-name">{{ task.fileName }}</span>
            <span class="download-speed">{{ task.speed }}</span>
          </div>
          <el-progress :percentage="task.percent" :color="customColors" />
          <div class="download-progress-info">
            {{ formatSize(task.loaded) }} / {{ formatSize(task.total) }}
          </div>
        </div>

        <!-- 文件列表 -->
        <div class="file-list-container">
          <el-table :data="fileList" class="file-table" :row-class-name="({row}) => row.is_dir ? 'folder-row' : ''">

            <el-table-column label="图标" width="50">
              <template #default="{ row }">
                <span v-if="row.is_dir" class="file-icon-text">📁</span>
                <span v-else class="file-icon-text">{{ getFileIcon(row.file_name) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="文件名" min-width="200">
              <template #default="{ row }">
                <span
                  class="file-name-link"
                  :class="{ 'folder-link': row.is_dir }"
                  @click="row.is_dir ? enterDir(row.file_id, row.file_name) : null"
                >
                  {{ row.file_name }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="大小" width="120">
              <template #default="{ row }">
                <span v-if="!row.is_dir">{{ formatSize(row.size) }}</span>
                <span v-else class="text-gray">-</span>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <span v-if="!row.is_dir">{{ getFileTypeName(row.file_name) }}</span>
                <span v-else class="text-gray">文件夹</span>
              </template>
            </el-table-column>
            <el-table-column label="上传时间" width="180">
              <template #default="{ row }">
                {{ row.upload_time }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-tooltip content="下载文件" placement="top">
                    <el-button
                      v-if="!row.is_dir"
                      size="small"
                      @click="handleDownload(row)"
                      class="table-action-btn"
                    >
                      ⬇️
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="分享文件" placement="top">
                    <el-button
                      v-if="!row.is_dir"
                      size="small"
                      @click="openShareDialog(row)"
                      class="table-action-btn"
                    >
                      🔗
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="移动文件" placement="top">
                    <el-button
                      v-if="!row.is_dir"
                      size="small"
                      @click="openMoveDialog(row)"
                      class="table-action-btn"
                    >
                      📦
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="AI分析" placement="top">
                    <el-button
                      v-if="!row.is_dir"
                      size="small"
                      @click="handleAiAnalyze(row)"
                      class="table-action-btn ai-btn"
                    >
                      🤖
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="删除" placement="top">
                    <el-button
                      size="small"
                      @click="handleDelete(row)"
                      class="table-action-btn delete-btn"
                    >
                      🗑️
                    </el-button>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 回收站视图 -->
      <div class="content-section recycle-section" v-if="viewMode === 'recycle'">
        <div class="recycle-header">
          <div class="recycle-title">
          <span>🗑️</span>
          <span>回收站</span>
          </div>
          <el-button
            class="delete-all-btn"
            :disabled="selectedRecycleFiles.length === 0"
            @click="handlePermanentDelete"
          >
            彻底删除选中 ({{ selectedRecycleFiles.length }})
          </el-button>
        </div>

        <el-table
          :data="recycleBinList"
          @selection-change="handleSelectionChange"
          ref="recycleTableRef"
          class="recycle-table"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="file_name" label="文件名" />
          <el-table-column label="大小" width="140">
            <template #default="{ row }">{{ formatSize(row.size) }}</template>
          </el-table-column>
          <el-table-column prop="delete_time" label="删除时间" width="200" />
          <el-table-column prop="remaining" label="剩余时间" width="140" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" class="restore-btn" @click="handleRestore(row)">
                恢复
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 弹窗组件 -->
    <UploadDialog ref="uploadDialogRef" v-model="uploadDialogVisible" @upload="handleUpload" />
    <ShareDialog v-model="shareDialogVisible" :target="shareTarget" />
    <DeleteAccountDialog v-model="deleteAccountDialogVisible" :user-id="user.user_id" :username="user.username" />

    <!-- 移动文件弹窗 -->
    <el-dialog v-model="moveDialogVisible" title="移动文件" width="450px">
      <div class="dialog-content">
      <el-form>
        <el-form-item label="目标文件夹" label-width="100px">
          <el-select v-model="moveTargetDirId" placeholder="请选择目标文件夹" style="width: 100%;">
            <el-option label="根目录" :value="rootDirId" />
            <el-option
              v-for="dir in moveAvailableDirs"
              :key="dir.dirId"
              :label="dir.dirName"
              :value="dir.dirId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      </div>
      <template #footer>
        <el-button @click="moveDialogVisible = false" class="dialog-cancel-btn">取消</el-button>
        <el-button type="primary" @click="handleMoveFile" class="dialog-confirm-btn">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新建文件夹弹窗 -->
    <el-dialog v-model="showNewFolderDialog" title="新建文件夹" width="450px">
      <div class="dialog-content">
      <el-input v-model="newFolderName" placeholder="请输入文件夹名称" class="folder-input" />
      </div>
      <template #footer>
        <el-button @click="showNewFolderDialog = false" class="dialog-cancel-btn">取消</el-button>
        <el-button type="primary" @click="handleCreateFolder" class="dialog-confirm-btn">确定</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="showPasswordDialog" width="450px">
      <template #header>
        <span class="dialog-header">修改密码</span>
        <el-link type="primary" class="forgot-link" @click="goToResetPassword">
          忘记原密码？
        </el-link>
      </template>
      <div class="dialog-content">
      <el-form :model="passwordForm" label-width="80px">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      </div>
      <template #footer>
        <el-button @click="showPasswordDialog = false" class="dialog-cancel-btn">取消</el-button>
        <el-button type="primary" @click="submitChangePassword" class="dialog-confirm-btn">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfoApi, changePassword } from '@/api/user'
import {
  uploadFileApi,
  getFileListApi,
  deleteFileApi,
  deleteDirectoryApi,
  getRecycleBinApi,
  restoreFileApi,
  createFolderApi,
  permanentDeleteApi,
} from '@/api/files'
import api from '@/utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import UploadDialog from '@/components/common/UploadDialog.vue'
import ShareDialog from '@/components/common/ShareDialog.vue'
import DeleteAccountDialog from '@/components/DeleteAccountDialog.vue'

const router = useRouter()

const showPasswordDialog = ref(false)
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const user = ref({})
const fileList = ref([])
const recycleBinList = ref([])
const uploadDialogVisible = ref(false)
const uploadDialogRef = ref(null)
const downloadTasks = ref([])
const viewMode = ref('files')

const rootDirId = ref(null)
const currentDirId = ref(null)
const breadcrumb = ref([])

const newFolderName = ref('')
const showNewFolderDialog = ref(false)

const recycleTableRef = ref(null)
const selectedRecycleFiles = ref([])

const shareDialogVisible = ref(false)
const deleteAccountDialogVisible = ref(false)

const moveDialogVisible = ref(false)
const moveTargetFile = ref(null)
const moveTargetDirId = ref(null)
const moveAvailableDirs = ref([])
const shareTarget = ref({ type: 0, id: 0, name: '' })

const customColors = [
  { color: '#667eea', percentage: 20 },
  { color: '#764ba2', percentage: 100 },
]

const roleText = computed(() => {
  const map = { 0: '免费用户', 1: 'VIP用户', 2: '超级管理员' }
  return map[user.value.role] || '未知'
})

const usagePercent = computed(() => {
  const used = user.value.used_storage || 0
  const total = user.value.storage_quota || 1
  return Math.round((used / total) * 100)
})

const remainingSpace = computed(() => {
  return (user.value.storage_quota || 0) - (user.value.used_storage || 0)
})

function formatSize(bytes) {
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

function getFileExtension(filename) {
  if (!filename) return ''
  const ext = filename.split('.').pop()
  return ext ? ext.toUpperCase() : ''
}

function getFileTypeName(filename) {
  const ext = filename?.split('.').pop()?.toLowerCase() || ''
  const typeMap = {
    'pdf': 'PDF文档', 'doc': 'Word文档', 'docx': 'Word文档',
    'xls': 'Excel表格', 'xlsx': 'Excel表格', 'ppt': 'PPT演示',
    'pptx': 'PPT演示', 'txt': '文本文档', 'jpg': '图片',
    'jpeg': '图片', 'png': '图片', 'gif': '图片',
    'mp4': '视频', 'avi': '视频', 'mp3': '音频',
    'zip': '压缩文件', 'rar': '压缩文件', '7z': '压缩文件'
  }
  return typeMap[ext] || (ext ? ext.toUpperCase() + '文件' : '未知')
}

function getFileIcon(filename) {
  const ext = filename?.split('.').pop()?.toLowerCase() || ''
  const iconMap = {
    'pdf': '📕', 'doc': '📘', 'docx': '📘',
    'xls': '📗', 'xlsx': '📗', 'ppt': '📙',
    'pptx': '📙', 'txt': '📝', 'md': '📝',
    'jpg': '🖼️', 'jpeg': '🖼️', 'png': '🖼️',
    'gif': '🖼️', 'svg': '🖼️', 'bmp': '🖼️',
    'mp4': '🎬', 'avi': '🎬', 'mov': '🎬',
    'mp3': '🎵', 'wav': '🎵', 'flac': '🎵',
    'zip': '📦', 'rar': '📦', '7z': '📦',
    'tar': '📦', 'gz': '📦', 'zipx': '📦',
    'html': '🌐', 'css': '🎨', 'js': '⚙️',
    'json': '📋', 'xml': '📋', 'sql': '🗄️',
    'java': '☕', 'py': '🐍', 'cpp': '🛠️',
    'c': '🛠️', 'go': '🐹', 'rs': '🦀',
    'vue': '💚', 'ts': '📘', 'md': '📝'
  }
  return iconMap[ext] || '📄'
}

function isDownloading(fileId) {
  return downloadTasks.value.some((task) => task.fileId === fileId)
}

function openShareDialog(row) {
  shareTarget.value = {
    type: row.is_dir ? 1 : 0,
    id: row.file_id,
    name: row.file_name,
  }
  shareDialogVisible.value = true
}

async function openMoveDialog(row) {
  moveTargetFile.value = row
  moveTargetDirId.value = rootDirId.value

  try {
    const res = await api.get('/api/directories/user')
    moveAvailableDirs.value = res.data.filter(dir => dir.parentDirId !== null)
  } catch {
    moveAvailableDirs.value = []
  }

  moveDialogVisible.value = true
}

async function handleMoveFile() {
  if (!moveTargetFile.value || !moveTargetDirId.value) {
    ElMessage.warning('请选择目标文件夹')
    return
  }

  try {
    await api.put('/api/files/move', {
      fileId: moveTargetFile.value.file_id,
      targetDirId: moveTargetDirId.value
    })

    ElMessage.success('移动成功')
    moveDialogVisible.value = false
    await fetchFileList(currentDirId.value)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '移动失败')
  }
}

async function fetchUserInfo() {
  try {
    const res = await getUserInfoApi()
    user.value = res.data
  } catch {
    ElMessage.error('获取用户信息失败，请重新登录')
    router.push('/login')
  }
}

async function fetchFileList(dirId) {
  const parentId = dirId !== undefined ? dirId : currentDirId.value
  try {
    const params = {}
    if (parentId) params.parent_dir_id = parentId
    const res = await getFileListApi(params)
    fileList.value = res.data
  } catch {
    ElMessage.error('获取文件列表失败')
  }
}

async function fetchRecycleBin() {
  try {
    const res = await getRecycleBinApi()
    recycleBinList.value = res.data
  } catch {
    ElMessage.error('获取回收站失败')
  }
}

async function initNavigation() {
  try {
    const res = await api.get('/api/files/root-dir')
    rootDirId.value = res.data.dir_id
    currentDirId.value = rootDirId.value
    breadcrumb.value = []
    await fetchFileList(rootDirId.value)
  } catch {
    await fetchFileList(null)
  }
}

function enterDir(dirId, dirName) {
  breadcrumb.value.push({ dirId: currentDirId.value, name: dirName })
  currentDirId.value = dirId
  fetchFileList(dirId)
}

function goToDir(dirId) {
  const index = breadcrumb.value.findIndex((item) => item.dirId === dirId)
  if (index !== -1) breadcrumb.value = breadcrumb.value.slice(0, index + 1)
  currentDirId.value = dirId
  fetchFileList(dirId)
}

function goToRoot() {
  currentDirId.value = rootDirId.value
  breadcrumb.value = []
  fetchFileList(rootDirId.value)
}

function goUp() {
  if (breadcrumb.value.length > 0) {
    const last = breadcrumb.value.pop()
    currentDirId.value = last.dirId
    fetchFileList(last.dirId)
  } else {
    goToRoot()
  }
}

watch(viewMode, (newVal) => {
  if (newVal === 'files') fetchFileList(currentDirId.value)
  else if (newVal === 'recycle') {
    selectedRecycleFiles.value = []
    fetchRecycleBin()
  }
})

async function handleUpload(file, onProgress) {
  if (file.raw.size > remainingSpace.value) {
    ElMessage.warning('文件大小超过剩余存储空间，无法上传')
    return
  }
  const formData = new FormData()
  formData.append('file', file.raw)
  formData.append('parent_dir_id', currentDirId.value || rootDirId.value)
  try {
    await uploadFileApi(formData, onProgress)
    uploadDialogRef.value?.markSuccess()
    ElMessage.success('上传成功')
    await Promise.all([fetchUserInfo(), fetchFileList(currentDirId.value)])
  } catch (error) {
    uploadDialogRef.value?.markError('上传失败')
  }
}

async function handleDownload(file) {
  const fileId = file.file_id
  if (isDownloading(fileId)) return

  const task = {
    fileId,
    fileName: file.file_name,
    percent: 0,
    speed: '0 B/s',
    loaded: 0,
    total: file.size,
  }
  downloadTasks.value.push(task)

  let lastTime = Date.now()
  let lastLoaded = 0

  try {
    const response = await api.get(`/api/files/download/${fileId}`, {
      responseType: 'blob',
      onDownloadProgress: (progressEvent) => {
        const loaded = progressEvent.loaded
        const total = progressEvent.total || file.size
        const target = downloadTasks.value.find((t) => t.fileId === fileId)
        if (target) {
          target.percent = Math.round((loaded / total) * 100)
          target.loaded = loaded
          const now = Date.now()
          const timeDiff = (now - lastTime) / 1000
          if (timeDiff >= 0.5) {
            const bytesDiff = loaded - lastLoaded
            const speed = bytesDiff / timeDiff
            target.speed = formatSize(speed) + '/s'
            lastTime = now
            lastLoaded = loaded
          }
        }
      },
    })
    const url = window.URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', file.file_name)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error(`文件 ${file.file_name} 下载失败`)
  } finally {
    downloadTasks.value = downloadTasks.value.filter((t) => t.fileId !== fileId)
  }
}

async function handleDelete(row) {
  const isDir = row.is_dir
  const name = row.file_name
  const id = row.file_id

  try {
    await ElMessageBox.confirm(`确定要删除文件"${row.file_name}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    if (isDir) {
      await deleteDirectoryApi(id)
    } else {
      await deleteFileApi(id)
    }

    ElMessage.success('删除成功')
    await Promise.all([fetchUserInfo(), fetchFileList(currentDirId.value)])
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

async function handleRestore(file) {
  try {
    await restoreFileApi(file.file_id)
    ElMessage.success('文件已恢复')
    await Promise.all([fetchUserInfo(), fetchFileList(currentDirId.value), fetchRecycleBin()])
  } catch {
    ElMessage.error('恢复失败')
  }
}

async function handleCreateFolder() {
  const name = newFolderName.value.trim()
  if (!name) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  try {
    await createFolderApi({ dir_name: name, parent_dir_id: currentDirId.value || rootDirId.value })
    ElMessage.success('文件夹创建成功')
    newFolderName.value = ''
    showNewFolderDialog.value = false
    await fetchFileList(currentDirId.value)
  } catch { /* empty */ }
}

function handleSelectionChange(selection) {
  selectedRecycleFiles.value = selection
}

async function handlePermanentDelete() {
  const filesToDelete = selectedRecycleFiles.value
  if (filesToDelete.length === 0) {
    ElMessage.warning('请先选择要彻底删除的文件')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要彻底删除选中的 ${filesToDelete.length} 个文件吗？此操作不可恢复！`,
      '危险操作',
      { confirmButtonText: '彻底删除', cancelButtonText: '取消', type: 'error' },
    )
    const fileIds = filesToDelete.map((f) => f.file_id)
    await permanentDeleteApi(fileIds)
    ElMessage.success('彻底删除成功')
    selectedRecycleFiles.value = []
    await fetchRecycleBin()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

function handleAiAnalyze(file) {
  router.push({
    path: '/ai/analysis',
    query: {
      fileId: file.file_id,
      fileName: file.file_name
    }
  })
}

function handleLogout() {
  localStorage.removeItem('token')
  router.push('/login')
}

function openChangePasswordDialog() {
  showPasswordDialog.value = true
}

function goToResetPassword() {
  showPasswordDialog.value = false
  router.push('/reset-password')
}

async function submitChangePassword() {
  if (!passwordForm.value.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!passwordForm.value.newPassword || passwordForm.value.newPassword.length < 6) {
    ElMessage.warning('新密码至少6位')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  try {
    await changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    showPasswordDialog.value = false
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    localStorage.removeItem('token')
    router.push('/login')
  } catch (error) {
    const msg = error.response?.data?.message || '修改失败'
    ElMessage.error(msg)
  }
}

onMounted(async () => {
  await fetchUserInfo()
  await initNavigation()
})
</script>

<style scoped>
.main-layout {
  display: flex;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  width: 280px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 32px 24px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(102, 126, 234, 0.1);
  position: relative;
  z-index: 10;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.08);
}

.user-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
}

.user-avatar {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 4px;
}

.user-role {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
  margin-bottom: 4px;
}

.user-email {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}

.nav-menu {
  margin-bottom: 32px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #475569;
  font-weight: 500;
}

.nav-item:hover {
  background: #f1f5f9;
}

.nav-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.nav-icon {
  font-size: 20px;
}

.storage-section {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  padding: 20px;
  border-radius: 16px;
  margin-bottom: 24px;
}

.storage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.storage-header span:first-child {
  font-weight: 600;
  color: #475569;
}

.storage-value {
  color: #334155;
  font-weight: 600;
}

.storage-bar-container {
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.storage-bar {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transition: width 0.3s ease;
}

.storage-warning {
  font-size: 12px;
  color: #ef4444;
  font-weight: 500;
  margin-left: 8px;
}

.quick-actions {
  margin-bottom: 24px;
}

.upgrade-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  border: none;
  border-radius: 12px;
  color: white;
  font-weight: 600;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
  transition: all 0.3s ease;
}

.upgrade-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(245, 158, 11, 0.4);
}

.vip-badge {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 50%, #d97706 100%);
  border-radius: 12px;
  color: white;
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(251, 191, 36, 0.4);
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.vip-icon {
  font-size: 18px;
}

.vip-text {
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.user-actions {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 24px;
  border-top: 1px solid #e2e8f0;
}

.user-actions .el-link {
  text-align: left;
  font-size: 14px;
  font-weight: 500;
}

/* 主内容区 */
.main-content {
  flex: 1;
  padding: 32px;
  overflow-y: auto;
  position: relative;
  z-index: 1;
}

.content-section {
  animation: fadeInUp 0.4s ease-out;
}

.recycle-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 28px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
}

.action-bar {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.action-left {
  display: flex;
  gap: 12px;
}

.upload-btn,
.new-folder-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  padding: 12px 20px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

.upload-btn:hover,
.new-folder-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  margin: 0 20px;
}

.breadcrumb-item {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 8px 14px;
  border-radius: 8px;
  transition: all 0.3s ease;
  font-weight: 500;
  color: #64748b;
}

.breadcrumb-item:hover {
  background: #f1f5f9;
  color: #667eea;
}

.breadcrumb-separator {
  color: #94a3b8;
  font-weight: 500;
}

.breadcrumb-icon {
  font-size: 16px;
}

.back-btn {
  border: 1px solid #e2e8f0;
  background: white;
  padding: 10px 16px;
  border-radius: 10px;
  font-weight: 500;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s ease;
}

.back-btn:hover {
  border-color: #667eea;
  color: #667eea;
  background: #f0f4ff;
}

/* 下载进度卡片 */
.download-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(102, 126, 234, 0.1);
}

.download-card-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  align-items: center;
}

.download-file-name {
  font-weight: 600;
  color: #1e293b;
}

.download-speed {
  color: #667eea;
  font-weight: 600;
  font-size: 14px;
}

.download-progress-info {
  font-size: 13px;
  color: #64748b;
  margin-top: 8px;
}

/* 文件列表容器 */
.file-list-container {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
}

/* 文件表格 */
.file-table {
  width: 100%;
}

.file-icon-text {
  font-size: 24px;
}

.file-name-link {
  font-weight: 600;
  color: #334155;
  cursor: default;
}

.folder-link {
  color: #667eea;
  cursor: pointer;
}

.folder-link:hover {
  color: #764ba2;
  text-decoration: underline;
}

.text-gray {
  color: #94a3b8;
}

.table-actions {
  display: flex;
  gap: 6px;
}

.table-action-btn {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  padding: 0;
  transition: all 0.2s ease;
}

.table-action-btn:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.table-action-btn.ai-btn {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  border: none;
  color: white;
}

.table-action-btn.ai-btn:hover {
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
}

.table-action-btn.delete-btn:hover {
  background: #fef2f2;
  border-color: #ef4444;
  color: #ef4444;
}

/* 回收站 */
.recycle-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.recycle-title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 10px;
}

.delete-all-btn {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border: none;
  border-radius: 12px;
  padding: 12px 24px;
  font-weight: 600;
  color: white;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.recycle-table {
  border-radius: 12px;
  overflow: hidden;
}

.restore-btn {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  color: white;
  font-weight: 600;
}

/* 弹窗样式 */
:deep(.el-dialog) {
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

:deep(.el-dialog__header) {
  padding: 28px 28px 24px;
}

:deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.dialog-content {
  padding: 0 28px 28px;
}

.dialog-header {
  font-size: 20px;
  font-weight: 700;
}

.forgot-link {
  font-size: 14px;
  font-weight: 500;
}

.folder-input {
  border-radius: 12px;
}

.dialog-cancel-btn {
  border: 1px solid #e2e8f0;
  background: white;
  padding: 10px 24px;
  border-radius: 10px;
  font-weight: 500;
  color: #475569;
}

.dialog-confirm-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  padding: 10px 24px;
  border-radius: 10px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.file-type-badge {
  display: inline-block;
  padding: 4px 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #64748b;
  cursor: help;
}

.ai-btn {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  border: none;
  color: white;
}

.ai-btn:hover {
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
}

/* 动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Element Plus 样式优化 */
:deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.el-table th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  padding: 16px 0;
}

:deep(.el-table td) {
  padding: 14px 0;
}

:deep(.el-table tr:hover > td) {
  background: #f0f4ff;
}

/* 只隐藏文件列表表格的全选复选框（回收站表格保留） */
:deep(.file-table .el-table__header-wrapper .el-checkbox) {
  display: none;
}

/* 修复文件列表表格表头与数据对齐 */
:deep(.file-table .el-table__header th:first-child) {
  padding-left: 0;
  text-align: center;
}

:deep(.file-table .el-table__body td:first-child) {
  text-align: center;
}
</style>