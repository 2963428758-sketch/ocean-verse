import http from '@/utils/http'

export const listUsers = (params: { page: number; size: number; keyword?: string }) =>
  http.get('/admin/user/list', { params })

export const updateUserStatus = (userId: number, status: number) =>
  http.put(`/admin/user/${userId}/status`, { status })

export const assignRoles = (userId: number, roleIds: number[]) =>
  http.post(`/admin/user/${userId}/roles`, { roleIds })

export const forceLogout = (userId: number) =>
  http.post(`/admin/user/${userId}/force-logout`)

export const listLoginLogs = (params: {
  page: number
  size: number
  username?: string
  status?: number
}) => http.get('/admin/log/login/list', { params })

export const listOperationLogs = (params: {
  page: number
  size: number
  module?: string
  operatorName?: string
}) => http.get('/admin/log/operation/list', { params })
