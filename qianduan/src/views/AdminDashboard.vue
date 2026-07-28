<template>
  <!-- 管理后台仪表盘主容器 -->
  <div class="admin-dashboard">
    <!-- 统计卡片网格区域 -->
    <div class="admin-stats-grid">
      <!-- 单个统计卡片 - 循环渲染 -->
      <div 
        v-for="(stat, index) in statCards" 
        :key="stat.key"
        class="admin-stat-card"
        :style="{ animationDelay: `${index * 0.08}s` }"
        @click="navigateTo(stat.path)"
      >
        <!-- 统计卡片图标 -->
        <div class="admin-stat-icon" :class="stat.iconClass">
          <el-icon><component :is="stat.icon" /></el-icon>
        </div>
        <!-- 统计卡片内容（数值+标签） -->
        <div class="admin-stat-content">
          <div class="admin-stat-value">{{ loading ? '-' : (dashboard[stat.key] || 0) }}</div>
          <div class="admin-stat-label">{{ stat.label }}</div>
        </div>
        <!-- 趋势标签（上升/下降百分比） -->
        <div class="admin-stat-trend" v-if="stat.trend !== undefined">
          <el-tag size="small" :type="stat.trend >= 0 ? 'success' : 'danger'">
            {{ stat.trend >= 0 ? '↑' : '↓' }} {{ Math.abs(stat.trend) }}%
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 主内容区域（系统概览 + 快捷操作） -->
    <div class="admin-dashboard-grid">
      <!-- 系统概览面板 -->
      <div class="admin-dashboard-section">
        <!-- 面板标题栏 -->
        <div class="admin-section-header">
          <h3 class="admin-section-title">系统概览</h3>
          <el-tag type="success" size="small" effect="dark">运行中</el-tag>
        </div>
        <!-- 系统信息卡片 -->
        <div class="admin-section-card">
          <div class="admin-system-info">
            <!-- 系统版本信息 -->
            <div class="admin-system-item">
              <span class="admin-system-label">系统版本</span>
              <span class="admin-system-value">v1.0.0</span>
            </div>
            <!-- 运行状态信息 -->
            <div class="admin-system-item">
              <span class="admin-system-label">运行状态</span>
              <span class="admin-system-value status-online">
                <span class="status-dot"></span>
                在线
              </span>
            </div>
            <!-- 当前时间 -->
            <div class="admin-system-item">
              <span class="admin-system-label">当前时间</span>
              <span class="admin-system-value">{{ currentTime }}</span>
            </div>
            <!-- 当前登录管理员账号 -->
            <div class="admin-system-item">
              <span class="admin-system-label">管理员账号</span>
              <span class="admin-system-value">{{ authStore.user?.name || '-' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 快捷操作面板 -->
      <div class="admin-dashboard-section">
        <!-- 面板标题栏 -->
        <div class="admin-section-header">
          <h3 class="admin-section-title">快捷操作</h3>
        </div>
        <!-- 快捷操作按钮网格 -->
        <div class="admin-actions-grid">
          <el-button
            v-for="action in quickActions"
            :key="action.path"
            type="primary"
            class="admin-action-btn"
            @click="navigateTo(action.path)"
          >
            <el-icon><component :is="action.icon" /></el-icon>
            <span>{{ action.label }}</span>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 最近动态区域 -->
    <div class="admin-dashboard-section">
      <!-- 标题栏（含刷新按钮） -->
      <div class="admin-section-header">
        <h3 class="admin-section-title">最近动态</h3>
        <el-button text size="small" @click="refreshDashboard">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
      <!-- 动态列表卡片 -->
      <div class="admin-section-card">
        <!-- 加载中骨架屏 -->
        <div v-if="loading" class="admin-loading-state">
          <el-skeleton :rows="3" animated />
        </div>
        <!-- 空状态提示 -->
        <div v-else-if="recentActivities.length === 0" class="admin-empty-state">
          <el-icon><InfoFilled /></el-icon>
          <span>暂无最近动态</span>
        </div>
        <!-- 动态列表 -->
        <div v-else class="admin-activity-list">
          <div
            v-for="(activity, index) in recentActivities"
            :key="index"
            class="admin-activity-item"
          >
            <!-- 动态图标 -->
            <div class="admin-activity-icon" :class="activity.type">
              <el-icon><component :is="activity.icon" /></el-icon>
            </div>
            <!-- 动态内容（标题+时间） -->
            <div class="admin-activity-content">
              <div class="admin-activity-title">{{ activity.title }}</div>
              <div class="admin-activity-time">{{ activity.time }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 引入Vue响应式API和生命周期钩子
import { ref, onMounted, onUnmounted, onActivated, computed } from 'vue'
// 引入Vue路由
import { useRouter } from 'vue-router'
// 引入Element Plus图标组件
import {
  User,
  UserFilled,
  Setting,
  Wallet,
  TrendCharts,
  Refresh,
  InfoFilled,
  Bell,
  Document,
  CircleCheck
} from '@element-plus/icons-vue'
// 引入管理员API
import { adminAPI } from '../utils/api'
// 引入认证状态管理
import { useAuthStore } from '../stores/auth'

// 路由实例
const router = useRouter()
// 认证状态管理实例
const authStore = useAuthStore()

// 仪表盘数据
const dashboard = ref({})
// 数据加载状态
const loading = ref(false)
// 当前时间显示
const currentTime = ref('')
// 定时器句柄
let timer = null

// 统计卡片配置数据
const statCards = computed(() => [
  {
    key: 'totalUsers',
    label: '总用户数',
    icon: User,
    iconClass: 'users-icon',
    path: '/admin/users',
    trend: 12
  },
  {
    key: 'activeUsers',
    label: '活跃用户',
    icon: UserFilled,
    iconClass: 'active-icon',
    path: '/admin/users',
    trend: 8
  },
  {
    key: 'adminUsers',
    label: '管理员',
    icon: Setting,
    iconClass: 'admin-icon',
    path: '/admin/users'
  },
  {
    key: 'paidUsers',
    label: '付费用户',
    icon: Wallet,
    iconClass: 'paid-icon',
    path: '/admin/stats',
    trend: 15
  }
])

// 快捷操作配置数据
const quickActions = [
  { label: '用户管理', path: '/admin/users', icon: User },
  { label: '数据统计', path: '/admin/stats', icon: TrendCharts },
  { label: '告警管理', path: '/admin/alerts', icon: Bell }
]

// 最近动态数据（根据仪表盘数据计算生成）
const recentActivities = computed(() => {
  const activities = []
  // 总用户数动态
  if (dashboard.value.totalUsers > 0) {
    activities.push({
      title: `系统共有 ${dashboard.value.totalUsers} 位注册用户`,
      time: '刚刚',
      type: 'info',
      icon: Document
    })
  }
  // 活跃用户动态
  if (dashboard.value.activeUsers > 0) {
    activities.push({
      title: `其中 ${dashboard.value.activeUsers} 位用户处于活跃状态`,
      time: '刚刚',
      type: 'success',
      icon: CircleCheck
    })
  }
  return activities
})

/**
 * 更新当前时间显示
 * 每秒刷新一次，格式化为本地时间字符串
 */
function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

/**
 * 获取仪表盘数据
 * 调用后端API获取统计数据
 */
async function fetchDashboard() {
  loading.value = true
  try {
    const data = await adminAPI.getDashboard()
    dashboard.value = data || {}
  } catch (error) {
    console.error('获取仪表盘数据失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 刷新仪表盘数据
 * 重新调用fetchDashboard获取最新数据
 */
function refreshDashboard() {
  fetchDashboard()
}

/**
 * 跳转到指定路由
 * @param {string} path - 目标路由路径
 */
function navigateTo(path) {
  router.push(path)
}

// 组件挂载时初始化
onMounted(() => {
  // 获取仪表盘数据
  fetchDashboard()
  // 初始化时间显示
  updateTime()
  // 启动定时器，每秒更新时间
  timer = setInterval(updateTime, 1000)
})

// 页面从缓存恢复时重新加载数据（keep-alive）
onActivated(() => {
  fetchDashboard()
})

// 组件卸载时清理定时器
onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
/* 仪表盘主容器 - 垂直排列 */
.admin-dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 统计卡片网格布局 */
.admin-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

/* 统计卡片样式 */
.admin-stat-card {
  background: rgba(30, 41, 59, 0.8);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.25s ease;
  cursor: pointer;
  animation: slideUp 0.4s ease forwards;
  opacity: 0;
}

/* 统计卡片悬停效果 */
.admin-stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
  border-color: rgba(59, 130, 246, 0.3);
}

/* 统计卡片图标容器 */
.admin-stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

/* 用户数图标样式 */
.users-icon {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2) 0%, rgba(59, 130, 246, 0.05) 100%);
  color: #3b82f6;
}

/* 活跃用户图标样式 */
.active-icon {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.2) 0%, rgba(34, 197, 94, 0.05) 100%);
  color: #22c55e;
}

/* 管理员图标样式 */
.admin-icon {
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.2) 0%, rgba(168, 85, 247, 0.05) 100%);
  color: #a855f7;
}

