import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: 'http://localhost:8080', // 后端 IP 和端口
  timeout: 300000, // 5分钟
})

// 请求拦截器：自动附加 token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    // 如果是 blob 类型（如文件下载），直接返回整个 response 对象
    if (response.config.responseType === 'blob') {
      return response
    }
    // 普通 JSON 请求直接返回 data
    return response.data
  },
  (error) => {
    const msg = error.response?.data?.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(error)
  },
)

export default api
