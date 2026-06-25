import api from '@/utils/api'

/** 上传文件（带进度回调） */
export function uploadFileApi(formData, onProgress) {
  return api.post('/api/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round((progressEvent.loaded / progressEvent.total) * 100)
        onProgress(percent)
      }
    },
  })
}

// 下载文件（返回 blob）
export function downloadFileApi(fileId) {
  return api.get(`/api/files/download/${fileId}`, {
    responseType: 'blob',
  })
}

/** 删除文件（软删除） */
export function deleteFileApi(fileId) {
  return api.delete(`/api/files/delete/${fileId}`)
}

/** 删除文件夹（软删除） */
export function deleteDirectoryApi(dirId) {
  return api.delete(`/api/files/delete-dir/${dirId}`)
}

/** 获取回收站文件列表 */
export function getRecycleBinApi() {
  return api.get('/api/files/recycle/list')
}

/** 恢复回收站文件 */
export function restoreFileApi(fileId) {
  return api.put(`/api/files/restore/${fileId}`)
}

/** 新建文件夹 */
export function createFolderApi(data) {
  return api.post('/api/files/mkdir', data)
}

// 获取文件列表（支持传参）
export function getFileListApi(params) {
  return api.get('/api/files/list', { params })
}

/** 彻底删除回收站文件 */
export function permanentDeleteApi(fileIds) {
  return api.delete('/api/files/permanent-delete', { data: { file_ids: fileIds } })
}
