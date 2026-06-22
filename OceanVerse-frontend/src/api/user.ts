import http from '@/utils/http'

export const getProfile = () => http.get('/auth/info')

export const updateProfile = (data: { nickname?: string; realName?: string; avatarUrl?: string }) =>
  http.put('/user/profile', data)

export const updatePassword = (data: { oldPassword: string; newPassword: string }) =>
  http.put('/user/password', data)

export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/user/avatar', formData)
}

export const deleteAccount = () => http.delete('/user/account')

export const getLoginHistory = (page: number, size: number) =>
  http.get('/user/login-history', { params: { page, size } })
