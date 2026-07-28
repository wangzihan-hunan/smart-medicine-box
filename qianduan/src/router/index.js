// 路由配置模块
// 功能：定义应用的所有路由规则，包括页面路径、组件映射、
// 路由守卫（权限控制）、页面标题、滚动行为、进度条等
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

// 页面标题映射表
// key: 路由name, value: 页面标题
const PAGE_TITLES = {
  Home: '首页',
  Login: '登录',
  Register: '注册',
  Profile: '个人中心',
  Medicines: '药品管理',
  Prescriptions: '药方上传',
  Reminders: '用药提醒',
  Commands: '指令转发',
  Devices: '设备管理',
  Plans: '套餐中心',
  Logs: '操作日志',
  Notifications: '消息通知',
  About: '关于我们',
  Help: '帮助中心',
  AdminLogin: '管理员登录',
  AdminDashboard: '管理后台',
  AdminUsers: '用户管理',
  AdminStats: '数据统计',
  AdminAlerts: '告警管理',
  AdminLogs: '系统日志',
  NotFound: '页面未找到'
}

// 路由配置数组
// 每个路由对象包含：path（路径）、name（名称）、component（组件）、meta（元信息）
// meta.requiresAuth: 需要登录才能访问
// meta.guest: 仅游客可访问（已登录用户自动跳转）
// meta.requiresAdmin: 需要管理员权限
// meta.title: 页面标题
// meta.requiredPlan: 需要的套餐级别（预留，用于细粒度权限控制）
const routes = [
  // 首页
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { title: '首页' }
  },
  // 登录页（仅游客）
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { guest: true, title: '登录' }
  },
  // 注册页（仅游客）
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { guest: true, title: '注册' }
  },
  // 个人资料页（需登录）
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true, title: '个人中心' }
  },
  // 药品管理页（需登录）
  {
    path: '/medicines',
    name: 'Medicines',
    component: () => import('../views/Medicines.vue'),
    meta: { requiresAuth: true, title: '药品管理' }
  },
  // 处方管理页（需登录）
  {
    path: '/prescriptions',
    name: 'Prescriptions',
    component: () => import('../views/Prescriptions.vue'),
    meta: { requiresAuth: true, title: '药方上传' }
  },
  // 用药提醒页（需登录）
  {
    path: '/reminders',
    name: 'Reminders',
    component: () => import('../views/Reminders.vue'),
    meta: { requiresAuth: true, title: '用药提醒' }
  },
  // 设备指令页（需登录）
  {
    path: '/commands',
    name: 'Commands',
    component: () => import('../views/Commands.vue'),
    meta: { requiresAuth: true, title: '指令转发' }
  },
  // 设备管理页（需登录）
  {
    path: '/devices',
    name: 'Devices',
    component: () => import('../views/Devices.vue'),
    meta: { requiresAuth: true, title: '设备管理' }
  },
  // 套餐计划页（需登录）
  {
    path: '/plans',
    name: 'Plans',
    component: () => import('../views/Plans.vue'),
    meta: { requiresAuth: true, title: '套餐中心' }
  },
  // 日志页（需登录）
  {
    path: '/logs',
    name: 'Logs',
    component: () => import('../views/Logs.vue'),
    meta: { requiresAuth: true, title: '操作日志' }
  },
  // 通知页（需登录）
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('../views/Notifications.vue'),
    meta: { requiresAuth: true, title: '消息通知' }
  },
  // 关于页
  {
    path: '/about',
    name: 'About',
    component: () => import('../views/About.vue'),
    meta: { title: '关于我们' }
  },
  // 帮助页（需登录）
  {
    path: '/help',
    name: 'Help',
    component: () => import('../views/Help.vue'),
    meta: { requiresAuth: true, title: '帮助中心' }
  },
  // 管理员登录页（仅游客）
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/AdminLogin.vue'),
    meta: { guest: true, title: '管理员登录' }
  },
  // 管理员后台布局（需管理员权限）
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAdmin: true, title: '管理后台' },
    redirect: '/admin/dashboard',
    // 子路由 - 自动继承父路由的requiresAdmin权限
    children: [
      // 管理员仪表盘
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('../views/AdminDashboard.vue'),
        meta: { title: '管理后台' }
      },
      // 用户管理
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('../views/AdminUsers.vue'),
        meta: { title: '用户管理' }
      },
      // 统计数据
      {
        path: 'stats',
        name: 'AdminStats',
        component: () => import('../views/AdminStats.vue'),
        meta: { title: '数据统计' }
      },
      // 告警管理
      {
        path: 'alerts',
        name: 'AdminAlerts',
        component: () => import('../views/AdminAlerts.vue'),
        meta: { title: '告警管理' }
      },
      // 日志管理
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('../views/AdminLogs.vue'),
        meta: { title: '系统日志' }
      }
    ]
  },
  // 404页面 - 必须放在最后，匹配所有未命中的路径
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
    meta: { title: '页面未找到' }
  }
]

