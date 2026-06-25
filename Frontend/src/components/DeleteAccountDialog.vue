<template>
  <el-dialog v-model="visible" title="确认注销账户" width="500px">
    <el-alert type="warning" :closable="false" style="margin-bottom: 20px">
      <template #title>
        <strong>注销账户将导致以下影响：</strong>
      </template>
      <ul style="margin: 10px 0; padding-left: 20px">
        <li>您的所有文件将被移入回收站</li>
        <li>您的所有分享链接将失效</li>
        <li>账户数据将在30天后永久删除</li>
        <li><strong>此操作不可撤销</strong></li>
      </ul>
    </el-alert>

    <el-form label-width="100px">
      <el-form-item label="当前账户">
        <el-tag>{{ username }}</el-tag>
      </el-form-item>

      <el-form-item label="确认用户名">
        <el-input v-model="confirmUsername" :placeholder="`请输入 ${username}`" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        type="danger"
        @click="handleDelete"
        :loading="loading"
        :disabled="confirmUsername !== username"
      >
        确认注销
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAccountApi } from '@/api/share'
import { useRouter } from 'vue-router'

const props = defineProps({
  userId: { type: Number, required: true },
  username: { type: String, required: true },
})

const visible = defineModel()
const router = useRouter()
const loading = ref(false)
const confirmUsername = ref('')

async function handleDelete() {
  // 二次确认
  try {
    await ElMessageBox.confirm('确定要注销账户吗？此操作不可撤销！', '最后确认', {
      type: 'warning',
    })
  } catch {
    return
  }

  loading.value = true
  try {
    await deleteAccountApi({
      userId: props.userId,
      confirmUsername: confirmUsername.value,
    })
    ElMessage.success('账户已注销，感谢您的使用')
    visible.value = false

    // 清除登录状态，跳转到登录页
    localStorage.removeItem('token')
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  } catch {
    ElMessage.error('注销账户失败')
  } finally {
    loading.value = false
  }
}
</script>
