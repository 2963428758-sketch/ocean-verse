import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

const NO_REFRESH_URLS = ['/auth/login', '/auth/register', '/auth/refresh']

function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  router.push('/login')
}

http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        clearAuth()
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  async error => {
    // 网络级错误（后端未启动、代理失败）不走 Token 刷新流程
    if (!error.response) {
      ElMessage.error('网络连接失败，请检查后端服务是否已启动')
      return Promise.reject(error)
    }

    // 403 Forbidden — 权限不足，不触发登录重定向
    if (error.response.status === 403) {
      ElMessage.warning(error.response?.data?.message || '权限不足，无法执行此操作')
      return Promise.reject(error)
    }

    const originalRequest = error.config
    if (error.response.status === 401 && !originalRequest._retry) {
      const url = originalRequest.url || ''
      if (NO_REFRESH_URLS.some(u => url.includes(u))) {
        clearAuth()
        return Promise.reject(error)
      }

      const storedRefreshToken = localStorage.getItem('refreshToken')
      if (!storedRefreshToken) {
        clearAuth()
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise(resolve => {
          pendingRequests.push((newToken: string) => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            resolve(http(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const res = await axios.post('/api/auth/refresh', { refreshToken: storedRefreshToken })
        const data = res.data?.data
        if (res.data?.code === 200 && data?.accessToken) {
          localStorage.setItem('token', data.accessToken)
          if (data.refreshToken) {
            localStorage.setItem('refreshToken', data.refreshToken)
          }
          pendingRequests.forEach(cb => cb(data.accessToken))
          pendingRequests = []
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
          return http(originalRequest)
        } else {
          throw new Error('refresh failed')
        }
      } catch {
        clearAuth()
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }

    if (error.response?.status !== 401) {
      ElMessage.error(error.response?.data?.message || error.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default http
