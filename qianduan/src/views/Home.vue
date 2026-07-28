<template>
  <!-- 首页主容器 -->
  <div class="home-page">
    <!-- 英雄区域：页面顶部宣传区 -->
    <section class="hero-section">
      <div class="hero-content">
        <div class="hero-text">
          <h1 class="hero-title">智能药箱<br>守护家人健康</h1>
          <p class="hero-desc">智能识别药方，自动管理药品，贴心用药提醒。让健康管理变得简单、智能、贴心。</p>
          <div class="hero-buttons">
            <button class="btn-primary btn-lg" @click="showLoginDialog = true">立即体验</button>
            <router-link to="/about" class="btn-secondary btn-lg">了解更多</router-link>
          </div>
        </div>
        <!-- 英雄区统计卡片 -->
        <div class="hero-visual">
          <div class="hero-card" v-for="(stat, index) in heroStats" :key="index" :style="{ animationDelay: `${index * 0.1}s` }">
            <el-icon class="card-icon"><component :is="stat.icon" /></el-icon>
            <div class="card-info">
              <div class="card-value">{{ stat.value }}</div>
              <div class="card-label">{{ stat.label }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="hero-decoration"></div>
    </section>

    <!-- 核心功能展示区 -->
    <section class="features-section">
      <div class="page-container">
        <div class="section-header">
          <h2 class="section-title">核心功能</h2>
          <p class="section-subtitle">全方位的健康管理解决方案</p>
        </div>
        <!-- 功能卡片网格 -->
        <div class="features-grid">
          <div 
            class="feature-card" 
            v-for="(feature, index) in features" 
            :key="index"
            :style="{ animationDelay: `${index * 0.1}s` }"
          >
            <div class="feature-icon" :style="{ background: feature.gradient }">
              <el-icon><component :is="feature.icon" /></el-icon>
            </div>
            <h3 class="feature-title">{{ feature.title }}</h3>
            <p class="feature-desc">{{ feature.description }}</p>
            <router-link :to="feature.link" class="feature-link">了解详情 →</router-link>
          </div>
        </div>
      </div>
    </section>

    <!-- 数据统计区 -->
    <section class="stats-section">
      <div class="page-container">
        <div class="stats-grid">
          <div class="stat-card" v-for="(stat, index) in statsCards" :key="index">
            <el-icon class="stat-icon" :class="stat.color"><component :is="getStatIcon(index)" /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 今日提醒区 -->
    <section class="reminders-section">
      <div class="page-container">
        <div class="section-header">
          <h2 class="section-title">今日提醒</h2>
          <router-link to="/reminders" class="view-all">查看全部 →</router-link>
        </div>
        
        <!-- 加载中状态 -->
        <div v-if="loading" class="loading-spinner">
          <el-spinner type="circle" size="40" />
        </div>
        
        <!-- 空状态：暂无今日提醒 -->
        <div v-else-if="todayReminders.length === 0" class="empty-state">
          <el-icon class="empty-icon"><Bell /></el-icon>
          <div class="empty-text">暂无今日用药提醒</div>
        </div>
        
        <!-- 今日提醒列表 -->
        <div v-else class="reminders-list">
          <div 
            class="reminder-card" 
            v-for="(reminder, index) in todayReminders" 
            :key="reminder.id"
            :class="{ 'reminder-done': reminder.taken }"
            :style="{ animationDelay: `${index * 0.05}s` }"
          >
            <div class="reminder-time">
              <div class="time-value">{{ formatTime(reminder.time) }}</div>
              <div class="time-label">今日</div>
            </div>
            <div class="reminder-info">
              <div class="reminder-medicine">{{ reminder.medicineName }}</div>
              <div class="reminder-details">
                <span class="detail-item">剂量: {{ reminder.dosage }}</span>
                <span class="detail-item">频率: {{ reminder.frequency }}</span>
              </div>
            </div>
            <div class="reminder-status">
              <span v-if="reminder.taken" class="status-tag done">已服药</span>
              <span v-else class="status-tag pending">待服药</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 最近操作日志区 -->
    <section class="logs-section">
      <div class="page-container">
        <div class="section-header">
          <h2 class="section-title">最近操作日志</h2>
        </div>
        
        <!-- 日志加载中状态 -->
        <div v-if="logsLoading" class="loading-spinner">
          <el-spinner type="circle" size="40" />
        </div>
        
        <!-- 空状态：暂无操作日志 -->
        <div v-else-if="recentLogs.length === 0" class="empty-state">
          <el-icon class="empty-icon"><Clock /></el-icon>
          <div class="empty-text">暂无操作日志</div>
        </div>
        
        <!-- 操作日志列表 -->
        <div v-else class="logs-list">
          <div 
            class="log-card" 
            v-for="(log, index) in recentLogs" 
            :key="log.id"
            :style="{ animationDelay: `${index * 0.05}s` }"
          >
            <div class="log-icon" :class="getLogIconClass(log.operation)">
              <el-icon><component :is="getLogIcon(log.operation)" /></el-icon>
            </div>
            <div class="log-content">
              <div class="log-title">{{ log.operation }}</div>
              <div class="log-meta">
                <span class="log-module">{{ log.module }}</span>
                <span class="log-user">{{ log.userName || log.userPhone }}</span>
                <span class="log-time">{{ formatDateTime(log.createdAt) }}</span>
              </div>
              <div class="log-desc">{{ log.description }}</div>
            </div>
            <div class="log-status">
              <span v-if="log.success === false" class="status-tag error">失败</span>
              <span v-else class="status-tag success">成功</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 快捷入口区 -->
    <section class="quick-actions">
      <div class="page-container">
        <div class="section-header">
          <h2 class="section-title">快捷入口</h2>
        </div>
        <!-- 快捷入口卡片网格 -->
        <div class="actions-grid">
          <router-link to="/medicines" class="action-card" v-for="(action, index) in quickActions" :key="index">
            <el-icon class="action-icon"><component :is="action.icon" /></el-icon>
            <div class="action-text">{{ action.label }}</div>
          </router-link>
        </div>
      </div>
    </section>

    <!-- 登录弹窗 -->
    <el-dialog
      v-model="showLoginDialog"
      width="440px"
      :show-close="true"
      center
      class="login-dialog"
      :close-on-click-modal="false"
    >
      <!-- 弹窗头部 -->
      <template #header>
        <div class="dialog-header">
          <el-icon class="dialog-brand-icon"><TakeawayBox /></el-icon>
          <h2 class="dialog-title">欢迎回来</h2>
          <p class="dialog-subtitle">登录以体验智能药箱全部功能</p>
        </div>
      </template>

      <!-- 弹窗主体：登录表单 -->
      <div class="dialog-body">
        <form @submit.prevent="handleDialogLogin" class="dialog-login-form">
          <!-- 手机号输入 -->
          <div class="dialog-form-group">
            <label class="dialog-form-label">手机号</label>
            <div class="dialog-input-wrapper" :class="{ 'input-error': loginErrors.phone }">
              <el-icon class="dialog-input-icon"><Phone /></el-icon>
              <input
                type="tel"
                class="dialog-form-input"
                v-model="loginForm.phone"
                placeholder="请输入手机号"
                maxlength="11"
                @focus="loginErrors.phone = ''"
              />
            </div>
            <span v-if="loginErrors.phone" class="dialog-error-text">{{ loginErrors.phone }}</span>
          </div>

          <!-- 密码输入 -->
          <div class="dialog-form-group">
            <label class="dialog-form-label">密码</label>
            <div class="dialog-input-wrapper" :class="{ 'input-error': loginErrors.password }">
              <el-icon class="dialog-input-icon"><Lock /></el-icon>
              <input
                :type="showPassword ? 'text' : 'password'"
                class="dialog-form-input"
                v-model="loginForm.password"
                placeholder="请输入密码"
                @focus="loginErrors.password = ''"
              />
              <!-- 密码显示/隐藏切换按钮 -->
              <el-icon class="dialog-toggle-password" @click="showPassword = !showPassword">
                <component :is="showPassword ? Hide : View" />
              </el-icon>
            </div>
            <span v-if="loginErrors.password" class="dialog-error-text">{{ loginErrors.password }}</span>
          </div>

          <!-- 记住我和忘记密码 -->
          <div class="dialog-form-options">
            <label class="dialog-checkbox-label">
              <input type="checkbox" v-model="rememberMe" />
              <span>记住我</span>
            </label>
            <a href="#" class="dialog-forgot-link" @click.prevent>忘记密码？</a>
          </div>

          <!-- 登录按钮 -->
          <button type="submit" class="btn-primary btn-lg dialog-login-btn" :disabled="loginLoading">
            <span v-if="loginLoading">登录中...</span>
            <span v-else>登 录</span>
          </button>
        </form>

        <!-- 注册链接 -->
        <div class="dialog-register-link">
          还没有账号？<router-link to="/register" @click="showLoginDialog = false">立即注册</router-link>
        </div>

        <!-- 管理员登录入口 -->
        <div class="dialog-switch">
          <span class="dialog-switch-divider"></span>
          <router-link to="/admin/login" class="dialog-switch-link" @click="showLoginDialog = false">
            <el-icon><Setting /></el-icon>
            <span>管理员登录</span>
          </router-link>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
// 引入Vue组合式API
import { ref, reactive, onMounted, onActivated } from 'vue'
// 引入Vue路由
import { useRouter } from 'vue-router'
// 引入Element Plus消息提示组件
import { ElMessage } from 'element-plus'
// 引入API接口
import { medicineAPI, reminderAPI, prescriptionAPI, commandAPI, logAPI, authAPI, removeToken } from '../utils/api'
// 引入认证状态管理
import { useAuthStore } from '../stores/auth'
// 引入图标组件
import { Camera, TakeawayBox, Clock, Cellphone, Box, Bell, Document, ArrowRight, House, User, Upload, Edit, Delete, CircleCheck, CircleClose, Phone, Lock, View, Hide, Setting } from '@element-plus/icons-vue'
// 引入日期处理库
import dayjs from 'dayjs'

// 路由实例
const router = useRouter()
// 数据加载状态
const loading = ref(true)
// 日志加载状态
const logsLoading = ref(true)
// 认证状态存储
const authStore = useAuthStore()

// 登录弹窗显示状态
const showLoginDialog = ref(false)
// 登录按钮加载状态
const loginLoading = ref(false)
// 是否显示密码
const showPassword = ref(false)
// 是否记住我
const rememberMe = ref(false)
// 登录表单数据
const loginForm = reactive({ phone: '', password: '' })
// 登录表单错误信息
const loginErrors = reactive({ phone: '', password: '' })
// 统计数据
const stats = ref({
  medicineCount: 0,      // 药品数量
  todayReminders: 0,     // 今日提醒数
  prescriptionCount: 0,  // 药方记录数
  commandCount: 0        // 指令记录数
})
// 今日提醒列表
const todayReminders = ref([])
// 最近操作日志列表
const recentLogs = ref([])

// 英雄区统计数据配置
const heroStats = [
  { icon: TakeawayBox, value: '500+', label: '药品识别' },
  { icon: Bell, value: '10万+', label: '用药提醒' },
  { icon: Box, value: '10万+', label: '家庭用户' }
]

// 快捷入口配置
const quickActions = [
  { icon: TakeawayBox, label: '药品管理' },
  { icon: Document, label: '上传药方' },
  { icon: Clock, label: '用药提醒' },
  { icon: Cellphone, label: '设备指令' }
]

// 核心功能配置
const features = [
  {
    icon: Camera,
    title: '智能药方识别',
    description: '拍照上传药方，AI自动识别药品信息，快速录入系统。',
    link: '/prescriptions',
    gradient: 'linear-gradient(135deg, rgba(37, 99, 235, 0.15), rgba(37, 99, 235, 0.25))'
  },
  {
    icon: TakeawayBox,
    title: '药品库存管理',
    description: '实时追踪药品库存，低库存预警提醒，避免断药风险。',
    link: '/medicines',
    gradient: 'linear-gradient(135deg, rgba(16, 185, 129, 0.15), rgba(16, 185, 129, 0.25))'
  },
  {
    icon: Clock,
    title: '智能用药提醒',
    description: '根据药方自动生成用药提醒，支持多种提醒方式。',
    link: '/reminders',
    gradient: 'linear-gradient(135deg, rgba(245, 158, 11, 0.15), rgba(245, 158, 11, 0.25))'
  },
  {
    icon: ArrowRight,
    title: '设备指令转发',
    description: '远程控制智能药箱，一键发送取药指令，便捷又安全。',
    link: '/commands',
    gradient: 'linear-gradient(135deg, rgba(6, 182, 212, 0.15), rgba(6, 182, 212, 0.25))'
  }
]

// 统计卡片数据
const statsCards = ref([])

// 获取首页统计数据
async function fetchStats() {
  // 未登录时显示占位数据
  if (!authStore.isLoggedIn) {
    statsCards.value = [
      { value: '--', label: '药品数量', color: 'primary' },
      { value: '--', label: '今日提醒', color: 'success' },
      { value: '--', label: '药方记录', color: 'warning' },
      { value: '--', label: '指令记录', color: 'info' }
    ]
    loading.value = false
    return
  }
  
  try {
    // 并行请求所有数据
    const [medicines, reminders, prescriptions, commands] = await Promise.all([
      medicineAPI.getAll(),
      reminderAPI.getAll(),
      prescriptionAPI.getAll(),
      commandAPI.getAll()
    ])
    
    // 提取列表数据
    const medList = medicines && medicines.list ? medicines.list : []
    const remList = reminders && reminders.list ? reminders.list : []
    const prescList = prescriptions && prescriptions.list ? prescriptions.list : []
    const cmdList = commands && commands.list ? commands.list : []
    
    // 更新统计数据
    stats.value = {
      medicineCount: medList.length,
      todayReminders: remList.filter(r => !r.taken).length,
      prescriptionCount: prescList.length,
      commandCount: cmdList.length
    }
    
    // 更新统计卡片数据
    statsCards.value = [
      { value: stats.value.medicineCount, label: '药品数量', color: 'primary' },
      { value: stats.value.todayReminders, label: '今日提醒', color: 'success' },
      { value: stats.value.prescriptionCount, label: '药方记录', color: 'warning' },
      { value: stats.value.commandCount, label: '指令记录', color: 'info' }
    ]
    
    // 今日提醒取前5条
    todayReminders.value = remList.slice(0, 5)
  } catch (error) {
    console.error('获取数据失败:', error)
    stats.value = { medicineCount: 0, todayReminders: 0, prescriptionCount: 0, commandCount: 0 }
    statsCards.value = [
      { value: 0, label: '药品数量', color: 'primary' },
      { value: 0, label: '今日提醒', color: 'success' },
      { value: 0, label: '药方记录', color: 'warning' },
      { value: 0, label: '指令记录', color: 'info' }
    ]
  } finally {
    loading.value = false
  }
}

// 获取最近操作日志
async function fetchLogs() {
  // 未登录时不加载日志
  if (!authStore.isLoggedIn) {
    logsLoading.value = false
    return
  }
  
  try {
    const logs = await logAPI.getRecent()
    recentLogs.value = logs && logs.list ? logs.list : []
  } catch (error) {
    console.error('获取日志失败:', error)
    recentLogs.value = []
  } finally {
    logsLoading.value = false
  }
}

// 格式化时间字符串，只保留时:分
function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.substring(0, 5)
}

