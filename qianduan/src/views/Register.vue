<template>
  <!-- 注册页面根容器 -->
  <div class="register-page">
    <!-- 注册页面主容器，左右两栏布局 -->
    <div class="register-container">
      <!-- 左侧品牌展示区 -->
      <div class="register-left">
        <!-- 品牌标识区域 -->
        <div class="brand-section">
          <el-icon class="brand-icon"><TakeawayBox /></el-icon>
          <div class="brand-text">智能药箱</div>
          <div class="brand-desc">守护家人健康，从智能用药开始</div>
        </div>
        <!-- 功能特性列表 -->
        <div class="features-list">
          <div class="feature-item" v-for="(feature, index) in features" :key="index">
            <el-icon class="feature-icon"><component :is="feature.icon" /></el-icon>
            <span class="feature-text">{{ feature.text }}</span>
          </div>
        </div>
        <!-- 装饰性圆形背景元素 -->
        <div class="decorative-circle"></div>
      </div>
      
      <!-- 右侧注册表单区 -->
      <div class="register-right">
        <!-- 注册卡片容器 -->
        <div class="register-card">
          <h2 class="register-title">创建账号</h2>
          <p class="register-subtitle">开启智能健康管理之旅</p>
          
          <!-- 注册表单 -->
          <form @submit.prevent="handleRegister" class="register-form">
            <!-- 手机号输入框组 -->
            <div class="form-group">
              <label class="form-label" for="register-phone">手机号</label>
              <div class="input-wrapper" :class="{ 'input-error': errors.phone }">
                <el-icon class="input-icon" aria-hidden="true"><Phone /></el-icon>
                <input 
                  type="tel" 
                  class="form-input" 
                  id="register-phone"
                  v-model="form.phone" 
                  placeholder="请输入手机号"
                  maxlength="11"
                  aria-label="手机号"
                  @focus="clearError('phone')"
                />
                <span v-if="errors.phone" class="input-error-icon" aria-hidden="true">⚠️</span>
              </div>
              <span v-if="errors.phone" class="error-text">{{ errors.phone }}</span>
            </div>
            
            <!-- 用户名输入框组 -->
            <div class="form-group">
              <label class="form-label" for="register-name">用户名</label>
              <div class="input-wrapper" :class="{ 'input-error': errors.name }">
                <el-icon class="input-icon" aria-hidden="true"><User /></el-icon>
                <input 
                  type="text" 
                  class="form-input" 
                  id="register-name"
                  v-model="form.name" 
                  placeholder="请输入用户名"
                  aria-label="用户名"
                  @focus="clearError('name')"
                />
                <span v-if="errors.name" class="input-error-icon" aria-hidden="true">⚠️</span>
              </div>
              <span v-if="errors.name" class="error-text">{{ errors.name }}</span>
            </div>
            
            <!-- 密码输入框组，带密码强度显示 -->
            <div class="form-group">
              <label class="form-label" for="register-password">密码</label>
              <div class="input-wrapper" :class="{ 'input-error': errors.password }">
                <el-icon class="input-icon" aria-hidden="true"><Lock /></el-icon>
                <input 
                  :type="showPassword ? 'text' : 'password'" 
                  class="form-input" 
                  id="register-password"
                  v-model="form.password" 
                  placeholder="请输入密码"
                  aria-label="密码"
                  @focus="clearError('password')"
                  @input="calculatePasswordStrength(form.password)"
                />
                <el-icon class="toggle-password" @click="showPassword = !showPassword" aria-label="切换密码可见性">
                  <component :is="showPassword ? Hide : View" />
                </el-icon>
              </div>
              <!-- 密码强度指示器 -->
              <div v-if="form.password && !errors.password" class="password-strength">
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
              <span v-if="errors.password" class="error-text">{{ errors.password }}</span>
              <!-- 密码格式提示 -->
              <span v-if="!errors.password && !form.password" class="hint-text">密码至少8位，包含大小写字母和数字</span>
            </div>
            
            <!-- 确认密码输入框组 -->
            <div class="form-group">
              <label class="form-label" for="register-confirm-password">确认密码</label>
              <div class="input-wrapper" :class="{ 'input-error': errors.confirmPassword }">
                <el-icon class="input-icon" aria-hidden="true"><Lock /></el-icon>
                <input 
                  :type="showPassword ? 'text' : 'password'" 
                  class="form-input" 
                  id="register-confirm-password"
                  v-model="form.confirmPassword" 
                  placeholder="请再次输入密码"
                  aria-label="确认密码"
                  @focus="clearError('confirmPassword')"
                />
                <span v-if="errors.confirmPassword" class="input-error-icon" aria-hidden="true">⚠️</span>
              </div>
              <span v-if="errors.confirmPassword" class="error-text">{{ errors.confirmPassword }}</span>
            </div>
            
            <!-- 用户协议同意复选框 -->
            <label class="checkbox-label" for="register-agree">
              <input type="checkbox" id="register-agree" v-model="agreeTerms" aria-label="我已阅读并同意用户协议和隐私政策" />
              <span class="checkbox-text">我已阅读并同意</span>
              <a href="#" class="terms-link" @click.prevent>用户协议</a>
              <span class="checkbox-text">和</span>
              <a href="#" class="terms-link" @click.prevent>隐私政策</a>
            </label>
            <span v-if="errors.agreeTerms" class="error-text">{{ errors.agreeTerms }}</span>
            
            <!-- 注册提交按钮 -->
            <button type="submit" class="btn-primary btn-lg register-btn" :disabled="loading">
              <span v-if="loading" class="loading-text">注册中...</span>
              <span v-else>注 册</span>
            </button>
          </form>
          
          <!-- 登录跳转链接 -->
          <div class="login-link">
            已有账号？<router-link to="/login">立即登录</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 导入Vue组合式API
