import api from '@/utils/api'

export function analyzeFileApi(params) {
  const queryParams = new URLSearchParams()
  if (params.fileId) {
    queryParams.append('fileId', params.fileId)
  }
  if (params.prompt) {
    queryParams.append('prompt', params.prompt)
  }
  return api.post(`/api/ai/analyze?${queryParams.toString()}`)
}