// 格式化日期时间，显示相对时间
function formatDateTime(dateTimeStr) {
  if (!dateTimeStr) return ''
  const date = dayjs(dateTimeStr)
  if (!date.isValid()) return ''
  const now = dayjs()
  const diff = now.diff(date, 'minute')
  const hours = now.diff(date, 'hour')
  const days = now.diff(date, 'day')
  
  // 1分钟内显示"刚刚"
  if (diff < 1) return '刚刚'
  // 1小时内显示"X分钟前"
  if (diff < 60) return `${diff}分钟前`
  // 24小时内显示"X小时前"
  if (hours < 24) return `${hours}小时前`
  // 7天内显示"X天前"
  if (days < 7) return `${days}天前`
  
  // 超过7天显示具体日期
  return date.format('MM-DD HH:mm')
}

// 根据索引获取统计图标
function getStatIcon(index) {
  const icons = [TakeawayBox, Bell, Document, ArrowRight]
  return icons[index] || TakeawayBox
}

// 根据操作类型获取日志图标
function getLogIcon(operation) {
  if (!operation) return Document
  if (operation.includes('登录')) return User
  if (operation.includes('注册')) return User
  if (operation.includes('上传')) return Upload
  if (operation.includes('创建')) return Document
  if (operation.includes('更新')) return Edit
  if (operation.includes('删除')) return Delete
  if (operation.includes('成功')) return CircleCheck
  if (operation.includes('失败')) return CircleClose
  return Document
}

