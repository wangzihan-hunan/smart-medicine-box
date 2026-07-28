<!-- 消息通知页面模板 -->
<template>
  <!-- 通知页面主容器 -->
  <div class="notifications-page">
    <!-- 页面头部：标题和操作按钮 -->
    <div class="notifications-header">
      <h2 class="notifications-title">消息通知</h2>
      <!-- 操作按钮区域：全部标为已读、清空全部 -->
      <div class="notifications-actions">
        <el-button @click="testBrowserAlert" type="success">
          <el-icon><Bell /></el-icon>
          测试告警
        </el-button>
        <el-button @click="markAllAsRead" :disabled="!notifications.length">
          <el-icon><Check /></el-icon>
          全部标为已读
        </el-button>
        <el-button type="danger" @click="deleteAll" :disabled="!notifications.length">
          <el-icon><Delete /></el-icon>
          清空全部
        </el-button>
      </div>
    </div>

    <!-- 通知统计区域：显示未读数量和总数量 -->
    <div v-if="stats" class="notifications-stats">
      <span class="stat-item">
        <span class="stat-label">未读</span>
        <span class="stat-value unread">{{ stats.unreadCount }}</span>
      </span>
      <span class="stat-divider">|</span>
      <span class="stat-item">
        <span class="stat-label">全部</span>
        <span class="stat-value">{{ stats.totalCount }}</span>
      </span>
    </div>

    <!-- 空状态提示：当没有通知时显示 -->
    <div v-if="!loading && notifications.length === 0" class="notifications-empty">
      <el-icon class="empty-icon"><Bell /></el-icon>
      <p>暂无通知</p>
    </div>

    <!-- 通知列表区域：循环渲染所有通知 -->
    <div v-else class="notifications-list">
      <!-- 单条通知项：点击可标记为已读 -->
      <div 
        v-for="notification in notifications" 
        :key="notification.id"
        class="notification-item"
        :class="{ 'is-unread': !notification.readStatus }"
        @click="markAsRead(notification)"
      >
        <!-- 通知图标 -->
        <div class="notification-icon">
          <el-icon><Bell /></el-icon>
        </div>
        <!-- 通知内容：标题、正文、类型和时间 -->
        <div class="notification-content">
          <h3 class="notification-title">{{ notification.title }}</h3>
          <p class="notification-body">{{ notification.content }}</p>
          <div class="notification-meta">
            <span class="notification-type">{{ getTypeLabel(notification.type) }}</span>
            <span class="notification-time">{{ formatTime(notification.createdAt) }}</span>
          </div>
        </div>
        <!-- 通知操作按钮：标记已读/删除 -->
        <div class="notification-actions">
          <el-button size="small" type="text" @click.stop="markAsRead(notification)">
            {{ notification.readStatus ? '已读' : '标为已读' }}
          </el-button>
          <el-button size="small" type="text" @click.stop="deleteNotification(notification.id)">
            删除
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 引入Vue核心API：响应式引用、生命周期钩子
import { ref, onMounted, onActivated, onUnmounted } from 'vue'
// 引入Element Plus消息提示和确认对话框组件
import { ElMessage, ElMessageBox } from 'element-plus'
// 引入Element Plus图标组件
import { Bell, Check, Delete } from '@element-plus/icons-vue'
// 引入通知相关的API接口
import { notificationAPI } from '../utils/api'
import { alertWithSound, requestNotificationPermission, getNotificationPermission, PermissionStatus } from '../utils/browserAlert'

// 加载状态：控制加载动画显示
const loading = ref(false)
// 通知列表数据
const notifications = ref([])
// 通知统计数据（未读数量、总数量）
const stats = ref(null)

// 通知类型标签映射：将后端类型码转换为中文显示
const typeLabels = {
  'STOCK_ALERT': '库存预警',
  'EXPIRED_ALERT': '过期提醒',
  'SYSTEM': '系统通知',
  'ADMIN': '管理员通知',
  'PLAN': '套餐通知'
}

// 获取通知类型的中文标签
function getTypeLabel(type) {
  return typeLabels[type] || type || '通知'
}

