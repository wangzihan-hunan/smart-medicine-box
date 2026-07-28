/**
 * WebSocket 实时通知客户端模块
 *
 * 功能：基于 STOMP over 原生 WebSocket 实现前后端实时通信，
 * 支持接收点对点通知、公告广播，以及连接状态管理。
 *
 * 设计说明：
 * - 使用单例模式管理全局 STOMP Client 实例
 * - 自动携带 JWT Token 进行握手认证
 * - 连接断开后自动重连（指数退避，最大 30 秒间隔）
 * - 提供全局事件派发，供 Vue 组件监听通知到达
 */

import { Client } from '@stomp/stompjs'
import { getToken } from './api'

/** STOMP 客户端实例 */
let stompClient = null

/** 当前是否已连接 */
let connected = false

/** 重连尝试次数 */
let reconnectAttempts = 0

/** 重连定时器 ID */
let reconnectTimer = null

/** 用户通知订阅句柄 */
let userSubscription = null

/** 公告订阅句柄 */
let announcementSubscription = null

/** 最大重连间隔（毫秒） */
const MAX_RECONNECT_INTERVAL = 30000

/** 基础重连间隔（毫秒） */
const BASE_RECONNECT_INTERVAL = 3000

/**
 * 获取 WebSocket 连接地址
 * 开发环境通过 Vite 代理访问，生产环境使用相对路径
 * @returns {string} WebSocket 端点 URL
 */
function getWsUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const host = window.location.host
  return `${protocol}://${host}/ws/notifications`
}

/**
 * 创建并配置 STOMP 客户端
 * @returns {Client} STOMP Client 实例
 */
function createClient() {
  const client = new Client({
    // 使用原生 WebSocket 作为底层传输
    webSocketFactory: () => new WebSocket(getWsUrl()),

    // 连接成功后触发
    onConnect: (frame) => {
      console.log('[WebSocket] 连接成功:', frame)
      connected = true
      reconnectAttempts = 0
      subscribeChannels(client)
      dispatchEvent('ws:connected')
    },

    // STOMP 协议层错误
    onStompError: (frame) => {
      console.error('[WebSocket] STOMP 错误:', frame.headers['message'], frame.body)
      dispatchEvent('ws:error', { message: frame.headers['message'] })
    },

    // 底层 WebSocket 关闭时触发
    onWebSocketClose: () => {
      console.warn('[WebSocket] 连接断开')
      connected = false
      unsubscribeChannels()
      dispatchEvent('ws:disconnected')
      scheduleReconnect()
    },

    // 调试日志（开发环境启用）
    debug: (str) => {
      if (import.meta.env.DEV) {
        console.log('[WebSocket]', str)
      }
    },

    // 每次连接前附加 JWT Token 到自定义头
    connectHeaders: {
      Authorization: `Bearer ${getToken() || ''}`
    },

    // 心跳配置（毫秒）
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,

    // 自动重交由上层逻辑控制，关闭内置自动重连
    reconnectDelay: 0
  })

  return client
}

/**
 * 订阅所有需要的消息通道
 * @param {Client} client - 已连接的 STOMP 客户端
 */
function subscribeChannels(client) {
  // 订阅个人通知队列（点对点）
  userSubscription = client.subscribe('/user/queue/notifications', (message) => {
    try {
      const payload = JSON.parse(message.body)
      console.log('[WebSocket] 收到通知:', payload)
      dispatchEvent('ws:notification', payload)
    } catch (e) {
      console.error('[WebSocket] 解析通知消息失败:', e)
    }
  })

  // 订阅系统公告（广播）
  announcementSubscription = client.subscribe('/topic/announcements', (message) => {
    try {
      const payload = JSON.parse(message.body)
      console.log('[WebSocket] 收到公告:', payload)
      dispatchEvent('ws:announcement', payload)
    } catch (e) {
      console.error('[WebSocket] 解析公告消息失败:', e)
    }
  })
}

/**
 * 取消所有订阅
 */
function unsubscribeChannels() {
  if (userSubscription) {
    userSubscription.unsubscribe()
    userSubscription = null
  }
  if (announcementSubscription) {
    announcementSubscription.unsubscribe()
    announcementSubscription = null
  }
}

/**
 * 派发全局自定义事件
 * @param {string} eventName - 事件名称
 * @param {any} detail - 事件携带数据
 */
function dispatchEvent(eventName, detail = null) {
  window.dispatchEvent(new CustomEvent(eventName, { detail }))
}

/**
 * 调度自动重连
 * 使用指数退避策略，最大间隔不超过 MAX_RECONNECT_INTERVAL
 */
function scheduleReconnect() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
  }

  const delay = Math.min(
    BASE_RECONNECT_INTERVAL * Math.pow(2, reconnectAttempts),
    MAX_RECONNECT_INTERVAL
  )
  reconnectAttempts++

  console.log(`[WebSocket] ${delay}ms 后尝试第 ${reconnectAttempts} 次重连...`)

  reconnectTimer = setTimeout(() => {
    if (!connected) {
      connect()
    }
  }, delay)
}

/**
 * 建立 WebSocket 连接
 * 如果已有连接则先断开，确保状态干净
 */
export function connect() {
  // 避免重复连接
  if (stompClient && connected) {
    console.log('[WebSocket] 已经连接，跳过')
    return
  }

  // 无 Token 时不连接（未登录状态）
  const token = getToken()
  if (!token) {
    console.log('[WebSocket] 无 Token，暂不连接')
    return
  }

  // 断开旧连接
  disconnect()

  stompClient = createClient()
  stompClient.activate()
}

/**
 * 断开 WebSocket 连接并清理资源
 */
export function disconnect() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }

  unsubscribeChannels()

  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }

  connected = false
  reconnectAttempts = 0
  console.log('[WebSocket] 已断开连接')
}

/**
 * 获取当前连接状态
 * @returns {boolean} 是否已连接
 */
export function isConnected() {
  return connected
}