// 根据操作类型获取日志图标样式类
function getLogIconClass(operation) {
  if (!operation) return 'log-icon-default'
  if (operation.includes('登录')) return 'log-icon-primary'
  if (operation.includes('注册')) return 'log-icon-success'
  if (operation.includes('上传')) return 'log-icon-warning'
  if (operation.includes('创建')) return 'log-icon-info'
  if (operation.includes('更新')) return 'log-icon-primary'
  if (operation.includes('删除')) return 'log-icon-error'
  if (operation.includes('失败')) return 'log-icon-error'
  return 'log-icon-default'
}

// 校验登录表单
function validateLoginForm() {
  let isValid = true
  loginErrors.phone = ''
  loginErrors.password = ''
  
  // 手机号校验
  if (!loginForm.phone) {
    loginErrors.phone = '请输入手机号'
    isValid = false
  } else if (!/^1[3-9]\d{9}$/.test(loginForm.phone)) {
    loginErrors.phone = '请输入有效的手机号'
    isValid = false
  }
  
  // 密码校验
  if (!loginForm.password) {
    loginErrors.password = '请输入密码'
    isValid = false
  } else if (loginForm.password.length < 6) {
    loginErrors.password = '密码长度不能少于6位'
    isValid = false
  }
  
  return isValid
}