import { ref, reactive } from 'vue'
// 导入Vue路由函数
import { useRouter } from 'vue-router'
// 导入API工具函数
import { authAPI, setToken } from '../utils/api'
// 导入Element Plus图标组件
import { TakeawayBox, Camera, Clock, Cellphone, Phone, User, Lock, View, Hide } from '@element-plus/icons-vue'

// 定义组件名称（用于keep-alive的include/exclude匹配）
defineOptions({
  name: 'Register'
})

// 路由实例，用于页面跳转
const router = useRouter()
// 注册按钮加载状态，防止重复提交
const loading = ref(false)
// 密码是否可见的状态
const showPassword = ref(false)
// 是否同意用户协议的状态
const agreeTerms = ref(false)

// 左侧展示的功能特性列表数据
const features = [
  { icon: Camera, text: '智能药方识别' },
  { icon: Clock, text: '贴心用药提醒' },
  { icon: Cellphone, text: '远程设备控制' }
]

// 注册表单数据
const form = reactive({
  // 手机号
  phone: '',
  // 用户名
  name: '',
  // 密码
  password: '',
  // 确认密码
  confirmPassword: ''
})

// 表单验证错误信息
const errors = reactive({
  // 手机号错误信息
  phone: '',
  // 用户名错误信息
  name: '',
  // 密码错误信息
  password: '',
  // 确认密码错误信息
  confirmPassword: '',
  // 用户协议同意错误信息
  agreeTerms: ''
})

// 密码强度等级（0-5）
const passwordStrength = ref(0)
// 密码强度文字描述
const passwordStrengthText = ref('')

// 计算密码强度，根据密码包含的字符类型数量评定
function calculatePasswordStrength(password) {
  // 强度初始值
  let strength = 0
  // 长度>=8加1分
  if (password.length >= 8) strength += 1
  // 包含小写字母加1分
  if (/[a-z]/.test(password)) strength += 1
  // 包含大写字母加1分
  if (/[A-Z]/.test(password)) strength += 1
  // 包含数字加1分
  if (/[0-9]/.test(password)) strength += 1
  // 包含特殊字符加1分
  if (/[@$!%*?&]/.test(password)) strength += 1
  
  // 更新密码强度值
  passwordStrength.value = strength
  
  // 根据强度设置对应文字描述
  if (strength === 0) passwordStrengthText.value = ''
  else if (strength <= 2) passwordStrengthText.value = '弱'
  else if (strength <= 3) passwordStrengthText.value = '中'
  else if (strength <= 4) passwordStrengthText.value = '强'
  else passwordStrengthText.value = '很强'
}

