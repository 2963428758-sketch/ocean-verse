import http from '@/utils/http'

// 登录
export const login = (data: { username: string; password: string }) =>
  http.post('/auth/login', data)

// 注册
export const register = (data: { username: string; password: string; email: string }) =>
  http.post('/auth/register', data)

// 获取用户信息
export const getUserInfo = () => http.get('/auth/info')

// 退出登录
export const logout = () => http.post('/auth/logout')
