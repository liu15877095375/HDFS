import api from '@/utils/api'   // 改为引用实际存在的工具文件

export async function createOrder() {
    const res = await api.post('/api/payment/create', {})
    return res   // res 就是 { amount, orderId }
  }

export async function createAlipayPage() {
  const res = await api.post('/api/payment/alipay-page', {}, {
    responseType: 'text'  // 接收HTML文本
  })
  return res  // 返回HTML内容
}

export function paymentCallback(orderId) {
  return api.post('/api/payment/callback', { orderId })
}

export function getMyOrders() {
  return api.get('/api/payment/orders')
}
