import http from '@/utils/http'

export const getPermissionTree = () =>
  http.get('/admin/permission/tree')

export const getMyPermissions = () =>
  http.get('/admin/permission/my')

export const getUserPermissions = (userId: number) =>
  http.get(`/admin/permission/user/${userId}`)

export const getRolePermissions = (roleId: number) =>
  http.get(`/admin/permission/role/${roleId}`)
