<template>
  <!-- 数据统计页面主容器 -->
  <div class="admin-stats">
    <!-- 页面头部（标题+刷新按钮） -->
    <div class="admin-stats-header">
      <!-- 标题和描述 -->
      <div class="admin-stats-title">
        <h3>数据统计</h3>
        <p>系统数据统计与分析</p>
      </div>
      <!-- 刷新按钮 -->
      <el-button text size="small" @click="refreshStats">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 汇总统计卡片区域 -->
    <div class="admin-stats-summary">
      <!-- 单个统计卡片 - 循环渲染 -->
      <div
        v-for="(item, index) in summaryCards"
        :key="item.label"
        class="admin-summary-card"
        :class="item.type"
        :style="{ animationDelay: `${index * 0.08}s` }"
      >
        <!-- 统计卡片图标 -->
        <div class="admin-summary-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <!-- 统计卡片内容（数值+标签） -->
        <div class="admin-summary-content">
          <div class="admin-summary-value">{{ loading ? '-' : item.value }}</div>
          <div class="admin-summary-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <!-- 统计图表面板网格（套餐分布+角色分布） -->
    <div class="admin-stats-grid">
      <!-- 套餐分布面板 -->
      <div class="admin-stat-panel">
        <!-- 面板标题栏 -->
        <div class="admin-panel-header">
          <h4 class="admin-panel-title">套餐分布</h4>
          <el-tag size="small" type="info">用户数</el-tag>
        </div>
        <!-- 图表容器 -->
        <div class="admin-chart-container">
          <!-- 柱状图区域 -->
          <div class="admin-chart-bars">
            <!-- 单个柱状图项 - 循环渲染 -->
            <div
              v-for="(item, index) in planData"
              :key="item.label"
              class="admin-chart-bar-item"
              :style="{ animationDelay: `${index * 0.1}s` }"
            >
              <!-- 柱状图标签 -->
              <div class="admin-chart-bar-label">{{ item.label }}</div>
              <!-- 柱状图轨道和填充条 -->
              <div class="admin-chart-bar-track">
                <div class="admin-chart-bar-fill" :class="item.type" :style="{ width: item.percent + '%' }"></div>
              </div>
              <!-- 柱状图数值 -->
              <div class="admin-chart-bar-value">{{ item.value }}</div>
            </div>
          </div>
          <!-- 总用户数统计 -->
          <div class="admin-chart-total">总用户数：{{ totalUsers }}</div>
        </div>
      </div>

      <!-- 角色分布面板 -->
      <div class="admin-stat-panel">
        <!-- 面板标题栏 -->
        <div class="admin-panel-header">
          <h4 class="admin-panel-title">角色分布</h4>
          <el-tag size="small" type="info">占比</el-tag>
        </div>
        <!-- 饼图容器 -->
        <div class="admin-chart-container">
          <!-- 饼图区域 -->
          <div class="admin-pie-chart">
            <!-- 饼图切片 -->
            <div class="admin-pie-slices">
              <!-- 普通用户切片 -->
              <div
                class="admin-pie-slice user-slice"
                :style="{ transform: `rotate(0deg)`, '--percent': userPercent }"
              ></div>
              <!-- 管理员切片（仅当有管理员时显示） -->
              <div
                v-if="adminPercent > 0"
                class="admin-pie-slice admin-slice"
                :style="{ transform: `rotate(${userPercent * 3.6}deg)`, '--percent': adminPercent }"
              ></div>
            </div>
            <!-- 饼图中心显示总用户数 -->
            <div class="admin-pie-center">
              <div class="admin-pie-value">{{ totalUsers }}</div>
              <div class="admin-pie-label">总用户</div>
            </div>
          </div>
          <!-- 图例说明 -->
          <div class="admin-pie-legend">
            <!-- 普通用户图例 -->
            <div class="admin-pie-legend-item">
              <span class="admin-pie-legend-dot user"></span>
              <span>普通用户</span>
              <span class="admin-pie-legend-value">{{ userCount }} ({{ userPercent }}%)</span>
            </div>
            <!-- 管理员图例 -->
            <div class="admin-pie-legend-item">
              <span class="admin-pie-legend-dot admin"></span>
              <span>管理员</span>
              <span class="admin-pie-legend-value">{{ adminCount }} ({{ adminPercent }}%)</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 状态分布面板（通栏显示） -->
    <div class="admin-stat-panel wide">
      <!-- 面板标题栏 -->
      <div class="admin-panel-header">
        <h4 class="admin-panel-title">状态分布</h4>
        <el-tag size="small" type="info">账号状态</el-tag>
      </div>
      <!-- 状态卡片网格 -->
      <div class="admin-status-cards">
        <!-- 单个状态卡片 - 循环渲染 -->
        <div
          v-for="(item, index) in statusData"
          :key="item.label"
          class="admin-status-card"
          :class="item.type"
          :style="{ animationDelay: `${index * 0.08}s` }"
        >
          <!-- 状态图标 -->
          <div class="admin-status-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <!-- 状态信息（数值+标签） -->
          <div class="admin-status-info">
            <div class="admin-status-value">{{ item.value }}</div>
            <div class="admin-status-label">{{ item.label }}</div>
          </div>
          <!-- 状态占比百分比 -->
          <div class="admin-status-percent">{{ item.percent }}%</div>
        </div>
      </div>
    </div>

    <!-- 数据说明面板（通栏显示） -->
    <div class="admin-stat-panel wide">
      <!-- 面板标题栏 -->
      <div class="admin-panel-header">
        <h4 class="admin-panel-title">数据说明</h4>
      </div>
      <!-- 说明文字内容 -->
      <div class="admin-stats-note">
        <p><strong>套餐分布：</strong>展示用户当前订阅的套餐类型分布情况，包括免费版、基础版和专业版。</p>
        <p><strong>角色分布：</strong>展示系统中普通用户和管理员的比例。</p>
        <p><strong>状态分布：</strong>展示用户账号当前状态，包括正常、锁定、禁用等。</p>
      </div>
    </div>
  </div>