// 清除指定字段的错误信息
function clearError(field) {
  errors[field] = ''
}

// 表单验证函数，验证所有注册字段
function validateForm() {
  // 验证结果标记
  let isValid = true
  // 重置所有错误信息
  errors.phone = ''
  errors.name = ''
  errors.password = ''
  errors.confirmPassword = ''
  errors.agreeTerms = ''
  
  // 验证手机号
  if (!form.phone) {
    errors.phone = '请输入手机号'
    isValid = false
  } else if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    errors.phone = '请输入有效的手机号'
    isValid = false
  }
  
  // 验证用户名
  if (!form.name) {
    errors.name = '请输入用户名'
    isValid = false
  } else if (form.name.length > 50) {
    errors.name = '用户名长度不能超过50个字符'
    isValid = false
  }
  
  // 验证密码格式
  if (!form.password) {
    errors.password = '请输入密码'
    isValid = false
  } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,}$/.test(form.password)) {
    errors.password = '密码至少8位，包含大小写字母和数字'
    isValid = false
  }
  
  // 验证确认密码
  if (!form.confirmPassword) {
    errors.confirmPassword = '请确认密码'
    isValid = false
  } else if (form.password !== form.confirmPassword) {
    errors.confirmPassword = '两次输入的密码不一致'
    isValid = false
  }
  
  // 验证是否同意用户协议
  if (!agreeTerms.value) {
    errors.agreeTerms = '请阅读并同意用户协议和隐私政策'
    isValid = false
  }
  
  return isValid
}

// 处理注册提交
async function handleRegister() {
  // 先进行表单验证，验证失败则直接返回
  if (!validateForm()) return
  
  // 设置加载状态
  loading.value = true
  
  try {
    // 调用注册API
    const response = await authAPI.register(form)
    
    // 注册成功
    if (response && response.id) {
      // 跳转到登录页
      router.push('/login')
    } else {
      // 注册失败，显示错误信息
      errors.phone = '注册失败，请稍后重试'
    }
  } catch (error) {
    // 处理注册异常
    errors.phone = error.message || '注册失败，请检查网络连接'
  } finally {
    // 无论成功失败，都关闭加载状态
    loading.value = false
  }
}
</script>

<style scoped>
/* 注册页面整体样式 */
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8fafc 0%, #eff6ff 50%, #f0fdfa 100%);
  background-attachment: fixed;
}

/* 注册容器，左右两栏网格布局 */
.register-container {
  width: 100%;
  max-width: 900px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  padding: 40px;
}

/* 左侧品牌展示区样式 */
.register-left {
  background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%);
  border-radius: 24px;
  padding: 60px 40px;
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

/* 左上角装饰圆形 */
.register-left::before {
  content: '';
  position: absolute;
  top: -30%;
  right: -20%;
  width: 300px;
  height: 300px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

/* 左下角装饰圆形 */
.register-left::after {
  content: '';
  position: absolute;
  bottom: -20%;
  left: -10%;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
}

/* 中间装饰性渐变圆形 */
.decorative-circle {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(255,255,255,0.03) 0%, transparent 70%);
  pointer-events: none;
}

/* 品牌标识区域样式 */
.brand-section {
  position: relative;
  z-index: 1;
  margin-bottom: 40px;
}

/* 品牌图标样式 */
.brand-icon {
  font-size: 56px;
  margin-bottom: 16px;
}

/* 品牌名称文字样式 */
.brand-text {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}

/* 品牌描述文字样式 */
.brand-desc {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
}

/* 功能特性列表样式 */
.features-list {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 单个功能特性项样式 */
.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
}

/* 功能特性图标样式 */
.feature-icon {
  font-size: 24px;
}

/* 功能特性文字样式 */
.feature-text {
  color: rgba(255, 255, 255, 0.9);
}