// 测试浏览器告警功能
async function testBrowserAlert() {
  // 检查权限
  let permission = getNotificationPermission()
  if (permission === PermissionStatus.DEFAULT) {
    // 请求权限
    permission = await requestNotificationPermission()
  }
  
  if (permission === PermissionStatus.DENIED || permission === PermissionStatus.UNSUPPORTED) {
    ElMessage.warning('桌面通知权限未开启，请在浏览器设置中允许通知')
  }

  // 无论权限如何，都播放声音测试（声音不需要权限）
  // 如果有桌面通知权限，则同时显示桌面通知
  alertWithSound({
    title: '测试告警',
    body: `这是一条测试通知，当前时间：${new Date().toLocaleTimeString()}`,
    soundEnabled: true,
    soundOptions: {
      frequency: 1200,
      duration: 0.2,
      volume: 0.4,
      repeat: 2,
      interval: 0.1
    },
    notificationOptions: {
      requireInteraction: true,
      onClick: () => {
        ElMessage.info('你点击了桌面通知')
      }
    }
  })
  
  ElMessage.success('已触发测试告警，请查看桌面通知和声音效果')
}

// 格式化时间：将日期字符串转换为相对时间或日期格式
function formatTime(dateString) {
  if (!dateString) return ''
  const date = new Date(dateString)
  const now = new Date()
  const diff = now - date
  
  // 计算时间差（分钟、小时、天）
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  // 根据时间差返回不同格式
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  // 超过7天显示完整日期
  return date.toLocaleDateString('zh-CN')
}

// 获取通知列表数据
async function fetchNotifications() {
  loading.value = true
  try {
    // 调用API获取通知列表，第0页，每页50条
    const data = await notificationAPI.getNotifications(0, 50)
    notifications.value = data || []
  } catch (error) {
    console.error('获取通知失败:', error)
    ElMessage.error('获取通知失败')
  } finally {
    loading.value = false
  }
}

// 获取通知统计数据
async function fetchStats() {
  try {
    const data = await notificationAPI.getNotificationStats()
    stats.value = data || null
  } catch (error) {
    console.error('获取通知统计失败:', error)
  }
}

// 将单条通知标记为已读
async function markAsRead(notification) {
  // 如果已经是已读状态则直接返回
  if (notification.readStatus) return
  
  try {
    await notificationAPI.markNotificationAsRead(notification.id)
    // 更新本地状态为已读
    notification.readStatus = true
    // 更新未读统计数量
    if (stats.value) {
      stats.value.unreadCount--
    }
    ElMessage.success('已标记为已读')
  } catch (error) {
    console.error('标记已读失败:', error)
    ElMessage.error('操作失败')
  }
}

// 将所有通知标记为已读
async function markAllAsRead() {
  try {
    await notificationAPI.markAllNotificationsAsRead()
    // 批量更新本地所有通知为已读状态
    notifications.value.forEach(n => n.readStatus = true)
    // 重置未读统计为0
    if (stats.value) {
      stats.value.unreadCount = 0
    }
    ElMessage.success('所有通知已标记为已读')
  } catch (error) {
    console.error('标记全部已读失败:', error)
    ElMessage.error('操作失败')
  }
}

