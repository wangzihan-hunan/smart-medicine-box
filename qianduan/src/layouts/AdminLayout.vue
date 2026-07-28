<template>
  <div class="admin-layout" :class="{ 'sidebar-collapsed': sidebarCollapsed, 'mobile-open': mobileMenuOpen }">
    <!-- 移动端遮罩 -->
    <div class="admin-sidebar-overlay" @click="closeMobileMenu" v-if="mobileMenuOpen"></div>

    <aside class="admin-sidebar">
      <div class="admin-sidebar-header">
        <div class="admin-sidebar-logo">
          <el-icon><Setting /></el-icon>
          <span v-show="!sidebarCollapsed" class="logo-text">管理后台</span>
        </div>
        <el-button
          v-if="!isMobile"
          text
          size="small"
          class="admin-collapse-btn"
          @click="toggleSidebar"
        >
          <el-icon><component :is="sidebarCollapsed ? Expand : Fold" /></el-icon>
        </el-button>
      </div>

      <nav class="admin-sidebar-nav">
        <el-menu
          :default-active="activeMenu"
          :collapse="sidebarCollapsed"
          :collapse-transition="false"
          mode="vertical"
          background-color="transparent"
          text-color="rgba(255,255,255,0.7)"
          active-text-color="#fff"
          router
        >
          <el-menu-item index="/admin">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>
              <span>仪表盘</span>
            </template>
          </el-menu-item>

          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <template #title>
              <span>用户管理</span>
            </template>
          </el-menu-item>

          <el-menu-item index="/admin/alerts">
            <el-icon><Warning /></el-icon>
            <template #title>
              <span>告警管理</span>
            </template>
          </el-menu-item>

          <el-menu-item index="/admin/logs">
            <el-icon><Document /></el-icon>
            <template #title>
              <span>操作日志</span>
            </template>
          </el-menu-item>

          <el-menu-item index="/admin/stats">
            <el-icon><TrendCharts /></el-icon>
            <template #title>
              <span>数据统计</span>
            </template>
          </el-menu-item>
        </el-menu>
      </nav>

      <div class="admin-sidebar-footer">
        <div class="admin-user-info" :class="{ 'collapsed': sidebarCollapsed }">
          <el-avatar :size="32" class="admin-avatar">
            {{ authStore.user?.name?.charAt(0) || 'A' }}
          </el-avatar>
          <div v-show="!sidebarCollapsed" class="admin-user-text">
            <div class="admin-user-name">{{ authStore.user?.name || '管理员' }}</div>
            <div class="admin-user-role">管理员</div>
          </div>
        </div>
        <el-button
          text
          size="small"
          class="admin-logout-btn"
          :class="{ 'collapsed': sidebarCollapsed }"
          @click="handleLogout"
        >
          <el-icon><SwitchButton /></el-icon>
          <span v-show="!sidebarCollapsed">退出登录</span>
        </el-button>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-header">
        <div class="admin-header-left">
          <el-button
            v-if="isMobile"
            text
            size="small"
            class="admin-mobile-toggle"
            @click="openMobileMenu"
          >
            <el-icon><Expand /></el-icon>
          </el-button>
          <div class="admin-breadcrumb">
            <el-icon class="breadcrumb-icon"><component :is="currentMenuIcon" /></el-icon>
            <span class="admin-header-title">{{ pageTitle }}</span>
          </div>
        </div>
        <div class="admin-header-right">
          <span class="admin-current-time">{{ currentTime }}</span>
          <el-badge :value="notificationCount" :hidden="notificationCount === 0" class="admin-notification">
            <el-button text size="small">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
        </div>
      </header>

      <main class="admin-content">
        <router-view v-slot="{ Component }">
          <transition name="admin-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Setting,
  DataAnalysis,
  User,
  Warning,
  Document,
  TrendCharts,
  SwitchButton,
  Fold,
  Expand,
  Bell
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { adminAPI } from '../utils/api'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const sidebarCollapsed = ref(false)
const mobileMenuOpen = ref(false)
const isMobile = ref(false)
const currentTime = ref('')
const notificationCount = ref(0)
let timer = null
let resizeTimer = null

const menuMap = {
  '/admin': { title: '仪表盘', icon: DataAnalysis },
  '/admin/dashboard': { title: '仪表盘', icon: DataAnalysis },
  '/admin/users': { title: '用户管理', icon: User },
  '/admin/alerts': { title: '告警管理', icon: Warning },
  '/admin/logs': { title: '操作日志', icon: Document },
  '/admin/stats': { title: '数据统计', icon: TrendCharts }
}

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => {
  return menuMap[route.path]?.title || '管理后台'
})

