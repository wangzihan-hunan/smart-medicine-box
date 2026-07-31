<template>
  <!-- 登录页面根容器 -->
  <div class="login-page">
    <!-- 登录页面主容器，左右两栏布局 -->
    <div class="login-container">
      <!-- 左侧品牌展示区 -->
      <div class="login-left">
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
      
      <!-- 右侧登录表单区 -->
      <div class="login-right">
        <!-- 登录卡片容器 -->
        <div class="login-card">
          <h2 class="login-title">欢迎回来</h2>
          <p class="login-subtitle">请登录您的账号</p>
          
          <!-- 登录表单 -->
          <form @submit.prevent="handleLogin" class="login-form">
            <!-- 手机号输入框组 -->
            <div class="form-group">
              <label class="form-label" for="login-phone">手机号</label>
              <div class="input-wrapper" :class="{ 'input-error': errors.phone }">
                <el-icon class="input-icon" aria-hidden="true"><Phone /></el-icon>
                <input 
                  type="tel" 
                  class="form-input" 
                  id="login-phone"
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
            
            <!-- 密码输入框组 -->
            <div class="form-group">
              <label class="form-label" for="login-password">密码</label>
              <div class="input-wrapper" :class="{ 'input-error': errors.password }">
                <el-icon class="input-icon" aria-hidden="true"><Lock /></el-icon>
                <input 
                  :type="showPassword ? 'text' : 'password'" 
                  class="form-input" 
                  id="login-password"
                  v-model="form.password" 
                  placeholder="请输入密码"
                  aria-label="密码"
                  @focus="clearError('password')"
                />
                <el-icon class="toggle-password" @click="showPassword = !showPassword" aria-label="切换密码可见性">
                  <component :is="showPassword ? Hide : View" />
                </el-icon>
              </div>
              <span v-if="errors.password" class="error-text">{{ errors.password }}</span>
            </div>
            
            <!-- 字母验证码输入框组 -->
            <div class="form-group">
              <label class="form-label" for="login-captcha">验证码</label>
              <div class="input-wrapper captcha-wrapper" :class="{ 'input-error': errors.captcha }">
                <input 
                  type="text" 
                  class="form-input captcha-input" 
                  id="login-captcha"
                  v-model="form.captcha" 
                  placeholder="请输入验证码"
                  maxlength="4"
                  aria-label="验证码"
                  @focus="clearError('captcha')"
                />
                <div class="captcha-text-container">
                  <span class="captcha-text" @click="refreshCaptcha">{{ captchaText }}</span>
                  <span class="captcha-refresh" @click="refreshCaptcha">换一张</span>
                </div>
              </div>
              <span v-if="errors.captcha" class="error-text">{{ errors.captcha }}</span>
            </div>
            
            <!-- 表单选项：记住我和忘记密码 -->
            <div class="form-options">
              <label class="checkbox-label" for="login-remember">
                <input type="checkbox" id="login-remember" v-model="rememberMe" aria-label="记住我" />
                <span class="checkbox-text">记住我</span>
              </label>
              <a href="#" class="forgot-link" @click.prevent="showForgotPassword = true">忘记密码？</a>
            </div>
            
            <!-- 登录尝试次数提示 -->
            <div v-if="remainingAttempts > 0 && remainingAttempts < 5" class="attempts-warning">
              <el-icon class="attempts-icon"><Warning /></el-icon>
              <span>还剩 <strong>{{ remainingAttempts }}</strong> 次尝试机会，超出将锁定账号30分钟</span>
            </div>
            
            <!-- 登录提交按钮 -->
            <button type="submit" class="btn-primary btn-lg login-btn" :disabled="loading">
              <span v-if="loading" class="loading-text">登录中...</span>
              <span v-else>登 录</span>
            </button>
          </form>
          
          <!-- 注册跳转链接 -->
          <div class="register-link">
            还没有账号？<router-link to="/register">立即注册</router-link>
          </div>
          
          <!-- 管理员登录入口切换 -->
          <div class="login-switch">
            <span class="login-switch-divider"></span>
            <router-link to="/admin/login" class="login-switch-link">
              <el-icon><Setting /></el-icon>
              <span>管理员登录</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 忘记密码弹窗 -->
    <el-dialog
      v-model="showForgotPassword"
      title="忘记密码"
      width="400px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <form @submit.prevent="handleForgotPassword" class="forgot-form">
        <div class="form-group">
          <label class="form-label">手机号</label>
          <input 
            type="tel" 
            class="form-input" 
            v-model="forgotForm.phone" 
            placeholder="请输入注册手机号"
            maxlength="11"
            @focus="clearForgotError('phone')"
          />
          <span v-if="forgotErrors.phone" class="error-text">{{ forgotErrors.phone }}</span>
        </div>
        <div class="form-group">
          <label class="form-label">验证码</label>
          <div class="captcha-row">
            <input 
              type="text" 
              class="form-input captcha-input" 
              v-model="forgotForm.captcha" 
              placeholder="请输入验证码"
              maxlength="4"
            />
            <button type="button" class="btn-secondary captcha-btn" @click="sendCaptcha" :disabled="captchaCountdown > 0">
              {{ captchaCountdown > 0 ? `${captchaCountdown}秒后重发` : '发送验证码' }}
            </button>
          </div>
          <span v-if="forgotErrors.captcha" class="error-text">{{ forgotErrors.captcha }}</span>
        </div>
        <div class="form-group">
          <label class="form-label">新密码</label>
          <input 
            type="password" 
            class="form-input" 
            v-model="forgotForm.newPassword" 
            placeholder="请输入新密码（至少6位）"
            @focus="clearForgotError('newPassword')"
          />
          <span v-if="forgotErrors.newPassword" class="error-text">{{ forgotErrors.newPassword }}</span>
        </div>
      </form>
      <template #footer>
        <button class="btn-secondary" @click="showForgotPassword = false">取消</button>
        <button class="btn-primary" @click="handleForgotPassword" :disabled="forgotLoading">
          {{ forgotLoading ? '处理中...' : '确认修改' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 导入Vue组合式API
import { ref, reactive, onMounted } from 'vue'
// 导入Element Plus消息提示和弹窗组件
import { ElMessage, ElMessageBox } from 'element-plus'
// 导入Vue路由相关函数
import { useRouter, useRoute } from 'vue-router'
// 导入API工具函数
import { authAPI, removeToken } from '../utils/api'
// 导入认证状态管理store
import { useAuthStore } from '../stores/auth'
// 导入Element Plus图标组件
import { TakeawayBox, Camera, Clock, Cellphone, Phone, Lock, View, Hide, Setting, Warning } from '@element-plus/icons-vue'

// 定义组件名称（用于keep-alive的include/exclude匹配）
defineOptions({
  name: 'Login'
})

// 路由实例，用于页面跳转
const router = useRouter()
// 当前路由信息，用于获取重定向参数
const route = useRoute()
// 认证状态管理store实例
const authStore = useAuthStore()
// 登录按钮加载状态，防止重复提交
const loading = ref(false)
// 密码是否可见的状态
const showPassword = ref(false)
// 记住我复选框状态
const rememberMe = ref(false)
// 剩余登录尝试次数
const remainingAttempts = ref(0)
// 忘记密码弹窗显示状态
const showForgotPassword = ref(false)
// 忘记密码表单数据
const forgotForm = reactive({
  phone: '',
  captcha: '',
  newPassword: ''
})
// 忘记密码表单错误信息
const forgotErrors = reactive({
  phone: '',
  captcha: '',
  newPassword: ''
})
// 忘记密码加载状态
const forgotLoading = ref(false)
// 验证码倒计时
const captchaCountdown = ref(0)

// 左侧展示的功能特性列表数据
const features = [
  { icon: Camera, text: '智能药方识别' },
  { icon: Clock, text: '贴心用药提醒' },
  { icon: Cellphone, text: '远程设备控制' }
]

// 登录表单数据
const form = reactive({
  // 手机号
  phone: '',
  // 密码
  password: '',
  // 字母验证码
  captcha: ''
})

// 表单验证错误信息
const errors = reactive({
  // 手机号错误信息
  phone: '',
  // 密码错误信息
  password: '',
  // 验证码错误信息
  captcha: ''
})

// 字母验证码文本
const captchaText = ref('')

// 清除指定字段的错误信息
function clearError(field) {
  errors[field] = ''
}

// 表单验证函数，验证手机号、密码和验证码格式
function validateForm() {
  // 验证结果标记
  let isValid = true
  // 重置错误信息
  errors.phone = ''
  errors.password = ''
  errors.captcha = ''
  
  // 验证手机号
  if (!form.phone) {
    errors.phone = '请输入手机号'
    isValid = false
  } else if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    errors.phone = '请输入有效的手机号'
    isValid = false
  }
  
  // 验证密码
  if (!form.password) {
    errors.password = '请输入密码'
    isValid = false
  } else if (form.password.length < 6) {
    errors.password = '密码长度不能少于6位'
    isValid = false
  }
  
  // 验证字母验证码
  if (!form.captcha) {
    errors.captcha = '请输入验证码'
    isValid = false
  } else if (form.captcha.length !== 4) {
    errors.captcha = '验证码必须为4位'
    isValid = false
  }
  
  return isValid
}

// 刷新字母验证码
async function refreshCaptcha() {
  try {
    const res = await authAPI.getCaptchaText()
    captchaText.value = res.captcha || ''
  } catch (e) {
    captchaText.value = '获取失败'
  }
  form.captcha = ''
  errors.captcha = ''
}

// 页面加载时获取验证码
onMounted(() => {
  refreshCaptcha()
})

// 处理登录提交
async function handleLogin() {
  // 先进行表单验证，验证失败则直接返回
  if (!validateForm()) return
  
  // 设置加载状态
  loading.value = true
  
  try {
    // 先验证字母验证码
    const captchaResponse = await authAPI.verifyCaptcha(form.captcha)
    if (!captchaResponse) {
      errors.captcha = '验证码错误，请重新输入'
      refreshCaptcha()
      loading.value = false
      return
    }
    
    // 调用登录API
    const response = await authAPI.login(form)

    // 登录成功，获取到accessToken
    if (response && response.accessToken) {
      // 保存token到store
      authStore.login(response.accessToken)
      // 获取用户信息
      await authStore.fetchUserInfo()

      // 检查是否需要强制修改密码
      if (response.mustChangePassword) {
        ElMessage.warning('首次登录请先修改密码')
        router.push('/profile')
        return
      }

      // 普通用户登录成功，显示欢迎提示
      const userName = authStore.user?.name || '用户'
      ElMessage.success(`欢迎回来，${userName}！`)
      
      // 跳转到目标页面或首页
      const redirectPath = route.query.redirect || '/'
      router.push(redirectPath)
    } else {
      // 登录失败，显示错误信息
      errors.password = '登录失败，请检查账号密码'
    }
  } catch (error) {
    // 处理登录异常，区分不同错误类型
    const errorData = error.response?.data
    const msg = errorData?.message || error.message || '登录失败，请检查网络连接'
    
    // 区分不同错误类型
    if (msg.includes('锁定')) {
      // 账号被锁定
      errors.password = msg
    } else if (msg.includes('不存在')) {
      // 用户不存在
      errors.password = '该手机号未注册，请先注册账号'
    } else if (msg.includes('密码')) {
      // 密码错误
      errors.password = '密码错误，请重新输入'
      // 更新剩余尝试次数
      if (errorData?.remainingAttempts !== undefined) {
        remainingAttempts.value = errorData.remainingAttempts
      }
    } else {
      // 其他错误
      errors.password = msg
    }
  } finally {
    // 无论成功失败，都关闭加载状态
    loading.value = false
    // 登录失败时刷新验证码
    refreshCaptcha()
  }
}

// 清除忘记密码表单指定字段的错误信息
function clearForgotError(field) {
  forgotErrors[field] = ''
}

// 验证忘记密码表单
function validateForgotForm() {
  forgotErrors.phone = ''
  forgotErrors.captcha = ''
  forgotErrors.newPassword = ''
  
  let isValid = true
  
  if (!forgotForm.phone) {
    forgotErrors.phone = '请输入手机号'
    isValid = false
  } else if (!/^1[3-9]\d{9}$/.test(forgotForm.phone)) {
    forgotErrors.phone = '请输入有效的手机号'
    isValid = false
  }
  
  if (!forgotForm.captcha) {
    forgotErrors.captcha = '请输入验证码'
    isValid = false
  } else if (forgotForm.captcha.length !== 4) {
    forgotErrors.captcha = '验证码必须为4位'
    isValid = false
  }
  
  if (!forgotForm.newPassword) {
    forgotErrors.newPassword = '请输入新密码'
    isValid = false
  } else if (forgotForm.newPassword.length < 6) {
    forgotErrors.newPassword = '密码长度不能少于6位'
    isValid = false
  }
  
  return isValid
}

// 发送验证码
async function sendCaptcha() {
  if (!forgotForm.phone || !/^1[3-9]\d{9}$/.test(forgotForm.phone)) {
    ElMessage.warning('请先输入有效的手机号')
    return
  }
  
  try {
    await authAPI.sendCaptcha(forgotForm.phone)
    ElMessage.success('验证码已发送到您的手机')
    
    // 开始倒计时
    captchaCountdown.value = 60
    const timer = setInterval(() => {
      captchaCountdown.value--
      if (captchaCountdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    ElMessage.error('发送验证码失败，请重试')
  }
}

// 处理忘记密码提交
async function handleForgotPassword() {
  if (!validateForgotForm()) return
  
  forgotLoading.value = true
  
  try {
    await authAPI.resetPassword({
      phone: forgotForm.phone,
      captcha: forgotForm.captcha,
      newPassword: forgotForm.newPassword
    })
    
    ElMessage.success('密码重置成功，请使用新密码登录')
    showForgotPassword.value = false
    
    // 清空表单
    forgotForm.phone = ''
    forgotForm.captcha = ''
    forgotForm.newPassword = ''
  } catch (error) {
    const msg = error.response?.data?.message || error.message || '密码重置失败'
    ElMessage.error(msg)
  } finally {
    forgotLoading.value = false
  }
}
</script>

<style scoped>
/* 登录页面整体样式 */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8fafc 0%, #eff6ff 50%, #f0fdfa 100%);
  background-attachment: fixed;
}

/* 登录容器，左右两栏网格布局 */
.login-container {
  width: 100%;
  max-width: 900px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  padding: 40px;
}

/* 左侧品牌展示区样式 */
.login-left {
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
.login-left::before {
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
.login-left::after {
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

/* 右侧登录区域样式 */
.login-right {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 登录卡片样式 */
.login-card {
  background: white;
  border-radius: 24px;
  padding: 48px 40px;
  box-shadow: var(--card-shadow-lg);
  width: 100%;
  max-width: 400px;
  border: 1px solid var(--border-color);
}

/* 登录标题样式 */
.login-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}

/* 登录副标题样式 */
.login-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 32px;
}

/* 登录表单样式 */
.login-form {
  display: flex;
  flex-direction: column;
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

/* 表单选项区域样式（记住我+忘记密码） */
.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

/* 复选框标签样式 */
.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

/* 复选框输入框样式 */
.checkbox-label input {
  width: 16px;
  height: 16px;
  accent-color: var(--primary-color);
}

/* 复选框文字样式 */
.checkbox-text {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 忘记密码链接样式 */
.forgot-link {
  font-size: 14px;
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

/* 忘记密码链接悬停效果 */
.forgot-link:hover {
  text-decoration: underline;
}

/* 登录按钮样式 */
.login-btn {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  margin-bottom: 20px;
}

/* 注册链接区域样式 */
.register-link {
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
}

/* 注册链接样式 */
.register-link a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

/* 注册链接悬停效果 */
.register-link a:hover {
  text-decoration: underline;
}

/* 登录切换区域样式（管理员入口） */
.login-switch {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 20px;
  gap: 12px;
}

/* 登录切换分隔线样式 */
.login-switch-divider {
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, var(--border-color) 50%, transparent 100%);
}

/* 登录切换链接样式 */
.login-switch-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.08) 0%, rgba(99, 102, 241, 0.08) 100%);
  color: var(--primary-color);
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-fast);
  border: 1px solid rgba(59, 130, 246, 0.2);
}

/* 登录切换链接悬停效果 */
.login-switch-link:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(99, 102, 241, 0.15) 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
  text-decoration: none;
}

/* 登录切换链接图标样式 */
.login-switch-link .el-icon {
  font-size: 14px;
}

/* 加载中文字样式 */
.loading-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* 验证码输入框特殊样式 */
.captcha-wrapper {
  display: flex;
  gap: 12px;
}

/* 验证码输入框 */
.captcha-input {
  flex: 1;
  padding-right: 16px;
}

/* 验证码文本容器 */
.captcha-text-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
}

/* 验证码文本展示 */
.captcha-text {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 110px;
  height: 44px;
  border-radius: var(--border-radius-sm);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all var(--transition-fast);
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 4px;
  color: var(--text-primary);
  user-select: none;
  font-family: 'Courier New', monospace;
}

/* 验证码文本悬停效果 */
.captcha-text:hover {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1), 0 2px 8px rgba(37, 99, 235, 0.15);
  transform: translateY(-1px);
}

/* 验证码文本点击效果 */
.captcha-text:active {
  transform: translateY(0);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
}

/* 验证码刷新文字 */
.captcha-refresh {
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color var(--transition-fast);
}

/* 验证码刷新文字悬停效果 */
.captcha-refresh:hover {
  color: var(--primary-color);
}

/* 响应式布局 - 移动端适配 */
@media (max-width: 768px) {
  /* 移动端单列布局 */
  .login-container {
    grid-template-columns: 1fr;
    padding: 20px;
    gap: 24px;
  }
  
  /* 移动端左侧区域内边距调整 */
  .login-left {
    padding: 40px 24px;
  }
  
  /* 移动端登录卡片内边距调整 */
  .login-card {
    padding: 32px 24px;
  }
  
  /* 移动端登录标题字号调整 */
  .login-title {
    font-size: 24px;
  }
}
</style>