</template>

<script setup>
// 引入Vue响应式API、计算属性和生命周期钩子
import { ref, computed, onMounted, onActivated } from 'vue'
// 引入Element Plus图标组件
import {
  CircleCheck,
  Lock,
  User,
  UserFilled,
  Wallet,
  TrendCharts,
  Refresh,
  Warning
} from '@element-plus/icons-vue'
// 引入管理员API
import { adminAPI } from '../utils/api'

// 仪表盘统计数据
const dashboard = ref({})
// 数据加载状态
const loading = ref(false)

// 总用户数（计算属性）
const totalUsers = computed(() => dashboard.value.totalUsers || 0)
// 活跃用户数（计算属性）
const activeUsers = computed(() => dashboard.value.activeUsers || 0)
// 管理员用户数（计算属性）
const adminUsers = computed(() => dashboard.value.adminUsers || 0)
// 付费用户数（计算属性）
const paidUsers = computed(() => dashboard.value.paidUsers || 0)

// 免费版用户数（计算属性：总用户数-付费用户数）
const freeCount = computed(() => Math.max(0, totalUsers.value - paidUsers.value))
// 基础版用户数（计算属性：付费用户的70%）
const basicCount = computed(() => Math.max(0, Math.floor(paidUsers.value * 0.7)))
// 专业版用户数（计算属性：付费用户-基础版）
const proCount = computed(() => Math.max(0, paidUsers.value - basicCount.value))
// 普通用户数（计算属性：总用户数-管理员数）
const userCount = computed(() => Math.max(0, totalUsers.value - adminUsers.value))
// 锁定/禁用用户数（计算属性：总用户数-活跃用户数）
const lockedCount = computed(() => Math.max(0, totalUsers.value - activeUsers.value))

// 免费版用户占比
const freePercent = computed(() => totalUsers.value ? Math.round((freeCount.value / totalUsers.value) * 100) : 0)
// 基础版用户占比
const basicPercent = computed(() => totalUsers.value ? Math.round((basicCount.value / totalUsers.value) * 100) : 0)
// 专业版用户占比
const proPercent = computed(() => totalUsers.value ? Math.round((proCount.value / totalUsers.value) * 100) : 0)

// 普通用户占比
const userPercent = computed(() => totalUsers.value ? Math.round((userCount.value / totalUsers.value) * 100) : 0)
// 管理员占比
const adminPercent = computed(() => totalUsers.value ? Math.round((adminUsers.value / totalUsers.value) * 100) : 0)

// 活跃用户占比
const activePercent = computed(() => totalUsers.value ? Math.round((activeUsers.value / totalUsers.value) * 100) : 0)
// 锁定用户占比
const lockedPercent = computed(() => totalUsers.value ? Math.round((lockedCount.value / totalUsers.value) * 100) : 0)

