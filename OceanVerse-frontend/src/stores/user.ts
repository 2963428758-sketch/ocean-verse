import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getProfile } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const userId = ref<number | null>(null)
  const username = ref('')
  const nickname = ref('')
  const email = ref('')
  const phone = ref('')
  const realName = ref('')
  const avatarUrl = ref('')
  const role = ref('')
  const dataScope = ref(1)
  const status = ref(1)
  const createTime = ref('')
  const infoLoaded = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'SUPER_ADMIN' || role.value === 'ADMIN')

  function setLoginInfo(data: { token?: string; accessToken?: string; refreshToken?: string; userId: number; username: string; avatarUrl?: string; role?: string }) {
    token.value = data.accessToken || data.token || ''
    userId.value = data.userId
    username.value = data.username
    avatarUrl.value = data.avatarUrl || ''
    role.value = data.role || ''
    localStorage.setItem('token', token.value)
    if (data.refreshToken) {
      refreshToken.value = data.refreshToken
      localStorage.setItem('refreshToken', data.refreshToken)
    }
  }

  function setUserInfo(data: Record<string, any>) {
    userId.value = data.id ?? userId.value
    username.value = data.username ?? username.value
    nickname.value = data.nickname ?? ''
    email.value = data.email ?? ''
    phone.value = data.phone ?? ''
    realName.value = data.realName ?? ''
    avatarUrl.value = data.avatarUrl ?? avatarUrl.value
    role.value = data.role ?? role.value
    dataScope.value = data.dataScope ?? 1
    status.value = data.status ?? 1
    createTime.value = data.createTime ?? ''
    infoLoaded.value = true
  }

  async function fetchUserInfo() {
    try {
      const res: any = await getProfile()
      if (res.code === 200 && res.data) {
        setUserInfo(res.data)
      }
    } catch {
      // ignore
    }
  }

  function setToken(accessToken: string, newRefreshToken?: string) {
    token.value = accessToken
    localStorage.setItem('token', accessToken)
    if (newRefreshToken) {
      refreshToken.value = newRefreshToken
      localStorage.setItem('refreshToken', newRefreshToken)
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
    dataScope.value = 1
    status.value = 1
    createTime.value = ''
    infoLoaded.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  return {
    token, refreshToken, userId, username, nickname, email, phone, realName,
    avatarUrl, role, dataScope, status, createTime, infoLoaded,
    isLoggedIn, isAdmin,
    setLoginInfo, setUserInfo, setToken, fetchUserInfo, logout
  }
})
