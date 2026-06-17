import http from '@/utils/http'

export const getProfile = () => http.get('/auth/info')

export const updateProfile = (data: { realName?: string; email?: string; phone?: string; avatarUrl?: string }) =>
  http.put('/user/profile', data)

export const updatePassword = (data: { oldPassword: string; newPassword: string }) =>
  http.put('/user/password', data)
