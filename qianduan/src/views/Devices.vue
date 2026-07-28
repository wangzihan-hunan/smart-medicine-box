<!-- 设备管理页面模板 -->
<template>
  <!-- 设备管理页面主容器 -->
  <div class="devices-page">
    <div class="page-container">
      <!-- 页面头部：标题和副标题 -->
      <div class="page-header">
        <h2 class="section-title">设备管理</h2>
        <p class="section-subtitle">管理您绑定的智能药箱设备</p>
      </div>
      
      <!-- 加载中状态 -->
      <div v-if="loading" class="loading-state">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      
      <!-- 空状态：没有绑定设备时显示 -->
      <div v-else-if="devices.length === 0" class="empty-state">
        <el-icon class="empty-icon"><Box /></el-icon>
        <div class="empty-text">暂无绑定设备</div>
        <div class="empty-hint">发送指令后，设备将自动出现在这里</div>
        <button class="btn-primary" @click="$router.push('/commands')">前往发送指令</button>
      </div>
      
      <!-- 设备列表网格 -->
      <div v-else class="devices-grid">
        <!-- 单个设备卡片：点击查看详情 -->
        <div 
          v-for="device in devices" 
          :key="device.deviceId"
          class="device-card"
          @click="showDetail(device.deviceId)"
        >
          <!-- 设备卡片头部：图标、设备ID、状态 -->
          <div class="device-header">
            <div class="device-icon">
              <el-icon><Box /></el-icon>
            </div>
            <div class="device-info">
              <div class="device-id">{{ device.deviceId }}</div>
              <div class="device-status">
                <span :class="['status-dot', getStatusClass(device.lastStatus)]"></span>
                {{ getStatusLabel(device.lastStatus) }}
              </div>
            </div>
          </div>
          
          <!-- 设备统计数据：总指令数、已执行数 -->
          <div class="device-stats">
            <div class="stat-item">
              <div class="stat-value">{{ device.totalCommands }}</div>
              <div class="stat-label">总指令</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ device.executedCommands }}</div>
              <div class="stat-label">已执行</div>
            </div>
          </div>
          
          <!-- 设备底部信息：最后操作和时间 -->
          <div class="device-footer">
            <div class="footer-item">
              <span class="footer-label">最后操作</span>
              <span class="footer-value">{{ device.lastAction || '-' }}</span>
            </div>
            <div class="footer-item">
              <span class="footer-label">最后时间</span>
              <span class="footer-value">{{ device.lastCommandTime || '-' }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 设备详情弹窗 -->
      <el-dialog v-model="detailVisible" title="设备详情" width="600px" destroy-on-close>
        <!-- 弹窗加载中状态 -->
        <div v-if="detailLoading" class="dialog-loading">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        <!-- 设备详情内容 -->
        <div v-else-if="deviceDetail" class="detail-content">
          <div class="detail-header">
            <div class="detail-icon">
              <el-icon><Box /></el-icon>
            </div>
            <div>
              <div class="detail-id">{{ deviceDetail.deviceId }}</div>
              <div class="detail-status">
                <span :class="['status-dot', getStatusClass(deviceDetail.lastStatus)]"></span>
                {{ getStatusLabel(deviceDetail.lastStatus) }}
              </div>
            </div>
          </div>
          
          <!-- 详情统计数据：总指令数、已执行、执行率 -->
          <div class="detail-stats">
            <div class="detail-stat">
              <div class="detail-stat-value">{{ deviceDetail.totalCommands }}</div>
              <div class="detail-stat-label">总指令数</div>
            </div>
            <div class="detail-stat">
              <div class="detail-stat-value">{{ deviceDetail.executedCommands }}</div>
              <div class="detail-stat-label">已执行</div>
            </div>
            <div class="detail-stat">
              <div class="detail-stat-value">
                {{ deviceDetail.totalCommands > 0 ? Math.round(deviceDetail.executedCommands / deviceDetail.totalCommands * 100) : 0 }}%
              </div>
              <div class="detail-stat-label">执行率</div>
            </div>
          </div>
          
          <!-- 最近指令列表 -->
          <div class="detail-section">
            <h4>最近指令</h4>
            <div v-if="deviceDetail.recentCommands && deviceDetail.recentCommands.length > 0" class="commands-list">
              <div v-for="cmd in deviceDetail.recentCommands" :key="cmd.id" class="command-item">
                <div class="command-info">
                  <span class="command-action">{{ cmd.action }}</span>
                  <span :class="['command-status', getStatusClass(cmd.status)]">{{ cmd.status }}</span>
                </div>
                <div class="command-time">{{ cmd.time }}</div>
              </div>
            </div>
            <div v-else class="no-commands">暂无指令记录</div>
          </div>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
// 引入Vue核心API：响应式引用、生命周期钩子
import { ref, onMounted, onActivated } from 'vue'
// 引入Element Plus消息提示组件
import { ElMessage } from 'element-plus'
// 引入Element Plus图标组件
import { Loading, Box } from '@element-plus/icons-vue'
// 引入指令相关的API接口
import { commandAPI } from '../utils/api'

// 设备列表加载状态
const loading = ref(false)
// 设备详情加载状态
const detailLoading = ref(false)
// 设备列表数据
const devices = ref([])
// 详情弹窗显示状态
const detailVisible = ref(false)
// 设备详情数据
const deviceDetail = ref(null)

// 获取设备列表
async function fetchDevices() {
  loading.value = true
  try {
    const response = await commandAPI.getDevices()
    devices.value = response || []
  } catch (error) {
    console.error('获取设备列表失败:', error)
    ElMessage.error('获取设备列表失败')
  } finally {
    loading.value = false
  }
}

// 显示设备详情弹窗
async function showDetail(deviceId) {
  detailVisible.value = true
  detailLoading.value = true
  deviceDetail.value = null
  try {
    const response = await commandAPI.getDeviceDetail(deviceId)
    deviceDetail.value = response
  } catch (error) {
    console.error('获取设备详情失败:', error)
    ElMessage.error('获取设备详情失败')
  } finally {
    detailLoading.value = false
  }
}

// 根据状态文本获取对应的样式类名
function getStatusClass(status) {
  if (!status) return 'status-unknown'
  if (status.includes('执行')) return 'status-success'
  if (status.includes('发送')) return 'status-pending'
  if (status.includes('失败')) return 'status-failed'
  return 'status-unknown'
}

// 获取状态标签文本
function getStatusLabel(status) {
  if (!status) return '未知'
  return status
}

// 组件挂载时获取设备列表
onMounted(() => {
  fetchDevices()
})

// 页面从缓存恢复时重新加载数据（keep-alive）
onActivated(() => {
  fetchDevices()
})
</script>

<style scoped>
/* 设备管理页面主容器样式 */
.devices-page {
  padding-top: 80px;
  min-height: 100vh;
}

/* 页面头部样式 */
.page-header {
  margin-bottom: 32px;
}

/* 页面标题样式 */
.section-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

/* 页面副标题样式 */
.section-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 加载中状态和弹窗加载样式 */
.loading-state, .dialog-loading {
  text-align: center;
  padding: 60px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* 加载图标旋转动画 */
.loading-icon {
  animation: spin 1s linear infinite;
}

/* 旋转动画 */
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 空状态容器样式 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
}

/* 空状态图标样式 */
.empty-icon {
  font-size: 64px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

/* 空状态主文字 */
.empty-text {
  font-size: 18px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

/* 空状态提示文字 */
.empty-hint {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 24px;
}

/* 设备列表网格布局 */
.devices-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

/* 设备卡片样式 */
.device-card {
  background: white;
  border-radius: var(--border-radius-lg);
  padding: 24px;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all var(--transition-normal);
}

/* 设备卡片悬停效果 */
.device-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--card-shadow-hover);
}

/* 设备卡片头部样式 */
.device-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

/* 设备图标容器样式 */
.device-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--primary-50);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

