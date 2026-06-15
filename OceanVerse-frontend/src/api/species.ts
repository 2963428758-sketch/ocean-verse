import http from '@/utils/http'

// 物种列表
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