// 删除单条通知
async function deleteNotification(id) {
  try {
    // 弹出确认对话框
    await ElMessageBox.confirm('确定要删除这条通知吗？', '删除通知', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await notificationAPI.deleteNotification(id)
    // 从本地列表中移除该通知
    notifications.value = notifications.value.filter(n => n.id !== id)
    // 更新总数统计
    if (stats.value) {
      stats.value.totalCount--
    }
    ElMessage.success('删除成功')
  } catch (error) {
    // 用户取消操作时不显示错误
    if (error !== 'cancel') {
      console.error('删除通知失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 清空所有通知
async function deleteAll() {
  try {
    // 弹出警告确认对话框
    await ElMessageBox.confirm('确定要清空所有通知吗？此操作不可恢复。', '清空通知', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await notificationAPI.deleteAllNotifications()
    // 清空本地通知列表
    notifications.value = []
    // 重置所有统计数据
    if (stats.value) {
      stats.value.unreadCount = 0
      stats.value.totalCount = 0
    }
    ElMessage.success('已清空所有通知')
  } catch (error) {
    // 用户取消操作时不显示错误
    if (error !== 'cancel') {
      console.error('清空通知失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

// 处理实时推送的通知更新：追加新通知或刷新统计
function handleRealtimeUpdate(e) {
  const payload = e.detail
  if (!payload) return

  if (payload.action === 'NEW' && payload.id) {
    // 避免重复添加
    const exists = notifications.value.some(n => n.id === payload.id)
    if (!exists) {
      notifications.value.unshift({
        id: payload.id,
        title: payload.title,
        content: payload.content,
        type: payload.type,
        readStatus: false,
        createdAt: payload.createdAt || new Date().toISOString()
      })
      if (stats.value) {
        stats.value.unreadCount++
        stats.value.totalCount++
      }
    }
  } else if (payload.action === 'READ') {
    // 状态同步：重新拉取确保一致性
    fetchStats()
  }
}

// 组件挂载时获取通知列表和统计数据，并监听实时推送
onMounted(() => {
  fetchNotifications()
  fetchStats()
  window.addEventListener('notification:update', handleRealtimeUpdate)
})

// 页面从缓存恢复时重新加载数据（keep-alive）
onActivated(() => {
  fetchNotifications()
  fetchStats()
})

// 组件卸载时移除事件监听
onUnmounted(() => {
  window.removeEventListener('notification:update', handleRealtimeUpdate)
})
</script>

<style scoped>
/* 通知页面主容器样式 */
.notifications-page {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
}

/* 页面头部样式：标题和操作按钮的布局 */
.notifications-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

/* 页面标题样式 */
.notifications-title {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

/* 操作按钮容器样式 */
.notifications-actions {
  display: flex;
  gap: 12px;
}

/* 通知统计栏样式 */
.notifications-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: #f3f4f6;
  border-radius: 8px;
  margin-bottom: 20px;
}

/* 统计项样式 */
.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 统计标签样式 */
.stat-label {
  font-size: 14px;
  color: #6b7280;
}

/* 统计数值样式 */
.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}

/* 未读数量的特殊红色样式 */
.stat-value.unread {
  color: #ef4444;
}

/* 统计分隔符样式 */
.stat-divider {
  color: #d1d5db;
}

/* 空状态容器样式 */
.notifications-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

/* 空状态图标样式 */
.empty-icon {
  font-size: 48px;
  color: #d1d5db;
  margin-bottom: 16px;
}

/* 空状态文字样式 */
.notifications-empty p {
  color: #9ca3af;
  font-size: 16px;
  margin: 0;
}

/* 通知列表容器样式 */
.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 单条通知项样式 */
.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  transition: all 0.2s ease;
}

/* 通知项悬停效果 */
.notification-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

/* 未读通知的高亮样式 */
.notification-item.is-unread {
  background: #fef3c7;
  border-color: #fbbf24;
}

/* 通知图标容器样式 */
.notification-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eff6ff;
  border-radius: 12px;
  flex-shrink: 0;
}

/* 通知图标样式 */
.notification-icon el-icon {
  font-size: 24px;
  color: #3b82f6;
}

/* 通知内容区域样式 */
.notification-content {
  flex: 1;
  min-width: 0;
}

/* 通知标题样式 */
.notification-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px 0;
}

/* 通知正文样式：限制显示2行，超出省略 */
.notification-body {
  font-size: 14px;
  color: #4b5563;
  margin: 0 0 12px 0;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 通知元信息区域：类型和时间 */
.notification-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 通知类型标签样式 */
.notification-type {
  font-size: 12px;
  padding: 2px 8px;
  background: #f3f4f6;
  border-radius: 4px;
  color: #6b7280;
}

/* 通知时间样式 */
.notification-time {
  font-size: 12px;
  color: #9ca3af;
}

/* 通知操作按钮区域样式 */
.notification-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 操作按钮内边距调整 */
.notification-actions .el-button {
  padding: 4px 8px;
}
</style>
