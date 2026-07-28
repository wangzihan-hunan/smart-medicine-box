import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../utils/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('access_token'))
  const userInfo = ref({})

  const isLoggedIn = computed(() => !!token.value)

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('access_token', newToken)
    localStorage.setItem('isLoggedIn', 'true')
  }

  const setUserInfo = (info) => {
    userInfo.value = info
    if (info?.phone) {
      localStorage.setItem('userPhone', info.phone)
    }
  }

  const login = async (loginData) => {
    const res = await authApi.login(loginData)
    setToken(res.accessToken)
    const info = await authApi.getUserInfo()
    setUserInfo(info)
    return res
  }

  const logout = () => {
    token.value = null
    userInfo.value = {}
    localStorage.removeItem('access_token')
    localStorage.removeItem('isLoggedIn')
    localStorage.removeItem('userPhone')
  }

  const fetchUserInfo = async () => {
    if (!token.value) return
    try {
      const info = await authApi.getUserInfo()
      setUserInfo(info)
    } catch {
      logout()
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    logout,
    fetchUserInfo,
    setToken,
    setUserInfo
  }
})