// 处理登录提交
async function handleDialogLogin() {
  // 表单校验
  if (!validateLoginForm()) return
  
  // 清除旧的token和用户信息
  removeToken()
  authStore.logout()
  loginLoading.value = true
  
  try {
    const response = await authAPI.login(loginForm)
    
    if (response && response.accessToken) {
      // 保存token并获取用户信息
      authStore.login(response.accessToken)
      await authStore.fetchUserInfo()
      
      // 管理员账号跳转到管理员登录页
      if (authStore.isAdmin) {
        ElMessage.warning('检测到管理员账号，正在跳转管理员登录入口')
        authStore.logout()
        showLoginDialog.value = false
        await new Promise(resolve => setTimeout(resolve, 800))
        router.push('/admin/login')
      } else {
        // 普通用户登录成功
        ElMessage.success('登录成功')
        showLoginDialog.value = false
        router.push('/')
        // 刷新页面数据
        fetchStats()
        fetchLogs()
      }
    } else {
      loginErrors.password = '登录失败，请检查账号密码'
    }
  } catch (error) {
    const msg = error.message || '登录失败，请检查网络连接'
    // 权限不足时提示管理员账号
    if (msg.includes('权限不足')) {
      loginErrors.password = '此账号为管理员账号，请使用管理员登录入口'
    } else {
      loginErrors.password = msg
    }
  } finally {
    loginLoading.value = false
  }
}

