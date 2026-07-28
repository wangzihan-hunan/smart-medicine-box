<!-- 操作日志页面模板 -->
<template>
  <!-- 操作日志页面主容器 -->
  <div class="logs-page">
    <div class="page-container">
      <!-- 页面头部：标题和副标题 -->
      <div class="page-header">
        <h2 class="section-title">操作日志</h2>
        <p class="section-subtitle">查看您的账户操作记录</p>
      </div>
      
      <!-- 模块筛选栏 -->
      <div class="filter-bar">
        <!-- 模块筛选标签 -->
        <div class="filter-tabs">
          <button 
            class="filter-tab"
            :class="{ active: activeModule === 'all' }"
            @click="switchModule('all')"
          >
            全部
          </button>
          <button 
            v-for="mod in modules"
            :key="mod"
            class="filter-tab"
            :class="{ active: activeModule === mod }"
            @click="switchModule(mod)"
          >
            {{ mod }}
          </button>
        </div>
        <!-- 刷新按钮 -->
        <button class="btn-refresh" @click="fetchLogs">
          <el-icon><Refresh /></el-icon>
          刷新
        </button>
      </div>
      
      <!-- 日志列表区域 -->
      <!-- 加载中状态 -->
      <div v-if="loading" class="loading-state">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      
      <!-- 空状态：没有日志时显示 -->
      <div v-else-if="logs.length === 0" class="empty-state">
        <el-icon class="empty-icon"><Document /></el-icon>
        <div class="empty-text">暂无日志记录</div>
      </div>
      
      <!-- 日志列表 -->
      <div v-else class="logs-list">
        <!-- 单条日志项 -->
        <div 
          v-for="log in logs" 
          :key="log.id"
          class="log-item"
          :class="{ 'log-failed': !log.success }"
        >
          <!-- 状态图标：成功/失败 -->
          <div class="log-icon">
            <el-icon v-if="log.success" class="icon-success"><CircleCheck /></el-icon>
            <el-icon v-else class="icon-failed"><CircleClose /></el-icon>
          </div>
          <!-- 日志内容 -->
          <div class="log-content">
            <!-- 日志头部：模块、操作、时间 -->
            <div class="log-header">
              <span class="log-module">{{ log.module || '系统' }}</span>
              <span class="log-operation">{{ log.operation }}</span>
              <span class="log-time">{{ formatDateTime(log.createdAt) }}</span>
            </div>
            <!-- 日志描述 -->
            <div class="log-description">{{ log.description || '-' }}</div>
            <!-- 错误信息（失败时显示） -->
            <div v-if="!log.success && log.errorMessage" class="log-error">
              错误：{{ log.errorMessage }}
            </div>
            <!-- 日志元信息：IP、请求方法、耗时 -->
            <div class="log-meta">
              <span v-if="log.ipAddress" class="meta-tag">IP: {{ log.ipAddress }}</span>
              <span v-if="log.requestMethod" class="meta-tag">{{ log.requestMethod }}</span>
              <span v-if="log.durationMs" class="meta-tag">{{ log.durationMs }}ms</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 分页组件 -->
      <div v-if="total > 0" class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage + 1"
          :page-sizes="[10, 20, 50]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next"
          :total="total"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
// 引入Vue核心API：响应式引用、生命周期钩子
import { ref, onMounted, onActivated } from 'vue'
// 引入Element Plus消息提示组件
import { ElMessage } from 'element-plus'
// 引入Element Plus图标组件
import { Loading, Refresh, Document, CircleCheck, CircleClose } from '@element-plus/icons-vue'
// 引入日志相关的API接口
import { logAPI } from '../utils/api'

// 日志列表加载状态
const loading = ref(false)
// 日志列表数据
const logs = ref([])
// 日志总条数
const total = ref(0)
// 当前页码（从0开始）
const currentPage = ref(0)
// 每页条数
const pageSize = ref(20)
// 当前选中的模块筛选
const activeModule = ref('all')
// 可用模块列表（从日志数据中动态收集）
const modules = ref([])

