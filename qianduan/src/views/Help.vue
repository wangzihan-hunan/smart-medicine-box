<!-- 帮助与反馈页面模板 -->
<template>
  <!-- 帮助页面主容器 -->
  <div class="help-page">
    <div class="page-container">
      <h2 class="section-title">帮助与反馈</h2>
      
      <!-- 帮助内容区域：包含标签切换和内容展示 -->
      <div class="help-content">
        <!-- 帮助标签页导航：常见问题 / 意见反馈 -->
        <div class="help-tabs">
          <button 
            v-for="tab in tabs" 
            :key="tab.key"
            class="help-tab-btn"
            :class="{ 'tab-active': activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>
        
        <!-- 标签页内容区域 -->
        <div class="help-tab-content">
          <!-- 常见问题面板 -->
          <div v-if="activeTab === 'faq'" class="faq-section">
            <!-- FAQ列表：可折叠展开 -->
            <div class="faq-list">
              <div v-for="(faq, index) in faqList" :key="index" class="faq-item">
                <!-- 问题标题：点击展开/收起答案 -->
                <div class="faq-question" @click="toggleFaq(index)">
                  <el-icon class="faq-icon"><QuestionFilled /></el-icon>
                  <span class="faq-q-text">{{ faq.question }}</span>
                  <el-icon class="faq-arrow" :class="{ 'expanded': expandedFaq === index }"><ArrowDown /></el-icon>
                </div>
                <!-- 答案内容：展开时显示 -->
                <div v-show="expandedFaq === index" class="faq-answer">
                  {{ faq.answer }}
                </div>
              </div>
            </div>
          </div>
          
          <!-- 意见反馈面板 -->
          <div v-if="activeTab === 'feedback'" class="feedback-section">
            <!-- 提交反馈表单卡片 -->
            <div class="feedback-form-card">
              <h3 class="form-title">提交反馈</h3>
              <p class="form-desc">您的建议是我们改进的动力，感谢您的反馈！</p>
              
              <!-- 反馈表单 -->
              <form @submit.prevent="handleSubmit" class="feedback-form">
                <!-- 反馈类型选择 -->
                <div class="form-group">
                  <label class="form-label">反馈类型 <span class="required">*</span></label>
                  <div class="type-selector">
                    <button 
                      v-for="t in feedbackTypes" 
                      :key="t.value"
                      type="button"
                      class="type-btn"
                      :class="{ 'type-active': feedbackForm.type === t.value }"
                      @click="feedbackForm.type = t.value"
                    >
                      <el-icon><component :is="t.icon" /></el-icon>
                      <span>{{ t.label }}</span>
                    </button>
                  </div>
                </div>
                
                <!-- 反馈标题输入 -->
                <div class="form-group">
                  <label class="form-label">标题 <span class="required">*</span></label>
                  <input 
                    type="text" 
                    class="form-input" 
                    v-model="feedbackForm.title" 
                    placeholder="请简要描述问题或建议"
                    maxlength="200"
                  />
                  <span v-if="errors.title" class="error-text">{{ errors.title }}</span>
                </div>
                
                <!-- 详细描述输入 -->
                <div class="form-group">
                  <label class="form-label">详细描述 <span class="required">*</span></label>
                  <textarea 
                    class="form-textarea" 
                    v-model="feedbackForm.content" 
                    placeholder="请详细描述您遇到的问题或建议内容..."
                    rows="6"
                    maxlength="2000"
                  ></textarea>
                  <div class="textarea-footer">
                    <span v-if="errors.content" class="error-text">{{ errors.content }}</span>
                    <span class="char-count">{{ feedbackForm.content.length }} / 2000</span>
                  </div>
                </div>
                
                <!-- 联系方式输入（可选） -->
                <div class="form-group">
                  <label class="form-label">联系方式</label>
                  <input 
                    type="text" 
                    class="form-input" 
                    v-model="feedbackForm.contact" 
                    placeholder="可选，方便我们联系您（手机号/邮箱）"
                  />
                </div>
                
                <!-- 提交按钮 -->
                <div class="form-actions">
                  <button type="submit" class="btn-primary" :disabled="submitting">
                    <span v-if="submitting">提交中...</span>
                    <span v-else>提交反馈</span>
                  </button>
                </div>
              </form>
            </div>
            
            <!-- 我的反馈记录卡片 -->
            <div class="my-feedback-card">
              <div class="card-header">
                <h3 class="card-title">我的反馈</h3>
                <!-- 状态筛选标签 -->
                <div class="filter-tabs">
                  <button 
                    v-for="s in statusTabs" 
                    :key="s.value"
                    class="filter-tab"
                    :class="{ 'filter-active': currentStatus === s.value }"
                    @click="switchStatus(s.value)"
                  >
                    {{ s.label }}
                  </button>
                </div>
              </div>
              
              <!-- 加载中状态 -->
              <div v-if="feedbackLoading" class="loading-wrap">
                <el-icon class="loading-icon"><Loading /></el-icon>
                <span>加载中...</span>
              </div>
              
              <!-- 空状态 -->
              <div v-else-if="myFeedbacks.length === 0" class="empty-wrap">
                <el-icon class="empty-icon"><ChatDotRound /></el-icon>
                <p>暂无反馈记录</p>
              </div>
              
              <!-- 我的反馈列表 -->
              <div v-else class="my-feedback-list">
                <!-- 单条反馈记录 -->
                <div v-for="item in myFeedbacks" :key="item.id" class="feedback-item">
                  <div class="feedback-item-header">
                    <span class="feedback-type-tag" :class="'type-' + item.type">{{ getTypeLabel(item.type) }}</span>
                    <span class="feedback-status-tag" :class="'status-' + item.status">{{ getStatusLabel(item.status) }}</span>
                    <span class="feedback-date">{{ formatDate(item.createdAt) }}</span>
                  </div>
                  <div class="feedback-item-title">{{ item.title }}</div>
                  <div class="feedback-item-content">{{ item.content }}</div>
                  
                  <!-- 官方回复区域 -->
                  <div v-if="item.reply" class="feedback-reply">
                    <div class="reply-label">
                      <el-icon><Service /></el-icon>
                      <span>官方回复</span>
                      <span class="reply-date">{{ formatDate(item.repliedAt) }}</span>
                    </div>
                    <div class="reply-content">{{ item.reply }}</div>
                  </div>
                  
                  <!-- 反馈操作按钮 -->
                  <div class="feedback-item-actions">
                    <button class="text-btn danger" @click="deleteMyFeedback(item.id)">删除</button>
                  </div>
                </div>
                
                <!-- 加载更多按钮 -->
                <div v-if="hasMore" class="load-more">
                  <button class="btn-secondary" @click="loadMore" :disabled="loadingMore">
                    <span v-if="loadingMore">加载中...</span>
                    <span v-else>加载更多</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
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
// 引入反馈相关的API接口
import { feedbackAPI } from '../utils/api'
// 引入Element Plus图标组件
import { 
  QuestionFilled, ArrowDown, ChatDotRound, 
  HelpFilled, Lightning, Help,
  Service, Loading
} from '@element-plus/icons-vue'

