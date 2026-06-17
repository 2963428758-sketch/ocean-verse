import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http from '@/utils/http'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref<number | null>(
    localStorage.getItem('userId') ? Number(localStorage.getItem('userId')) : null
  )
  const username = ref(localStorage.getItem('username') || '')
  const avatarUrl = ref(localStorage.getItem('avatarUrl') || '')
  const role = ref(localStorage.getItem('role') || '')
  const infoLoaded = ref(false)

  const isLoggedIn = computed(() => !!token.value)

  function setLoginInfo(data: { token: string; userId: number; username: string; avatarUrl?: string; role?: string }) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    avatarUrl.value = data.avatarUrl || ''
    role.value = data.role || ''
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', String(data.userId))
    localStorage.setItem('username', data.username)
    if (data.avatarUrl) localStorage.setItem('avatarUrl', data.avatarUrl)
    if (data.role) localStorage.setItem('role', data.role)
    infoLoaded.value = true
  }

  async function fetchUserInfo() {
    if (infoLoaded.value) return
    try {
      const res: any = await http.get('/auth/user/info')
      const data = res.data || res
      if (data) {
        userId.value = data.id ?? data.userId ?? userId.value
        username.value = data.username ?? username.value
        avatarUrl.value = data.avatarUrl ?? data.avatar_url ?? avatarUrl.value
        role.value = data.role ?? role.value
        localStorage.setItem('userId', String(userId.value))
        localStorage.setItem('username', username.value)
        if (avatarUrl.value) localStorage.setItem('avatarUrl', avatarUrl.value)
        if (role.value) localStorage.setItem('role', role.value)
      }
    } catch {
      // token invalid, ignore
    } finally {
      infoLoaded.value = true
    }
  }

  function logout() {
    token.value = ''
    userId.value = null
    username.value = ''
    avatarUrl.value = ''
    role.value = ''
    infoLoaded.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('avatarUrl')
    localStorage.removeItem('role')
  }

  return { token, userId, username, avatarUrl, role, isLoggedIn, infoLoaded, setLoginInfo, fetchUserInfo, logout }
})