// 组件挂载时加载数据
onMounted(() => {
  fetchStats()
  fetchLogs()
})

// 页面从缓存恢复时重新加载数据（keep-alive）
onActivated(() => {
  fetchStats()
  fetchLogs()
})
</script>

<style scoped>
/* 首页整体样式 */
.home-page {
  padding-top: 64px;
}

/* 英雄区域样式 */
.hero-section {
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 50%, #3b82f6 100%);
  padding: 80px 20px 100px;
  position: relative;
  overflow: hidden;
}

/* 英雄区装饰圆形（右上） */
.hero-section::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 600px;
  height: 600px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 50%;
}

/* 英雄区装饰圆形（左下） */
.hero-section::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -10%;
  width: 400px;
  height: 400px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 50%;
}

/* 英雄区装饰元素 */
.hero-decoration {
  position: absolute;
  top: 20%;
  left: 10%;
  width: 100px;
  height: 100px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 50%;
  filter: blur(40px);
}

/* 英雄区内容布局 */
.hero-content {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 60px;
  align-items: center;
  position: relative;
  z-index: 1;
}

/* 英雄区文字样式 */
.hero-text {
  color: white;
}

/* 英雄区标题样式 */
.hero-title {
  font-size: 48px;
  font-weight: 700;
  line-height: 1.15;
  margin-bottom: 20px;
  letter-spacing: -0.03em;
}

