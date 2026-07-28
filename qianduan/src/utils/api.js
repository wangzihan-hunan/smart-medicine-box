// API 接口封装模块
// 功能：统一管理所有 HTTP 请求，包括 axios 实例配置、请求/响应拦截器、
// Token 管理、请求取消、内存缓存、自动重试以及各业务模块的 API 接口定义

import axios from 'axios'

// ==================== 常量定义 ====================

/** API 基础地址前缀 */
const API_BASE_URL = '/api/v1'
/** 缓存有效期：5 分钟（毫秒） */
const CACHE_TTL = 5 * 60 * 1000
/** Token 存储键名（统一使用 JSON 对象存储） */
const TOKEN_KEY = 'medicine-box-auth'

// ==================== 全局状态 ====================

/**
 * 正在进行的请求映射表
 * 键：请求唯一标识（method:url:params）
 * 值：AbortController 实例
 */
const pendingRequests = new Map()

/**
 * 内存缓存映射表
 * 键：请求唯一标识
 * 值：{ data: 响应数据, timestamp: 缓存时间戳 }
 */
const memoryCache = new Map()

// ==================== Axios 实例 ====================

/**
 * axios 实例配置
 * - baseURL: 请求基础路径
 * - timeout: 请求超时时间 10 秒
 * - withCredentials: 携带 Cookie
 * - headers: 默认 JSON 内容类型
 */
const instance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ==================== 请求拦截器 ====================

/**
 * 请求拦截器：在发送请求前统一处理
 * 1. 从本地存储获取 Token 并添加到请求头
 * 2. 添加防缓存时间戳（仅 GET）
 * 3. 取消同一 URL 的重复请求（防止竞态条件）
 * 4. 初始化重试计数器
 * 5. 开发环境打印请求日志
 */
