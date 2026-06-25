<template>
  <div class="ai-analysis-container">
    <el-card class="box-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>📄 AI 文件智能分析</span>
        </div>
      </template>

      <el-form :model="formData" label-width="80px" class="form-area">
        <el-form-item label="文件ID">
          <el-input v-model="formData.fileId" placeholder="请输入文件ID" />
        </el-form-item>

        <el-form-item label="文件名" v-if="selectedFileName">
          <el-tag type="info" size="large">{{ selectedFileName }}</el-tag>
        </el-form-item>

        <el-form-item label="分析指令">
          <el-input
            v-model="formData.prompt"
            type="textarea"
            :rows="4"
            placeholder="例如：帮我提取这份文件的核心摘要、总结要点、提取关键词、翻译成中文"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item style="text-align: center;">
          <el-button
            type="primary"
            :loading="loading"
            @click="handleAnalyze"
          >
            开始分析
          </el-button>
          <el-button @click="handleBack">返回文件列表</el-button>
        </el-form-item>
      </el-form>

      <div v-if="analysisResult" class="result-area">
        <el-divider content-position="left">分析结果</el-divider>
        <div class="result-content">
          <pre>{{ analysisResult.summary }}</pre>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { analyzeFileApi } from '@/api/ai'

const route = useRoute()
const router = useRouter()

const formData = ref({
  fileId: '',
  prompt: '帮我提取这份文件的核心摘要，用简洁的语言总结'
})

const selectedFileName = ref('')
const loading = ref(false)
const analysisResult = ref(null)

onMounted(() => {
  if (route.query.fileId) {
    formData.value.fileId = route.query.fileId
    selectedFileName.value = route.query.fileName || ''
  }
})

const handleAnalyze = async () => {
  if (!formData.value.fileId) {
    ElMessage.warning('请输入文件ID')
    return
  }

  loading.value = true
  analysisResult.value = null

  try {
    const res = await analyzeFileApi(formData.value)
    if (res.code === 200) {
      analysisResult.value = res.data
      ElMessage.success('分析完成！')
    } else {
      ElMessage.error(res.message || '分析失败')
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('请求失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}

const handleBack = () => {
  router.push('/main')
}
</script>

<style scoped>
.ai-analysis-container {
  padding: 48px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
}

.box-card {
  max-width: 900px;
  margin: 0 auto;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-2xl);
  border: none;
  overflow: hidden;
}

.box-card :deep(.el-card__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px 32px;
}

.card-header {
  font-weight: 700;
  font-size: 20px;
  color: white;
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-area {
  margin-bottom: 32px;
  padding: 0 8px;
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

:deep(.el-textarea__inner) {
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  transition: all var(--transition-fast);
  background: var(--bg-secondary);
}

:deep(.el-textarea__inner:hover) {
  border-color: var(--primary-300);
}

:deep(.el-textarea__inner:focus) {
  border-color: var(--primary-400);
  box-shadow: 0 0 0 2px var(--primary-100);
  background: var(--bg-primary);
}

:deep(.el-button) {
  border-radius: var(--radius-md);
  font-weight: 600;
  padding: 12px 24px;
  transition: all var(--transition-normal);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.result-area {
  margin-top: 24px;
  padding: 0 8px;
}

:deep(.el-divider__text) {
  font-weight: 700;
  color: var(--text-primary);
  font-size: 16px;
  background: var(--bg-primary);
  padding: 0 20px;
}

.result-content {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  padding: 28px 32px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  max-height: 500px;
  overflow-y: auto;
}

pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
  font-size: 15px;
  line-height: 1.8;
  color: var(--text-primary);
  margin: 0;
}
</style>
