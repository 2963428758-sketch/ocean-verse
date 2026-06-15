import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref<number | null>(null)
  const username = ref('')
  const avatarUrl = ref('')

  const isLoggedIn = computed(() => !!token.value)

  function setLoginInfo(data: { token: string; userId: number; username: string; avatarUrl?: string }) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    avatarUrl.value = data.avatarUrl || ''
    localStorage.setItem('token', data.token)
  }

  function logout() {
    token.value = ''
    userId.value = null
    username.value = ''
    avatarUrl.value = ''
    localStorage.removeItem('token')
  }

  return { token, userId, username, avatarUrl, isLoggedIn, setLoginInfo, logout }
})