/* 付费用户图标样式 */
.paid-icon {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.2) 0%, rgba(245, 158, 11, 0.05) 100%);
  color: #f59e0b;
}

/* 统计卡片内容区域 */
.admin-stat-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

/* 统计数值样式 */
.admin-stat-value {
  font-size: 28px;
  font-weight: 700;
  color: white;
  line-height: 1.2;
}

/* 统计标签样式 */
.admin-stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 4px;
}

/* 趋势标签区域 */
.admin-stat-trend {
  flex-shrink: 0;
}

/* 主内容网格布局（系统概览+快捷操作） */
.admin-dashboard-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

/* 仪表盘面板区域 */
.admin-dashboard-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 面板标题栏 */
.admin-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 面板标题文字 */
.admin-section-title {
  font-size: 16px;
  font-weight: 600;
  color: white;
  margin: 0;
}

/* 面板卡片容器 */
.admin-section-card {
  background: rgba(30, 41, 59, 0.8);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  flex: 1;
}

/* 系统信息网格布局 */
.admin-system-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 20px;
}

/* 单个系统信息项 */
.admin-system-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* 系统信息标签 */
.admin-system-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

/* 系统信息值 */
.admin-system-value {
  font-size: 14px;
  font-weight: 600;
  color: white;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 在线状态文字颜色 */
.status-online {
  color: #22c55e;
}

/* 在线状态小圆点 */
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 8px #22c55e;
  animation: pulse 2s infinite;
}

