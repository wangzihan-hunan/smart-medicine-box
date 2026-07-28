<template>
  <div id="app" class="app-container">
    <el-container v-if="authStore.isLoggedIn">
      <el-header class="app-header">
        <div class="header-left">
          <el-icon class="logo-icon"><FirstAidKit /></el-icon>
          <span class="app-title">智能药箱助手</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ authStore.userInfo.name || authStore.userInfo.phone }}
              <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-container>
        <el-aside width="200px" class="app-aside">
          <el-menu
            :default-active="$route.path"
            router
            class="app-menu"
            background-color="#2c3e50"
            text-color="#ffffff"
            active-text-color="#409eff"
          >
            <el-menu-item index="/">
              <el-icon><HomeFilled /></el-icon>
              <span>首页</span>
            </el-menu-item>
            <el-menu-item index="/medicines">
              <el-icon><FirstAidKit /></el-icon>
              <span>药品管理</span>
            </el-menu-item>
            <el-menu-item index="/prescriptions">
              <el-icon><Document /></el-icon>
              <span>药方上传</span>
            </el-menu-item>
            <el-menu-item index="/reminders">
              <el-icon><AlarmClock /></el-icon>
              <span>用药提醒</span>
            </el-menu-item>
            <el-menu-item index="/commands">
              <el-icon><Cpu /></el-icon>
              <span>设备指令</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-main class="app-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
    <router-view v-else />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import {
  FirstAidKit, User, ArrowDown, SwitchButton,
  HomeFilled, Document, AlarmClock, Cpu
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.app-container {
  min-height: 100vh;
}
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #2c3e50;
  color: #fff;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.logo-icon {
  font-size: 24px;
}
.app-title {
  font-size: 18px;
  font-weight: bold;
}
.header-right {
  display: flex;
  align-items: center;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #fff;
  cursor: pointer;
}
.dropdown-icon {
  font-size: 12px;
}
.app-aside {
  background: #2c3e50;
}
.app-menu {
  border-right: none;
}
.app-main {
  background: #f5f7fa;
  padding: 20px;
}
</style>