// 汇总统计卡片数据（计算属性）
const summaryCards = computed(() => [
  { label: '总用户数', value: totalUsers.value, icon: User, type: 'primary' },
  { label: '活跃用户', value: activeUsers.value, icon: UserFilled, type: 'success' },
  { label: '管理员', value: adminUsers.value, icon: TrendCharts, type: 'warning' },
  { label: '付费用户', value: paidUsers.value, icon: Wallet, type: 'info' }
])

// 套餐分布数据（计算属性）
const planData = computed(() => [
  { label: '免费版', value: freeCount.value, percent: freePercent.value, type: 'free' },
  { label: '基础版', value: basicCount.value, percent: basicPercent.value, type: 'basic' },
  { label: '专业版', value: proCount.value, percent: proPercent.value, type: 'pro' }
])

// 状态分布数据（计算属性）
const statusData = computed(() => [
  { label: '正常', value: activeUsers.value, percent: activePercent.value, type: 'active', icon: CircleCheck },
  { label: '锁定/禁用', value: lockedCount.value, percent: lockedPercent.value, type: 'locked', icon: Lock },
  { label: '管理员', value: adminUsers.value, percent: adminPercent.value, type: 'admin', icon: UserFilled }
])

/**
 * 获取统计数据
 * 调用仪表盘API获取统计数据
 */