// 当前激活的标签页：faq（常见问题）或 feedback（意见反馈）
const activeTab = ref('faq')

// 标签页配置
const tabs = [
  { key: 'faq', label: '常见问题' },
  { key: 'feedback', label: '意见反馈' }
]

// 常见问题列表数据
const faqList = [
  {
    question: '如何添加药品？',
    answer: '您可以在"药品管理"页面点击"添加药品"按钮，填写药品名称、分类、规格、生产厂家、库存等信息后保存即可。也可以通过药方上传功能，系统会自动识别药品信息。'
  },
  {
    question: '如何设置用药提醒？',
    answer: '在"用药提醒"页面点击"添加提醒"，选择药品、设置用药时间、剂量和频率，系统会按时发送提醒通知。您可以设置多次提醒，也支持按星期重复。'
  },
  {
    question: 'AI 药方识别如何使用？',
    answer: '在"药方上传"页面，点击"上传图片"，选择清晰的药方照片，系统会自动识别药方中的药品名称、用量等信息。识别结果可一键添加到药品库中。'
  },
  {
    question: '智能药箱设备如何连接？',
    answer: '请确保智能药箱已接通电源并连接网络。在"设备管理"页面点击"添加设备"，按照提示输入设备编号完成绑定。绑定成功后即可远程控制药箱。'
  },
  {
    question: '忘记密码怎么办？',
    answer: '在登录页面点击"忘记密码"，按照提示通过手机号验证码重置密码。如果您未绑定手机号，请联系客服协助处理。'
  },
  {
    question: '如何升级套餐？',
    answer: '在"个人中心"或"套餐"页面查看可用套餐，选择您需要的套餐后点击"升级"，按照提示完成支付即可。升级后立即生效。'
  }
]