const currentMenuIcon = computed(() => {
  return menuMap[route.path]?.icon || DataAnalysis
})

function updateTime() {
  currentTime.value = new Date().toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function handleResize() {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    const mobile = window.innerWidth <= 768
    isMobile.value = mobile
    if (mobile) {
      sidebarCollapsed.value = false
      mobileMenuOpen.value = false
    } else {
      mobileMenuOpen.value = false
    }
  }, 100)
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function openMobileMenu() {
  mobileMenuOpen.value = true
}

function closeMobileMenu() {
  mobileMenuOpen.value = false
}

function handleLogout() {
  authStore.logout()
  router.push('/admin/login')
}

async function fetchNotificationCount() {
  try {
    const data = await adminAPI.getLogStats()
    notificationCount.value = data.total || 0
  } catch (error) {
    console.error('获取日志数量失败:', error)
    notificationCount.value = 0
  }
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  handleResize()
  window.addEventListener('resize', handleResize)
  fetchNotificationCount()
})

onUnmounted(() => {
  clearInterval(timer)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #0f172a;
}

.admin-sidebar {
  width: 240px;
  min-width: 240px;
  background: #0f172a;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease, min-width 0.3s ease;
  overflow: hidden;
  position: relative;
  z-index: 100;
}

.admin-layout.sidebar-collapsed .admin-sidebar {
  width: 64px;
  min-width: 64px;
}

.admin-sidebar-header {
  height: 64px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.admin-sidebar-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 700;
  color: white;
  overflow: hidden;
  white-space: nowrap;
}

.admin-sidebar-logo .el-icon {
  font-size: 24px;
  color: #3b82f6;
  flex-shrink: 0;
}

.logo-text {
  transition: opacity 0.3s ease;
}

.admin-collapse-btn {
  color: rgba(255, 255, 255, 0.5);
  padding: 6px;
  flex-shrink: 0;
}

.admin-collapse-btn:hover {
  color: white;
  background: rgba(255, 255, 255, 0.1);
}

.admin-sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
  overflow-x: hidden;
}

.admin-sidebar-nav :deep(.el-menu) {
  border-right: none;
}

.admin-sidebar-nav :deep(.el-menu-item) {
  margin-bottom: 4px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
}

.admin-sidebar-nav :deep(.el-menu-item:hover) {
  background: rgba(59, 130, 246, 0.15);
}

.admin-sidebar-nav :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.25) 0%, rgba(99, 102, 241, 0.25) 100%);
  color: #fff;
  font-weight: 500;
}

.admin-sidebar-nav :deep(.el-menu-item .el-icon) {
  color: inherit;
}

.admin-sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.admin-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  overflow: hidden;
  white-space: nowrap;
}

.admin-user-info.collapsed {
  justify-content: center;
  margin-bottom: 8px;
}

.admin-avatar {
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  color: white;
  flex-shrink: 0;
}

.admin-user-text {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-user-name {
  font-size: 14px;
  font-weight: 600;
  color: white;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-user-role {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.admin-logout-btn {
  width: 100%;
  color: rgba(255, 255, 255, 0.7);
  justify-content: flex-start;
  padding-left: 0;
  overflow: hidden;
  white-space: nowrap;
}

.admin-logout-btn.collapsed {
  justify-content: center;
  padding-left: 0;
}

.admin-logout-btn:hover {
  color: #ef4444;
}

.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.admin-header {
  height: 64px;
  background: rgba(30, 41, 59, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.admin-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.admin-mobile-toggle {
  color: rgba(255, 255, 255, 0.7);
  font-size: 18px;
}

.admin-breadcrumb {
  display: flex;
  align-items: center;
  gap: 10px;
}

.breadcrumb-icon {
  color: #3b82f6;
  font-size: 18px;
}

.admin-header-title {
  font-size: 18px;
  font-weight: 600;
  color: white;
}

.admin-header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.admin-current-time {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  font-family: monospace;
}

.admin-notification {
  color: rgba(255, 255, 255, 0.7);
}

.admin-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  overflow-x: hidden;
}

.admin-fade-enter-active,
.admin-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.admin-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.admin-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.admin-sidebar-overlay {
  display: none;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .admin-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    width: 240px;
    min-width: 240px;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
    z-index: 200;
  }

  .admin-layout.mobile-open .admin-sidebar {
    transform: translateX(0);
  }

  .admin-layout.mobile-open .admin-sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 150;
  }

  .admin-sidebar-header {
    justify-content: flex-start;
  }

  .admin-collapse-btn {
    display: none;
  }

  .admin-header {
    padding: 0 16px;
  }

  .admin-content {
    padding: 16px;
  }

  .admin-current-time {
    display: none;
  }
}
</style>
