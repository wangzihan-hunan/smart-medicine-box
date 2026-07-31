<template>
  <div class="app-wrapper">
    <template v-if="!isAdminRoute">
      <Navbar />
      <main class="main-content">
        <router-view v-slot="{ Component }">
          <!--
            直接渲染组件，使用 :key 强制每次路由切换重新创建
            不使用 <transition>，因为 out-in 模式会等旧组件离开后才渲染新组件，
            导致登录↔注册切换时出现白屏，需要手动刷新才显示内容
          -->
          <component :is="Component" :key="$route.fullPath" />
        </router-view>
      </main>
      <Footer />
    </template>
    <template v-else>
      <router-view v-slot="{ Component }">
        <component :is="Component" :key="$route.fullPath" />
      </router-view>
    </template>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Navbar from './components/Navbar.vue'
import Footer from './components/Footer.vue'
import { useAuthStore } from './stores/auth'
import { connect as wsConnect } from './utils/websocket'
import { ElNotification } from 'element-plus'
import { alertWithSound, requestNotificationPermission, getNotificationPermission, PermissionStatus } from './utils/browserAlert'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 判断是否为管理后台路由
const isAdminRoute = computed(() => {
  return route.path.startsWith('/admin')
})

// 监听 401 未授权事件：清除 Pinia 状态并跳转登录页
function handleUnauthorized(e) {
  authStore.logout()
  const isAdmin = route.path.startsWith('/admin')
  const loginPath = isAdmin ? '/admin/login' : '/login'
  // 避免在登录页重复跳转
  if (route.path !== loginPath) {
    router.push({ path: loginPath, query: { redirect: route.fullPath } })
  }
}

// 监听实时通知推送事件：显示桌面通知、声音提醒并刷新未读计数
function handleWsNotification(e) {
  const payload = e.detail
  if (!payload) return

  // 根据 action 类型处理：NEW 表示新通知，READ 表示状态同步
  if (payload.action === 'NEW') {
    // 1. 页面内右上角浮层通知（Element Plus）
    ElNotification({
      title: payload.title || '新通知',
      message: payload.content || '',
      type: payload.type === 'MEDICINE_REMINDER' ? 'warning' : 'info',
      duration: 5000,
      position: 'top-right'
    })

    // 2. 系统级桌面通知 + 声音提醒
    // 优先针对用药提醒进行特殊配置（更大音量、更长响、需手动关闭）
    const isMedicineReminder = payload.type === 'MEDICINE_REMINDER'
    
    alertWithSound({
      title: payload.title || '智能药箱提醒',
      body: payload.content || '',
      soundEnabled: true,
      soundOptions: isMedicineReminder ? {
        frequency: 1000,
        duration: 0.25,
        volume: 0.5,
        repeat: 3,
        interval: 0.15
      } : {
        frequency: 800,
        duration: 0.15,
        volume: 0.3,
        repeat: 1,
        interval: 0.1
      },
      notificationOptions: {
        tag: `med-notification-${payload.id}`,
        requireInteraction: isMedicineReminder,
        onClick: () => {
          // 点击通知时跳转到通知中心或首页
          if (isMedicineReminder) {
            router.push('/')
          } else {
            router.push('/notifications')
          }
        }
      }
    })
  }

  // 派发全局事件供 Navbar / Notifications.vue 消费
  window.dispatchEvent(new CustomEvent('notification:update', {
    detail: payload
  }))
}

onMounted(() => {
  document.body.classList.add('app-loaded')
  window.addEventListener('api:unauthorized', handleUnauthorized)
  window.addEventListener('ws:notification', handleWsNotification)

  // 请求桌面通知权限（现代浏览器通常要求用户交互后才能请求，此处做首次尝试）
  if (getNotificationPermission() === PermissionStatus.DEFAULT) {
    requestNotificationPermission().then(perm => {
      if (perm === PermissionStatus.GRANTED) {
        console.log('[App] 通知权限已获取')
      }
    })
  }

  // 如果页面刷新后已有 Token（Pinia 从 sessionStorage 恢复），主动建立 WebSocket 连接
  if (authStore.isLoggedIn) {
    wsConnect()
  }
})

onUnmounted(() => {
  document.body.classList.remove('app-loaded')
  window.removeEventListener('api:unauthorized', handleUnauthorized)
  window.removeEventListener('ws:notification', handleWsNotification)
})
</script>

<style>
.app-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
}
</style>