// 当前展开的FAQ索引
const expandedFaq = ref(null)

// 切换FAQ的展开/收起状态
function toggleFaq(index) {
  expandedFaq.value = expandedFaq.value === index ? null : index
}

// 反馈类型选项配置
const feedbackTypes = [
  { value: 'BUG', label: '问题反馈', icon: HelpFilled },
  { value: 'SUGGESTION', label: '功能建议', icon: Lightning },
  { value: 'CONSULT', label: '使用咨询', icon: Help }
]

// 反馈状态筛选标签配置
const statusTabs = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '待处理' },
  { value: 'REPLIED', label: '已回复' },
  { value: 'RESOLVED', label: '已解决' }
]

// 反馈表单数据
const feedbackForm = reactive({
  type: 'BUG',
  title: '',
  content: '',
  contact: ''
})

// 表单错误信息
const errors = reactive({
  title: '',
  content: ''
})

// 提交中状态
const submitting = ref(false)

// 验证反馈表单
function validateForm() {
  errors.title = ''
  errors.content = ''
  let valid = true
  
  // 验证标题
  if (!feedbackForm.title.trim()) {
    errors.title = '请输入反馈标题'
    valid = false
  }
  
  // 验证详细描述
  if (!feedbackForm.content.trim()) {
    errors.content = '请输入详细描述'
    valid = false
  } else if (feedbackForm.content.trim().length < 10) {
    errors.content = '描述内容不能少于10个字'
    valid = false
  }
  
  return valid
}