// 简易进度条状态管理
// 避免引入额外依赖，使用原生DOM操作实现顶部进度条
let progressTimer = null
const showProgress = () => {
  // 如果已有进度条，先清除
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
  // 创建进度条元素
  let bar = document.getElementById('router-progress-bar')
  if (!bar) {
    bar = document.createElement('div')
    bar.id = 'router-progress-bar'
    bar.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      height: 3px;
      background: linear-gradient(90deg, #667eea, #764ba2);
      z-index: 9999;
      transition: width 0.3s ease;
      width: 0%;
    `
    document.body.appendChild(bar)
  }
  // 渐进式加载动画
  let progress = 0
  bar.style.width = '0%'
  bar.style.opacity = '1'
  progressTimer = setInterval(() => {
    progress += Math.random() * 15
    if (progress > 80) progress = 80
    bar.style.width = progress + '%'
  }, 200)
}

// 隐藏并完成进度条
const finishProgress = () => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
  const bar = document.getElementById('router-progress-bar')
  if (bar) {
    bar.style.width = '100%'
    setTimeout(() => {
      bar.style.opacity = '0'
      setTimeout(() => {
        bar.style.width = '0%'
      }, 300)
    }, 200)
  }
}

// 创建路由实例
// history: 使用 HTML5 History 模式
// scrollBehavior: 页面滚动行为，有保存的位置则恢复，否则滚到顶部
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || { top: 0 }
  }
})

// 全局前置守卫：路由跳转前的权限校验
// 使用 return 方式替代 next()，避免死循环风险（Vue Router 4 推荐写法）
// 处理逻辑（按优先级）：
// 1. 需要管理员权限的页面 → 检查登录状态 + 管理员角色
// 2. 需要登录的页面 → 检查登录状态
// 3. 仅游客可访问的页面（登录/注册）→ 已登录则跳转
// 4. 管理员访问首页 → 跳转到管理后台
// 5. 其他情况 → 正常跳转
router.beforeEach((to, from) => {
  const authStore = useAuthStore()

  // 显示顶部进度条
  showProgress()

  // 1. 需要管理员权限的路由（含子路由继承的meta）
  if (to.matched.some(record => record.meta.requiresAdmin)) {
    if (!authStore.isLoggedIn) {
      // 未登录 → 跳转管理员登录页，保留重定向地址
      return { path: '/admin/login', query: { redirect: to.fullPath } }
    }
    if (!authStore.isAdmin) {
      // 已登录但不是管理员 → 跳转首页
      return '/'
    }
    // 是管理员 → 允许通过
    return true
  }

  // 2. 需要登录才能访问的页面
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!authStore.isLoggedIn) {
      // 未登录 → 跳转登录页，保留重定向地址
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    // 已登录 → 允许通过
    return true
  }

  // 3. 仅游客可访问的页面（登录/注册）
  if (to.matched.some(record => record.meta.guest)) {
    if (authStore.isLoggedIn) {
      // 已登录 → 管理员去管理后台，普通用户去首页
      return authStore.isAdmin ? '/admin' : '/'
    }
    // 未登录 → 允许通过
    return true
  }

  // 4. 管理员访问首页 → 自动跳转管理后台
  if (to.path === '/' && authStore.isLoggedIn && authStore.isAdmin) {
    return '/admin'
  }

  // 5. 其他情况 → 正常跳转
  return true
})

// 全局后置钩子：路由跳转完成后
// 功能：设置页面标题、完成进度条
router.afterEach((to) => {
  // 设置页面标题
  const title = to.meta.title || PAGE_TITLES[to.name] || '智能药箱'
  document.title = `${title} - 智能药箱管理系统`

  // 完成进度条
  finishProgress()
})

// 路由错误处理：懒加载组件失败等情况
router.onError((error) => {
  console.error('路由错误:', error)
  // 完成进度条，避免卡住
  finishProgress()
  // 显示错误提示（使用原生alert，避免引入额外依赖）
  alert('页面加载失败，请刷新重试')
})

// 导出路由实例
export default router
