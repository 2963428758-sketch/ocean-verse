import http from '@/utils/http'

export interface CaptchaVO {
  captchaKey: string
  expression: string
  expireSeconds: number
}

export const getCaptcha = () =>
  http.get<CaptchaVO>('/captcha')

export const login = (data: { username: string; password: string; captchaKey: string; captchaCode: string }) =>
  http.post('/auth/login', data)

export const register = (data: { username: string; password: string; captchaKey: string; captchaCode: string }) =>
  http.post('/auth/register', data)

export const getUserInfo = () => http.get('/auth/info')

export const logout = () => http.post('/auth/logout')

export const refreshToken = (refreshToken: string) =>
  http.post('/auth/refresh', { refreshToken })
