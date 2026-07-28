<template>
  <!-- 用户管理页面主容器 -->
  <div class="admin-users">
    <!-- 页面头部（标题+用户总数） -->
    <div class="admin-users-header">
      <!-- 标题和描述 -->
      <div class="admin-users-title">
        <h3>用户管理</h3>
        <p>查看和管理所有用户账号</p>
      </div>
      <!-- 用户总数统计 -->
      <div class="admin-users-count">
        共 <strong>{{ totalElements }}</strong> 位用户
      </div>
    </div>

    <!-- 工具栏（搜索+操作按钮） -->
    <div class="admin-users-toolbar">
      <!-- 搜索输入框 -->
      <el-input
        v-model="searchKeyword"
        placeholder="搜索手机号或姓名"
        class="admin-search-input"
        clearable
        @clear="handleSearch"
        @input="handleSearchInput"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <!-- 搜索按钮 -->
      <el-button type="primary" :loading="loading" @click="handleSearch">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
      <!-- 刷新按钮 -->
      <el-button @click="refreshData">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
      <!-- 新增用户按钮 -->
      <el-button type="success" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新增用户
      </el-button>
    </div>

    <!-- 批量操作工具栏（选中用户时显示） -->
    <div v-if="selectedIds.length > 0" class="admin-batch-toolbar">
      <!-- 已选用户数量提示 -->
      <span class="admin-batch-info">已选择 <strong>{{ selectedIds.length }}</strong> 个用户</span>
      <!-- 批量重置密码 -->
      <el-button @click="batchResetPassword">
        <el-icon><Refresh /></el-icon>
        重置密码
      </el-button>
      <!-- 批量冻结 -->
      <el-button type="warning" @click="batchFreeze">
        <el-icon><Lock /></el-icon>
        冻结
      </el-button>
      <!-- 批量解封 -->
      <el-button type="success" @click="batchUnblock">
        <el-icon><CircleCheck /></el-icon>
        解封
      </el-button>
      <!-- 批量删除 -->
      <el-button type="danger" @click="batchDelete">
        <el-icon><Delete /></el-icon>
        删除
      </el-button>
      <!-- 取消选择 -->
      <el-button @click="clearSelection">取消选择</el-button>
    </div>

    <!-- 用户表格区域 -->
    <div class="admin-users-table-wrapper">
      <!-- 用户列表表格 -->
      <el-table
        :data="users"
        stripe
        class="admin-table admin-users-table"
        :empty-text="loading ? '加载中...' : '暂无数据'"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <!-- 多选列 -->
        <el-table-column type="selection" width="50" />
        <!-- 用户ID列 -->
        <el-table-column prop="id" label="ID" width="60" />

        <!-- 用户信息列（头像+姓名+手机号） -->
        <el-table-column label="用户" min-width="150">
          <template #default="scope">
            <div class="admin-user-cell">
              <!-- 用户头像（首字母） -->
              <div class="admin-user-avatar">{{ getInitials(scope.row.name) }}</div>
              <!-- 用户信息 -->
              <div class="admin-user-info">
                <div class="admin-user-name">{{ scope.row.name || '未设置' }}</div>
                <div class="admin-user-phone">{{ scope.row.phone }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 角色列 -->
        <el-table-column prop="role" label="角色" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.role === 'ADMIN' ? 'danger' : ''" size="small">
              {{ scope.row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 账号状态列 -->
        <el-table-column prop="status" label="状态" width="90">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 套餐列 -->
        <el-table-column prop="plan" label="套餐" width="100">
          <template #default="scope">
            <el-tag :type="getPlanType(scope.row.plan)" size="small">
              {{ getPlanLabel(scope.row.plan) }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- AI使用次数列 -->
        <el-table-column label="AI 使用" width="120">
          <template #default="scope">
            <el-tooltip :content="`本月已使用 ${scope.row.monthAiCount || 0} 次`" placement="top">
              <span class="admin-ai-usage">
                {{ scope.row.monthAiCount || 0 }} / {{ scope.row.monthAiLimit || 5 }}
              </span>
            </el-tooltip>
          </template>
        </el-table-column>

        <!-- 最后登录时间列 -->
        <el-table-column prop="lastLoginTime" label="最后登录" width="150">
          <template #default="scope">
            {{ formatDate(scope.row.lastLoginTime) || '从未登录' }}
          </template>
        </el-table-column>

        <!-- 注册时间列 -->
        <el-table-column prop="createdAt" label="注册时间" width="150">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>

        <!-- 操作列（固定在右侧） -->
        <el-table-column label="操作" width="380" fixed="right">
          <template #default="scope">
            <!-- 查看详情按钮 -->
            <el-button
              text
              size="small"
              class="admin-table-btn"
              @click="showDetail(scope.row)"
            >
              详情
            </el-button>
            <!-- 编辑按钮 -->
            <el-button
              text
              size="small"
              class="admin-table-btn"
              @click="openEdit(scope.row)"
            >
              编辑
            </el-button>
            <!-- 封禁按钮（非封禁状态显示） -->
            <el-button
              v-if="scope.row.status !== 'BLOCKED'"
              text
              size="small"
              class="admin-table-btn admin-btn-block"
              @click="confirmBlock(scope.row)"
            >
              封禁
            </el-button>
            <!-- 冻结按钮（非冻结状态显示） -->
            <el-button
              v-if="scope.row.status !== 'SUSPENDED'"
              text
              size="small"
              class="admin-table-btn admin-btn-freeze"
              @click="confirmFreeze(scope.row)"
            >
              冻结
            </el-button>
            <!-- 解封按钮（非正常状态显示） -->
            <el-button
              v-if="scope.row.status !== 'ACTIVE'"
              text
              size="small"
              class="admin-table-btn admin-btn-unblock"
              @click="confirmUnblock(scope.row)"
            >
              解封
            </el-button>
            <!-- 删除按钮 -->
            <el-button
              text
              size="small"
              class="admin-table-btn admin-btn-danger"
              @click="confirmDelete(scope.row)"
            >
              删除
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

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="用户详情" width="560px" destroy-on-close>
      <div v-if="currentUser" class="admin-detail-content">
        <!-- 基本信息区块 -->
        <div class="admin-detail-section">
          <h4>基本信息</h4>
          <!-- 用户ID -->
          <div class="admin-detail-row">
            <span class="admin-detail-label">用户ID</span>
            <span>{{ currentUser.id }}</span>
          </div>
          <!-- 姓名 -->
          <div class="admin-detail-row">
            <span class="admin-detail-label">姓名</span>
            <span>{{ currentUser.name || '未设置' }}</span>
          </div>
          <!-- 手机号 -->
          <div class="admin-detail-row">
            <span class="admin-detail-label">手机号</span>
            <span>{{ currentUser.phone }}</span>
          </div>
          <!-- 角色 -->
          <div class="admin-detail-row">
            <span class="admin-detail-label">角色</span>
            <el-tag :type="currentUser.role === 'ADMIN' ? 'danger' : ''" size="small">
              {{ currentUser.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </div>
          <!-- 账号状态 -->
          <div class="admin-detail-row">
            <span class="admin-detail-label">状态</span>
            <el-tag :type="getStatusType(currentUser.status)" size="small">
              {{ getStatusLabel(currentUser.status) }}
            </el-tag>
          </div>
          <!-- 手机验证状态 -->
          <div class="admin-detail-row">
            <span class="admin-detail-label">手机验证</span>
            <el-tag :type="currentUser.phoneVerified ? 'success' : 'info'" size="small">
              {{ currentUser.phoneVerified ? '已验证' : '未验证' }}
            </el-tag>
          </div>
        </div>

        <!-- 套餐信息区块 -->
        <div class="admin-detail-section">
          <h4>套餐信息</h4>
          <!-- 当前套餐 -->
          <div class="admin-detail-row">
            <span class="admin-detail-label">当前套餐</span>
            <el-tag :type="getPlanType(currentUser.plan)" size="small">
              {{ getPlanLabel(currentUser.plan) }}
            </el-tag>
          </div>
          <!-- 套餐到期时间 -->
          <div class="admin-detail-row">
            <span class="admin-detail-label">套餐到期</span>
            <span>{{ formatDate(currentUser.planExpireAt) || '无期限' }}</span>
          </div>
          <!-- 本月AI使用次数 -->
          <div class="admin-detail-row">
            <span class="admin-detail-label">本月AI使用</span>
            <span>{{ currentUser.monthAiCount || 0 }} / {{ currentUser.monthAiLimit || 5 }} 次</span>
          </div>
        </div>

        <!-- 安全信息区块 -->
        <div class="admin-detail-section">
          <h4>安全信息</h4>
          <!-- 登录失败次数 -->
          <div class="admin-detail-row">
            <span class="admin-detail-label">登录失败次数</span>
            <span :class="{ 'admin-text-danger': (currentUser.loginAttempts || 0) > 3 }">
              {{ currentUser.loginAttempts || 0 }}
            </span>
          </div>
          <!-- 账号锁定时间 -->
          <div class="admin-detail-row">
            <span class="admin-detail-label">账号锁定时间</span>
            <span>{{ formatDate(currentUser.lockTime) || '未锁定' }}</span>
          </div>
          <!-- 最后登录IP -->
          <div class="admin-detail-row">
            <span class="admin-detail-label">最后登录IP</span>
            <span>{{ currentUser.lastIp || '-' }}</span>
          </div>
          <!-- 最后登录时间 -->
          <div class="admin-detail-row">
            <span class="admin-detail-label">最后登录时间</span>
            <span>{{ formatDate(currentUser.lastLoginTime) || '从未登录' }}</span>
          </div>
        </div>

        <!-- 其他信息区块 -->
        <div class="admin-detail-section">
          <h4>其他</h4>
          <!-- 注册时间 -->
          <div class="admin-detail-row">
            <span class="admin-detail-label">注册时间</span>
            <span>{{ formatDate(currentUser.createdAt) }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 编辑用户弹窗 -->
    <el-dialog v-model="showEditDialog" title="编辑用户" width="480px" destroy-on-close>
      <!-- 编辑表单 -->
      <el-form :model="editForm" label-width="80px" class="admin-form">
        <!-- 姓名输入 -->
        <el-form-item label="姓名">
          <el-input v-model="editForm.name" placeholder="用户姓名" />
        </el-form-item>

        <!-- 角色选择 -->
        <el-form-item label="角色">
          <el-select v-model="editForm.role" placeholder="选择角色" class="admin-form-select">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>

        <!-- 状态选择 -->
        <el-form-item label="状态">
          <el-select v-model="editForm.status" placeholder="选择状态" class="admin-form-select">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="禁用" value="SUSPENDED" />
            <el-option label="锁定" value="LOCKED" />
          </el-select>
        </el-form-item>

        <!-- 套餐选择 -->
        <el-form-item label="套餐">
          <el-select v-model="editForm.plan" placeholder="选择套餐" class="admin-form-select">
            <el-option label="免费版" value="FREE" />
            <el-option label="基础版" value="BASIC" />
            <el-option label="专业版" value="PROFESSIONAL" />
          </el-select>
        </el-form-item>

        <!-- 套餐时长 -->
        <el-form-item label="套餐时长">
          <el-input-number v-model="editForm.planMonths" :min="1" :max="12" />
          <span class="admin-form-unit">个月</span>
        </el-form-item>

        <!-- AI限额 -->
        <el-form-item label="AI限额">
          <el-input-number v-model="editForm.monthAiLimit" :min="0" :max="9999" />
          <span class="admin-form-unit">次/月</span>
        </el-form-item>
      </el-form>

      <!-- 弹窗底部按钮 -->
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveEdit" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="showResetPasswordDialog" title="重置密码" width="400px" destroy-on-close>
      <!-- 重置密码表单 -->
      <el-form :model="resetPasswordForm" label-width="100px" class="admin-form">
        <el-form-item label="新密码">
          <el-input
            v-model="resetPasswordForm.password"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <!-- 弹窗底部按钮 -->
      <template #footer>
        <el-button @click="showResetPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="saveResetPassword" :loading="resetting">确认重置</el-button>
      </template>
    </el-dialog>

    <!-- 新增用户弹窗 -->
    <el-dialog v-model="showCreateDialog" title="新增用户" width="500px" destroy-on-close>
      <!-- 新增用户表单 -->
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px" class="admin-form">
        <!-- 手机号输入 -->
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="createForm.phone" placeholder="请输入11位手机号" maxlength="11" />
        </el-form-item>

        <!-- 姓名输入 -->
        <el-form-item label="姓名" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入用户姓名" maxlength="20" />
        </el-form-item>

        <!-- 密码输入 -->
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="createForm.password"
            type="password"
            placeholder="请输入密码（至少6位）"
            show-password
            maxlength="20"
          />
        </el-form-item>

        <!-- 角色选择 -->
        <el-form-item label="角色">
          <el-select v-model="createForm.role">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>

        <!-- 套餐选择 -->
        <el-form-item label="套餐">
          <el-select v-model="createForm.plan">
            <el-option label="免费版" value="FREE" />
            <el-option label="基础版" value="BASIC" />
            <el-option label="专业版" value="PROFESSIONAL" />
          </el-select>
        </el-form-item>

        <!-- AI限额 -->
        <el-form-item label="AI限额">
          <el-input-number v-model="createForm.monthAiLimit" :min="0" :max="9999" />
          <span class="admin-form-unit">次/月</span>
        </el-form-item>
      </el-form>

      <!-- 弹窗底部按钮 -->
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 引入Vue响应式API和生命周期钩子
import { ref, onMounted, onActivated } from 'vue'
// 引入Element Plus消息和对话框组件
import { ElMessage, ElMessageBox } from 'element-plus'
// 引入Element Plus图标组件
import { Search, Refresh, Plus, Lock, CircleCheck, Delete } from '@element-plus/icons-vue'
// 引入管理员API
import { adminAPI } from '../utils/api'

// 列表加载状态
const loading = ref(false)
// 编辑保存状态
const saving = ref(false)
// 重置密码状态
const resetting = ref(false)
// 创建用户状态
const creating = ref(false)
// 用户列表数据
const users = ref([])
// 用户总数
const totalElements = ref(0)
// 当前页码（从0开始）
const currentPage = ref(0)
// 每页显示数量
const pageSize = ref(20)
// 搜索关键词
const searchKeyword = ref('')
// 选中的用户ID列表
const selectedIds = ref([])
// 搜索防抖定时器
let searchTimer = null

// 用户详情弹窗显示状态
const showDetailDialog = ref(false)
// 当前查看的用户数据
const currentUser = ref(null)

// 编辑弹窗显示状态
const showEditDialog = ref(false)
// 编辑表单数据
const editForm = ref({
  id: null,
  name: '',
  role: 'USER',
  status: 'ACTIVE',
  plan: 'FREE',
  planMonths: 1,
  monthAiLimit: 5
})

// 重置密码弹窗显示状态
const showResetPasswordDialog = ref(false)
// 重置密码表单数据
const resetPasswordForm = ref({
  id: null,
  password: '123456'
})

// 新增用户弹窗显示状态
const showCreateDialog = ref(false)
// 新增用户表单引用
const createFormRef = ref(null)
// 新增用户表单数据
const createForm = ref({
  phone: '',
  name: '',
  password: '',
  role: 'USER',
  plan: 'FREE',
  monthAiLimit: 10
})
// 新增用户表单验证规则
const createRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

/**
 * 获取用户列表数据
 * 根据搜索关键词、分页参数调用API获取用户数据
 */
async function fetchUsers() {
  loading.value = true
  try {
    const data = await adminAPI.getUsers(searchKeyword.value || null, currentPage.value, pageSize.value)
    users.value = data.list || []
    totalElements.value = data.totalElements || 0
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 搜索输入防抖处理
 * 输入停止500ms后自动触发搜索
 */
function handleSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    handleSearch()
  }, 500)
}

/**
 * 执行搜索操作
 * 重置到第一页并重新获取用户列表
 */
function handleSearch() {
  currentPage.value = 0
  fetchUsers()
}

/**
 * 刷新数据
 * 清空搜索条件并重新获取用户列表
 */
function refreshData() {
  searchKeyword.value = ''
  currentPage.value = 0
  fetchUsers()
}

/**
 * 分页页码变化处理
 * @param {number} page - 目标页码（从1开始）
 */
function handlePageChange(page) {
  currentPage.value = page - 1
  fetchUsers()
}

/**
 * 每页数量变化处理
 * @param {number} size - 每页显示数量
 */
function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 0
  fetchUsers()
}

/**
 * 显示用户详情
 * @param {object} user - 用户数据对象
 */
function showDetail(user) {
  currentUser.value = user
  showDetailDialog.value = true
}

/**
 * 打开编辑用户弹窗
 * @param {object} user - 用户数据对象
 */
function openEdit(user) {
  editForm.value = {
    id: user.id,
    name: user.name || '',
    role: user.role || 'USER',
    status: user.status || 'ACTIVE',
    plan: user.plan || 'FREE',
    planMonths: 1,
    monthAiLimit: user.monthAiLimit || 5
  }
  showEditDialog.value = true
}

/**
 * 保存编辑的用户信息
 * 并行调用多个API更新用户的各个属性
 */
async function saveEdit() {
  saving.value = true
  try {
    const userId = editForm.value.id
    const requests = []

    // 更新用户名（如果有变化）
    if (editForm.value.name !== undefined) {
      requests.push(adminAPI.updateUserName(userId, editForm.value.name))
    }

    // 更新用户角色
    requests.push(adminAPI.updateUserRole(userId, editForm.value.role))
    // 更新用户状态
    requests.push(adminAPI.updateUserStatus(userId, editForm.value.status))
    // 更新用户套餐
    requests.push(adminAPI.updateUserPlan(userId, editForm.value.plan, editForm.value.planMonths))
    // 更新AI限额
    requests.push(adminAPI.updateUserAiLimit(userId, editForm.value.monthAiLimit))

    // 并行执行所有更新请求
    await Promise.all(requests)

    ElMessage.success('保存成功')
    showEditDialog.value = false
    fetchUsers()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

/**
 * 打开重置密码弹窗
 * @param {object} user - 用户数据对象
 */
function openResetPassword(user) {
  resetPasswordForm.value = {
    id: user.id,
    password: '123456'
  }
  showResetPasswordDialog.value = true
}

/**
 * 保存重置密码
 * 验证密码长度后调用API重置密码
 */
async function saveResetPassword() {
  if (!resetPasswordForm.value.password || resetPasswordForm.value.password.length < 6) {
    ElMessage.warning('密码长度不能少于6位')
    return
  }

  resetting.value = true
  try {
    await adminAPI.resetUserPassword(resetPasswordForm.value.id, resetPasswordForm.value.password)
    ElMessage.success('密码重置成功')
    showResetPasswordDialog.value = false
  } catch (error) {
    console.error('密码重置失败:', error)
    ElMessage.error('密码重置失败')
  } finally {
    resetting.value = false
  }
}

/**
 * 打开新增用户弹窗
 * 重置表单为默认值
 */
function openCreateDialog() {
  createForm.value = {
    phone: '',
    name: '',
    password: '',
    role: 'USER',
    plan: 'FREE',
    monthAiLimit: 10
  }
  showCreateDialog.value = true
}

/**
 * 保存新增用户
 * 先验证表单，再调用创建用户API
 */
async function saveCreate() {
  if (!createFormRef.value) return

  // 表单验证
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }

  creating.value = true
  try {
    await adminAPI.createUser(createForm.value)
    ElMessage.success('用户创建成功')
    showCreateDialog.value = false
    fetchUsers()
  } catch (error) {
    console.error('创建用户失败:', error)
    ElMessage.error(error.response?.data?.message || '创建用户失败')
  } finally {
    creating.value = false
  }
}

/**
 * 弹出管理员密码确认弹窗（高危操作二次验证）
 * @param {string} actionText - 操作描述（如"删除"、"封禁"）
 * @param {string} detail - 操作详情（如用户名）
 * @returns {Promise<string|null>} 管理员密码或 null（取消）
 */
async function promptAdminPassword(actionText, detail) {
  try {
    const { value } = await ElMessageBox.prompt(
      `即将${actionText} "${detail}"，此操作为高危操作，请输入管理员密码以确认：`,
      '安全验证',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        inputType: 'password',
        inputPlaceholder: '请输入管理员密码',
        inputValidator: (val) => {
          if (!val || val.trim() === '') return '密码不能为空'
          return true
        }
      }
    )
    return value
  } catch {
    return null
  }
}

/**
 * 确认删除用户
 * 弹出密码确认对话框，验证后调用删除API
 * @param {object} user - 用户数据对象
 */
async function confirmDelete(user) {
  const adminPassword = await promptAdminPassword('删除用户', user.name || user.phone)
  if (!adminPassword) return

  try {
    await adminAPI.deleteUser(user.id, adminPassword)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch (error) {
    const msg = error.response?.data?.message || '删除失败'
    ElMessage.error(msg)
  }
}

/**
 * 确认封禁用户
 * 弹出输入框填写封禁原因，再验证管理员密码后调用封禁API
 * @param {object} user - 用户数据对象
 */
async function confirmBlock(user) {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      '请输入封禁原因',
      '封禁账号',
      { confirmButtonText: '确定', cancelButtonText: '取消', inputValue: '违反平台规定' }
    )

    const adminPassword = await promptAdminPassword('封禁用户', user.name || user.phone)
    if (!adminPassword) return

    await adminAPI.blockUser(user.id, reason, adminPassword)
    ElMessage.success('账号已封禁')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      const msg = error.response?.data?.message || '封禁失败'
      ElMessage.error(msg)
    }
  }
}

/**
 * 确认冻结用户
 * 弹出密码确认对话框，验证后调用冻结API
 * @param {object} user - 用户数据对象
 */
async function confirmFreeze(user) {
  const adminPassword = await promptAdminPassword('冻结用户', user.name || user.phone)
  if (!adminPassword) return

  try {
    await adminAPI.freezeUser(user.id, adminPassword)
    ElMessage.success('账号已冻结')
    fetchUsers()
  } catch (error) {
    const msg = error.response?.data?.message || '冻结失败'
    ElMessage.error(msg)
  }
}

/**
 * 确认解封/解冻用户
 * 弹出密码确认对话框，验证后调用解封API
 * @param {object} user - 用户数据对象
 */
async function confirmUnblock(user) {
  const adminPassword = await promptAdminPassword('解封用户', user.name || user.phone)
  if (!adminPassword) return

  try {
    await adminAPI.unblockUser(user.id, adminPassword)
    ElMessage.success('账号已解封')
    fetchUsers()
  } catch (error) {
    const msg = error.response?.data?.message || '解封失败'
    ElMessage.error(msg)
  }
}

/**
 * 表格选择变化处理
 * 更新选中的用户ID列表
 * @param {Array} selection - 选中的用户对象数组
 */
function handleSelectionChange(selection) {
  selectedIds.value = selection.map(item => item.id)
}

/**
 * 清空选择
 * 清空已选中的用户ID列表
 */
function clearSelection() {
  selectedIds.value = []
}

/**
 * 批量删除用户
 * 确认后调用批量删除API
 */
async function batchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 个用户吗？此操作不可恢复。`,
      '批量删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await adminAPI.batchDeleteUsers(selectedIds.value)
    ElMessage.success(`成功删除 ${selectedIds.value.length} 个用户`)
    selectedIds.value = []
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

/**
 * 批量冻结用户
 * 确认后调用批量更新状态API（冻结状态）
 */
async function batchFreeze() {
  try {
    await ElMessageBox.confirm(
      `确定要冻结选中的 ${selectedIds.value.length} 个用户吗？`,
      '批量冻结',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await adminAPI.batchUpdateUserStatus(selectedIds.value, 'SUSPENDED')
    ElMessage.success(`成功冻结 ${selectedIds.value.length} 个用户`)
    selectedIds.value = []
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量冻结失败:', error)
      ElMessage.error('批量冻结失败')
    }
  }
}

/**
 * 批量解封用户
 * 确认后调用批量更新状态API（正常状态）
 */
async function batchUnblock() {
  try {
    await ElMessageBox.confirm(
      `确定要解封选中的 ${selectedIds.value.length} 个用户吗？`,
      '批量解封',
      { confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    await adminAPI.batchUpdateUserStatus(selectedIds.value, 'ACTIVE')
    ElMessage.success(`成功解封 ${selectedIds.value.length} 个用户`)
    selectedIds.value = []
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量解封失败:', error)
      ElMessage.error('批量解封失败')
    }
  }
}

/**
 * 批量重置密码
 * 输入新密码后，为所有选中用户重置密码
 */
async function batchResetPassword() {
  try {
    const { value: password } = await ElMessageBox.prompt(
      '请输入新密码（默认123456）',
      '批量重置密码',
      { confirmButtonText: '确定', cancelButtonText: '取消', inputValue: '123456' }
    )
    if (!password || password.length < 6) {
      ElMessage.warning('密码长度不能少于6位')
      return
    }
    // 并行重置所有选中用户的密码
    const promises = selectedIds.value.map(id => adminAPI.resetUserPassword(id, password))
    await Promise.all(promises)
    ElMessage.success(`成功重置 ${selectedIds.value.length} 个用户的密码`)
    selectedIds.value = []
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量重置密码失败:', error)
      ElMessage.error('批量重置密码失败')
    }
  }
}

/**
 * 获取姓名首字母（用于头像显示）
 * @param {string} name - 用户姓名
 * @returns {string} 首字母大写，空则返回?
 */
function getInitials(name) {
  if (!name) return '?'
  return name.charAt(0).toUpperCase()
}

/**
 * 获取状态对应的标签类型
 * @param {string} status - 状态值
 * @returns {string} Element Plus标签类型
 */
function getStatusType(status) {
  const types = { 'ACTIVE': 'success', 'SUSPENDED': 'warning', 'LOCKED': 'danger', 'BLOCKED': 'danger', 'PENDING': 'info' }
  return types[status] || 'info'
}

/**
 * 获取状态对应的中文标签
 * @param {string} status - 状态值
 * @returns {string} 状态中文描述
 */
function getStatusLabel(status) {
  const labels = { 'ACTIVE': '正常', 'SUSPENDED': '冻结', 'LOCKED': '锁定', 'BLOCKED': '封禁', 'PENDING': '待验证' }
  return labels[status] || status
}

/**
 * 获取套餐对应的标签类型
 * @param {string} plan - 套餐值
 * @returns {string} Element Plus标签类型
 */
function getPlanType(plan) {
  const types = { 'FREE': 'info', 'BASIC': 'warning', 'PROFESSIONAL': 'success' }
  return types[plan] || 'info'
}

/**
 * 获取套餐对应的中文标签
 * @param {string} plan - 套餐值
 * @returns {string} 套餐中文描述
 */
function getPlanLabel(plan) {
  const labels = { 'FREE': '免费版', 'BASIC': '基础版', 'PROFESSIONAL': '专业版' }
  return labels[plan] || plan
}

/**
 * 格式化日期时间
 * @param {string} dateStr - 日期字符串
 * @returns {string} 格式化后的本地时间字符串
 */
function formatDate(dateStr) {
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

// 组件挂载时获取用户列表
onMounted(() => {
  fetchUsers()
})

// 页面从缓存恢复时重新加载数据（keep-alive）
onActivated(() => {
  fetchUsers()
})
</script>

<style scoped>
/* 用户管理页面主容器 */
.admin-users {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 页面头部布局 */
.admin-users-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
}

/* 页面标题样式 */
.admin-users-title h3 {
  font-size: 20px;
  font-weight: 600;
  color: white;
  margin: 0 0 4px 0;
}

/* 页面描述样式 */
.admin-users-title p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

/* 用户总数统计样式 */
.admin-users-count {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

/* 用户总数数字样式 */
.admin-users-count strong {
  color: #3b82f6;
  font-size: 20px;
}

/* 工具栏布局 */
.admin-users-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

/* 搜索输入框宽度 */
.admin-search-input {
  width: 280px;
}

/* 批量操作工具栏样式 */
.admin-batch-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #ef4444;
  border-radius: 8px;
  color: white;
}

/* 批量操作信息文字 */
.admin-batch-info {
  font-size: 14px;
  font-weight: 500;
}

/* 批量操作数字 */
.admin-batch-info strong {
  font-size: 18px;
}

/* 用户表格容器样式 */
.admin-users-table-wrapper {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

/* 表格主体背景 */
.admin-users-table :deep(.el-table) {
  background: #ffffff;
  color: #1f2937;
}

/* 表格表头样式 */
.admin-users-table :deep(.el-table th.el-table__cell) {
  background: #f3f4f6;
  color: #1f2937;
  border-bottom: 1px solid #e5e7eb;
  font-weight: 600;
}

/* 表格单元格样式 */
.admin-users-table :deep(.el-table td.el-table__cell) {
  color: #1f2937;
  background: #ffffff;
  border-bottom: 1px solid #f1f5f9;
}

/* 斑马纹行样式 */
.admin-users-table :deep(.el-table tr.el-table__row--striped td.el-table__cell) {
  background: #f9fafb;
}

/* 行悬停效果 */
.admin-users-table :deep(.el-table tr:hover > td.el-table__cell) {
  background: #eff6ff !important;
}

/* 斑马纹行悬停效果 */
.admin-users-table :deep(.el-table .el-table__row--striped:hover td.el-table__cell) {
  background: #eff6ff !important;
}

/* 当前选中行样式 */
.admin-users-table :deep(.el-table .current-row td.el-table__cell) {
  background: #dbeafe !important;
}

/* 表格底部边框 */
.admin-users-table :deep(.el-table__inner-wrapper::before) {
  background: #e5e7eb;
}

/* 固定列补丁样式 */
.admin-users-table :deep(.el-table__fixed-right-patch),
.admin-users-table :deep(.el-table__fixed-left-patch) {
  background: #f3f4f6;
}

/* 用户单元格布局 */
.admin-user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 用户头像样式 */
.admin-user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  font-weight: 600;
}

/* 用户信息容器 */
.admin-user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* 用户姓名样式 */
.admin-user-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

/* 用户手机号样式 */
.admin-user-phone {
  font-size: 12px;
  color: #6b7280;
}

/* AI使用次数样式 */
.admin-ai-usage {
  font-size: 13px;
  color: #4b5563;
  font-family: monospace;
  cursor: help;
}

/* 表格操作按钮基础样式 */
.admin-table-btn {
  color: #1f2937 !important;
  padding: 4px 8px;
  font-weight: 500;
}

/* 表格操作按钮悬停效果 */
.admin-table-btn:hover {
  color: #3b82f6 !important;
  background: rgba(59, 130, 246, 0.08) !important;
}

/* 表格操作按钮聚焦样式 */
.admin-table-btn:focus-visible {
  color: #1d4ed8 !important;
  outline: 2px solid rgba(59, 130, 246, 0.5);
  outline-offset: 1px;
  border-radius: 4px;
}

/* 封禁按钮颜色 */
.admin-btn-block {
  color: #b91c1c !important;
}

/* 封禁按钮悬停效果 */
.admin-btn-block:hover {
  color: #ffffff !important;
  background: #ef4444 !important;
}

/* 封禁按钮聚焦样式 */
.admin-btn-block:focus-visible {
  outline: 2px solid #ef4444;
  outline-offset: 1px;
  border-radius: 4px;
}

/* 冻结按钮颜色 */
.admin-btn-freeze {
  color: #b45309 !important;
}

/* 冻结按钮悬停效果 */
.admin-btn-freeze:hover {
  color: #ffffff !important;
  background: #f59e0b !important;
}

/* 冻结按钮聚焦样式 */
.admin-btn-freeze:focus-visible {
  outline: 2px solid #f59e0b;
  outline-offset: 1px;
  border-radius: 4px;
}

/* 解封按钮颜色 */
.admin-btn-unblock {
  color: #15803d !important;
}

/* 解封按钮悬停效果 */
.admin-btn-unblock:hover {
  color: #ffffff !important;
  background: #22c55e !important;
}

/* 解封按钮聚焦样式 */
.admin-btn-unblock:focus-visible {
  outline: 2px solid #22c55e;
  outline-offset: 1px;
  border-radius: 4px;
}

/* 删除按钮颜色 */
.admin-btn-danger {
  color: #991b1b !important;
}

/* 删除按钮悬停效果 */
.admin-btn-danger:hover {
  color: #ffffff !important;
  background: #ef4444 !important;
}

/* 删除按钮聚焦样式 */
.admin-btn-danger:focus-visible {
  outline: 2px solid #ef4444;
  outline-offset: 1px;
  border-radius: 4px;
}

/* 分页组件样式 */
.admin-pagination {
  padding: 16px;
  display: flex;
  justify-content: flex-end;
  background: #ffffff;
}

/* 分页按钮样式 */
.admin-pagination :deep(.el-pagination button),
.admin-pagination :deep(.el-pagination .el-pager li) {
  background: #ffffff;
  color: #4b5563;
}

/* 分页按钮悬停效果 */
.admin-pagination :deep(.el-pagination button:hover),
.admin-pagination :deep(.el-pagination .el-pager li:hover) {
  color: #3b82f6;
}

/* 当前页码激活样式 */
.admin-pagination :deep(.el-pagination .el-pager li.is-active) {
  background: #3b82f6 !important;
  color: #ffffff !important;
}

/* 分页选择器输入框样式 */
.admin-pagination :deep(.el-pagination .el-pagination__sizes .el-select .el-input__wrapper),
.admin-pagination :deep(.el-pagination .el-pagination__jump .el-input__wrapper) {
  background: #ffffff;
  box-shadow: 0 0 0 1px #d1d5db inset;
}

/* 分页总数文字 */
.admin-pagination :deep(.el-pagination__total) {
  color: #4b5563;
}

/* 分页当前页样式 */
.admin-pagination :deep(.el-pagination .el-pager li.active) {
  background: #3b82f6;
  color: white;
}

/* 详情弹窗内容布局 */
.admin-detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 详情区块样式 */
.admin-detail-section {
  background: rgba(15, 23, 42, 0.5);
  border-radius: 8px;
  padding: 16px;
}

/* 详情区块标题 */
.admin-detail-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 12px 0;
  text-transform: uppercase;
  letter-spacing: 1px;
}

/* 详情行布局 */
.admin-detail-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

/* 最后一行去掉底边 */
.admin-detail-row:last-child {
  border-bottom: none;
}

/* 详情标签样式 */
.admin-detail-label {
  width: 100px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  flex-shrink: 0;
}

/* 危险文字样式 */
.admin-text-danger {
  color: #ef4444;
  font-weight: 600;
}

/* 编辑表单标签样式 */
.admin-form :deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.7);
}

/* 表单选择器宽度 */
.admin-form-select {
  width: 100%;
}

/* 表单单位文字 */
.admin-form-unit {
  margin-left: 8px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
}

/* 移动端响应式适配 */
@media (max-width: 768px) {
  /* 头部改为垂直布局 */
  .admin-users-header {
    flex-direction: column;
  }

  /* 搜索框占满宽度 */
  .admin-search-input {
    width: 100%;
  }

  /* 工具栏占满宽度 */
  .admin-users-toolbar {
    width: 100%;
  }

  /* 分页居中显示 */
  .admin-pagination {
    justify-content: center;
  }
}
</style>