/* 设备信息区域 */
.device-info {
  flex: 1;
}

/* 设备ID样式 */
.device-id {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

/* 设备状态行样式 */
.device-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

/* 状态指示点样式 */
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

/* 成功状态颜色 */
.status-dot.status-success {
  background: var(--success-color);
}

/* 待处理状态颜色 */
.status-dot.status-pending {
  background: var(--warning-color, #f59e0b);
}

/* 失败状态颜色 */
.status-dot.status-failed {
  background: var(--danger-color);
}

/* 未知状态颜色 */
.status-dot.status-unknown {
  background: var(--text-muted);
}

/* 设备统计区域样式 */
.device-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
  padding: 16px;
  background: var(--bg-secondary, #f9fafb);
  border-radius: var(--border-radius);
}

/* 统计项样式 */
.stat-item {
  text-align: center;
}

/* 统计数值样式 */
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

/* 统计标签样式 */
.stat-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

/* 设备底部信息区域 */
.device-footer {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

/* 底部信息项样式 */
.footer-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

/* 底部标签样式 */
.footer-label {
  color: var(--text-muted);
}

/* 底部数值样式 */
.footer-value {
  color: var(--text-primary);
}

/* 详情弹窗内容样式 */
.detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 详情头部样式 */
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
}

/* 详情图标容器样式 */
.detail-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: var(--primary-50);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

/* 详情设备ID样式 */
.detail-id {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

/* 详情设备状态样式 */
.detail-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-secondary);
}

/* 详情统计区域布局 */
.detail-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

/* 详情统计项样式 */
.detail-stat {
  text-align: center;
  padding: 16px;
  background: var(--bg-secondary, #f9fafb);
  border-radius: var(--border-radius);
}

/* 详情统计数值样式 */
.detail-stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--primary-color);
}

/* 详情统计标签样式 */
.detail-stat-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

/* 详情分区标题样式 */
.detail-section h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

/* 指令列表容器样式 */
.commands-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
}

/* 单条指令项样式 */
.command-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: var(--bg-secondary, #f9fafb);
  border-radius: var(--border-radius-sm);
}

/* 指令信息区域 */
.command-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 指令动作样式 */
.command-action {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

/* 指令状态标签样式 */
.command-status {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
}

/* 成功状态标签颜色 */
.command-status.status-success {
  background: rgba(34, 197, 94, 0.1);
  color: var(--success-color);
}

/* 待处理状态标签颜色 */
.command-status.status-pending {
  background: rgba(245, 158, 11, 0.1);
  color: var(--warning-color, #f59e0b);
}

/* 失败状态标签颜色 */
.command-status.status-failed {
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger-color);
}

/* 未知状态标签颜色 */
.command-status.status-unknown {
  background: rgba(107, 114, 128, 0.1);
  color: var(--text-muted);
}

/* 指令时间样式 */
.command-time {
  font-size: 12px;
  color: var(--text-muted);
}

/* 无指令提示样式 */
.no-commands {
  text-align: center;
  padding: 20px;
  color: var(--text-muted);
  font-size: 14px;
}

/* 主要按钮样式 */
.btn-primary {
  padding: 10px 24px;
  background: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--border-radius-sm);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

/* 主要按钮悬停效果 */
.btn-primary:hover {
  background: var(--primary-dark);
}

/* 平板响应式：单列布局 */
@media (max-width: 768px) {
  .devices-grid {
    grid-template-columns: 1fr;
  }
  
  .detail-stats {
    grid-template-columns: 1fr;
  }
}
</style>
