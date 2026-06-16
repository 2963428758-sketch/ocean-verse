import http from '@/utils/http'

export const listUsers = (params: { page: number; size: number; keyword?: string }) =>
  http.get('/admin/user/list', { params })

export const updateUserStatus = (userId: number, status: number) =>
  http.put(`/admin/user/${userId}/status`, { status })
