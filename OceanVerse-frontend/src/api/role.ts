import http from '@/utils/http'

export const listRoles = (params: { page: number; size: number; keyword?: string }) =>
  http.get('/admin/role/list', { params })

export const getRole = (roleId: number) =>
  http.get(`/admin/role/${roleId}`)

export const createRole = (data: { roleCode: string; roleName: string; description?: string }) =>
  http.post('/admin/role', data)

export const updateRole = (roleId: number, data: { roleName: string; description?: string; status?: number }) =>
  http.put(`/admin/role/${roleId}`, data)

export const deleteRole = (roleId: number) =>
  http.delete(`/admin/role/${roleId}`)

export const toggleRoleStatus = (roleId: number, status: number) =>
  http.put(`/admin/role/${roleId}/status`, { status })

export const assignPermissions = (roleId: number, permissionIds: number[]) =>
  http.post('/admin/role/assign-permissions', { roleId, permissionIds })
