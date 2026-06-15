import http from '@/utils/http'

// 观测列表
export const getObservationList = (params: any) => http.get('/observation/list', { params })

// 观测详情
export const getObservationDetail = (id: number) => http.get(`/observation/${id}`)

// 创建观测
export const createObservation = (data: any) => http.post('/observation', data)

// 更新观测
export const updateObservation = (id: number, data: any) => http.put(`/observation/${id}`, data)

// 删除观测
export const deleteObservation = (id: number) => http.delete(`/observation/${id}`)