async function fetchStats() {
  loading.value = true
  try {
    const data = await adminAPI.getDashboard()
    dashboard.value = data || {}
  } catch (error) {
    console.error('获取统计数据失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 刷新统计数据
 * 重新调用fetchStats获取最新数据
 */
function refreshStats() {
  fetchStats()
}

// 组件挂载时获取统计数据
onMounted(() => {
  fetchStats()
})

// 页面从缓存恢复时重新加载数据（keep-alive）
onActivated(() => {
  fetchStats()
})
</script>

<style scoped>
/* 数据统计页面主容器 */
.admin-stats {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 页面头部布局 */
.admin-stats-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

/* 页面标题样式 */
.admin-stats-title h3 {
  font-size: 20px;
  font-weight: 600;
  color: white;
  margin: 0 0 4px 0;
}

/* 页面描述样式 */
.admin-stats-title p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

/* 汇总统计卡片网格布局 */
.admin-stats-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

/* 汇总统计卡片样式 */
.admin-summary-card {
  background: rgba(30, 41, 59, 0.8);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  gap: 14px;
  animation: slideUp 0.4s ease forwards;
  opacity: 0;
}

/* 主色调卡片图标样式 */
.admin-summary-card.primary .admin-summary-icon {
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
}

/* 成功色卡片图标样式 */
.admin-summary-card.success .admin-summary-icon {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

/* 警告色卡片图标样式 */
.admin-summary-card.warning .admin-summary-icon {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
}

/* 信息色卡片图标样式 */
.admin-summary-card.info .admin-summary-icon {
  background: rgba(168, 85, 247, 0.15);
  color: #a855f7;
}

/* 汇总卡片图标容器 */
.admin-summary-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

/* 汇总卡片内容区域 */
.admin-summary-content {
  display: flex;
  flex-direction: column;
}

/* 汇总数值样式 */
.admin-summary-value {
  font-size: 24px;
  font-weight: 700;
  color: white;
}

/* 汇总标签样式 */
.admin-summary-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

/* 统计图表网格布局 */
.admin-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
}

/* 统计图表面板样式 */
.admin-stat-panel {
  background: rgba(30, 41, 59, 0.8);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

/* 通栏面板（占满整行） */
.admin-stat-panel.wide {
  grid-column: 1 / -1;
}

/* 面板标题栏布局 */
.admin-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

/* 面板标题文字 */
.admin-panel-title {
  font-size: 16px;
  font-weight: 600;
  color: white;
  margin: 0;
}

/* 图表容器布局 */
.admin-chart-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 柱状图区域布局 */
.admin-chart-bars {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 单个柱状图项布局 */
.admin-chart-bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
  animation: slideUp 0.4s ease forwards;
  opacity: 0;
}

/* 柱状图标签 */
.admin-chart-bar-label {
  width: 60px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  flex-shrink: 0;
}

/* 柱状图轨道 */
.admin-chart-bar-track {
  flex: 1;
  height: 10px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 5px;
  overflow: hidden;
}

/* 柱状图填充条 */
.admin-chart-bar-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.8s ease;
}

/* 免费版柱状图颜色 */
.admin-chart-bar-fill.free {
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
}

/* 基础版柱状图颜色 */
.admin-chart-bar-fill.basic {
  background: linear-gradient(90deg, #f59e0b, #f97316);
}

/* 专业版柱状图颜色 */
.admin-chart-bar-fill.pro {
  background: linear-gradient(90deg, #22c55e, #10b981);
}

/* 柱状图数值 */
.admin-chart-bar-value {
  width: 40px;
  font-size: 14px;
  font-weight: 600;
  color: white;
  text-align: right;
  flex-shrink: 0;
}

/* 柱状图总数统计 */
.admin-chart-total {
  text-align: right;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

/* 饼图容器 */
.admin-pie-chart {
  position: relative;
  width: 160px;
  height: 160px;
  margin: 0 auto;
}

/* 饼图切片容器 */
.admin-pie-slices {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  position: relative;
  overflow: hidden;
}

/* 饼图单个切片 */
.admin-pie-slice {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

/* 普通用户切片颜色 */
.admin-pie-slice.user-slice {
  background: conic-gradient(
    #3b82f6 calc(var(--percent) * 1%),
    transparent calc(var(--percent) * 1%)
  );
}

/* 管理员切片颜色 */
.admin-pie-slice.admin-slice {
  background: conic-gradient(
    #a855f7 calc(var(--percent) * 1%),
    transparent calc(var(--percent) * 1%)
  );
}

/* 饼图中心区域 */
.admin-pie-center {
  position: absolute;
  top: 25%;
  left: 25%;
  width: 50%;
  height: 50%;
  background: #1e293b;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

/* 饼图中心数值 */
.admin-pie-value {
  font-size: 24px;
  font-weight: 700;
  color: white;
}

/* 饼图中心标签 */
.admin-pie-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

/* 饼图图例区域 */
.admin-pie-legend {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 16px;
}

/* 单个图例项 */
.admin-pie-legend-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

/* 图例圆点 */
.admin-pie-legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 8px;
}

/* 普通用户图例颜色 */
.admin-pie-legend-dot.user {
  background: #3b82f6;
}

/* 管理员图例颜色 */
.admin-pie-legend-dot.admin {
  background: #a855f7;
}

/* 图例数值样式 */
.admin-pie-legend-value {
  font-weight: 600;
  color: white;
}

/* 状态卡片网格布局 */
.admin-status-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

/* 状态卡片样式 */
.admin-status-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.2);
  animation: slideUp 0.4s ease forwards;
  opacity: 0;
}

/* 正常状态卡片左边框 */
.admin-status-card.active {
  border-left: 4px solid #22c55e;
}

/* 锁定状态卡片左边框 */
.admin-status-card.locked {
  border-left: 4px solid #ef4444;
}

/* 管理员状态卡片左边框 */
.admin-status-card.admin {
  border-left: 4px solid #a855f7;
}

/* 状态卡片图标容器 */
.admin-status-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 正常状态图标样式 */
.admin-status-card.active .admin-status-icon {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

/* 锁定状态图标样式 */
.admin-status-card.locked .admin-status-icon {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

/* 管理员状态图标样式 */
.admin-status-card.admin .admin-status-icon {
  background: rgba(168, 85, 247, 0.15);
  color: #a855f7;
}

/* 状态卡片信息区域 */
.admin-status-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 状态数值样式 */
.admin-status-value {
  font-size: 24px;
  font-weight: 700;
  color: white;
}

/* 状态标签样式 */
.admin-status-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

/* 状态百分比样式 */
.admin-status-percent {
  font-size: 18px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
}

/* 数据说明文字区域 */
.admin-stats-note {
  display: flex;
  flex-direction: column;
  gap: 10px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  line-height: 1.6;
}

/* 说明段落样式 */
.admin-stats-note p {
  margin: 0;
}

/* 说明加粗文字 */
.admin-stats-note strong {
  color: rgba(255, 255, 255, 0.9);
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

/* 移动端响应式适配 */
@media (max-width: 768px) {
  /* 图表网格改为单列 */
  .admin-stats-grid {
    grid-template-columns: 1fr;
  }

  /* 状态卡片改为单列 */
  .admin-status-cards {
    grid-template-columns: 1fr;
  }

  /* 汇总卡片改为两列 */
  .admin-stats-summary {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
