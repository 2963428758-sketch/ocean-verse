import http from '@/utils/http'

/**
 * 导出物种分布数据
 * GET /api/visual/export/species?format=excel&all=true&iucnStatus=VU,EN&family=...
 */
export const exportSpecies = (params: {
  format: 'excel' | 'csv'
  all?: boolean
  iucnStatus?: string
  family?: string
}) => http.get('/visual/export/species', { params, responseType: 'blob' })

/**
 * 导出观测记录数据
 * GET /api/visual/export/observation?format=excel&all=true&observationType=...&startDate=...&endDate=...
 */
export const exportObservation = (params: {
  format: 'excel' | 'csv'
  all?: boolean
  observationType?: string
  startDate?: string
  endDate?: string
}) => http.get('/visual/export/observation', { params, responseType: 'blob' })

/**
 * 导出统计汇总数据（多 Sheet）
 * GET /api/visual/export/statistics?format=excel&period=monthly
 */
export const exportStatistics = (params: {
  format: 'excel' | 'csv'
  period?: 'monthly' | 'weekly' | 'daily'
}) => http.get('/visual/export/statistics', { params, responseType: 'blob' })

/**
 * 预览观测记录（带筛选，返回前 20 条 JSON）
 * GET /api/visual/export/observation/preview
 */
export const getObservationPreview = (params: {
  observationType?: string
  startDate?: string
  endDate?: string
}) => http.get('/visual/export/observation/preview', { params })
