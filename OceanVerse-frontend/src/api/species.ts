import http from '@/utils/http'

// 物种列表（分页查询）
export const getSpeciesList = (params: any) => http.get('/species/list', { params })

// 物种详情
export const getSpeciesDetail = (id: number) => http.get(`/species/${id}`)

// 创建物种
export const createSpecies = (data: any) => http.post('/species', data)

// 更新物种
export const updateSpecies = (id: number, data: any) => http.put(`/species/${id}`, data)

// 删除物种
export const deleteSpecies = (id: number) => http.delete(`/species/${id}`)

// 物种统计
export const getSpeciesStats = () => http.get('/species/statistics')

// 物种分布信息
export const getSpeciesDistributions = (id: number) => http.get(`/species/${id}/distributions`)

// ==================== 物种媒体 ====================

// 上传物种图片（支持多文件）
export const uploadSpeciesMedia = (speciesId: number, files: File[]) => {
  const formData = new FormData()
  files.forEach(f => formData.append('files', f))
  return http.post(`/species/${speciesId}/media`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 获取物种的所有媒体资源
export const getSpeciesMedia = (speciesId: number) => http.get(`/species/${speciesId}/media`)

// 删除媒体资源
export const deleteSpeciesMedia = (mediaId: number) => http.delete(`/species/media/${mediaId}`)

// 设置主图
export const setPrimaryMedia = (mediaId: number) => http.put(`/species/media/${mediaId}/primary`)
