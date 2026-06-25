<template>
  <el-dialog v-model="visible" title="上传文件" width="500px" :close-on-click-modal="false" :close-on-press-escape="false">
    <!-- 上传区域 -->
    <div v-if="!uploading" class="upload-area">
      <el-upload class="upload-demo" drag :auto-upload="false" :on-change="handleFile">
        <el-icon><UploadFilled /></el-icon>
        <div>将文件拖到此处，或<em>点击上传</em></div>
      </el-upload>
    </div>

    <!-- 上传进度区域 -->
    <div v-else class="progress-area">
      <div class="file-info">
        <el-icon><FolderOpened /></el-icon>
        <span>{{ fileName }}</span>
      </div>
      <div class="progress-wrapper">
        <el-progress
          :percentage="uploadProgress"
          :status="progressStatus"
          :stroke-width="20"
          :show-text="true"
        />
      </div>
      <div class="progress-text">
        <span>{{ uploadStatus }}</span>
      </div>
      <div class="progress-actions">
        <el-button size="small" @click="cancelUpload" :disabled="uploadProgress === 100">
          取消上传
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { UploadFilled, FolderOpened } from '@element-plus/icons-vue'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'upload'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const uploading = ref(false)
const uploadProgress = ref(0)
const fileName = ref('')
const uploadStatus = ref('准备上传...')
const cancelToken = ref(null)

const progressStatus = computed(() => {
  if (uploadProgress.value === 100) return 'success'
  return 'active'
})

function handleFile(file) {
  uploading.value = true
  fileName.value = file.name
  uploadProgress.value = 0
  uploadStatus.value = '正在上传...'

  emit('upload', file, (progress) => {
    uploadProgress.value = progress
    // 当 HTTP 请求发送完成时（100%），显示"正在处理..."而非"上传成功"
    // 因为后端可能还在处理（计算哈希、上传到HDFS等）
    if (progress === 100) {
      uploadStatus.value = '服务器处理中，请稍候...'
    }
  })
}

// 取消上传
function cancelUpload() {
  if (cancelToken.value) {
    cancelToken.value.cancel('用户取消上传')
  }
  uploading.value = false
  uploadProgress.value = 0
  fileName.value = ''
  uploadStatus.value = '准备上传...'
}

// 外部调用：标记上传成功并关闭
function markSuccess() {
  uploadStatus.value = '上传成功！'
  setTimeout(() => {
    visible.value = false
    uploading.value = false
    uploadProgress.value = 0
    fileName.value = ''
  }, 1000)
}

// 外部调用：标记上传失败
function markError(message) {
  uploadStatus.value = message || '上传失败'
  setTimeout(() => {
    uploading.value = false
    uploadProgress.value = 0
    fileName.value = ''
    uploadStatus.value = '准备上传...'
  }, 2000)
}

// 暴露方法给父组件
defineExpose({ markSuccess, markError, cancelUpload })
</script>

<style scoped>
.upload-area {
  padding: 20px 0;
}

.progress-area {
  padding: 20px 0;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  font-size: 14px;
  color: #606266;
}

.progress-wrapper {
  margin-bottom: 10px;
}

.progress-text {
  text-align: center;
  font-size: 13px;
  color: #909399;
  margin-bottom: 15px;
}

.progress-actions {
  display: flex;
  justify-content: center;
}
</style>
