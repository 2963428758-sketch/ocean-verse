import http from '@/utils/http'

// 仪表盘数据
export const getDashboardData = () => http.get('/visual/dashboard')

// 物种分布
export const getSpeciesDistribution = (params?: any) =>
  http.get('/visual/species/distribution', { params })

// 观测趋势
export const getObservationTrend = (params?: any) =>
  http.get('/visual/trend/observation', { params })

// 按科统计
export const getSpeciesByFamily = () => http.get('/visual/statistics/family')

// IUCN 统计
export const getSpeciesByIucn = () => http.get('/visual/statistics/iucn')