/* 英雄区描述样式 */
.hero-desc {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.8;
  margin-bottom: 32px;
}

/* 英雄区按钮组样式 */
.hero-buttons {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

/* 英雄区视觉卡片容器 */
.hero-visual {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 英雄区统计卡片样式 */
.hero-card {
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  animation: slideUp 0.5s ease forwards;
  opacity: 0;
}

/* 英雄卡片图标样式 */
.hero-card .card-icon {
  font-size: 36px;
}

/* 英雄卡片信息区域 */
.hero-card .card-info {
  flex: 1;
}

/* 英雄卡片数值样式 */
.hero-card .card-value {
  font-size: 28px;
  font-weight: 700;
  color: white;
  letter-spacing: -0.02em;
}

/* 英雄卡片标签样式 */
.hero-card .card-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

/* 区块头部样式 */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
}

/* 区块副标题样式 */
.section-subtitle {
  font-size: 16px;
  color: var(--text-secondary);
  margin-top: 8px;
}

/* 查看全部链接样式 */
.view-all {
  font-size: 14px;
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
  transition: color var(--transition-fast);
}

/* 查看全部链接悬停效果 */
.view-all:hover {
  color: var(--primary-dark);
}

/* 核心功能区块样式 */
.features-section {
  padding: 60px 0;
}

/* 功能卡片网格布局 */
.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

/* 功能卡片样式 */
.feature-card {
  background: white;
  border-radius: var(--border-radius-lg);
  padding: 32px 24px;
  text-align: center;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color);
  transition: all var(--transition-normal);
  animation: slideUp 0.5s ease forwards;
  opacity: 0;
}

/* 功能卡片悬停效果 */
.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--card-shadow-hover);
  border-color: var(--border-color-hover);
}

/* 功能图标样式 */
.feature-icon {
  width: 64px;
  height: 64px;
  border-radius: var(--border-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  margin: 0 auto 20px;
}

/* 功能标题样式 */
.feature-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  letter-spacing: -0.01em;
}

/* 功能描述样式 */
.feature-desc {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 20px;
}

/* 功能链接样式 */
.feature-link {
  font-size: 14px;
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
  transition: opacity var(--transition-fast);
}

/* 功能链接悬停效果 */
.feature-link:hover {
  opacity: 0.8;
}

/* 数据统计区块样式 */
.stats-section {
  padding: 40px 0;
}

/* 统计卡片网格布局 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

/* 今日提醒区块样式 */
.reminders-section {
  padding: 60px 0;
}

/* 提醒列表容器样式 */
.reminders-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 提醒卡片样式 */
.reminder-card {
  background: white;
  border-radius: var(--border-radius-md);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color);
  border-left: 4px solid var(--warning-color);
  transition: all var(--transition-normal);
  animation: slideUp 0.4s ease forwards;
  opacity: 0;
}

/* 提醒卡片悬停效果 */
.reminder-card:hover {
  box-shadow: var(--card-shadow-hover);
}

/* 已完成提醒卡片样式 */
.reminder-card.reminder-done {
  opacity: 0.6;
  border-left-color: var(--success-color);
}

/* 提醒时间区域样式 */
.reminder-time {
  text-align: center;
  min-width: 80px;
}

/* 时间数值样式 */
.time-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

/* 时间标签样式 */
.time-label {
  font-size: 12px;
  color: var(--text-muted);
}

/* 提醒信息区域样式 */
.reminder-info {
  flex: 1;
}

