import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref<number | null>(
    localStorage.getItem('userId') ? Number(localStorage.getItem('userId')) : null
  )
  const username = ref(localStorage.getItem('username') || '')
  const avatarUrl = ref(localStorage.getItem('avatarUrl') || '')

  const isLoggedIn = computed(() => !!token.value)

  function setLoginInfo(data: { token: string; userId: number; username: string; avatarUrl?: string }) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    avatarUrl.value = data.avatarUrl || ''
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', String(data.userId))
    localStorage.setItem('username', data.username)
    if (data.avatarUrl) localStorage.setItem('avatarUrl', data.avatarUrl)
  }

  function logout() {
    token.value = ''
    userId.value = null
    username.value = ''
    avatarUrl.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('avatarUrl')
  }

  return { token, userId, username, avatarUrl, isLoggedIn, setLoginInfo, logout }
})
