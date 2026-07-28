<template>
  <!-- 告警管理页面主容器 -->
  <div class="admin-alerts">
    <!-- 页面头部（标题+统计卡片） -->
    <div class="admin-alerts-header">
      <!-- 标题和描述 -->
      <div class="admin-alerts-title">
        <h3>告警管理</h3>
        <p>查看和处理系统告警</p>
      </div>
      <!-- 告警统计卡片区域 -->
      <div class="admin-alerts-stats">
        <!-- 单个统计卡片 - 循环渲染（待处理/已解决/总数） -->
        <div
          v-for="(item, index) in statItems"
          :key="item.key"
          class="admin-stat-mini"
          :class="item.type"
          :style="{ animationDelay: `${index * 0.08}s` }"
        >
          <!-- 统计数值 -->
          <div class="admin-stat-mini-value">{{ item.value }}</div>
          <!-- 统计标签 -->
          <div class="admin-stat-mini-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <!-- 工具栏（筛选+操作按钮） -->
    <div class="admin-alerts-toolbar">
      <!-- 筛选条件区域 -->
      <div class="admin-alerts-filters">
        <!-- 状态筛选下拉框 -->
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable class="admin-filter-select">
          <el-option label="全部" value="" />
          <el-option label="待处理" value="PENDING" />
          <el-option label="已解决" value="RESOLVED" />
          <el-option label="已忽略" value="IGNORED" />
        </el-select>
        <!-- 查询按钮 -->
        <el-button type="primary" :loading="loading" @click="fetchAlerts">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <!-- 重置按钮 -->
        <el-button @click="resetFilters">重置</el-button>
      </div>
      <!-- 操作按钮区域 -->
      <div class="admin-alerts-actions">
        <!-- 触发测试告警按钮 -->
        <el-button type="warning" plain @click="triggerTestAlert">
          <el-icon><Bell /></el-icon>
          测试告警
        </el-button>
      </div>
    </div>

    <!-- 告警表格区域 -->
    <div class="admin-alerts-table">
      <!-- 告警列表表格 -->
      <el-table
        :data="alerts"
        stripe
        class="admin-table"
        :empty-text="loading ? '加载中...' : '暂无数据'"
        v-loading="loading"
      >
        <!-- 告警ID列 -->
        <el-table-column prop="id" label="ID" width="70" />
        <!-- 告警级别列 -->
        <el-table-column prop="level" label="级别" width="90">
          <template #default="scope">
            <el-tag :type="getLevelType(scope.row.level)" size="small" effect="dark">
              {{ getLevelLabel(scope.row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 告警类型列 -->
        <el-table-column prop="type" label="类型" width="120">
          <template #default="scope">
            {{ getTypeLabel(scope.row.type) }}
          </template>
        </el-table-column>
        <!-- 告警标题列 -->
        <el-table-column prop="title" label="标题" min-width="140" />
        <!-- 告警内容列（超出显示气泡提示） -->
        <el-table-column prop="content" label="内容" min-width="160" show-overflow-tooltip>
          <template #default="scope">
            <span class="admin-content-text">{{ scope.row.content }}</span>
          </template>
        </el-table-column>
        <!-- 告警状态列 -->
        <el-table-column prop="status" label="状态" width="90">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small" effect="light">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 创建时间列 -->
        <el-table-column prop="createdAt" label="时间" width="160">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <!-- 操作列（固定在右侧） -->
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <!-- 解决按钮（仅待处理状态显示） -->
            <el-button
              v-if="scope.row.status === 'PENDING'"
              text
              size="small"
              class="admin-table-btn"
              @click="handleAlert(scope.row, 'RESOLVED')"
            >
              解决
            </el-button>
            <!-- 忽略按钮（仅待处理状态显示） -->
            <el-button
              v-if="scope.row.status === 'PENDING'"
              text
              size="small"
              class="admin-table-btn"
              @click="handleAlert(scope.row, 'IGNORED')"
            >
              忽略
            </el-button>
            <!-- 查看详情按钮 -->
            <el-button
              text
              size="small"
              class="admin-table-btn"
              @click="showDetail(scope.row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <el-pagination
        v-if="totalElements > 0"
        :current-page="currentPage + 1"
        :page-size="pageSize"
        :total="totalElements"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
        class="admin-pagination"
      />
    </div>

    <!-- 告警详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="告警详情" width="640px" destroy-on-close>
      <div v-if="currentAlert" class="admin-detail-content">
        <!-- 告警级别 -->
        <div class="admin-detail-row">
          <span class="admin-detail-label">告警级别</span>
          <el-tag :type="getLevelType(currentAlert.level)" effect="dark">{{ getLevelLabel(currentAlert.level) }}</el-tag>
        </div>
        <!-- 告警类型 -->
        <div class="admin-detail-row">
          <span class="admin-detail-label">告警类型</span>
          <span>{{ getTypeLabel(currentAlert.type) }}</span>
        </div>
        <!-- 告警标题 -->
        <div class="admin-detail-row">
          <span class="admin-detail-label">告警标题</span>
          <span class="admin-detail-value">{{ currentAlert.title }}</span>
        </div>
        <!-- 告警内容 -->
        <div class="admin-detail-row">
          <span class="admin-detail-label">告警内容</span>
          <span class="admin-detail-content-text">{{ currentAlert.content }}</span>
        </div>
        <!-- 触发指标值 -->
        <div class="admin-detail-row">
          <span class="admin-detail-label">触发值</span>
          <span class="admin-detail-value">{{ currentAlert.metricValue || '-' }}</span>
        </div>
        <!-- 告警阈值 -->
        <div class="admin-detail-row">
          <span class="admin-detail-label">阈值</span>
          <span class="admin-detail-value">{{ currentAlert.thresholdValue || '-' }}</span>
        </div>
        <!-- 告警触发时间 -->
        <div class="admin-detail-row">
          <span class="admin-detail-label">告警时间</span>
          <span class="admin-detail-value">{{ formatDate(currentAlert.createdAt) }}</span>
        </div>
        <!-- 处理人（已处理时显示） -->
        <div v-if="currentAlert.handledBy" class="admin-detail-row">
          <span class="admin-detail-label">处理人</span>
          <span class="admin-detail-value">{{ currentAlert.handledBy }}</span>
        </div>
        <!-- 处理时间（已处理时显示） -->
        <div v-if="currentAlert.handledAt" class="admin-detail-row">
          <span class="admin-detail-label">处理时间</span>
          <span class="admin-detail-value">{{ formatDate(currentAlert.handledAt) }}</span>
        </div>
        <!-- 处理备注（已处理时显示） -->
        <div v-if="currentAlert.handleRemark" class="admin-detail-row">
          <span class="admin-detail-label">处理备注</span>
          <span class="admin-detail-content-text">{{ currentAlert.handleRemark }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
// 引入Vue响应式API、计算属性和生命周期钩子
import { ref, onMounted, onActivated, computed } from 'vue'
// 引入Element Plus消息和对话框组件
import { ElMessage, ElMessageBox } from 'element-plus'
// 引入Element Plus图标组件
import { Search, Bell } from '@element-plus/icons-vue'
// 引入管理员API
import { adminAPI } from '../utils/api'

// 列表加载状态
const loading = ref(false)
// 告警列表数据
const alerts = ref([])
// 告警统计数据
const stats = ref({})
// 当前页码（从0开始）
const currentPage = ref(0)
// 每页显示数量
const pageSize = ref(20)
// 告警总数
const totalElements = ref(0)
// 状态筛选值（默认筛选待处理）
const filterStatus = ref('PENDING')

// 详情弹窗显示状态
const showDetailDialog = ref(false)
// 当前查看的告警数据
const currentAlert = ref(null)

// 统计卡片数据（计算属性）
const statItems = computed(() => [
  { key: 'pending', value: stats.value.pending || 0, label: '待处理', type: 'pending' },
  { key: 'resolved', value: stats.value.resolved || 0, label: '已解决', type: 'resolved' },
  { key: 'total', value: stats.value.total || 0, label: '总数', type: 'total' }
])

/**
 * 获取告警列表数据
 * 根据状态筛选条件和分页参数调用API获取告警数据
 */
async function fetchAlerts() {
  loading.value = true
  try {
    const data = await adminAPI.getAlerts(filterStatus.value, currentPage.value, pageSize.value)
    alerts.value = data.list || []
    totalElements.value = data.totalElements || 0
  } catch (error) {
    console.error('获取告警列表失败:', error)
    ElMessage.error('获取告警列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 获取告警统计数据
 * 调用API获取各状态告警数量统计
 */
async function fetchStats() {
  try {
    const data = await adminAPI.getAlertStats()
    stats.value = data || {}
  } catch (error) {
    console.error('获取告警统计失败:', error)
  }
}

/**
 * 重置筛选条件
 * 清空状态筛选，重置到第一页并重新获取告警列表
 */
function resetFilters() {
  filterStatus.value = ''
  currentPage.value = 0
  fetchAlerts()
}

/**
 * 分页页码变化处理
 * @param {number} page - 目标页码（从1开始）
 */
function handlePageChange(page) {
  currentPage.value = page - 1
  fetchAlerts()
}

/**
 * 每页数量变化处理
 * @param {number} size - 每页显示数量
 */
function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 0
  fetchAlerts()
}

/**
 * 处理告警（解决或忽略）
 * 弹出输入框填写处理备注，确认后调用处理API
 * @param {object} alert - 告警数据对象
 * @param {string} status - 目标状态（RESOLVED/IGNORED）
 */
async function handleAlert(alert, status) {
  try {
    const { value: remark } = await ElMessageBox.prompt(
      `请输入处理备注（${status === 'RESOLVED' ? '解决' : '忽略'}告警）`,
      '处理告警',
      { confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    await adminAPI.handleAlert(alert.id, status, remark)
    ElMessage.success('处理成功')
    // 刷新列表和统计数据
    fetchAlerts()
    fetchStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('处理告警失败:', error)
      ElMessage.error('处理失败')
    }
  }
}

/**
 * 显示告警详情
 * @param {object} alert - 告警数据对象
 */
function showDetail(alert) {
  currentAlert.value = alert
  showDetailDialog.value = true
}

/**
 * 触发测试告警
 * 调用后端API触发一条测试告警，用于测试告警系统
 */
async function triggerTestAlert() {
  try {
    await adminAPI.triggerTestAlert('WARNING', '前端触发的测试告警')
    ElMessage.success('测试告警已触发')
    // 重置筛选条件以便看到新触发的告警
    filterStatus.value = ''
    currentPage.value = 0
    fetchAlerts()
    fetchStats()
  } catch (error) {
    console.error('触发测试告警失败:', error)
    ElMessage.error('触发测试告警失败')
  }
}

/**
 * 获取告警级别对应的标签类型
 * @param {string} level - 告警级别（CRITICAL/WARNING/INFO）
 * @returns {string} Element Plus标签类型
 */
function getLevelType(level) {
  const types = { 'CRITICAL': 'danger', 'WARNING': 'warning', 'INFO': 'info' }
  return types[level] || 'info'
}

/**
 * 获取告警级别对应的中文标签
 * @param {string} level - 告警级别
 * @returns {string} 级别中文描述
 */
function getLevelLabel(level) {
  const labels = { 'CRITICAL': '严重', 'WARNING': '警告', 'INFO': '提示' }
  return labels[level] || level
}

/**
 * 获取告警类型对应的中文标签
 * @param {string} type - 告警类型
 * @returns {string} 类型中文描述
 */
function getTypeLabel(type) {
  const labels = {
    'error_rate': '异常率',
    'response_time': '响应时间',
    'cpu_usage': 'CPU使用率',
    'memory_usage': '内存使用率',
    'manual': '手动触发',
    'service_down': '服务下线'
  }
  return labels[type] || type
}

/**
 * 获取告警状态对应的标签类型
 * @param {string} status - 状态值（PENDING/RESOLVED/IGNORED）
 * @returns {string} Element Plus标签类型
 */
function getStatusType(status) {
  const types = { 'PENDING': 'warning', 'RESOLVED': 'success', 'IGNORED': 'info' }
  return types[status] || 'info'
}

/**
 * 获取告警状态对应的中文标签
 * @param {string} status - 状态值
 * @returns {string} 状态中文描述
 */
function getStatusLabel(status) {
  const labels = { 'PENDING': '待处理', 'RESOLVED': '已解决', 'IGNORED': '已忽略' }
  return labels[status] || status
}

/**
 * 格式化日期时间
 * @param {string} dateStr - 日期字符串
 * @returns {string} 格式化后的本地时间字符串（含秒）
 */
function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 组件挂载时初始化数据
onMounted(() => {
  // 获取告警列表
  fetchAlerts()
  // 获取告警统计
  fetchStats()
})

// 页面从缓存恢复时重新加载数据（keep-alive）
onActivated(() => {
  fetchAlerts()
  fetchStats()
})
</script>

<style scoped>
/* 告警管理页面主容器 - 垂直排列 */
.admin-alerts {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 页面头部布局 - 左右分布 */
.admin-alerts-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
}

/* 页面标题样式 */
.admin-alerts-title h3 {
  font-size: 20px;
  font-weight: 600;
  color: white;
  margin: 0 0 4px 0;
}

/* 页面描述样式 */
.admin-alerts-title p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

/* 告警统计卡片区域布局 */
.admin-alerts-stats {
  display: flex;
  gap: 12px;
}

/* 迷你统计卡片样式 */
.admin-stat-mini {
  background: rgba(30, 41, 59, 0.8);
  border-radius: 8px;
  padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  min-width: 80px;
  text-align: center;
  animation: slideUp 0.4s ease forwards;
  opacity: 0;
}

/* 待处理统计卡片左边框颜色 */
.admin-stat-mini.pending {
  border-left: 3px solid #f59e0b;
}

/* 已解决统计卡片左边框颜色 */
.admin-stat-mini.resolved {
  border-left: 3px solid #22c55e;
}

/* 总数统计卡片左边框颜色 */
.admin-stat-mini.total {
  border-left: 3px solid #3b82f6;
}

/* 统计数值样式 */
.admin-stat-mini-value {
  font-size: 24px;
  font-weight: 700;
  color: white;
}

/* 统计标签样式 */
.admin-stat-mini-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 2px;
}

/* 工具栏布局 - 左右分布 */
.admin-alerts-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

/* 筛选条件区域布局 */
.admin-alerts-filters {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

/* 筛选下拉框宽度 */
.admin-filter-select {
  width: 160px;
}

/* 操作按钮区域布局 */
.admin-alerts-actions {
  display: flex;
  gap: 12px;
}

/* 告警表格容器样式 */
.admin-alerts-table {
  background: rgba(30, 41, 59, 0.8);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

/* 表格主体背景透明 */
.admin-table :deep(.el-table) {
  background: transparent;
}

/* 表格表头样式 */
.admin-table :deep(.el-table th) {
  background: rgba(0, 0, 0, 0.2);
  color: rgba(255, 255, 255, 0.8);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

/* 表格单元格样式 */
.admin-table :deep(.el-table td) {
  color: rgba(255, 255, 255, 0.8);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

/* 行悬停效果 */
.admin-table :deep(.el-table tr:hover > td) {
  background: rgba(59, 130, 246, 0.1);
}

/* 内容文字样式 */
.admin-content-text {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

/* 表格操作按钮样式 */
.admin-table-btn {
  color: rgba(255, 255, 255, 0.7);
  padding: 4px 8px;
}

/* 表格操作按钮悬停效果 */
.admin-table-btn:hover {
  color: #3b82f6;
}

/* 分页组件样式 */
.admin-pagination {
  padding: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 分页按钮样式 */
.admin-pagination :deep(.el-pagination button),
.admin-pagination :deep(.el-pagination .el-pager li) {
  color: rgba(255, 255, 255, 0.7);
}

/* 当前页码激活样式 */
.admin-pagination :deep(.el-pagination .el-pager li.active) {
  background: #3b82f6;
  color: white;
}

/* 详情弹窗内容布局 */
.admin-detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 详情行布局 */
.admin-detail-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

/* 最后一行去掉底边 */
.admin-detail-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

/* 详情标签样式 */
.admin-detail-label {
  width: 100px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
  flex-shrink: 0;
}

/* 详情数值样式 */
.admin-detail-value {
  color: white;
  font-size: 14px;
  flex: 1;
}

/* 详情内容文字样式（长文本） */
.admin-detail-content-text {
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  line-height: 1.6;
  flex: 1;
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
  /* 头部改为垂直布局 */
  .admin-alerts-header {
    flex-direction: column;
  }

  /* 统计卡片占满宽度 */
  .admin-alerts-stats {
    width: 100%;
  }

  /* 工具栏改为垂直布局 */
  .admin-alerts-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  /* 筛选条件占满宽度 */
  .admin-alerts-filters {
    width: 100%;
  }

  /* 筛选下拉框占满宽度 */
  .admin-filter-select {
    width: 100%;
  }

  /* 分页居中显示 */
  .admin-pagination {
    justify-content: center;
  }
}
</style>
