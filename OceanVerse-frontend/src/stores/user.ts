import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http from '@/utils/http'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const userId = ref<number | null>(
    localStorage.getItem('userId') ? Number(localStorage.getItem('userId')) : null
  )
  const username = ref(localStorage.getItem('username') || '')
  const nickname = ref('')
  const email = ref('')
  const phone = ref('')
  const realName = ref('')
  const avatarUrl = ref(localStorage.getItem('avatarUrl') || '')
  const role = ref(localStorage.getItem('role') || '')
  const createTime = ref('')
  const infoLoaded = ref(false)

  const isLoggedIn = computed(() => !!token.value)

  function setLoginInfo(data: { accessToken: string; refreshToken: string; userId: number; username: string; avatarUrl?: string; role?: string; nickname?: string }) {
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    userId.value = data.userId
    username.value = data.username
    nickname.value = data.nickname || ''
    avatarUrl.value = data.avatarUrl || ''
    role.value = data.role || ''
    localStorage.setItem('token', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('userId', String(data.userId))
    localStorage.setItem('username', data.username)
    if (data.avatarUrl) localStorage.setItem('avatarUrl', data.avatarUrl)
    if (data.role) localStorage.setItem('role', data.role)
    infoLoaded.value = true
  }

  function setUserInfo(data: { id?: number; userId?: number; username?: string; nickname?: string; email?: string; phone?: string; realName?: string; avatarUrl?: string; role?: string; createTime?: string }) {
    userId.value = data.id ?? data.userId ?? userId.value
    username.value = data.username ?? username.value
    nickname.value = data.nickname ?? ''
    email.value = data.email ?? ''
    phone.value = data.phone ?? ''
    realName.value = data.realName ?? ''
    avatarUrl.value = data.avatarUrl ?? avatarUrl.value
    role.value = data.role ?? role.value
    createTime.value = data.createTime ?? ''
    if (data.avatarUrl) localStorage.setItem('avatarUrl', data.avatarUrl)
    if (data.role) localStorage.setItem('role', data.role)
  }

  async function fetchUserInfo() {
    if (infoLoaded.value) return
    try {
      const res: any = await http.get('/auth/info')
      const data = res.data || res
      if (data) {
        setUserInfo(data)
      }
    } catch {
      // token invalid, ignore
    } finally {
      infoLoaded.value = true
    }
  }

  function logout() {
    token.value = ''
    refreshToken.value = ''
    userId.value = null
    username.value = ''
    nickname.value = ''
    email.value = ''
    phone.value = ''
    realName.value = ''
    avatarUrl.value = ''
    role.value = ''
    createTime.value = ''
    infoLoaded.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('avatarUrl')
    localStorage.removeItem('role')
  }

  return { token, refreshToken, userId, username, nickname, email, phone, realName, avatarUrl, role, createTime, isLoggedIn, infoLoaded, setLoginInfo, setUserInfo, fetchUserInfo, logout }
})
