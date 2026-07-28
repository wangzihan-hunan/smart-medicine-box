<!-- 个人中心页面模板 -->
<template>
  <!-- 个人中心页面主容器 -->
  <div class="profile-page">
    <div class="page-container">
      <h2 class="section-title">个人中心</h2>
      
      <!-- 个人中心内容区域：左侧资料卡片 + 右侧统计卡片 -->
      <div class="profile-content">
        <!-- 个人资料卡片 -->
        <div class="profile-card">
          <!-- 卡片头部：用户头像和基本信息 -->
          <div class="profile-header">
            <div class="profile-avatar">
              <span class="avatar-text">{{ userInitial }}</span>
            </div>
            <div class="profile-info">
              <h3 class="profile-name">{{ user.name || '用户' }}</h3>
              <p class="profile-phone">{{ user.phone }}</p>
            </div>
          </div>
          
          <!-- 标签页导航：基本信息 / 修改密码 -->
          <div class="profile-tabs">
            <button 
              v-for="tab in tabs" 
              :key="tab.key"
              class="tab-btn"
              :class="{ 'tab-active': activeTab === tab.key }"
              @click="activeTab = tab.key"
            >
              {{ tab.label }}
            </button>
          </div>
          
          <!-- 标签页内容区域 -->
          <div class="tab-content">
            <!-- 基本信息面板 -->
            <div v-if="activeTab === 'info'" class="tab-pane">
              <!-- 基本信息表单 -->
              <form @submit.prevent="handleUpdate" class="profile-form">
                <!-- 用户名输入 -->
                <div class="form-group">
                  <label class="form-label">用户名</label>
                  <input type="text" class="form-input" v-model="editForm.name" placeholder="请输入用户名" />
                </div>
                <!-- 手机号（只读） -->
                <div class="form-group">
                  <label class="form-label">手机号</label>
                  <input type="tel" class="form-input" :value="user.phone" disabled />
                </div>
                <!-- 当前套餐（只读） -->
                <div class="form-group">
                  <label class="form-label">当前套餐</label>
                  <input type="text" class="form-input" :value="getPlanLabel(user.plan)" disabled />
                </div>
                <!-- 套餐到期时间（非免费套餐时显示） -->
                <div class="form-group" v-if="user.plan !== 'FREE' && user.planExpireAt">
                  <label class="form-label">套餐到期时间</label>
                  <input type="text" class="form-input" :value="formatDate(user.planExpireAt)" disabled />
                </div>
                <!-- 本月AI使用次数（只读） -->
                <div class="form-group">
                  <label class="form-label">本月AI使用</label>
                  <input type="text" class="form-input" :value="`${user.monthAiCount || 0} / ${user.monthAiLimit || 5} 次`" disabled />
                </div>
                <!-- 保存按钮 -->
                <div class="form-actions">
                  <button type="submit" class="btn-primary" :disabled="loading">保存修改</button>
                </div>
              </form>
            </div>
            
            <!-- 修改密码面板 -->
            <div v-if="activeTab === 'password'" class="tab-pane">
              <!-- 修改密码表单 -->
              <form @submit.prevent="handleChangePassword" class="profile-form">
                <!-- 当前密码输入 -->
                <div class="form-group">
                  <label class="form-label">当前密码</label>
                  <input type="password" class="form-input" v-model="passwordForm.oldPassword" placeholder="请输入当前密码" />
                  <span v-if="errors.oldPassword" class="error-text">{{ errors.oldPassword }}</span>
                </div>
                <!-- 新密码输入，带强度指示器 -->
                <div class="form-group">
                  <label class="form-label">新密码</label>
                  <input 
                    type="password" 
                    class="form-input" 
                    v-model="passwordForm.newPassword" 
                    placeholder="请输入新密码"
                    @input="calcPasswordStrength(passwordForm.newPassword)"
                  />
                  <!-- 密码强度指示器 -->
                  <div v-if="passwordForm.newPassword && !errors.newPassword" class="password-strength">
                    <div class="strength-bar">
                      <span 
                        v-for="i in 5" 
                        :key="i" 
                        class="strength-segment"
                        :class="{ 
                          'strength-active': i <= passwordStrength,
                          'strength-weak': i <= 2 && i <= passwordStrength,
                          'strength-medium': i === 3 && i <= passwordStrength,
                          'strength-strong': i >= 4 && i <= passwordStrength
                        }"
                      ></span>
                    </div>
                    <span class="strength-text" :class="{
                      'text-weak': passwordStrength <= 2,
                      'text-medium': passwordStrength === 3,
                      'text-strong': passwordStrength >= 4
                    }">{{ passwordStrengthText }}</span>
                  </div>
                  <span v-if="errors.newPassword" class="error-text">{{ errors.newPassword }}</span>
                  <span v-if="!errors.newPassword && !passwordForm.newPassword" class="hint-text">密码至少8位，包含大小写字母和数字</span>
                </div>
                <!-- 确认新密码输入 -->
                <div class="form-group">
                  <label class="form-label">确认新密码</label>
                  <input type="password" class="form-input" v-model="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
                  <span v-if="errors.confirmPassword" class="error-text">{{ errors.confirmPassword }}</span>
                </div>
                <!-- 修改密码按钮 -->
                <div class="form-actions">
                  <button type="submit" class="btn-primary" :disabled="loading">修改密码</button>
                </div>
              </form>
            </div>
          </div>
        </div>
        
        <!-- 使用统计卡片 -->
        <div class="stats-card">
          <h3 class="card-title">使用统计</h3>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-value">{{ stats.medicines }}</div>
              <div class="stat-label">药品数量</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.reminders }}</div>
              <div class="stat-label">提醒次数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.prescriptions }}</div>
              <div class="stat-label">药方记录</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.commands }}</div>
              <div class="stat-label">指令次数</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 引入Vue核心API：响应式引用、响应式对象、生命周期钩子