/* 药品名称样式 */
.reminder-medicine {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

/* 提醒详情区域 */
.reminder-details {
  display: flex;
  gap: 20px;
}

/* 详情项样式 */
.detail-item {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 状态区域样式 */
.reminder-status {
  flex-shrink: 0;
}

/* 状态标签通用样式 */
.status-tag {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

/* 待服药状态样式 */
.status-tag.pending {
  background: var(--warning-50);
  color: var(--warning-color);
}

/* 已服药状态样式 */
.status-tag.done {
  background: var(--success-50);
  color: var(--success-color);
}

/* 操作日志区块样式 */
.logs-section {
  padding: 60px 0;
  background: var(--bg-secondary);
}

/* 日志列表容器样式 */
.logs-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 日志卡片样式 */
.log-card {
  background: white;
  border-radius: var(--border-radius-md);
  padding: 16px 20px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color);
  transition: all var(--transition-normal);
  animation: slideUp 0.4s ease forwards;
  opacity: 0;
}

/* 日志卡片悬停效果 */
.log-card:hover {
  box-shadow: var(--card-shadow-hover);
}

/* 日志图标样式 */
.log-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--border-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

/* 日志图标-主色 */
.log-icon-primary {
  background: var(--primary-50);
  color: var(--primary-color);
}

/* 日志图标-成功色 */
.log-icon-success {
  background: var(--success-50);
  color: var(--success-color);
}

/* 日志图标-警告色 */
.log-icon-warning {
  background: var(--warning-50);
  color: var(--warning-color);
}

/* 日志图标-信息色 */
.log-icon-info {
  background: var(--info-50);
  color: var(--info-color);
}

/* 日志图标-错误色 */
.log-icon-error {
  background: var(--error-50);
  color: var(--error-color);
}

/* 日志图标-默认色 */
.log-icon-default {
  background: var(--bg-secondary);
  color: var(--text-secondary);
}

/* 日志内容区域样式 */
.log-content {
  flex: 1;
  min-width: 0;
}

/* 日志标题样式 */
.log-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}

/* 日志元信息区域 */
.log-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 4px;
}

/* 日志模块标签样式 */
.log-module {
  font-size: 12px;
  color: var(--primary-color);
  background: var(--primary-50);
  padding: 2px 8px;
  border-radius: 4px;
}

/* 日志用户样式 */
.log-user {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 日志时间样式 */
.log-time {
  font-size: 12px;
  color: var(--text-muted);
}

/* 日志描述样式 */
.log-desc {
  font-size: 13px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 日志状态区域样式 */
.log-status {
  flex-shrink: 0;
}

/* 成功状态标签样式 */
.status-tag.success {
  background: var(--success-50);
  color: var(--success-color);
}

/* 错误状态标签样式 */
.status-tag.error {
  background: var(--error-50);
  color: var(--error-color);
}

/* 快捷入口区块样式 */
.quick-actions {
  padding: 60px 0;
}

/* 快捷入口卡片网格布局 */
.actions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

/* 快捷入口卡片样式 */
.action-card {
  background: white;
  border-radius: var(--border-radius-md);
  padding: 32px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-decoration: none;
  color: var(--text-primary);
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color);
  transition: all var(--transition-normal);
}

/* 快捷入口卡片悬停效果 */
.action-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--card-shadow-hover);
  border-color: var(--primary-color);
}

/* 快捷入口图标样式 */
.action-icon {
  font-size: 36px;
}

/* 快捷入口文字样式 */
.action-text {
  font-size: 16px;
  font-weight: 500;
}

/* 平板响应式样式 */
@media (max-width: 900px) {
  .hero-content {
    grid-template-columns: 1fr;
    gap: 40px;
  }
  
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .actions-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .hero-title {
    font-size: 36px;
  }
}

/* 手机响应式样式 */
@media (max-width: 600px) {
  .features-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .actions-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .hero-title {
    font-size: 28px;
  }
  
  .hero-desc {
    font-size: 16px;
  }
  
  .reminder-card {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .reminder-details {
    flex-wrap: wrap;
  }
}

/* 登录弹窗样式 */
.login-dialog :deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

/* 登录弹窗头部 */
.login-dialog :deep(.el-dialog__header) {
  padding: 0;
  margin: 0;
}

/* 登录弹窗主体 */
.login-dialog :deep(.el-dialog__body) {
  padding: 0;
}

/* 登录弹窗关闭按钮 */
.login-dialog :deep(.el-dialog__headerbtn) {
  top: 16px;
  right: 16px;
  z-index: 10;
}

/* 弹窗头部样式 */
.dialog-header {
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%);
  padding: 36px 32px 28px;
  text-align: center;
  color: white;
  position: relative;
  overflow: hidden;
}