/* 右侧注册区域样式 */
.register-right {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 注册卡片样式 */
.register-card {
  background: white;
  border-radius: 24px;
  padding: 48px 40px;
  box-shadow: var(--card-shadow-lg);
  width: 100%;
  max-width: 400px;
  border: 1px solid var(--border-color);
}

/* 注册标题样式 */
.register-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}

/* 注册副标题样式 */
.register-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 32px;
}

/* 注册表单样式 */
.register-form {
  display: flex;
  flex-direction: column;
}

/* 表单组样式 */
.form-group {
  margin-bottom: 20px;
}

/* 表单标签样式 */
.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 8px;
  letter-spacing: -0.01em;
}

/* 输入框包裹层样式 */
.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  border-radius: var(--border-radius-sm);
  transition: all var(--transition-fast);
}

/* 输入框错误状态样式 */
.input-wrapper.input-error {
  border: 1px solid var(--danger-color);
}

/* 输入框左侧图标样式 */
.input-icon {
  position: absolute;
  left: 14px;
  font-size: 16px;
  z-index: 1;
}

/* 输入框错误图标样式 */
.input-error-icon {
  position: absolute;
  right: 14px;
  font-size: 16px;
  z-index: 1;
}

/* 表单输入框样式 */
.form-input {
  width: 100%;
  padding: 14px 16px 14px 44px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-sm);
  font-size: 15px;
  transition: all var(--transition-fast);
  background: #fafafa;
  outline: none;
}

/* 输入框聚焦状态样式 */
.form-input:focus {
  border-color: var(--primary-color);
  background: white;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* 输入框占位符样式 */
.form-input::placeholder {
  color: var(--text-muted);
}

/* 密码可见性切换按钮样式 */
.toggle-password {
  position: absolute;
  right: 14px;
  cursor: pointer;
  font-size: 16px;
  transition: opacity var(--transition-fast);
}

/* 密码切换按钮悬停效果 */
.toggle-password:hover {
  opacity: 0.8;
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

/* 密码强度指示器样式 */
.password-strength {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

/* 密码强度条容器 */
.strength-bar {
  display: flex;
  gap: 4px;
  flex: 1;
}

/* 密码强度段样式 */
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

/* 中强度颜色 */
.strength-segment.strength-active.strength-medium {
  background: var(--warning-color);
}

/* 高强度颜色 */
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

/* 中强度文字颜色 */
.text-medium {
  color: var(--warning-color);
}

/* 高强度文字颜色 */
.text-strong {
  color: var(--success-color);
}

/* 复选框标签样式 */
.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

/* 复选框输入框样式 */
.checkbox-label input {
  width: 16px;
  height: 16px;
  accent-color: var(--primary-color);
}

/* 复选框文字样式 */
.checkbox-text {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 用户协议链接样式 */
.terms-link {
  font-size: 13px;
  color: var(--primary-color);
  text-decoration: none;
}

/* 用户协议链接悬停效果 */
.terms-link:hover {
  text-decoration: underline;
}

/* 注册按钮样式 */
.register-btn {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  margin-bottom: 20px;
}

/* 登录链接区域样式 */
.login-link {
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
}

/* 登录链接样式 */
.login-link a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

/* 登录链接悬停效果 */
.login-link a:hover {
  text-decoration: underline;
}

/* 加载中文字样式 */
.loading-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* 响应式布局 - 移动端适配 */
@media (max-width: 768px) {
  /* 移动端单列布局 */
  .register-container {
    grid-template-columns: 1fr;
    padding: 20px;
    gap: 24px;
  }
  
  /* 移动端左侧区域内边距调整 */
  .register-left {
    padding: 40px 24px;
  }
  
  /* 移动端注册卡片内边距调整 */
  .register-card {
    padding: 32px 24px;
  }
  
  /* 移动端注册标题字号调整 */
  .register-title {
    font-size: 24px;
  }
  
  /* 移动端复选框布局调整 */
  .checkbox-label {
    flex-direction: row;
  }
}
</style>