/* 快捷操作按钮网格 */
.admin-actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
}

/* 快捷操作按钮样式 */
.admin-action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(99, 102, 241, 0.15) 100%);
  border: 1px solid rgba(59, 130, 246, 0.25);
  border-radius: 12px;
  height: auto;
}

/* 快捷操作按钮悬停效果 */
.admin-action-btn:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.25) 0%, rgba(99, 102, 241, 0.25) 100%);
  border-color: rgba(59, 130, 246, 0.4);
  transform: translateY(-2px);
}

/* 快捷操作按钮图标大小 */
.admin-action-btn .el-icon {
  font-size: 22px;
}

/* 快捷操作按钮文字大小 */
.admin-action-btn span {
  font-size: 13px;
}

/* 加载状态区域 */
.admin-loading-state {
  padding: 8px 0;
}

/* 空状态提示 */
.admin-empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 0;
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
}

/* 动态列表容器 */
.admin-activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 单个动态项 */
.admin-activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.4);
  transition: background 0.2s ease;
}

/* 动态项悬停效果 */
.admin-activity-item:hover {
  background: rgba(15, 23, 42, 0.6);
}

/* 动态图标容器 */
.admin-activity-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 信息类型动态图标 */
.admin-activity-icon.info {
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
}

/* 成功类型动态图标 */
.admin-activity-icon.success {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

/* 警告类型动态图标 */
.admin-activity-icon.warning {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
}

/* 动态内容区域 */
.admin-activity-content {
  flex: 1;
  min-width: 0;
}

/* 动态标题 */
.admin-activity-title {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

/* 动态时间 */
.admin-activity-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 2px;
}

/* 滑入动画 */
@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 脉冲动画（用于在线状态点） */
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* 平板响应式适配 */
@media (max-width: 900px) {
  .admin-dashboard-grid {
    grid-template-columns: 1fr;
  }
}

/* 手机响应式适配 */
@media (max-width: 640px) {
  .admin-stats-grid {
    grid-template-columns: 1fr;
  }

  .admin-system-info {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
