// 应用入口文件
// 功能：初始化 Vue 应用实例，注册插件（Pinia、Element Plus、路由），
// 全局注册图标组件，并挂载应用到 DOM
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router/index.js'
import './style.css'
import {
  // 外卖盒图标
  TakeawayBox,
  // 文档图标
  Document,
  // 时钟图标
  Clock,
  // 上传图标
  Upload,
  // 铃铛图标
  Bell,
  // 设置图标
  Setting,
  // 用户图标
  User,
  // 房屋图标
  House,
  // 手机图标
  Cellphone,
  // 搜索图标
  Search,
  // 加号图标
  Plus,
  // 编辑图标
  Edit,
  // 删除图标
  Delete,
  // 勾选图标
  Check,
  // 相机图标
  Camera,
  // 打开文件夹图标
  FolderOpened,
  // 信息填充图标
  InfoFilled,
  // 电话图标
  Phone,
  // 锁图标
  Lock,
  // 查看图标
  View,
  // 隐藏图标
  Hide,
  // 右箭头图标
  ArrowRight,
  // 盒子图标
  Box,
  // 视频播放图标
  VideoPlay,
  // 左箭头图标
  ArrowLeft,
  // 消息图标
  Message
} from '@element-plus/icons-vue'

// 创建 Vue 应用实例
const app = createApp(App)

// 创建 Pinia 状态管理实例，并注册持久化插件
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

// 注册 Pinia 插件
app.use(pinia)
// 注册 Element Plus UI 组件库
app.use(ElementPlus)
// 注册路由
app.use(router)

// 全局注册 Element Plus 图标组件
app.component('TakeawayBox', TakeawayBox)
app.component('Document', Document)
app.component('Clock', Clock)
app.component('Upload', Upload)
app.component('Bell', Bell)
app.component('Setting', Setting)
app.component('User', User)
app.component('House', House)
app.component('Cellphone', Cellphone)
app.component('Search', Search)
app.component('Plus', Plus)
app.component('Edit', Edit)
app.component('Delete', Delete)
app.component('Check', Check)
app.component('Camera', Camera)
app.component('FolderOpened', FolderOpened)
app.component('InfoFilled', InfoFilled)
app.component('Phone', Phone)
app.component('Lock', Lock)
app.component('View', View)
app.component('Hide', Hide)
app.component('ArrowRight', ArrowRight)
app.component('Box', Box)
app.component('VideoPlay', VideoPlay)
app.component('ArrowLeft', ArrowLeft)
app.component('Message', Message)

// 将应用挂载到 #app 元素上
app.mount('#app')
