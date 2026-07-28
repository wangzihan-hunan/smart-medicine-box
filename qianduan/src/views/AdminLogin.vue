<template>
  <!-- 管理员登录页面主容器 -->
  <div class="admin-login-page">
    <!-- 背景装饰层 -->
    <div class="admin-login-bg"></div>
    <!-- 登录容器（居中定位） -->
    <div class="admin-login-container">
      <!-- 登录卡片主体 -->
      <div class="admin-login-card">
        <!-- 品牌标识区域 -->
        <div class="admin-brand">
          <!-- 品牌图标 -->
          <div class="admin-brand-icon">
            <el-icon><Setting /></el-icon>
          </div>
          <!-- 品牌标题 -->
          <h1 class="admin-brand-title">管理后台</h1>
          <!-- 品牌描述 -->
          <p class="admin-brand-desc">智能药箱系统管理中心</p>
        </div>
        
        <!-- 登录表单 -->
        <form @submit.prevent="handleLogin" class="admin-login-form">
          <!-- 手机号输入框组 -->
          <div class="admin-form-group">
            <!-- 手机号输入框 -->
            <el-input 
              v-model="form.phone" 
              placeholder="请输入管理员手机号"
              prefix-icon="Phone"
              size="large"
              :validate-event="false"
              @focus="clearError('phone')"
            />
            <!-- 手机号错误提示 -->
            <span v-if="errors.phone" class="admin-error-text">{{ errors.phone }}</span>
          </div>
          
          <!-- 密码输入框组 -->
          <div class="admin-form-group">
            <!-- 密码输入框 -->
            <el-input 
              v-model="form.password" 
              type="password" 
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              :validate-event="false"
              @focus="clearError('password')"
            />
            <!-- 密码错误提示 -->
            <span v-if="errors.password" class="admin-error-text">{{ errors.password }}</span>
          </div>
          
          <!-- 表单选项（记住我） -->
          <div class="admin-form-options">
            <el-checkbox v-model="rememberMe" size="small">记住我</el-checkbox>
          </div>
          
          <!-- 登录按钮 -->
          <el-button 
            type="primary" 
            size="large" 
            class="admin-login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </form>
        
        <!-- 页脚版权信息 -->
        <div class="admin-footer">
          <span>智能药箱管理系统 v1.0</span>
        </div>
        
        <!-- 切换到普通用户登录入口 -->
        <div class="admin-switch">
          <!-- 分割线 -->
          <span class="admin-switch-divider"></span>
          <!-- 普通用户登录链接 -->
          <router-link to="/login" class="admin-switch-link">
            <el-icon><User /></el-icon>
            <span>普通用户登录</span>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 引入Vue响应式API
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
// 引入Vue路由
import { useRouter, useRoute } from 'vue-router'
// 引入Element Plus图标组件
import { Setting, Phone, Lock, User } from '@element-plus/icons-vue'
// 引入管理员API和token工具函数
import { adminAPI, removeToken } from '../utils/api'
// 引入认证状态管理
import { useAuthStore } from '../stores/auth'

// 路由实例
const router = useRouter()
// 当前路由信息，用于获取重定向参数
const route = useRoute()
// 认证状态管理实例
const authStore = useAuthStore()
// 登录按钮加载状态
const loading = ref(false)
// 是否记住我
const rememberMe = ref(false)

// 登录表单数据
const form = reactive({
  // 手机号
  phone: '',
  // 密码
  password: ''
})

// 表单验证错误信息
const errors = reactive({
  // 手机号错误信息
  phone: '',
  // 密码错误信息
  password: ''
})

/**
 * 清除指定字段的错误信息
 * @param {string} field - 字段名（phone/password）
 */
function clearError(field) {
  errors[field] = ''
}

/**
 * 验证登录表单
 * @returns {boolean} 表单是否通过验证
 */
function validateForm() {
  // 验证结果标志
  let isValid = true
  // 先清除所有错误信息
  errors.phone = ''
  errors.password = ''
  
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
  
  return isValid
}

/**
 * 处理登录操作
 * 验证表单 -> 清除旧token -> 调用登录接口 -> 保存token -> 获取用户信息 -> 跳转管理后台
 */