/* 弹窗头部装饰圆形 */
.dialog-header::before {
  content: '';
  position: absolute;
  top: -30%;
  right: -20%;
  width: 180px;
  height: 180px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

/* 弹窗品牌图标样式 */
.dialog-brand-icon {
  font-size: 40px;
  margin-bottom: 12px;
  position: relative;
  z-index: 1;
}

/* 弹窗标题样式 */
.dialog-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 6px;
  position: relative;
  z-index: 1;
}

/* 弹窗副标题样式 */
.dialog-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  position: relative;
  z-index: 1;
}

/* 弹窗主体内容样式 */
.dialog-body {
  padding: 32px;
}

/* 登录表单样式 */
.dialog-login-form {
  display: flex;
  flex-direction: column;
}

/* 表单组样式 */
.dialog-form-group {
  margin-bottom: 20px;
}

/* 表单标签样式 */
.dialog-form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 8px;
}

/* 输入框包装器样式 */
.dialog-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  transition: all 0.2s ease;
  background: #fafafa;
}

/* 输入框聚焦时样式 */
.dialog-input-wrapper:focus-within {
  border-color: #2563eb;
  background: white;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* 输入框错误状态样式 */
.dialog-input-wrapper.input-error {
  border-color: #ef4444;
}

/* 输入框图标样式 */
.dialog-input-icon {
  position: absolute;
  left: 14px;
  font-size: 16px;
  color: #9ca3af;
  z-index: 1;
}

/* 表单输入框样式 */
.dialog-form-input {
  width: 100%;
  padding: 13px 16px 13px 44px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  background: transparent;
  outline: none;
}

/* 输入框占位符样式 */
.dialog-form-input::placeholder {
  color: #9ca3af;
}

/* 密码显示切换按钮 */
.dialog-toggle-password {
  position: absolute;
  right: 14px;
  cursor: pointer;
  font-size: 16px;
  color: #9ca3af;
  transition: opacity 0.2s ease;
}

/* 密码切换按钮悬停效果 */
.dialog-toggle-password:hover {
  opacity: 0.7;
}

/* 错误提示文字样式 */
.dialog-error-text {
  display: block;
  font-size: 12px;
  color: #ef4444;
  margin-top: 6px;
}

/* 表单选项区域样式 */
.dialog-form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

/* 复选框标签样式 */
.dialog-checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #6b7280;
}

/* 复选框样式 */
.dialog-checkbox-label input {
  width: 16px;
  height: 16px;
  accent-color: #2563eb;
}

/* 忘记密码链接样式 */
.dialog-forgot-link {
  font-size: 14px;
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
}

/* 忘记密码链接悬停效果 */
.dialog-forgot-link:hover {
  text-decoration: underline;
}

/* 登录按钮样式 */
.dialog-login-btn {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  border: none;
  cursor: pointer;
}

/* 登录按钮禁用状态 */
.dialog-login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 注册链接区域样式 */
.dialog-register-link {
  text-align: center;
  font-size: 14px;
  color: #6b7280;
  margin-top: 20px;
}

/* 注册链接样式 */
.dialog-register-link a {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
}

/* 注册链接悬停效果 */
.dialog-register-link a:hover {
  text-decoration: underline;
}

/* 切换登录方式区域样式 */
.dialog-switch {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 20px;
  gap: 12px;
}

/* 分割线样式 */
.dialog-switch-divider {
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, #e5e7eb 50%, transparent 100%);
}

/* 切换链接样式 */
.dialog-switch-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.08) 0%, rgba(99, 102, 241, 0.08) 100%);
  color: #2563eb;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
  border: 1px solid rgba(59, 130, 246, 0.2);
}

/* 切换链接悬停效果 */
.dialog-switch-link:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(99, 102, 241, 0.15) 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
  text-decoration: none;
}

/* 切换链接图标样式 */
.dialog-switch-link .el-icon {
  font-size: 14px;
}

/* 英雄区按钮样式修正 */
.hero-buttons .btn-primary {
  cursor: pointer;
  border: none;
  font-family: inherit;
}
</style>
