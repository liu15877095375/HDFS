<template>
  <el-dialog v-model="visible" title="分享文件" width="500px">
    <!-- 创建分享表单 -->
    <div v-if="!shareResult">
      <el-form label-width="80px">
        <!-- 分享权限 -->
        <el-form-item label="分享权限">
          <el-radio-group v-model="form.permission">
            <el-radio :value="1">仅查看</el-radio>
            <el-radio :value="2">可下载</el-radio>
            <el-radio :value="3">可转存</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 有效期 -->
        <el-form-item label="有效期">
          <el-radio-group v-model="form.expireDays">
            <el-radio :value="1">1天</el-radio>
            <el-radio :value="7">7天</el-radio>
            <el-radio :value="30">30天</el-radio>
            <el-radio :value="365">永久</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 提取码 -->
        <el-form-item label="提取码">
          <el-switch v-model="needCode" active-text="需要" inactive-text="不需要" />
          <el-input
            v-if="needCode"
            v-model="form.extractCode"
            placeholder="4位提取码"
            maxlength="10"
            style="width: 150px; margin-left: 10px"
          />
          <el-button v-if="needCode" @click="generateCode" size="small" style="margin-left: 5px">
            随机
          </el-button>
        </el-form-item>
      </el-form>

      <div style="text-align: right; margin-top: 20px">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="loading">创建分享</el-button>
      </div>
    </div>

    <!-- 分享结果 -->
    <div v-else>
      <el-form label-width="80px">
        <el-form-item label="分享链接">
          <el-input v-model="shareResult.shareUrl" readonly>
            <template #append>
              <el-button @click="copyLink">复制</el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item v-if="shareResult.extractCode" label="提取码">
          <el-input v-model="shareResult.extractCode" readonly>
            <template #append>
              <el-button @click="copyCode">复制</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>

      <div style="text-align: right; margin-top: 20px">
        <el-button type="primary" @click="visible = false">关闭</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createShareApi } from '@/api/share'

const props = defineProps({
  target: {
    type: Object,
    required: true,
    // target 对象包含: type(0-文件,1-目录), id, name
  },
})

const visible = defineModel()
const loading = ref(false)
const needCode = ref(false)
const shareResult = ref(null)

const form = reactive({
  permission: 2,
  expireDays: 7,
  extractCode: '',
})

// 监听弹窗打开/关闭，重置状态
watch(visible, (val) => {
  if (val) {
    // 弹窗打开时重置状态
    shareResult.value = null
    needCode.value = false
    form.permission = 2
    form.expireDays = 7
    form.extractCode = ''
  }
})

// 生成随机提取码
function generateCode() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  let code = ''
  for (let i = 0; i < 4; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  form.extractCode = code
}

// 创建分享
async function handleCreate() {
  loading.value = true
  try {
    const res = await createShareApi({
      targetType: props.target.type,
      targetId: props.target.id,
      permission: form.permission,
      expireDays: form.expireDays,
      extractCode: needCode.value ? form.extractCode : null,
    })
    shareResult.value = res.data
    ElMessage.success('分享创建成功')
  } catch {
    ElMessage.error('创建分享失败')
  } finally {
    loading.value = false
  }
}

// 复制链接
async function copyLink() {
  try {
    await navigator.clipboard.writeText(shareResult.value.shareUrl)
    ElMessage.success('已复制链接')
  } catch {
    ElMessage.error('复制失败')
  }
}

// 复制提取码
async function copyCode() {
  try {
    await navigator.clipboard.writeText(shareResult.value.extractCode)
    ElMessage.success('已复制提取码')
  } catch {
    ElMessage.error('复制失败')
  }
}
</script>