// 提交反馈表单
async function handleSubmit() {
  if (!validateForm()) return
  
  submitting.value = true
  try {
    await feedbackAPI.submit(feedbackForm)
    ElMessage.success('反馈提交成功，感谢您的宝贵意见！')
    // 重置表单
    feedbackForm.title = ''
    feedbackForm.content = ''
    feedbackForm.contact = ''
    // 刷新我的反馈列表
    fetchMyFeedbacks()
  } catch (error) {
    ElMessage.error(error.message || '提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// 当前筛选的反馈状态
const currentStatus = ref('')
// 反馈列表加载状态
const feedbackLoading = ref(false)
// 我的反馈列表数据
const myFeedbacks = ref([])
// 当前页码
const currentPage = ref(0)
// 是否还有更多数据
const hasMore = ref(false)
// 加载更多的状态
const loadingMore = ref(false)

// 获取我的反馈列表
async function fetchMyFeedbacks() {
  feedbackLoading.value = true
  currentPage.value = 0
  try {
    const res = await feedbackAPI.getMyFeedbacks(0, 10, currentStatus.value)
    myFeedbacks.value = res.list || []
    hasMore.value = currentPage.value + 1 < res.totalPages
  } catch (error) {
    console.error('获取反馈列表失败:', error)
  } finally {
    feedbackLoading.value = false
  }
}

// 加载更多反馈
async function loadMore() {
  loadingMore.value = true
  currentPage.value += 1
  try {
    const res = await feedbackAPI.getMyFeedbacks(currentPage.value, 10, currentStatus.value)
    // 追加新数据到列表
    myFeedbacks.value = [...myFeedbacks.value, ...(res.list || [])]
    hasMore.value = currentPage.value + 1 < res.totalPages
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loadingMore.value = false
  }
}

// 切换反馈状态筛选
function switchStatus(status) {
  currentStatus.value = status
  fetchMyFeedbacks()
}

// 删除我的反馈
async function deleteMyFeedback(id) {
  try {
    await ElMessageBox.confirm('确定删除这条反馈吗？', '确认删除', {
      type: 'warning'
    })
    await feedbackAPI.deleteFeedback(id)
    ElMessage.success('删除成功')
    // 刷新列表
    fetchMyFeedbacks()
  } catch (error) {
    // 用户取消时不显示错误
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 获取反馈类型的中文标签
function getTypeLabel(type) {
  const map = { 'BUG': '问题反馈', 'SUGGESTION': '功能建议', 'CONSULT': '使用咨询' }
  return map[type] || type
}

// 获取反馈状态的中文标签
function getStatusLabel(status) {
  const map = { 'PENDING': '待处理', 'REPLIED': '已回复', 'RESOLVED': '已解决' }
  return map[status] || status
}

// 格式化日期时间
function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 组件挂载时获取我的反馈列表
onMounted(() => {
  fetchMyFeedbacks()
})

// 页面从缓存恢复时重新加载数据（keep-alive）
onActivated(() => {
  fetchMyFeedbacks()
})
</script>

<style scoped>
/* 帮助页面主容器样式 */
.help-page {
  padding-top: 80px;
  min-height: 100vh;
  background: var(--bg-color);
}

/* 页面标题样式 */
.section-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 24px;
}

/* 帮助内容区域布局 */
.help-content {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

/* 帮助标签页导航样式 */
.help-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  background: white;
  padding: 8px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  width: fit-content;
}

/* 标签页按钮样式 */
.help-tab-btn {
  padding: 10px 24px;
  border: none;
  background: transparent;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;
}

/* 标签页按钮悬停效果 */
.help-tab-btn:hover {
  color: var(--primary-color);
  background: var(--bg-color);
}

/* 激活的标签页按钮样式 */
.help-tab-btn.tab-active {
  color: white;
  background: var(--primary-color);
}

/* FAQ区域卡片样式 */
.faq-section {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

/* FAQ列表容器样式 */
.faq-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 单条FAQ项样式 */
.faq-item {
  border-bottom: 1px solid var(--border-color);
}

/* 最后一条FAQ去掉底部边框 */
.faq-item:last-child {
  border-bottom: none;
}

/* FAQ问题行样式 */
.faq-question {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
  cursor: pointer;
  transition: all 0.2s;
}

/* FAQ问题悬停效果 */
.faq-question:hover {
  color: var(--primary-color);
}

/* FAQ图标样式 */
.faq-icon {
  font-size: 20px;
  color: var(--primary-color);
  flex-shrink: 0;
}

/* FAQ问题文字样式 */
.faq-q-text {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

/* FAQ展开箭头样式 */
.faq-arrow {
  font-size: 16px;
  color: var(--text-muted);
  transition: transform 0.3s;
}

/* 展开状态的箭头旋转效果 */
.faq-arrow.expanded {
  transform: rotate(180deg);
}

/* FAQ答案样式 */
.faq-answer {
  padding: 0 32px 16px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
  animation: slideDown 0.3s ease;
}

/* 答案展开动画 */
@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 反馈区域布局：两列 */
.feedback-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: start;
}

/* 反馈表单卡片和我的反馈卡片通用样式 */
.feedback-form-card,
.my-feedback-card {
  background: white;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

/* 表单标题样式 */
.form-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

/* 表单描述文字样式 */
.form-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 24px;
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
}

/* 必填标记样式 */
.required {
  color: var(--danger-color);
}

/* 反馈类型选择器布局 */
.type-selector {
  display: flex;
  gap: 12px;
}

/* 类型选择按钮样式 */
.type-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  border: 2px solid var(--border-color);
  background: var(--bg-color);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--text-secondary);
  font-size: 13px;
}

/* 类型按钮悬停效果 */
.type-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

/* 选中的类型按钮样式 */
.type-btn.type-active {
  border-color: var(--primary-color);
  background: rgba(37, 99, 235, 0.08);
  color: var(--primary-color);
}

/* 类型按钮图标大小 */
.type-btn .el-icon {
  font-size: 24px;
}

/* 文本输入框样式 */
.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s;
  background: white;
}

/* 输入框聚焦效果 */
.form-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* 文本域样式 */
.form-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s;
  resize: vertical;
  font-family: inherit;
  line-height: 1.6;
}

/* 文本域聚焦效果 */
.form-textarea:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* 文本域底部区域：错误信息和字数统计 */
.textarea-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}

/* 字数统计样式 */
.char-count {
  font-size: 12px;
  color: var(--text-muted);
}

/* 错误提示文字样式 */
.error-text {
  font-size: 12px;
  color: var(--danger-color);
  margin-top: 6px;
  display: block;
}

/* 表单操作按钮区域 */
.form-actions {
  margin-top: 24px;
}

