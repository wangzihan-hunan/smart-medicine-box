<!-- 套餐中心页面模板 -->
<template>
  <!-- 套餐中心页面主容器 -->
  <div class="plans-page">
    <div class="page-container">
      <!-- 页面头部：标题和副标题 -->
      <div class="page-header">
        <h2 class="section-title">套餐中心</h2>
        <p class="section-subtitle">选择适合您的套餐，解锁更多功能</p>
      </div>
      
      <!-- 当前套餐状态卡片 -->
      <div v-if="myPlan" class="current-plan-card" :class="myPlan.plan.toLowerCase()">
        <div class="current-plan-header">
          <div class="current-plan-info">
            <div class="current-plan-label">当前套餐</div>
            <div class="current-plan-name">{{ myPlan.planName }}</div>
            <div class="current-plan-status">
              <!-- 套餐状态标签：生效中/已过期 -->
              <span :class="['status-badge', myPlan.active ? 'active' : 'expired']">
                {{ myPlan.active ? '生效中' : '已过期' }}
              </span>
              <!-- 到期时间（非免费套餐时显示） -->
              <span v-if="myPlan.expireAt && myPlan.plan !== 'FREE'" class="expire-info">
                到期：{{ formatDate(myPlan.expireAt) }}
              </span>
            </div>
          </div>
        </div>
        
        <!-- 套餐使用情况统计 -->
        <div class="current-plan-usage">
          <!-- AI识别使用量 -->
          <div class="usage-item">
            <div class="usage-label">AI识别</div>
            <div class="usage-value">
              {{ myPlan.aiUsed }} / {{ myPlan.aiLimit === -1 ? '不限' : myPlan.aiLimit }}
            </div>
            <!-- 使用进度条（有上限时显示） -->
            <div class="usage-bar" v-if="myPlan.aiLimit !== -1">
              <div 
                class="usage-progress" 
                :style="{ width: getUsagePercent(myPlan.aiUsed, myPlan.aiLimit) + '%' }"
              ></div>
            </div>
          </div>
          <!-- 药品数量限制 -->
          <div class="usage-item">
            <div class="usage-label">药品数量</div>
            <div class="usage-value">
              {{ myPlan.medicineLimit === -1 ? '不限' : myPlan.medicineLimit + ' 个' }}
            </div>
          </div>
          <!-- 提醒数量限制 -->
          <div class="usage-item">
            <div class="usage-label">提醒数量</div>
            <div class="usage-value">
              {{ myPlan.reminderLimit === -1 ? '不限' : myPlan.reminderLimit + ' 个' }}
            </div>
          </div>
        </div>
      </div>
      
      <!-- 套餐列表区域 -->
      <!-- 加载中状态 -->
      <div v-if="loading" class="loading-spinner">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      
      <!-- 套餐卡片网格 -->
      <div v-else class="plans-grid">
        <!-- 单个套餐卡片 -->
        <div 
          v-for="(plan, key) in plans" 
          :key="key"
          class="plan-card"
          :class="{ 
            'plan-popular': key === 'BASIC',
            'plan-current': myPlan && myPlan.plan === key
          }"
        >
          <!-- 最受欢迎标签（基础版显示） -->
          <div v-if="key === 'BASIC'" class="plan-badge">最受欢迎</div>
          
          <!-- 套餐头部：名称和价格 -->
          <div class="plan-header">
            <h3 class="plan-name">{{ plan.displayName }}</h3>
            <div class="plan-price">
              <span class="price-symbol">¥</span>
              <span class="price-value">{{ plan.price }}</span>
              <span class="price-unit">/月</span>
            </div>
          </div>
          
          <!-- 套餐功能特性列表 -->
          <div class="plan-features">
            <div class="feature-item">
              <el-icon class="feature-icon"><Check /></el-icon>
              <span>AI识别：{{ plan.aiLimit === 2147483647 ? '不限' : plan.aiLimit + '次/月' }}</span>
            </div>
            <div class="feature-item">
              <el-icon class="feature-icon"><Check /></el-icon>
              <span>药品管理：{{ plan.medicineLimit === 2147483647 ? '不限' : plan.medicineLimit + '个' }}</span>
            </div>
            <div class="feature-item">
              <el-icon class="feature-icon"><Check /></el-icon>
              <span>用药提醒：{{ plan.reminderLimit === 2147483647 ? '不限' : plan.reminderLimit + '个' }}</span>
            </div>
            <!-- 处方识别（非免费套餐） -->
            <div v-if="key !== 'FREE'" class="feature-item">
              <el-icon class="feature-icon"><Check /></el-icon>
              <span>处方识别</span>
            </div>
            <!-- 优先客服支持（专业版） -->
            <div v-if="key === 'PROFESSIONAL'" class="feature-item">
              <el-icon class="feature-icon"><Check /></el-icon>
              <span>优先客服支持</span>
            </div>
          </div>
          
          <!-- 订阅时长选择 -->
          <div class="plan-duration">
            <label class="duration-label">订阅时长</label>
            <div class="duration-options">
              <button 
                v-for="m in [1, 3, 12]"
                :key="m"
                class="duration-btn"
                :class="{ active: selectedDuration[key] === m }"
                @click="selectedDuration[key] = m"
              >
                {{ m === 12 ? '1年' : m + '个月' }}
                <!-- 年付优惠标签 -->
                <span v-if="m === 12" class="duration-discount">省20%</span>
              </button>
            </div>
          </div>
          
          <!-- 升级/当前套餐按钮 -->
          <button 
            class="plan-btn"
            :disabled="myPlan && myPlan.plan === key && myPlan.active"
            @click="handleUpgrade(key)"
          >
            {{ myPlan && myPlan.plan === key && myPlan.active ? '当前套餐' : '升级套餐' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 引入Vue核心API：响应式引用、响应式对象、生命周期钩子
import { ref, reactive, onMounted, onActivated } from 'vue'
// 引入Element Plus消息提示和确认对话框组件
import { ElMessage, ElMessageBox } from 'element-plus'
// 引入Element Plus图标组件
import { Loading, Check } from '@element-plus/icons-vue'
// 引入套餐相关的API接口
import { planAPI } from '../utils/api'

// 套餐列表加载状态
const loading = ref(false)
// 升级操作进行中状态
const upgrading = ref(false)
// 当前用户套餐信息
const myPlan = ref(null)
// 可用套餐列表
const plans = ref({})
// 各套餐选中的订阅时长
const selectedDuration = reactive({})

// 获取当前用户套餐信息
async function fetchMyPlan() {
  try {
    const response = await planAPI.getMyPlan()
    myPlan.value = response
  } catch (error) {
    console.error('获取套餐信息失败:', error)
  }
}

// 获取所有可用套餐列表
async function fetchPlans() {
  loading.value = true
  try {
    const response = await planAPI.getAllPlans()
    plans.value = response || {}
    // 初始化每个套餐的默认订阅时长为1个月
    Object.keys(plans.value).forEach(key => {
      selectedDuration[key] = 1
    })
  } catch (error) {
    console.error('获取套餐列表失败:', error)
    ElMessage.error('获取套餐列表失败')
  } finally {
    loading.value = false
  }
}

// 处理升级套餐
async function handleUpgrade(planKey) {
  const months = selectedDuration[planKey] || 1
  const plan = plans.value[planKey]
  if (!plan) return
  
  // 计算总价（年付8折）
  const totalPrice = (plan.price * months * (months === 12 ? 0.8 : 1)).toFixed(2)
  
  try {
    // 弹出确认对话框
    await ElMessageBox.confirm(
      `确定要升级到「${plan.displayName}」套餐吗？\n订阅时长：${months === 12 ? '1年' : months + '个月'}\n费用：¥${totalPrice}`,
      '确认升级',
      { confirmButtonText: '确认升级', cancelButtonText: '取消', type: 'info' }
    )
    
    upgrading.value = true
    await planAPI.upgrade(planKey, months)
    ElMessage.success('套餐升级成功！')
    // 刷新用户套餐信息
    await fetchMyPlan()
  } catch (error) {
    // 用户取消操作时不显示错误
    if (error !== 'cancel') {
      console.error('升级失败:', error)
      ElMessage.error(error.message || '升级失败')
    }
  } finally {
    upgrading.value = false
  }
}

// 计算使用量百分比
function getUsagePercent(used, limit) {
  if (!limit || limit === -1) return 0
  return Math.min(100, (used / limit) * 100)
}

// 格式化日期
function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

// 组件挂载时获取当前套餐和可用套餐列表
onMounted(() => {
  fetchMyPlan()
  fetchPlans()
})

// 页面从缓存恢复时重新加载数据（keep-alive）
onActivated(() => {
  fetchMyPlan()
  fetchPlans()
})
</script>

<style scoped>
/* 套餐中心页面主容器样式 */
.plans-page {
  padding-top: 80px;
  min-height: 100vh;
}

/* 页面头部样式 */
.page-header {
  text-align: center;
  margin-bottom: 32px;
}

/* 页面标题样式 */
.section-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

/* 页面副标题样式 */
.section-subtitle {
  font-size: 15px;
  color: var(--text-secondary);
}

/* 当前套餐卡片样式 */
.current-plan-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: var(--border-radius-lg);
  padding: 28px;
  color: white;
  margin-bottom: 40px;
  box-shadow: 0 10px 40px rgba(102, 126, 234, 0.3);
}

/* 免费套餐卡片背景色 */
.current-plan-card.free {
  background: linear-gradient(135deg, #95a5a6 0%, #7f8c8d 100%);
}

/* 基础版套餐卡片背景色 */
.current-plan-card.basic {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

/* 专业版套餐卡片背景色 */
.current-plan-card.professional {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

/* 当前套餐头部样式 */
.current-plan-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

/* 当前套餐标签样式 */
.current-plan-label {
  font-size: 13px;
  opacity: 0.8;
  margin-bottom: 4px;
}

/* 当前套餐名称样式 */
.current-plan-name {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 12px;
}

/* 当前套餐状态行样式 */
.current-plan-status {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

/* 状态徽章样式 */
.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

/* 生效中状态样式 */
.status-badge.active {
  background: rgba(255, 255, 255, 0.25);
}

/* 已过期状态样式 */
.status-badge.expired {
  background: rgba(239, 68, 68, 0.3);
}

/* 到期时间文字样式 */
.expire-info {
  opacity: 0.85;
}

/* 套餐使用量区域样式 */
.current-plan-usage {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

/* 使用量项样式 */
.usage-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 使用量标签样式 */
.usage-label {
  font-size: 12px;
  opacity: 0.8;
}

/* 使用量数值样式 */
.usage-value {
  font-size: 18px;
  font-weight: 600;
}

/* 使用进度条容器 */
.usage-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  overflow: hidden;
}

/* 使用进度条填充 */
.usage-progress {
  height: 100%;
  background: white;
  border-radius: 2px;
  transition: width 0.3s;
}

/* 加载中状态样式 */
.loading-spinner {
  text-align: center;
  padding: 60px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* 加载图标旋转动画 */
.loading-icon {
  animation: spin 1s linear infinite;
}

/* 旋转动画 */
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 套餐卡片网格布局 */
.plans-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  max-width: 1000px;
  margin: 0 auto;
}

/* 套餐卡片样式 */
.plan-card {
  background: white;
  border-radius: var(--border-radius-lg);
  padding: 32px 24px;
  border: 2px solid var(--border-color);
  position: relative;
  transition: all var(--transition-normal);
  display: flex;
  flex-direction: column;
}

/* 套餐卡片悬停效果 */
.plan-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--card-shadow-hover);
}

/* 最受欢迎套餐卡片样式 */
.plan-card.plan-popular {
  border-color: var(--primary-color);
}

/* 当前套餐卡片样式 */
.plan-card.plan-current {
  border-color: var(--success-color);
  background: linear-gradient(180deg, rgba(34, 197, 94, 0.03) 0%, white 100%);
}

/* 最受欢迎徽章样式 */
.plan-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--primary-color);
  color: white;
  padding: 4px 16px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

/* 套餐头部样式 */
.plan-header {
  text-align: center;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 24px;
}

/* 套餐名称样式 */
.plan-name {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 12px;
}

/* 套餐价格区域 */
.plan-price {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 2px;
}

/* 价格符号样式 */
.price-symbol {
  font-size: 18px;
  color: var(--text-secondary);
}

/* 价格数值样式 */
.price-value {
  font-size: 36px;
  font-weight: 700;
  color: var(--primary-color);
}

/* 价格单位样式 */
.price-unit {
  font-size: 14px;
  color: var(--text-muted);
}

/* 套餐功能特性区域 */
.plan-features {
  flex: 1;
  margin-bottom: 24px;
}

/* 功能特性项样式 */
.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  font-size: 14px;
  color: var(--text-primary);
}

/* 功能图标样式 */
.feature-icon {
  color: var(--success-color);
  font-size: 16px;
  flex-shrink: 0;
}

/* 订阅时长区域 */
.plan-duration {
  margin-bottom: 20px;
}

/* 时长标签样式 */
.duration-label {
  display: block;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

/* 时长选项容器 */
.duration-options {
  display: flex;
  gap: 8px;
}

/* 时长选项按钮样式 */
.duration-btn {
  flex: 1;
  padding: 8px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-sm);
  background: white;
  cursor: pointer;
  font-size: 13px;
  transition: all var(--transition-fast);
  position: relative;
}

/* 时长按钮悬停效果 */
.duration-btn:hover {
  border-color: var(--primary-color);
}

/* 选中的时长按钮样式 */
.duration-btn.active {
  border-color: var(--primary-color);
  background: var(--primary-50);
  color: var(--primary-color);
  font-weight: 500;
}

/* 优惠标签样式 */
.duration-discount {
  display: block;
  font-size: 10px;
  color: var(--danger-color);
  margin-top: 2px;
}

/* 套餐按钮样式 */
.plan-btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: var(--border-radius-sm);
  background: var(--primary-color);
  color: white;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

/* 套餐按钮悬停效果 */
.plan-btn:hover:not(:disabled) {
  background: var(--primary-dark);
}

/* 按钮禁用状态 */
.plan-btn:disabled {
  background: var(--border-color);
  color: var(--text-muted);
  cursor: not-allowed;
}

/* 平板响应式：单列布局 */
@media (max-width: 768px) {
  .current-plan-usage {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .plans-grid {
    grid-template-columns: 1fr;
  }
}
</style>
