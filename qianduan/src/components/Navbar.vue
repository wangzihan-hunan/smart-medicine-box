<template>
  <nav class="navbar" :class="{ 'navbar-scrolled': isScrolled }">
    <div class="navbar-container">
      <div class="navbar-brand">
        <router-link :to="authStore.isAdmin ? '/admin' : '/' " class="brand-link">
          <el-icon class="brand-icon"><TakeawayBox /></el-icon>
          <span class="brand-text">智能药箱</span>
        </router-link>
      </div>
      
      <div class="navbar-menu" :class="{ 'navbar-menu-active': isMenuOpen }">
        <template v-for="item in navItems" :key="item.path">
          <el-badge
            v-if="item.path === '/notifications' && unreadCount > 0"
            :value="unreadCount"
            :max="99"
            class="nav-badge"
          >
            <router-link
              :to="item.path"
              class="nav-link"
              :class="{ 'nav-link-active': $route.path === item.path }"
              @click="isMenuOpen = false"
            >
              <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
              <span class="nav-text">{{ item.label }}</span>
            </router-link>
          </el-badge>
          <router-link
            v-else
            :to="item.path"
            class="nav-link"
            :class="{ 'nav-link-active': $route.path === item.path }"
            @click="isMenuOpen = false"
          >
            <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
            <span class="nav-text">{{ item.label }}</span>
          </router-link>
        </template>
        
        <div class="navbar-divider"></div>
        
        <div v-if="authStore.isLoggedIn" class="navbar-user">
          <router-link to="/profile" class="user-link" @click="isMenuOpen = false">
            <span class="user-avatar">{{ authStore.user?.name ? authStore.user.name.charAt(0) : 'U' }}</span>
            <span class="user-name">{{ authStore.user?.name || '用户' }}</span>
          </router-link>
          <button class="logout-btn" @click="handleLogout">
            <span>退出</span>
          </button>
        </div>
        
        <div v-else class="navbar-auth">
          <router-link to="/login" class="auth-link" @click="isMenuOpen = false">登录</router-link>
          <router-link to="/register" class="auth-btn" @click="isMenuOpen = false">注册</router-link>
        </div>
      </div>
      
      <button class="menu-toggle" @click="isMenuOpen = !isMenuOpen" :class="{ 'menu-toggle-active': isMenuOpen }">
        <span class="menu-icon"></span>
        <span class="menu-icon"></span>
        <span class="menu-icon"></span>
      </button>
    </div>
  </nav>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { notificationAPI } from '../utils/api'

const router = useRouter()
const authStore = useAuthStore()
const isMenuOpen = ref(false)
const isScrolled = ref(false)
// 未读通知数量，用于导航栏角标显示
const unreadCount = ref(0)

import { House, TakeawayBox, Document, Clock, Cellphone, InfoFilled, User, Warning, DataAnalysis, Bell, QuestionFilled } from '@element-plus/icons-vue'

const navItems = computed(() => {
  if (authStore.isAdmin) {
    return [
      { path: '/admin', label: '管理后台', icon: House },
      { path: '/admin/users', label: '用户管理', icon: User },
      { path: '/admin/alerts', label: '告警管理', icon: Warning },
      { path: '/admin/stats', label: '数据统计', icon: DataAnalysis }
    ]
  }
  return [
      { path: '/', label: '首页', icon: House },
      { path: '/medicines', label: '药品管理', icon: TakeawayBox },
      { path: '/prescriptions', label: '药方上传', icon: Document },
      { path: '/reminders', label: '用药提醒', icon: Clock },
      { path: '/commands', label: '指令转发', icon: Cellphone },
      { path: '/notifications', label: '消息通知', icon: Bell },
      { path: '/help', label: '帮助反馈', icon: QuestionFilled },
      { path: '/about', label: '关于我们', icon: InfoFilled }
    ]
})

const userInitial = ref('U')

async function fetchUser() {
  if (authStore.isLoggedIn && !authStore.user) {
    await authStore.fetchUserInfo()
  }
  if (authStore.user) {
    userInitial.value = authStore.user.name ? authStore.user.name.charAt(0) : 'U'
  }
}

// 获取未读通知数量，用于导航栏角标
async function fetchUnreadCount() {
  if (!authStore.isLoggedIn) return
  try {
    const data = await notificationAPI.getNotificationStats()
    if (data && typeof data.unreadCount === 'number') {
      unreadCount.value = data.unreadCount
    }
  } catch (error) {
    console.error('获取未读通知数失败:', error)
  }
}

