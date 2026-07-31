// 认证状态管理模块
// 功能：使用 Pinia 管理用户认证状态，包括 Token、用户信息、
// 登录状态、管理员角色判断，以及登录/登出/获取用户信息等操作
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI, setToken } from '../utils/api'
import { connect as wsConnect, disconnect as wsDisconnect } from '../utils/websocket'

/**
 * 从 JWT token 中解码 payload 部分
 * JWT 由三部分组成：header.payload.signature，payload 是 Base64Url 编码的 JSON
 * @param {string} token - JWT token 字符串
 * @returns {Object|null} 解码后的 payload 对象，失败返回 null
 */
function decodeJwtPayload(token) {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const payload = parts[1]
    // Base64Url 转 Base64
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(atob(base64).split('').map(c =>
      '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
    ).join(''))
    return JSON.parse(json)
  } catch {
    return null
  }
}

// 定义认证 Store（使用 Setup 语法）
// persist 配置：状态持久化到 sessionStorage（浏览器关闭后自动清除，降低 XSS 持久化风险）
export const useAuthStore = defineStore('auth', () => {
  // Token 状态
  const token = ref(null)
  // 用户信息状态
  const user = ref(null)
  // 计算属性：是否已登录（根据 Token 是否存在判断）
  const isLoggedIn = computed(() => !!token.value)
  // 计算属性：是否为管理员
  // 优先从 user 对象中获取角色（API 返回的权威数据），
  // 其次从 JWT token 中解码角色（登录后立即可用，无需等待 fetchUserInfo）
  const isAdmin = computed(() => {
    if (user.value?.role === 'ADMIN') return true
    if (token.value) {
      const payload = decodeJwtPayload(token.value)
      return payload?.role === 'ADMIN'
    }
    return false
  })

  // 登录：设置 Token 并保存到本地存储，同时建立 WebSocket 连接
  function login(accessToken) {
    token.value = accessToken
    setToken(accessToken)
    // 登录成功后连接实时通知推送服务
    wsConnect()
  }

  // 获取当前用户信息
  // 成功时更新 user 状态并返回用户数据，失败时返回 null
  async function fetchUserInfo() {
    try {
      const response = await authAPI.getProfile()
      user.value = response
      return response
    } catch (error) {
      console.error('获取用户信息失败:', error)
      return null
    }
  }

  // 登出：断开 WebSocket 连接，清除 Token 和用户信息
  function logout() {
    // 先断开实时推送连接，避免后续收到无效消息
    wsDisconnect()
    token.value = null
    user.value = null
  }

  // 返回可在组件中使用的状态和方法
  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    login,
    fetchUserInfo,
    logout
  }
}, {
  // 持久化配置
  persist: {
    key: 'medicine-box-auth',
    storage: sessionStorage,
    paths: ['token', 'user']
  }
})
