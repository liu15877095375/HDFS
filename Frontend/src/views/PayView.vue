<template>
  <!-- 支付成功界面 -->
  <div v-if="isPaySuccess" class="success-container">
    <div class="success-card">
      <div class="success-icon">✓</div>
      <h2>恭喜您！已成功升级为 VIP 会员</h2>
      <p class="success-message">您的VIP权益已生效，享受100GB超大存储空间</p>
      <div class="vip-info">
        <div class="info-item">
          <span class="label">会员等级</span>
          <span class="value">VIP用户</span>
        </div>
        <div class="info-item">
          <span class="label">存储空间</span>
          <span class="value">100GB</span>
        </div>
        <div class="info-item">
          <span class="label">有效期至</span>
          <span class="value">{{ vipExpire || '暂无' }}</span>
        </div>
      </div>
      <el-button type="primary" size="large" @click="goHome">
        返回首页
      </el-button>
    </div>
  </div>

  <!-- 正常支付页面 -->
  <div v-else class="pay-container">
    <h2>升级 VIP 会员</h2>
    <div class="vip-card">
      <h3>VIP 会员</h3>
      <p class="price">¥19.90<small>/月</small></p>
      <ul class="benefits">
        <li>100GB 超大存储空间</li>
        <li>高速下载通道</li>
        <li>有效期30天，可续费叠加</li>
      </ul>
      <el-button type="primary" size="large" @click="handlePay" :loading="loading">
        立即开通
      </el-button>
    </div>

    <!-- 订单记录 -->
    <div class="order-history" v-if="orders && orders.length">
      <h3>我的订单</h3>
      <el-table :data="orders" stripe>
        <el-table-column prop="orderId" label="订单号" width="120" />
        <el-table-column label="套餐">
          <template #default>VIP会员</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额(元)" />
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'info'"
            >
              {{ row.status === 1 ? '支付成功' : row.status === 2 ? '支付失败' : '待支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { createAlipayPage, getMyOrders } from '@/api/payment'
import { getUserInfoApi } from '@/api/user'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const orders = ref([])
const vipExpire = ref('')

// 直接从 localStorage 读取用户信息的函数
const loadUserFromStorage = () => {
  try {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)
      console.log('从 localStorage 读取的用户信息:', user)
      return user
    }
  } catch (e) {
    console.error('读取 localStorage 失败', e)
  }
  return null
}

// 判断是否是支付成功回调
const isPaySuccess = computed(() => {
  return route.path === '/pay/result' || route.query.out_trade_no
})