import { ref, reactive, onMounted, onActivated } from 'vue'
// 引入相关API接口：认证、药品、提醒、药方、指令
import { authAPI, medicineAPI, reminderAPI, prescriptionAPI, commandAPI } from '../utils/api'

// 当前激活的标签页
const activeTab = ref('info')
// 加载状态
const loading = ref(false)
// 用户信息数据
const user = ref({})
// 用户头像首字母
const userInitial = ref('U')

// 标签页配置
const tabs = [
  { key: 'info', label: '基本信息' },
  { key: 'password', label: '修改密码' }
]

// 编辑用户信息的表单数据
const editForm = reactive({
  name: ''
})

// 修改密码的表单数据
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单错误信息
const errors = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码强度等级（0-5）
const passwordStrength = ref(0)
// 密码强度文字描述
const passwordStrengthText = ref('')

// 计算密码强度
function calcPasswordStrength(password) {
  let strength = 0
  // 长度大于等于8位加1分
  if (password.length >= 8) strength += 1
  // 包含小写字母加1分
  if (/[a-z]/.test(password)) strength += 1
  // 包含大写字母加1分
  if (/[A-Z]/.test(password)) strength += 1
  // 包含数字加1分
  if (/[0-9]/.test(password)) strength += 1
  // 包含特殊字符加1分
  if (/[@$!%*?&]/.test(password)) strength += 1
  
  passwordStrength.value = strength
  
  // 根据强度等级返回文字描述
  if (strength === 0) passwordStrengthText.value = ''
  else if (strength <= 2) passwordStrengthText.value = '弱'
  else if (strength <= 3) passwordStrengthText.value = '中'
  else if (strength <= 4) passwordStrengthText.value = '强'
  else passwordStrengthText.value = '很强'
}

// 使用统计数据
const stats = reactive({
  medicines: 0,
  reminders: 0,
  prescriptions: 0,
  commands: 0
})