// 监听通知更新事件：新通知到达或已读状态同步时刷新角标
function handleNotificationUpdate(e) {
  const payload = e.detail
  if (!payload) return

  if (payload.action === 'NEW') {
    unreadCount.value++
  } else if (payload.action === 'READ') {
    // 全部已读时后端同步 READ 动作，角标归零
    unreadCount.value = 0
  } else {
    // 其他情况重新拉取准确数量
    fetchUnreadCount()
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function handleResize() {
  if (window.innerWidth > 768) {
    isMenuOpen.value = false
  }
}

function handleScroll() {
  isScrolled.value = window.scrollY > 20
}

onMounted(() => {
  fetchUser()
  fetchUnreadCount()
  window.addEventListener('resize', handleResize)
  window.addEventListener('scroll', handleScroll)
  window.addEventListener('notification:update', handleNotificationUpdate)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('notification:update', handleNotificationUpdate)
})
</script>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  z-index: 1000;
  transition: all var(--transition-normal);
}

.navbar-scrolled {
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}

.navbar-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.navbar-brand {
  flex-shrink: 0;
}

.brand-link {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--text-primary);
  transition: opacity var(--transition-fast);
}

.brand-link:hover {
  opacity: 0.85;
}

.brand-icon {
  font-size: 28px;
}

.brand-text {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.02em;
}

.navbar-menu {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  text-decoration: none;
  color: var(--text-secondary);
  border-radius: var(--border-radius-sm);
  transition: all var(--transition-fast);
  font-size: 14px;
  font-weight: 500;
  position: relative;
}

.nav-link::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  background: var(--primary-color);
  transition: width var(--transition-fast);
}

.nav-link:hover {
  color: var(--primary-color);
  background: var(--primary-50);
}

.nav-link:hover::before {
  width: 40%;
}

.nav-link-active {
  color: var(--primary-color);
  background: var(--primary-50);
}

.nav-link-active::before {
  width: 40%;
}

.nav-icon {
  font-size: 16px;
}

.navbar-divider {
  width: 1px;
  height: 24px;
  background: var(--border-color);
  margin: 0 8px;
}

.navbar-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-link {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: var(--text-primary);
  padding: 6px 12px;
  border-radius: var(--border-radius-sm);
  transition: all var(--transition-fast);
}

.user-link:hover {
  background: rgba(0, 0, 0, 0.04);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  transition: transform var(--transition-fast);
}

.user-link:hover .user-avatar {
  transform: scale(1.05);
}

.user-name {
  font-size: 14px;
  font-weight: 500;
}

.logout-btn {
  background: none;
  border: none;
  padding: 8px 16px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border-radius: var(--border-radius-sm);
  transition: all var(--transition-fast);
}

.logout-btn:hover {
  color: var(--danger-color);
  background: var(--danger-50);
}

.navbar-auth {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auth-link {
  text-decoration: none;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: var(--border-radius-sm);
  transition: all var(--transition-fast);
}

.auth-link:hover {
  color: var(--primary-color);
  background: var(--primary-50);
}

.auth-btn {
  text-decoration: none;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  color: white;
  padding: 8px 20px;
  border-radius: var(--border-radius-sm);
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
}

.auth-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.4);
}

.menu-toggle {
  display: none;
  flex-direction: column;
  gap: 5px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: var(--border-radius-sm);
  transition: background var(--transition-fast);
}

.menu-toggle:hover {
  background: rgba(0, 0, 0, 0.04);
}

.menu-icon {
  width: 24px;
  height: 2px;
  background: var(--text-primary);
  border-radius: 1px;
  transition: all var(--transition-fast);
}

.menu-toggle-active .menu-icon:nth-child(1) {
  transform: rotate(45deg) translateY(7px);
}

.menu-toggle-active .menu-icon:nth-child(2) {
  opacity: 0;
}

.menu-toggle-active .menu-icon:nth-child(3) {
  transform: rotate(-45deg) translateY(-7px);
}

/* 导航栏通知角标：与链接对齐 */
.nav-badge :deep(.el-badge__content) {
  top: 6px;
  right: 2px;
}

@media (max-width: 768px) {
  .navbar-menu {
    position: fixed;
    top: 64px;
    left: 0;
    right: 0;
    background: rgba(255, 255, 255, 0.98);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    flex-direction: column;
    padding: 16px;
    gap: 0;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
    transform: translateY(-100%);
    opacity: 0;
    visibility: hidden;
    transition: all var(--transition-normal);
  }
  
  .navbar-menu-active {
    transform: translateY(0);
    opacity: 1;
    visibility: visible;
  }
  
  .nav-link {
    width: 100%;
    justify-content: flex-start;
    padding: 12px 16px;
    font-size: 15px;
  }
  
  .navbar-divider {
    width: 100%;
    height: 1px;
    margin: 12px 0;
  }
  
  .navbar-user,
  .navbar-auth {
    width: 100%;
    justify-content: center;
    padding: 8px 0;
  }
  
  .menu-toggle {
    display: flex;
  }
}
</style>