/* 主要按钮样式 */
.btn-primary {
  padding: 12px 32px;
  background: var(--primary-color);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

/* 主要按钮悬停效果 */
.btn-primary:hover {
  background: #2563eb;
}

/* 按钮禁用状态 */
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 卡片头部样式 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

/* 卡片标题样式 */
.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 筛选标签容器 */
.filter-tabs {
  display: flex;
  gap: 4px;
}

/* 筛选标签按钮样式 */
.filter-tab {
  padding: 6px 12px;
  border: none;
  background: transparent;
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

/* 筛选标签悬停效果 */
.filter-tab:hover {
  background: var(--bg-color);
}

/* 激活的筛选标签样式 */
.filter-tab.filter-active {
  color: var(--primary-color);
  background: rgba(37, 99, 235, 0.1);
  font-weight: 500;
}

/* 加载中和空状态容器样式 */
.loading-wrap,
.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--text-muted);
  gap: 12px;
}

/* 加载图标旋转动画 */
.loading-icon {
  font-size: 32px;
  animation: spin 1s linear infinite;
}

/* 旋转动画 */
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 空状态图标样式 */
.empty-icon {
  font-size: 48px;
  opacity: 0.3;
}

/* 我的反馈列表容器 */
.my-feedback-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 单条反馈记录样式 */
.feedback-item {
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  transition: all 0.2s;
}

/* 反馈记录悬停效果 */
.feedback-item:hover {
  border-color: var(--primary-color);
}

/* 反馈记录头部：类型、状态、日期 */
.feedback-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

/* 反馈类型标签样式 */
.feedback-type-tag {
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 500;
  border-radius: 12px;
  color: white;
}

/* 问题反馈类型标签颜色 */
.type-BUG {
  background: #ef4444;
}

/* 功能建议类型标签颜色 */
.type-SUGGESTION {
  background: #f59e0b;
}

/* 使用咨询类型标签颜色 */
.type-CONSULT {
  background: #8b5cf6;
}

/* 反馈状态标签样式 */
.feedback-status-tag {
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 500;
  border-radius: 12px;
}

/* 待处理状态标签颜色 */
.status-PENDING {
  background: #fef3c7;
  color: #d97706;
}

/* 已回复状态标签颜色 */
.status-REPLIED {
  background: #dbeafe;
  color: #2563eb;
}

/* 已解决状态标签颜色 */
.status-RESOLVED {
  background: #d1fae5;
  color: #059669;
}

/* 反馈日期样式 */
.feedback-date {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-muted);
}

/* 反馈标题样式 */
.feedback-item-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

/* 反馈内容样式 */
.feedback-item-content {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 12px;
}

/* 官方回复区域样式 */
.feedback-reply {
  margin: 12px 0;
  padding: 12px;
  background: #f0fdf4;
  border-radius: 8px;
  border-left: 3px solid #10b981;
}

/* 回复标签行样式 */
.reply-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #059669;
  margin-bottom: 8px;
}

/* 回复日期样式 */
.reply-date {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-muted);
  font-weight: normal;
}

/* 回复内容样式 */
.reply-content {
  font-size: 13px;
  color: #065f46;
  line-height: 1.6;
}

/* 反馈操作按钮区域 */
.feedback-item-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border-color);
}

/* 文字按钮样式 */
.text-btn {
  padding: 4px 8px;
  border: none;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  color: var(--text-secondary);
}

/* 文字按钮悬停效果 */
.text-btn:hover {
  background: var(--bg-color);
}

/* 危险文字按钮颜色 */
.text-btn.danger {
  color: var(--danger-color);
}

/* 危险文字按钮悬停效果 */
.text-btn.danger:hover {
  background: #fef2f2;
}

/* 加载更多区域 */
.load-more {
  text-align: center;
  padding-top: 8px;
}

/* 次要按钮样式 */
.btn-secondary {
  padding: 8px 24px;
  background: white;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

/* 次要按钮悬停效果 */
.btn-secondary:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

/* 次要按钮禁用状态 */
.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 平板响应式：反馈区域改为单列 */
@media (max-width: 1024px) {
  .feedback-section {
    grid-template-columns: 1fr;
  }
}

/* 手机响应式调整 */
@media (max-width: 640px) {
  .faq-section {
    padding: 20px;
  }
  
  .feedback-form-card,
  .my-feedback-card {
    padding: 20px;
  }
  
  .type-selector {
    flex-direction: column;
  }
  
  .help-tabs {
    width: 100%;
  }
  
  .help-tab-btn {
    flex: 1;
  }
}
</style>
