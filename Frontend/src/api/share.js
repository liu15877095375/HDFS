import api from '@/utils/api'

/** 创建分享 */
export function createShareApi(data) {
  return api.post('/api/share', data)
}

/** 获取分享详情（公开接口） */
export function getShareDetailApi(link, extractCode) {
  const params = extractCode ? { extractCode } : {}
  return api.get(`/api/share/${link}`, { params })
}

/** 删除分享 */
export function deleteShareApi(link) {
  return api.delete(`/api/share/${link}`)
}

/** 获取我的分享列表 */
export function getMySharesApi() {
  return api.get('/api/share/my')
}

/** 注销账户 */
export function deleteAccountApi(data) {
  return api.post('/api/account/delete', data)
}
