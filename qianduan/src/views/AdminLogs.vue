<template>
  <!-- 操作日志页面主容器 -->
  <div class="admin-logs">
    <!-- 页面头部（标题+刷新按钮） -->
    <div class="admin-logs-header">
      <!-- 标题和描述 -->
      <div class="admin-logs-title">
        <h3>操作日志</h3>
        <p>查看用户和系统操作日志</p>
      </div>
      <!-- 刷新按钮 -->
      <el-button text size="small" @click="fetchLogs">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 筛选工具栏 -->
    <div class="admin-logs-toolbar">
      <!-- 筛选条件区域 -->
      <div class="admin-logs-filters">
        <!-- 用户ID输入框 -->
        <el-input
          v-model="userIdInput"
          placeholder="用户ID"
          class="admin-filter-input"
          clearable
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
        <!-- 模块筛选下拉框 -->
        <el-select v-model="filterModule" placeholder="模块筛选" clearable class="admin-filter-select">
          <el-option
            v-for="m in modules"
            :key="m"
            :label="m"
            :value="m"
          />
        </el-select>
        <!-- 查询按钮 -->
        <el-button type="primary" :loading="loading" @click="handleSearch">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <!-- 重置按钮 -->
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </div>

    <!-- 日志表格区域 -->
    <div class="admin-logs-table">
      <!-- 日志列表表格 -->
      <el-table
        :data="logs"
        stripe
        class="admin-table"
        :empty-text="loading ? '加载中...' : '暂无日志'"
        v-loading="loading"
      >
        <!-- 日志ID列 -->
        <el-table-column prop="id" label="ID" width="60" />

        <!-- 用户信息列 -->
        <el-table-column label="用户" width="150">
          <template #default="scope">
            <div class="admin-log-user">
              <span class="admin-log-name">{{ scope.row.userName || '-' }}</span>
              <span class="admin-log-phone">{{ scope.row.userPhone || '-' }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 模块列 -->
        <el-table-column prop="module" label="模块" width="100">
          <template #default="scope">
            <el-tag size="small" type="info">{{ scope.row.module || '-' }}</el-tag>
          </template>
        </el-table-column>

        <!-- 操作列 -->
        <el-table-column prop="operation" label="操作" width="120" />

        <!-- 描述列 -->
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <span class="admin-content-text">{{ scope.row.description || '-' }}</span>
          </template>
        </el-table-column>

        <!-- 请求信息列（方法+URL） -->
        <el-table-column label="请求" width="180">
          <template #default="scope">
            <div v-if="scope.row.requestMethod" class="admin-log-request">
              <!-- 请求方法标签 -->
              <el-tag size="small" :type="getMethodType(scope.row.requestMethod)">
                {{ scope.row.requestMethod }}
              </el-tag>
              <!-- 请求URL -->
              <span class="admin-log-url">{{ scope.row.requestUrl }}</span>
            </div>
            <span v-else class="admin-text-muted">-</span>
          </template>
        </el-table-column>

        <!-- IP地址列 -->
        <el-table-column prop="ipAddress" label="IP" width="130">
          <template #default="scope">
            {{ scope.row.ipAddress || '-' }}
          </template>
        </el-table-column>

        <!-- 操作状态列（成功/失败） -->
        <el-table-column label="状态" width="70">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.success ? 'success' : 'danger'">
              {{ scope.row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 耗时列 -->
        <el-table-column prop="durationMs" label="耗时" width="80">
          <template #default="scope">
            <span v-if="scope.row.durationMs" :class="{ 'admin-slow': scope.row.durationMs > 1000 }">
              {{ scope.row.durationMs }}ms
            </span>
            <span v-else class="admin-text-muted">-</span>
          </template>
        </el-table-column>

        <!-- 创建时间列 -->
        <el-table-column prop="createdAt" label="时间" width="160">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
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
  </div>
</template>

<script setup>
// 引入Vue响应式API和生命周期钩子
import { ref, onMounted, onActivated } from 'vue'
// 引入Element Plus消息组件
import { ElMessage } from 'element-plus'
// 引入Element Plus图标组件
import { Search, Refresh, User } from '@element-plus/icons-vue'
// 引入管理员API
import { adminAPI } from '../utils/api'

// 列表加载状态
const loading = ref(false)
// 日志列表数据
const logs = ref([])
// 模块列表（用于筛选下拉框）
const modules = ref([])
// 日志总数
const totalElements = ref(0)
// 当前页码（从0开始）
const currentPage = ref(0)
// 每页显示数量
const pageSize = ref(20)
// 用户ID输入值
const userIdInput = ref('')
// 模块筛选值
const filterModule = ref('')

/**
 * 获取日志列表数据
 * 根据用户ID、模块筛选条件和分页参数调用API获取日志
 */
async function fetchLogs() {
  loading.value = true
  try {
    const userId = userIdInput.value ? parseInt(userIdInput.value) : null
    const data = await adminAPI.getLogs(userId, filterModule.value || null, currentPage.value, pageSize.value)
    logs.value = data.list || []
    totalElements.value = data.totalElements || 0
  } catch (error) {
    console.error('获取日志失败:', error)
    ElMessage.error('获取日志失败')
  } finally {
    loading.value = false
  }
}

/**
 * 获取模块列表
 * 调用API获取所有日志模块，用于下拉筛选
 */
async function fetchModules() {
  try {
    const data = await adminAPI.getLogModules()
    modules.value = data || []
  } catch (error) {
    console.error('获取模块列表失败:', error)
  }
}

/**
 * 执行查询操作
 * 重置到第一页并重新获取日志列表
 */
function handleSearch() {
  currentPage.value = 0
  fetchLogs()
}

/**
 * 重置筛选条件
 * 清空所有筛选条件并重新获取日志列表
 */
function resetFilters() {
  userIdInput.value = ''
  filterModule.value = ''
  currentPage.value = 0
  fetchLogs()
}

/**
 * 分页页码变化处理
 * @param {number} page - 目标页码（从1开始）
 */
function handlePageChange(page) {
  currentPage.value = page - 1
  fetchLogs()
}

/**
 * 每页数量变化处理
 * @param {number} size - 每页显示数量
 */
function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 0
  fetchLogs()
}

/**
 * 获取HTTP请求方法对应的标签类型
 * @param {string} method - HTTP请求方法（GET/POST/PUT/DELETE/PATCH）
 * @returns {string} Element Plus标签类型
 */
function getMethodType(method) {
  const types = { 'GET': 'success', 'POST': 'primary', 'PUT': 'warning', 'DELETE': 'danger', 'PATCH': 'info' }
  return types[method] || 'info'
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
  // 获取日志列表
  fetchLogs()
  // 获取模块列表
  fetchModules()
})

// 页面从缓存恢复时重新加载数据（keep-alive）
onActivated(() => {
  fetchLogs()
  fetchModules()
})
</script>

<style scoped>
/* 操作日志页面主容器 */
.admin-logs {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 页面头部布局 */
.admin-logs-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

/* 页面标题样式 */
.admin-logs-title h3 {
  font-size: 20px;
  font-weight: 600;
  color: white;
  margin: 0 0 4px 0;
}

/* 页面描述样式 */
.admin-logs-title p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

/* 工具栏布局 */
.admin-logs-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

/* 筛选条件区域布局 */
.admin-logs-filters {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

/* 筛选输入框宽度 */
.admin-filter-input {
  width: 120px;
}

/* 筛选下拉框宽度 */
.admin-filter-select {
  width: 160px;
}

/* 日志表格容器样式 */
.admin-logs-table {
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

/* 日志用户信息布局 */
.admin-log-user {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* 日志用户名样式 */
.admin-log-name {
  font-size: 13px;
  color: white;
  font-weight: 500;
}

/* 日志用户手机号样式 */
.admin-log-phone {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

/* 日志内容文字样式 */
.admin-content-text {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

/* 请求信息布局 */
.admin-log-request {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 请求URL样式 */
.admin-log-url {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 弱化文字样式 */
.admin-text-muted {
  color: rgba(255, 255, 255, 0.3);
}

/* 慢请求高亮样式（超过1秒） */
.admin-slow {
  color: #ef4444;
  font-weight: 600;
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

/* 移动端响应式适配 */
@media (max-width: 768px) {
  /* 工具栏改为垂直布局 */
  .admin-logs-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  /* 筛选条件占满宽度 */
  .admin-logs-filters {
    width: 100%;
  }

  /* 筛选输入框占满宽度 */
  .admin-filter-input,
  .admin-filter-select {
    width: 100%;
  }

  /* 分页居中显示 */
  .admin-pagination {
    justify-content: center;
  }
}
</style>
