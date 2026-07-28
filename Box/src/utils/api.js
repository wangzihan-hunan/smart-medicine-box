import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

api.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data.data
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        localStorage.removeItem('access_token')
        localStorage.removeItem('isLoggedIn')
        window.location.href = '/login'
      } else if (status === 403) {
        ElMessage.error('权限不足')
      } else if (status === 429) {
        ElMessage.error('请求过于频繁，请稍后再试')
      } else {
        ElMessage.error(error.response.data?.message || '服务器错误')
      }
    } else {
      ElMessage.error('网络错误')
    }
    return Promise.reject(error)
  }
)

export const authApi = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  getUserInfo: () => api.get('/auth/me'),
  updateUserInfo: (name) => api.put('/auth/me', null, { params: { name } }),
  changePassword: (data) => api.put('/auth/change-password', data)
}

export const medicineApi = {
  getAll: () => api.get('/medicines'),
  add: (data) => api.post('/medicines', data),
  update: (id, data) => api.put(`/medicines/${id}`, data),
  delete: (id) => api.delete(`/medicines/${id}`),
  recognize: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/medicines/recognize', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export const prescriptionApi = {
  upload: (data) => api.post('/prescription/upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  getHistory: () => api.get('/prescription/history'),
  delete: (id) => api.delete(`/prescription/${id}`)
}

export const reminderApi = {
  getAll: () => api.get('/medication'),
  add: (data) => api.post('/medication', data),
  update: (id, data) => api.put(`/medication/${id}`, data),
  delete: (id) => api.delete(`/medication/${id}`),
  getToday: () => api.get('/medication/today')
}

export const commandApi = {
  forward: (data) => api.post('/device/forward', data),
  getHistory: () => api.get('/device/history'),
  execute: (id) => api.post(`/device/execute/${id}`),
  delete: (id) => api.delete(`/device/${id}`)
}

export default api