instance.interceptors.request.use(
  config => {
    // 1. Token 注入
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 2. 防缓存：GET 请求添加时间戳
    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now()
      }
    }

    // 3. 请求取消：取消同一 URL 的旧请求
    const requestKey = getRequestKey(config)
    if (pendingRequests.has(requestKey)) {
      pendingRequests.get(requestKey).abort('DUPLICATE')
    }
    const controller = new AbortController()
    config.signal = controller.signal
    pendingRequests.set(requestKey, controller)

    // 4. 初始化重试计数
    config._retryCount = config._retryCount || 0

    // 5. 开发环境请求日志
    if (import.meta.env.DEV) {
      console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`, {
        params: config.params,
        data: config.data
      })
    }

    return config
  },
  error => {
    if (import.meta.env.DEV) {
      console.error('[API Request Error]', error)
    }
    return Promise.reject(error)
  }
)

// ==================== 响应拦截器 ====================

/**
 * 响应拦截器：统一处理响应数据和错误
 * 成功响应：
 *   1. 从 pending 中移除
 *   2. 开发环境打印响应日志
 *   3. 提取业务数据
 * 失败响应：
 *   1. 从 pending 中移除
 *   2. 自动重试（GET 请求网络错误，最多 3 次）
 *   3. 根据 HTTP 状态码处理（401/403/500）
 *   4. 开发环境打印错误日志
 */
instance.interceptors.response.use(
  response => {
    const { config } = response
    const requestKey = getRequestKey(config)

    // 1. 从 pending 中移除
    pendingRequests.delete(requestKey)

    // 2. 开发环境响应日志
    if (import.meta.env.DEV) {
      console.log(`[API Response] ${config.url}`, response.data)
    }

    // 3. 提取业务数据
    const res = response.data
    if (res.code === 200 || res.status === 'success') {
      return res.data
    }
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  async error => {
    const { config } = error
    const requestKey = config ? getRequestKey(config) : null

    // 1. 从 pending 中移除
    if (requestKey) {
      pendingRequests.delete(requestKey)
    }

    // 2. 处理请求取消（非重试错误）
    if (error.name === 'CanceledError' || error.name === 'AbortError') {
      if (error.message === 'DUPLICATE') {
        return Promise.reject(new Error('请求已取消（重复请求）'))
      }
      if (error.message === 'PAGE_LEAVE') {
        return Promise.reject(new Error('请求已取消（页面离开）'))
      }
      return Promise.reject(new Error('请求已取消'))
    }

    // 3. 开发环境错误日志
    if (import.meta.env.DEV) {
      console.error(`[API Error] ${config?.url || 'unknown'}`, error.message, error.response?.data)
    }

    // 4. 自动重试：GET 请求遇到网络错误时自动重试（最多 3 次）
    if (config && config.method === 'get' && config._retryCount < 3) {
      const isNetworkError = !error.response
      const isTimeout = ['ECONNABORTED', 'ETIMEDOUT'].some(code => error.code === code)
      const isRetryableMsg = ['Network Error', 'ERR_NETWORK'].some(msg => error.message?.includes(msg))

      if (isNetworkError || isTimeout || isRetryableMsg) {
        config._retryCount++
        const delay = 1000 * config._retryCount
        if (import.meta.env.DEV) {
          console.log(`[API Retry] ${config.url} 第 ${config._retryCount} 次重试，${delay}ms 后`)
        }
        await new Promise(resolve => setTimeout(resolve, delay))
        return instance(config)
      }
    }

    // 5. HTTP 状态码错误处理
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        // 401 未授权：清除 Token 并派发全局事件，由路由守卫处理跳转
        removeToken()
        if (typeof window !== 'undefined') {
          window.dispatchEvent(new CustomEvent('api:unauthorized', {
            detail: { message: '登录已过期，请重新登录' }
          }))
        }
        return Promise.reject(new Error('登录已过期，请重新登录'))
      } else if (status === 403) {
        // 403 禁止访问：通常是权限不足或管理员密码验证失败
        const message = data?.message || '权限不足，无法执行此操作'
        return Promise.reject(new Error(message))
      } else if (status === 404) {
        return Promise.reject(new Error('请求的资源不存在，请刷新页面后重试'))
      } else if (status === 409) {
        return Promise.reject(new Error(data?.message || '资源冲突，请检查数据后重试'))
      } else if (status === 422) {
        return Promise.reject(new Error(data?.message || '请求参数校验失败，请检查输入'))
      } else if (status === 429) {
        return Promise.reject(new Error('请求过于频繁，请稍后再试'))
      } else if (status === 500) {
        return Promise.reject(new Error('服务器内部错误，请稍后重试'))
      } else if (status === 503) {
        return Promise.reject(new Error('服务暂时不可用，请稍后重试'))
      } else if (data && data.message) {
        return Promise.reject(new Error(data.message))
      }
    }

    // 6. 无响应（网络断开等）
    return Promise.reject(new Error('网络请求失败，请检查网络连接'))
  }
)

// ==================== Token 管理 ====================

/**
 * 从本地存储获取 Token
 * 统一从 medicine-box-auth JSON 对象中读取 token 字段
 * @returns {string|null} Token 字符串或 null
 */
export function getToken() {
  try {
    const authData = localStorage.getItem(TOKEN_KEY)
    if (authData) {
      const parsed = JSON.parse(authData)
      return parsed.token || null
    }
  } catch (e) {
    console.error('读取 token 失败:', e)
  }
  return null
}

/**
 * 设置 Token 到本地存储
 * 统一使用 medicine-box-auth JSON 对象存储，包含 token 和时间戳
 * @param {string} token - JWT Token 字符串
 */
export function setToken(token) {
  const authData = JSON.stringify({ token, timestamp: Date.now() })
  localStorage.setItem(TOKEN_KEY, authData)
  // 清理旧版存储（兼容过渡）
  localStorage.removeItem('access_token')
}

/**
 * 从本地存储移除 Token
 * 同时清理新版和旧版存储键
 */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('access_token')
}

// ==================== 缓存管理 ====================

/**
 * 生成请求唯一标识（用于缓存和请求取消）
 * 排除防缓存时间戳 _t，确保同一请求的不同时间调用生成相同 key
 * @param {Object} config - axios 请求配置
 * @returns {string} 请求唯一标识
 */
function getRequestKey(config) {
  const method = (config.method || 'get').toLowerCase()
  const url = config.url || ''
  // 复制 params 并移除防缓存时间戳
  const params = { ...(config.params || {}) }
  delete params._t
  return `${method}:${url}:${JSON.stringify(params)}`
}

/**
 * 清除所有内存缓存
 * 可在页面切换、用户登出等场景调用
 */
export function clearCache() {
  memoryCache.clear()
  if (import.meta.env.DEV) {
    console.log('[API Cache] 缓存已清除')
  }
}

/**
 * 带缓存的 GET 请求
 * 先检查内存缓存，未命中则发送请求并缓存结果
 * @param {string} url - 请求路径
 * @param {Object} config - axios 配置
 * @returns {Promise<any>} 响应数据
 */
export async function cachedGet(url, config = {}) {
  const requestKey = getRequestKey({ url, method: 'get', params: config.params })
  const cached = memoryCache.get(requestKey)
  if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
    if (import.meta.env.DEV) {
      console.log(`[API Cache] ${url} - 缓存命中`)
    }
    return cached.data
  }
  const data = await instance.get(url, config)
  memoryCache.set(requestKey, { data, timestamp: Date.now() })
  return data
}

// ==================== 请求取消 ====================

/**
 * 取消所有正在进行的请求
 * 通常在页面切换、组件卸载时调用，避免旧请求浪费资源
 */
export function cancelAllPendingRequests() {
  pendingRequests.forEach((controller, key) => {
    controller.abort('PAGE_LEAVE')
  })
  pendingRequests.clear()
  if (import.meta.env.DEV) {
    console.log('[API Cancel] 已取消所有待处理请求')
  }
}

// ==================== 认证 API ====================

/**
 * 认证相关 API 接口
 */
export const authAPI = {
  /** 用户登录
   * @param {Object} data - 登录参数 { phone, password }
   * @returns {Promise<Object>} */
  login: (data) => instance.post('/auth/login', data),
  /** 用户注册
   * @param {Object} data - 注册参数
   * @returns {Promise<Object>} */
  register: (data) => instance.post('/auth/register', data),
  /** 获取当前用户信息
   * @returns {Promise<Object>} */
  getProfile: () => instance.get('/auth/me'),
  /** 更新当前用户信息
   * @param {Object} data - 用户信息
   * @returns {Promise<Object>} */
  updateProfile: (data) => instance.put('/auth/me', data),
  /** 修改密码
   * @param {Object} data - 密码参数
   * @returns {Promise<Object>} */
  changePassword: (data) => instance.put('/auth/change-password', data),
  /** 获取所有用户列表
   * @returns {Promise<Object>} */
  getAllUsers: () => instance.get('/auth/users'),
  /** 发送验证码（找回密码）
   * @param {string} phone - 手机号
   * @returns {Promise<Object>} */
  sendCaptcha: (phone) => instance.post('/auth/send-captcha', { phone }),
  /** 重置密码（找回密码）
   * @param {Object} data - 重置参数
   * @returns {Promise<Object>} */
  resetPassword: (data) => instance.put('/auth/reset-password', data),
  /** 获取字母验证码字符串
   * @returns {Promise<string>} 验证码字符串 */
  getCaptchaText: () => instance.get('/captcha/text'),
  /** 验证字母验证码
   * @param {string} captcha - 验证码
   * @returns {Promise<Object>} */
  verifyCaptcha: (captcha) => instance.post('/captcha/verify', { captcha })
}

// ==================== 药品 API ====================

/**
 * 药品相关 API 接口
 */
export const medicineAPI = {
  /**
   * 获取药品列表（支持分页和筛选）
   * @param {Object} params - 查询参数对象
   * @param {number} params.page - 页码，默认 0
   * @param {number} params.size - 每页数量，默认 20
   * @param {string} [params.keyword] - 搜索关键词（可选）
   * @param {string} [params.category] - 药品分类（可选）
   * @param {number} [params.lowStock] - 低库存阈值（可选）
   * @param {boolean} [params.expired] - 是否只显示过期药品（可选）
   * @returns {Promise<Object>}
   */
  getAll: (params = {}) => {
    const { page = 0, size = 20, ...filters } = params
    return instance.get('/medicines', { params: { page, size, ...filters } })
  },

  /**
   * 搜索药品（关键词 + 分页）
   * @param {string} keyword - 搜索关键词
   * @param {number} page - 页码，默认 0
   * @param {number} size - 每页数量，默认 20
   * @returns {Promise<Object>}
   */
  search: (keyword, page = 0, size = 20) => instance.get('/medicines', { params: { keyword, page, size } }),

  /** 根据 ID 获取药品详情
   * @param {number} id - 药品 ID
   * @returns {Promise<Object>} */
  getById: (id) => instance.get(`/medicines/${id}`),
  /** 创建新药品
   * @param {Object} data - 药品数据
   * @returns {Promise<Object>} */
  create: (data) => instance.post('/medicines', data),
  /** 更新药品信息
   * @param {number} id - 药品 ID
   * @param {Object} data - 更新数据
   * @returns {Promise<Object>} */
  update: (id, data) => instance.put(`/medicines/${id}`, data),
  /** 删除药品
   * @param {number} id - 药品 ID
   * @returns {Promise<Object>} */
  delete: (id) => instance.delete(`/medicines/${id}`),
  /** 图片识别药品（上传图片）
   * @param {FormData} formData - 图片表单数据
   * @returns {Promise<Object>} */
  recognize: (formData) => instance.post('/medicines/recognize', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  /** 获取药品分类列表（带缓存）
   * @returns {Promise<Object>} */
  getCategories: () => cachedGet('/medicines/categories'),
  /** 获取库存不足的药品
   * @returns {Promise<Object>} */
  getLowStock: () => instance.get('/medicines/low-stock'),
  /** 获取已过期的药品
   * @returns {Promise<Object>} */
  getExpired: () => instance.get('/medicines/expired')
}

// ==================== 提醒 API ====================

/**
 * 用药提醒相关 API 接口
 */
export const reminderAPI = {
  /**
   * 获取今日用药提醒列表
   * @param {number} page - 页码，默认 0
   * @param {number} size - 每页数量，默认 20
   * @returns {Promise<Object>}
   */
  getToday: (page = 0, size = 20) => instance.get('/medication/today', { params: { page, size } }),

  /**
   * 获取所有用药提醒列表（当前后端仅支持今日提醒，保留兼容）
   * @deprecated 请使用 getToday 获取今日提醒
   * @param {number} page - 页码，默认 0
   * @param {number} size - 每页数量，默认 20
   * @returns {Promise<Object>}
   */
  getAll: (page = 0, size = 20) => instance.get('/medication/today', { params: { page, size } }),

  /** 创建用药提醒
   * @param {Object} data - 提醒数据
   * @returns {Promise<Object>} */
  create: (data) => instance.post('/medication/reminders', data),
  /** 标记提醒为已服用
   * @param {number} id - 提醒 ID
   * @returns {Promise<Object>} */
  markTaken: (id) => instance.post(`/medication/reminders/${id}/mark-taken`),
  /** 更新用药提醒
   * @param {number} id - 提醒 ID
   * @param {Object} data - 更新数据
   * @returns {Promise<Object>} */
  update: (id, data) => instance.put(`/medication/reminders/${id}`, data),
  /** 删除用药提醒
   * @param {number} id - 提醒 ID
   * @returns {Promise<Object>} */
  delete: (id) => instance.delete(`/medication/reminders/${id}`)
}

// ==================== 处方 API ====================

/**
 * 处方相关 API 接口
 */
export const prescriptionAPI = {
  /** 获取处方历史列表
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   * @returns {Promise<Object>} */
  getAll: (page = 0, size = 20) => instance.get('/prescription/history', { params: { page, size } }),
  /** 上传处方
   * @param {FormData} data - 处方表单数据
   * @returns {Promise<Object>} */
  upload: (data) => instance.post('/prescription/upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  /** 更新处方信息
   * @param {number} id - 处方 ID
   * @param {Object} data - 更新数据
   * @returns {Promise<Object>} */
  update: (id, data) => instance.put(`/prescription/${id}`, data),
  /** 删除处方
   * @param {number} id - 处方 ID
   * @returns {Promise<Object>} */
  delete: (id) => instance.delete(`/prescription/${id}`)
}

// ==================== 设备指令 API ====================

/**
 * 设备指令相关 API 接口
 */
export const commandAPI = {
  /** 获取设备指令历史列表
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   * @returns {Promise<Object>} */
  getAll: (page = 0, size = 20) => instance.get('/device/history', { params: { page, size } }),
  /** 创建并下发设备指令
   * @param {Object} data - 指令数据
   * @returns {Promise<Object>} */
  create: (data) => instance.post('/device/forward', data),
  /** 执行指定的设备指令
   * @param {number} id - 指令 ID
   * @returns {Promise<Object>} */
  execute: (id) => instance.post(`/device/execute/${id}`),
  /** 删除设备指令
   * @param {number} id - 指令 ID
   * @returns {Promise<Object>} */
  delete: (id) => instance.delete(`/device/${id}`),
  /** 获取设备列表（带缓存）
   * @returns {Promise<Object>} */
  getDevices: () => cachedGet('/device/list'),
  /** 获取设备详情
   * @param {string} deviceId - 设备 ID
   * @returns {Promise<Object>} */
  getDeviceDetail: (deviceId) => instance.get(`/device/${deviceId}`)
}

// ==================== 日志 API ====================

/**
 * 日志相关 API 接口
 */
export const logAPI = {
  /** 获取最近的日志
   * @returns {Promise<Object>} */
  getRecent: () => instance.get('/logs/recent'),
  /** 获取日志列表（分页）
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   * @returns {Promise<Object>} */
  getAll: (page = 0, size = 20) => instance.get('/logs', { params: { page, size } }),
  /** 按模块获取日志
   * @param {string} module - 模块名称
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   * @returns {Promise<Object>} */
  getByModule: (module, page = 0, size = 20) => instance.get(`/logs/module/${module}`, { params: { page, size } })
}

// ==================== 套餐 API ====================

/**
 * 套餐/订阅计划相关 API 接口
 */
export const planAPI = {
  /** 获取当前用户的套餐
   * @returns {Promise<Object>} */
  getMyPlan: () => instance.get('/plans/my'),
  /** 获取所有套餐列表
   * @returns {Promise<Object>} */
  getAllPlans: () => instance.get('/plans'),
  /**
   * 升级套餐
   * @param {string} plan - 套餐标识
   * @param {number} months - 购买月数，默认 1
   * @returns {Promise<Object>}
   */
  upgrade: (plan, months = 1) => instance.post('/plans/upgrade', { plan, months })
}

// ==================== 管理员 API ====================

/**
 * 管理员相关 API 接口
 * 所有写操作参数统一通过请求体（data）传递
 */
export const adminAPI = {
  /** 管理员登录
   * @param {Object} data - 登录参数
   * @returns {Promise<Object>} */
  login: (data) => instance.post('/admin/login', data),
  /** 获取管理员仪表盘数据
   * @returns {Promise<Object>} */
  getDashboard: () => instance.get('/admin/dashboard'),
  /**
   * 获取用户列表（支持搜索和分页）
   * @param {string} keyword - 搜索关键词
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   * @returns {Promise<Object>}
   */
  getUsers: (keyword, page = 0, size = 20) => instance.get('/admin/users', { params: { keyword, page, size } }),
  /** 获取单个用户详情
   * @param {number} id - 用户 ID
   * @returns {Promise<Object>} */
  getUser: (id) => instance.get(`/admin/users/${id}`),
  /** 更新用户名称
   * @param {number} id - 用户 ID
   * @param {string} name - 新名称
   * @returns {Promise<Object>} */
  updateUserName: (id, name) => instance.put(`/admin/users/${id}/name`, { name }),
  /** 更新用户状态
   * @param {number} id - 用户 ID
   * @param {string} status - 新状态
   * @returns {Promise<Object>} */
  updateUserStatus: (id, status) => instance.put(`/admin/users/${id}/status`, { status }),
  /** 更新用户角色
   * @param {number} id - 用户 ID
   * @param {string} role - 新角色
   * @returns {Promise<Object>} */
  updateUserRole: (id, role) => instance.put(`/admin/users/${id}/role`, { role }),
  /** 更新用户套餐
   * @param {number} id - 用户 ID
   * @param {string} plan - 套餐标识
   * @param {number} months - 购买月数
   * @returns {Promise<Object>} */
  updateUserPlan: (id, plan, months = 1) => instance.put(`/admin/users/${id}/plan`, { plan, months }),
  /** 更新用户 AI 使用次数限制
   * @param {number} id - 用户 ID
   * @param {number} limit - 限制次数
   * @returns {Promise<Object>} */
  updateUserAiLimit: (id, limit) => instance.put(`/admin/users/${id}/ai-limit`, { limit }),
  /** 重置用户密码
   * @param {number} id - 用户 ID
   * @param {string} newPassword - 新密码
   * @returns {Promise<Object>} */
  resetUserPassword: (id, newPassword) => instance.put(`/admin/users/${id}/password`, { newPassword }),
  /** 删除用户（需管理员密码二次验证）
   * @param {number} id - 用户 ID
   * @param {string} adminPassword - 管理员密码
   * @returns {Promise<Object>} */
  deleteUser: (id, adminPassword) => instance.delete(`/admin/users/${id}`, { params: { adminPassword } }),
  /** 账号封禁（需管理员密码二次验证）
   * @param {number} id - 用户 ID
   * @param {string} reason - 封禁原因
   * @param {string} adminPassword - 管理员密码
   * @returns {Promise<Object>} */
  blockUser: (id, reason, adminPassword) => instance.post(`/admin/users/${id}/block`, null, { params: { reason, adminPassword } }),
  /** 账号冻结（需管理员密码二次验证）
   * @param {number} id - 用户 ID
   * @param {string} adminPassword - 管理员密码
   * @returns {Promise<Object>} */
  freezeUser: (id, adminPassword) => instance.post(`/admin/users/${id}/freeze`, null, { params: { adminPassword } }),
  /** 账号解封（需管理员密码二次验证）
   * @param {number} id - 用户 ID
   * @param {string} adminPassword - 管理员密码
   * @returns {Promise<Object>} */
  unblockUser: (id, adminPassword) => instance.post(`/admin/users/${id}/unblock`, null, { params: { adminPassword } }),
  /** 创建用户
   * @param {Object} data - 用户数据
   * @returns {Promise<Object>} */
  createUser: (data) => instance.post('/admin/users', data),
  /** 批量删除用户
   * @param {number[]} ids - 用户 ID 数组
   * @returns {Promise<Object>} */
  batchDeleteUsers: (ids) => instance.delete('/admin/users/batch', { data: ids }),
  /** 批量更新用户状态
   * @param {number[]} ids - 用户 ID 数组
   * @param {string} status - 新状态
   * @returns {Promise<Object>} */
  batchUpdateUserStatus: (ids, status) => instance.put('/admin/users/batch/status', { ids, status }),
  /** 批量更新用户角色
   * @param {number[]} ids - 用户 ID 数组
   * @param {string} role - 新角色
   * @returns {Promise<Object>} */
  batchUpdateUserRole: (ids, role) => instance.put('/admin/users/batch/role', { ids, role }),
  /**
   * 获取告警列表
   * @param {string} status - 告警状态
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   * @returns {Promise<Object>}
   */
  getAlerts: (status, page = 0, size = 20) => instance.get('/admin/alerts', { params: { status, page, size } }),
  /** 获取告警统计数据
   * @returns {Promise<Object>} */
  getAlertStats: () => instance.get('/admin/alerts/stats'),
  /** 处理告警
   * @param {number} id - 告警 ID
   * @param {string} status - 处理状态
   * @param {string} remark - 备注
   * @returns {Promise<Object>} */
  handleAlert: (id, status, remark) => instance.put(`/admin/alerts/${id}/handle`, { status, remark }),
  /** 触发测试告警
   * @param {string} level - 告警级别
   * @param {string} title - 告警标题
   * @returns {Promise<Object>} */
  triggerTestAlert: (level, title) => instance.post('/admin/alerts/test', { level, title }),
  /** 获取指标汇总
   * @returns {Promise<Object>} */
  getMetricsSummary: () => instance.get('/admin/alerts/metrics/summary'),
  /**
   * 获取日志列表（管理员）
   * @param {number} userId - 用户 ID
   * @param {string} module - 模块名称
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   * @returns {Promise<Object>}
   */
  getLogs: (userId, module, page = 0, size = 20) => instance.get('/admin/logs', { params: { userId, module, page, size } }),
  /** 获取日志模块列表
   * @returns {Promise<Object>} */
  getLogModules: () => instance.get('/admin/logs/modules'),
  /** 获取日志统计数据
   * @returns {Promise<Object>} */
  getLogStats: () => instance.get('/admin/logs/stats'),
  /** 导出药品数据
   * @returns {Promise<Object>} */
  exportMedicines: () => instance.get('/medicines/export'),
  /** 获取通知列表
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   * @returns {Promise<Object>} */
  getNotifications: (page = 0, size = 20) => instance.get('/notifications', { params: { page, size } }),
  /** 获取通知统计
   * @returns {Promise<Object>} */
  getNotificationStats: () => instance.get('/notifications/stats'),
  /** 标记单个通知为已读
   * @param {number} id - 通知 ID
   * @returns {Promise<Object>} */
  markNotificationAsRead: (id) => instance.put(`/notifications/${id}/read`),
  /** 标记所有通知为已读
   * @returns {Promise<Object>} */
  markAllNotificationsAsRead: () => instance.put('/notifications/read-all'),
  /** 删除单个通知
   * @param {number} id - 通知 ID
   * @returns {Promise<Object>} */
  deleteNotification: (id) => instance.delete(`/notifications/${id}`),
  /** 删除所有通知
   * @returns {Promise<Object>} */
  deleteAllNotifications: () => instance.delete('/notifications')
}

// ==================== 通知 API ====================

/**
 * 通知相关 API 接口（普通用户）
 */
export const notificationAPI = {
  /** 获取通知列表
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   * @returns {Promise<Object>} */
  getNotifications: (page = 0, size = 20) => instance.get('/notifications', { params: { page, size } }),
  /** 获取通知统计
   * @returns {Promise<Object>} */
  getNotificationStats: () => instance.get('/notifications/stats'),
  /** 标记单个通知为已读
   * @param {number} id - 通知 ID
   * @returns {Promise<Object>} */
  markNotificationAsRead: (id) => instance.put(`/notifications/${id}/read`),
  /** 标记所有通知为已读
   * @returns {Promise<Object>} */
  markAllNotificationsAsRead: () => instance.put('/notifications/read-all'),
  /** 删除单个通知
   * @param {number} id - 通知 ID
   * @returns {Promise<Object>} */
  deleteNotification: (id) => instance.delete(`/notifications/${id}`),
  /** 删除所有通知
   * @returns {Promise<Object>} */
  deleteAllNotifications: () => instance.delete('/notifications')
}

// ==================== 反馈 API ====================

/**
 * 反馈相关 API 接口
 */
export const feedbackAPI = {
  /** 提交反馈
   * @param {Object} data - 反馈数据
   * @returns {Promise<Object>} */
  submit: (data) => instance.post('/feedbacks', data),
  /**
   * 获取我的反馈列表
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   * @param {string} status - 状态筛选
   * @returns {Promise<Object>}
   */
  getMyFeedbacks: (page = 0, size = 10, status = '') => instance.get('/feedbacks/my', { params: { page, size, status } }),
  /** 获取反馈详情
   * @param {number} id - 反馈 ID
   * @returns {Promise<Object>} */
  getDetail: (id) => instance.get(`/feedbacks/${id}`),
  /** 删除反馈
   * @param {number} id - 反馈 ID
   * @returns {Promise<Object>} */
  deleteFeedback: (id) => instance.delete(`/feedbacks/${id}`),
  /**
   * 获取所有反馈列表（管理员）
   * @param {number} page - 页码
   * @param {number} size - 每页数量
   * @param {string} status - 状态筛选
   * @param {string} type - 类型筛选
   * @returns {Promise<Object>}
   */
  getAllFeedbacks: (page = 0, size = 10, status = '', type = '') => instance.get('/feedbacks', { params: { page, size, status, type } }),
  /** 回复反馈
   * @param {number} id - 反馈 ID
   * @param {string} reply - 回复内容
   * @returns {Promise<Object>} */
  replyFeedback: (id, reply) => instance.put(`/feedbacks/${id}/reply`, { reply }),
  /** 更新反馈状态
   * @param {number} id - 反馈 ID
   * @param {string} status - 新状态
   * @returns {Promise<Object>} */
  updateStatus: (id, status) => instance.put(`/feedbacks/${id}/status`, { status }),
  /** 获取待处理反馈数量
   * @returns {Promise<Object>} */
  getPendingCount: () => instance.get('/feedbacks/stats/pending-count')
}

// ==================== 默认导出 ====================

export default instance
