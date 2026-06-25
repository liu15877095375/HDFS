<template>
    <div class="change-password-container">
      <h2>修改密码</h2>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        style="max-width: 460px; margin: 20px auto;"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            show-password
            placeholder="请输入原密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码（至少6位）"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitForm">确认修改</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </template>
  
  <script setup>
  import { reactive, ref } from 'vue';
  import { ElMessage } from 'element-plus'; // 若使用 Element Plus
  import { changePassword } from '@/api/user';
  import { useRouter } from 'vue-router';
  
  const router = useRouter();
  const formRef = ref(null);
  
  const form = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  
  // 自定义确认密码验证
  const validateConfirmPassword = (rule, value, callback) => {
    if (value !== form.newPassword) {
      callback(new Error('两次输入的新密码不一致'));
    } else {
      callback();
    }
  };
  
  const rules = {
    oldPassword: [
      { required: true, message: '请输入原密码', trigger: 'blur' }
    ],
    newPassword: [
      { required: true, message: '请输入新密码', trigger: 'blur' },
      { min: 6, message: '新密码长度不能少于6位', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: '请确认新密码', trigger: 'blur' },
      { validator: validateConfirmPassword, trigger: 'blur' }
    ]
  };
  
  const submitForm = () => {
    formRef.value.validate(async (valid) => {
      if (!valid) return;
  
      try {
        const res = await changePassword({
          oldPassword: form.oldPassword,
          newPassword: form.newPassword,
        });
  
        if (res.code === 200) {
          ElMessage.success('密码修改成功，请重新登录');
          // 清除本地 token 并跳转到登录页
          localStorage.removeItem('token');
          router.push('/login');
        } else {
          ElMessage.error(res.message || '修改失败');
        }
      } catch (error) {
        // 根据后端返回 (如 400 错误)
        if (error.response?.data?.message) {
          ElMessage.error(error.response.data.message);
        } else {
          ElMessage.error('网络错误，请稍后再试');
        }
      }
    });
  };
  
  const resetForm = () => {
    formRef.value?.resetFields();
  };
  </script>
  
  <style scoped>
.change-password-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 48px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.change-password-container::before {
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

.change-password-container::after {
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

.change-password-container h2 {
  font-size: 36px;
  font-weight: 800;
  color: white;
  margin-bottom: 32px;
  text-align: center;
  position: relative;
  z-index: 1;
}

.change-password-container > :deep(.el-form) {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-radius: var(--radius-xl);
  box-shadow:
    0 50px 100px -20px rgba(0, 0, 0, 0.25),
    0 30px 60px -30px rgba(0, 0, 0, 0.3);
  padding: 48px;
  position: relative;
  z-index: 1;
  animation: fadeInUp 0.6s ease-out;
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

:deep(.el-button--default) {
  border: 1px solid var(--border-light);
  background: white;
  color: var(--text-secondary);
}

:deep(.el-button--default:hover) {
  border-color: var(--primary-400);
  color: var(--primary-600);
  background: var(--primary-50);
}
</style>