const handlePay = async () => {
  loading.value = true
  try {
    const htmlContent = await createAlipayPage()
    const newWindow = window.open('', '_blank', 'width=800,height=600')
    if (newWindow) {
      newWindow.document.write(htmlContent)
      newWindow.document.close()
      ElMessage.success('正在跳转到支付宝沙箱进行支付，请支付完成后返回页面')

      let checkCount = 0
      const checkInterval = setInterval(async () => {
        checkCount++
        try {
          await fetchOrders()
          const latestOrder = orders.value[0]
          if (latestOrder && latestOrder.status === 1) {
            clearInterval(checkInterval)
            ElMessage.success('开通成功，VIP权益已生效！')
            await refreshUserInfo()
            window.location.reload()
          }
        } catch (e) {
          console.error('检查订单失败', e)
        }

        if (checkCount >= 60) {
          clearInterval(checkInterval)
          ElMessage.warning('支付超时，请稍后在订单列表查看')
        }
      }, 2000)
    } else {
      ElMessage.error('浏览器阻止了弹窗，请允许弹窗后重试')
    }
  } catch (error) {
    console.error('支付失败完整错误:', error)
    const msg = error.response?.data?.message || error.message || '支付失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

const fetchOrders = async () => {
  try {
    const res = await getMyOrders()
    orders.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const refreshUserInfo = async () => {
  try {
    // 先尝试从 localStorage 读取（注意是 vip_expire）
    const localUser = loadUserFromStorage()
    if (localUser) {
      const expireTime = localUser.vip_expire || localUser.vipExpire || ''
      if (expireTime) {
        vipExpire.value = expireTime
        console.log('从 localStorage 成功获取日期:', expireTime)
        return
      }
    }
    
    // 如果 localStorage 没有，再从 API 获取
    const res = await getUserInfoApi()
    localStorage.setItem('user', JSON.stringify(res.data))
    vipExpire.value = res.data.vip_expire || res.data.vipExpire || ''
    console.log('从 API 获取日期:', vipExpire.value)
  } catch (e) {
    console.error('刷新用户信息失败', e)
  }
}

const goHome = async () => {
  await refreshUserInfo()
  router.push('/')
}

onMounted(async () => {
  // 先立即从 localStorage 读取
  const localUser = loadUserFromStorage()
  if (localUser) {
    vipExpire.value = localUser.vip_expire || localUser.vipExpire || ''
  }
  
  // 再并行获取订单和用户信息
  await Promise.all([fetchOrders(), refreshUserInfo()])
})

onActivated(async () => {
  // 先立即从 localStorage 读取
  const localUser = loadUserFromStorage()
  if (localUser) {
    vipExpire.value = localUser.vip_expire || localUser.vipExpire || ''
  }
  
  await Promise.all([fetchOrders(), refreshUserInfo()])
})
</script>

<style scoped>
/* 成功界面样式 - 背景改为紫色 */
.success-container {
min-height: 100vh;
display: flex;
align-items: center;
justify-content: center;
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
padding: 24px;
}

.success-card {
background: white;
border-radius: 20px;
padding: 48px;
text-align: center;
box-shadow: 0 20px 60px -12px rgba(0, 0, 0, 0.25);
max-width: 450px;
width: 100%;
}

.success-icon {
width: 100px;
height: 100px;
background: linear-gradient(135deg, #10b981 0%, #059669 100%);
border-radius: 50%;
display: flex;
align-items: center;
justify-content: center;
margin: 0 auto 24px;
font-size: 56px;
color: white;
font-weight: bold;
}

.success-card h2 {
font-size: 28px;
font-weight: 700;
color: #1f2937;
margin-bottom: 12px;
}

.success-message {
color: #6b7280;
font-size: 16px;
margin-bottom: 32px;
}

.vip-info {
background: #f8fafc;
border-radius: 16px;
padding: 24px;
margin-bottom: 32px;
}

.info-item {
display: flex;
justify-content: space-between;
padding: 12px 0;
border-bottom: 1px solid #e2e8f0;
}

.info-item:last-child {
border-bottom: none;
}

.info-item .label {
color: #6b7280;
font-size: 14px;
}

.info-item .value {
color: #1f2937;
font-weight: 600;
font-size: 14px;
}

/* 支付页面样式 */
.pay-container {
max-width: 700px;
margin: 0 auto;
padding: 48px 24px;
min-height: 100vh;
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.pay-container h2 {
text-align: center;
color: white;
font-size: 32px;
font-weight: 700;
margin-bottom: 32px;
}

.vip-card {
text-align: center;
border: none;
border-radius: 16px;
padding: 48px 40px;
background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
margin-bottom: 40px;
box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
position: relative;
overflow: hidden;
}

.vip-card::before {
content: '';
position: absolute;
top: 0;
left: 0;
right: 0;
height: 4px;
background: linear-gradient(90deg, #f59e0b, #d97706, #f59e0b);
}

.vip-card h3 {
font-size: 28px;
font-weight: 800;
color: #78350f;
margin-bottom: 8px;
}

.price {
font-size: 48px;
font-weight: 800;
background: linear-gradient(135deg, #b45309 0%, #d97706 100%);
-webkit-background-clip: text;
-webkit-text-fill-color: transparent;
background-clip: text;
margin: 16px 0;
}

.price small {
font-size: 18px;
color: #78350f;
opacity: 0.8;
}

.benefits {
list-style: none;
padding: 0;
margin: 28px auto;
color: #78350f;
}

.benefits li {
margin: 12px 0;
font-size: 16px;
display: flex;
align-items: center;
justify-content: center;
gap: 8px;
}

.benefits li::before {
content: '✓';
color: #10b981;
font-weight: bold;
font-size: 20px;
}

:deep(.el-button--primary) {
background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
border: none;
padding: 16px 48px;
font-size: 18px;
font-weight: 700;
border-radius: 16px;
box-shadow: 0 8px 25px rgba(245, 158, 11, 0.4);
transition: all 0.3s ease;
}

:deep(.el-button--primary:hover) {
transform: translateY(-3px);
box-shadow: 0 12px 35px rgba(245, 158, 11, 0.5);
}

.order-history {
margin-top: 48px;
background: white;
border-radius: 16px;
padding: 32px;
box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.order-history h3 {
font-size: 20px;
font-weight: 700;
color: #1f2937;
margin-bottom: 20px;
}

:deep(.el-table) {
border-radius: 12px;
overflow: hidden;
box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

:deep(.el-table th) {
background: #f8fafc;
color: #475569;
font-weight: 600;
}
</style>