async function handleLogin() {
  // 表单验证不通过则直接返回
  if (!validateForm()) return
  
  // 清除旧的token和用户状态
  removeToken()
  authStore.logout()
  console.log('[AdminLogin] 已清除旧token，准备登录:', form.phone)
  
  // 设置加载状态
  loading.value = true
  
  try {
    // 调用管理员登录接口
    const response = await adminAPI.login(form)
    console.log('[AdminLogin] 登录响应:', response)
    
    // 登录成功，获取到accessToken
    if (response && response.accessToken) {
      console.log('[AdminLogin] 获取到token，保存并获取用户信息')
      // 保存token到状态管理
      authStore.login(response.accessToken)
      // 获取用户信息
      const userInfo = await authStore.fetchUserInfo()
      console.log('[AdminLogin] 用户信息:', userInfo)

      // 检查是否需要强制修改密码
      if (response.mustChangePassword) {
        ElMessage.warning('首次登录请先修改密码')
        router.push('/admin/profile')
        return
      }

      // 跳转到之前访问的页面或管理后台首页
      const redirectPath = route.query.redirect || '/admin'
      router.push(redirectPath)
    } else {
      // 响应中没有token，登录失败
      console.error('[AdminLogin] 响应中没有accessToken')
      errors.password = '登录失败，请检查账号密码'
    }
  } catch (error) {
    // 捕获登录异常
    console.error('[AdminLogin] 登录错误:', error)
    console.error('[AdminLogin] 错误详情:', {
      message: error.message,
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      config: error.config?.url
    })
    // 根据错误状态码显示不同提示
    if (error.response?.status === 403 || error.message?.includes('权限')) {
      errors.password = '账号权限不足，请确认使用管理员账号登录'
    } else {
      errors.password = error.message || '登录失败，请检查网络连接'
    }
  } finally {
    // 无论成功失败，都关闭加载状态
    loading.value = false
  }
}
</script>

<style scoped>
/* 登录页面主容器 - 全屏居中布局 */
.admin-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: #0f172a;
}

/* 登录页面背景装饰 - 渐变光晕效果 */
.admin-login-bg {
  position: absolute;
  inset: 0;
  background: 
    radial-gradient(ellipse at 20% 20%, rgba(59, 130, 246, 0.15) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 80%, rgba(168, 85, 247, 0.15) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(34, 197, 94, 0.08) 0%, transparent 70%);
  background-size: cover;
}

/* 登录容器 - 限制最大宽度 */
.admin-login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 20px;
}

/* 登录卡片 - 毛玻璃效果 */
.admin-login-card {
  background: rgba(30, 41, 59, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 48px 40px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

/* 品牌标识区域 */
.admin-brand {
  text-align: center;
  margin-bottom: 40px;
}

/* 品牌图标样式 */
.admin-brand-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: white;
}

/* 品牌标题样式 */
.admin-brand-title {
  font-size: 28px;
  font-weight: 700;
  color: white;
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}

/* 品牌描述样式 */
.admin-brand-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
}

/* 登录表单布局 */
.admin-login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 表单项容器 */
.admin-form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* 错误提示文字样式 */
.admin-error-text {
  font-size: 12px;
  color: #ef4444;
}

/* 表单选项区域（记住我等） */
.admin-form-options {
  display: flex;
  justify-content: flex-start;
}

/* 复选框标签文字颜色 */
.admin-form-options :deep(.el-checkbox__label) {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

/* 登录按钮样式 */
.admin-login-btn {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  border: none;
  border-radius: 12px;
  margin-top: 8px;
}

/* 登录按钮悬停效果 */
.admin-login-btn:hover {
  background: linear-gradient(135deg, #2563eb 0%, #4f46e5 100%);
}

/* 页脚版权信息 */
.admin-footer {
  text-align: center;
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

/* 页脚文字样式 */
.admin-footer span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

/* 切换登录方式区域 */
.admin-switch {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 24px;
  gap: 12px;
}

/* 分割线样式 */
.admin-switch-divider {
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.1) 50%, transparent 100%);
}

/* 切换登录链接样式 */
.admin-switch-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-fast);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* 切换链接悬停效果 */
.admin-switch-link:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
  transform: translateY(-1px);
  text-decoration: none;
}

/* 切换链接图标大小 */
.admin-switch-link .el-icon {
  font-size: 14px;
}

/* 移动端响应式适配 */
@media (max-width: 480px) {
  /* 登录卡片内边距调整 */
  .admin-login-card {
    padding: 36px 24px;
  }
  
  /* 品牌标题字号缩小 */
  .admin-brand-title {
    font-size: 24px;
  }
}
</style>