// 获取日志列表数据
async function fetchLogs() {
  loading.value = true
  try {
    let response
    // 根据是否选择模块调用不同的API
    if (activeModule.value === 'all') {
      response = await logAPI.getAll(currentPage.value, pageSize.value)
    } else {
      response = await logAPI.getByModule(activeModule.value, currentPage.value, pageSize.value)
    }
    logs.value = response && response.list ? response.list : []
    total.value = response && response.totalElements ? response.totalElements : 0
    
    // 收集模块列表（仅首次加载全部时收集）
    if (activeModule.value === 'all' && logs.value.length > 0 && modules.value.length === 0) {
      const moduleSet = new Set()
      logs.value.forEach(log => {
        if (log.module) moduleSet.add(log.module)
      })
      modules.value = Array.from(moduleSet)
    }
  } catch (error) {
    console.error('获取日志失败:', error)
    ElMessage.error('获取日志失败')
  } finally {
    loading.value = false
  }
}

// 切换模块筛选
function switchModule(mod) {
  activeModule.value = mod
  currentPage.value = 0
  fetchLogs()
}

// 处理每页条数变化
function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 0
  fetchLogs()
}

// 处理页码变化
function handleCurrentChange(page) {
  currentPage.value = page - 1
  fetchLogs()
}

// 格式化日期时间
function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 组件挂载时获取日志列表
onMounted(() => {
  fetchLogs()
})

// 页面从缓存恢复时重新加载数据（keep-alive）
onActivated(() => {
  fetchLogs()
})
</script>

<style scoped>
/* 操作日志页面主容器样式 */
.logs-page {
  padding-top: 80px;
  min-height: 100vh;
}

/* 页面头部样式 */
.page-header {
  margin-bottom: 24px;
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

/* 筛选栏样式 */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

/* 筛选标签容器 */
.filter-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* 筛选标签按钮样式 */
.filter-tab {
  padding: 6px 16px;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background: white;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

/* 筛选标签悬停效果 */
.filter-tab:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

/* 激活的筛选标签样式 */
.filter-tab.active {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: white;
}

/* 刷新按钮样式 */
.btn-refresh {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-sm);
  background: white;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

/* 刷新按钮悬停效果 */
.btn-refresh:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

/* 加载中状态样式 */
.loading-state {
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
  font-size: 56px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

/* 空状态文字样式 */
.empty-text {
  font-size: 16px;
  color: var(--text-secondary);
}

/* 日志列表容器样式 */
.logs-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 单条日志项样式 */
.log-item {
  display: flex;
  gap: 16px;
  padding: 16px 20px;
  background: white;
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color);
  transition: all var(--transition-fast);
}

/* 日志项悬停效果 */
.log-item:hover {
  box-shadow: var(--card-shadow);
}

/* 失败日志的左侧红色边框标识 */
.log-item.log-failed {
  border-left: 3px solid var(--danger-color);
}

/* 日志图标样式 */
.log-icon {
  flex-shrink: 0;
  font-size: 24px;
}

/* 成功图标颜色 */
.icon-success {
  color: var(--success-color);
}

/* 失败图标颜色 */
.icon-failed {
  color: var(--danger-color);
}

/* 日志内容区域 */
.log-content {
  flex: 1;
  min-width: 0;
}

/* 日志头部样式 */
.log-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

/* 日志模块标签样式 */
.log-module {
  padding: 2px 10px;
  background: var(--primary-50);
  color: var(--primary-color);
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

/* 日志操作名称样式 */
.log-operation {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

/* 日志时间样式 */
.log-time {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: auto;
}

/* 日志描述样式 */
.log-description {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

/* 错误信息样式 */
.log-error {
  font-size: 13px;
  color: var(--danger-color);
  margin-bottom: 8px;
  padding: 6px 10px;
  background: rgba(239, 68, 68, 0.05);
  border-radius: var(--border-radius-sm);
}

/* 日志元信息区域 */
.log-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* 元信息标签样式 */
.meta-tag {
  font-size: 12px;
  color: var(--text-muted);
  padding: 2px 8px;
  background: var(--bg-secondary, #f5f5f5);
  border-radius: 4px;
}

/* 分页容器样式 */
.pagination-container {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

/* 手机响应式调整 */
@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .log-time {
    margin-left: 0;
    width: 100%;
  }
}
</style>