// 获取用户信息
async function fetchUser() {
  try {
    const response = await authAPI.getProfile()
    if (response) {
      user.value = response
      // 获取用户名首字母作为头像
      userInitial.value = response.name ? response.name.charAt(0) : 'U'
      editForm.name = response.name || ''
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

// 获取使用统计数据（并行请求）
async function fetchStats() {
  try {
    // 并行获取所有统计数据
    const [medicines, reminders, prescriptions, commands] = await Promise.all([
      medicineAPI.getAll(),
      reminderAPI.getAll(),
      prescriptionAPI.getAll(),
      commandAPI.getAll()
    ])
    
    // 提取列表数据，兼容不同返回格式
    const medList = medicines && medicines.list ? medicines.list : []
    const remList = reminders && reminders.list ? reminders.list : []
    const prescList = prescriptions && prescriptions.list ? prescriptions.list : []
    const cmdList = commands && commands.list ? commands.list : []
    
    // 更新统计数据
    stats.medicines = medList.length
    stats.reminders = remList.length
    stats.prescriptions = prescList.length
    stats.commands = cmdList.length
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 更新用户基本信息
async function handleUpdate() {
  loading.value = true
  
  try {
    const response = await authAPI.updateProfile(editForm)
    if (response) {
      user.value = response
      alert('更新成功')
    }
  } catch (error) {
    console.error('更新失败:', error)
    alert(error.message || '更新失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 验证密码修改表单
function validatePasswordForm() {
  let isValid = true
  errors.oldPassword = ''
  errors.newPassword = ''
  errors.confirmPassword = ''
  
  // 验证当前密码
  if (!passwordForm.oldPassword) {
    errors.oldPassword = '请输入当前密码'
    isValid = false
  }
  
  // 验证新密码格式
  if (!passwordForm.newPassword) {
    errors.newPassword = '请输入新密码'
    isValid = false
  } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,}$/.test(passwordForm.newPassword)) {
    errors.newPassword = '密码至少8位，包含大小写字母和数字'
    isValid = false
  }
  
  // 验证确认密码
  if (!passwordForm.confirmPassword) {
    errors.confirmPassword = '请确认新密码'
    isValid = false
  } else if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    errors.confirmPassword = '两次输入的密码不一致'
    isValid = false
  }
  
  return isValid
}

// 处理修改密码
async function handleChangePassword() {
  if (!validatePasswordForm()) return
  
  loading.value = true
  
  try {
    const response = await authAPI.changePassword(passwordForm)
    if (response.code === 200) {
      alert('密码修改成功')
      // 清空表单
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    }
  } catch (error) {
    console.error('修改密码失败:', error)
    errors.oldPassword = '当前密码不正确'
  } finally {
    loading.value = false
  }
}

// 获取套餐类型的中文标签
function getPlanLabel(plan) {
  const labels = { 'FREE': '免费版', 'BASIC': '基础版', 'PROFESSIONAL': '专业版' }
  return labels[plan] || '免费版'
}

// 格式化日期
function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

// 组件挂载时获取用户信息和统计数据
onMounted(() => {
  fetchUser()
  fetchStats()
})

// 页面从缓存恢复时重新加载数据（keep-alive）
onActivated(() => {
  fetchUser()
  fetchStats()
})
</script>

<style scoped>
/* 个人中心页面主容器样式 */
.profile-page {
  padding-top: 80px;
}

/* 个人中心内容区域布局：左侧资料卡片 + 右侧统计卡片 */
.profile-content {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 24px;
}

/* 个人资料卡片样式 */
.profile-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

/* 卡片头部渐变背景样式 */
.profile-header {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  padding: 40px 32px;
  display: flex;
  align-items: center;
  gap: 24px;
}

/* 用户头像样式 */
.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  border: 3px solid rgba(255, 255, 255, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 头像文字样式 */
.avatar-text {
  font-size: 32px;
  font-weight: 700;
  color: white;
}

/* 用户信息区域样式 */
.profile-info {
  color: white;
}

/* 用户姓名样式 */
.profile-name {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 4px;
}

/* 用户手机号样式 */
.profile-phone {
  font-size: 14px;
  opacity: 0.8;
}

/* 标签页导航样式 */
.profile-tabs {
  display: flex;
  border-bottom: 1px solid var(--border-color);
}

/* 标签页按钮样式 */
.tab-btn {
  padding: 16px 32px;
  background: none;
  border: none;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
}

/* 标签页按钮悬停效果 */
.tab-btn:hover {
  color: var(--primary-color);
}

/* 激活的标签页样式 */
.tab-active {
  color: var(--primary-color);
  font-weight: 600;
}

/* 激活标签页的底部指示条 */
.tab-active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--primary-color);
}

/* 标签页内容区域样式 */
.tab-content {
  padding: 32px;
}

/* 标签页面板淡入动画 */
.tab-pane {
  animation: fadeIn 0.3s ease;
}

/* 个人资料表单样式 */
.profile-form {
  max-width: 400px;
}

/* 表单组样式 */
.form-group {
  margin-bottom: 24px;
}

/* 表单标签样式 */
.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 8px;
}

/* 表单输入框样式 */
.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 15px;
  transition: all var(--transition-fast);
}

/* 输入框聚焦效果 */
.form-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

/* 输入框禁用状态 */
.form-input:disabled {
  background: #f5f5f5;
  color: var(--text-muted);
  cursor: not-allowed;
}

/* 错误提示文字样式 */
.error-text {
  display: block;
  font-size: 12px;
  color: var(--danger-color);
  margin-top: 6px;
}

/* 提示文字样式 */
.hint-text {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 6px;
}

/* 密码强度指示器容器 */
.password-strength {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

/* 强度进度条容器 */
.strength-bar {
  display: flex;
  gap: 4px;
  flex: 1;
}

/* 强度分段样式 */
.strength-segment {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: var(--border-color);
  transition: all var(--transition-fast);
}

/* 弱强度颜色 */
.strength-segment.strength-active.strength-weak {
  background: var(--danger-color);
}

/* 中等强度颜色 */
.strength-segment.strength-active.strength-medium {
  background: var(--warning-color);
}

/* 强强度颜色 */
.strength-segment.strength-active.strength-strong {
  background: var(--success-color);
}

/* 强度文字样式 */
.strength-text {
  font-size: 12px;
  font-weight: 500;
  min-width: 30px;
}

/* 弱强度文字颜色 */
.text-weak {
  color: var(--danger-color);
}

/* 中等强度文字颜色 */
.text-medium {
  color: var(--warning-color);
}

/* 强强度文字颜色 */
.text-strong {
  color: var(--success-color);
}

/* 表单操作按钮区域 */
.form-actions {
  margin-top: 32px;
}

/* 统计卡片样式 */
.stats-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  padding: 24px;
}

/* 统计卡片标题样式 */
.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
}

/* 统计数据网格布局 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

/* 统计项样式 */
.stat-item {
  text-align: center;
  padding: 16px;
  background: #fafafa;
  border-radius: 12px;
}

/* 统计数值样式 */
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary-color);
  margin-bottom: 4px;
}

/* 统计标签样式 */
.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 淡入动画 */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 平板响应式：改为单列布局 */
@media (max-width: 900px) {
  .profile-content {
    grid-template-columns: 1fr;
  }
  
  .stats-card {
    order: -1;
  }
}

/* 手机响应式调整 */
@media (max-width: 600px) {
  .profile-header {
    padding: 24px;
    flex-direction: column;
    text-align: center;
  }
  
  .tab-content {
    padding: 20px;
  }
}
</style>
