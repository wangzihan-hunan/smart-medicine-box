<template>
  <!-- 指令转发页面主容器 -->
  <div class="commands-page">
    <!-- 页面内容容器 -->
    <div class="page-container">
      <!-- 页面头部：标题和发送指令按钮 -->
      <div class="page-header">
        <h2 class="section-title">指令转发</h2>
        <!-- 发送指令按钮，点击打开发送弹窗 -->
        <button class="btn-primary" @click="showAddModal = true">
          <el-icon><ArrowRight /></el-icon>
          <span>发送指令</span>
        </button>
      </div>
      
      <!-- 加载中状态 -->
      <div v-if="loading" class="loading-spinner">
        <div class="spinner"></div>
        <span>加载中...</span>
      </div>
      
      <!-- 空状态：暂无指令记录时显示 -->
      <div v-else-if="commands.length === 0" class="empty-state">
        <el-icon class="empty-icon"><Cellphone /></el-icon>
        <div class="empty-text">暂无指令记录</div>
        <button class="btn-primary" @click="showAddModal = true">发送指令</button>
      </div>
      
      <!-- 指令列表区域 -->
      <div v-else class="commands-list">
        <!-- 单个指令卡片 -->
        <div 
          class="command-card" 
          v-for="command in commands" 
          :key="command.id"
        >
          <!-- 指令类型图标 -->
          <el-icon class="command-icon"><component :is="getCommandIcon(command.action)" /></el-icon>
          <!-- 指令详细信息：类型、时间、参数 -->
          <div class="command-info">
            <h3 class="command-type">{{ getCommandText(command.action) }}</h3>
            <p class="command-date">发送时间: {{ command.time || '-' }}</p>
            <p v-if="command.params" class="command-params">参数: {{ command.params }}</p>
          </div>
          <!-- 指令状态标签 -->
          <div class="command-status">
            <span 
              class="status-tag" 
              :class="getStatusClass(command.status)"
            >
              {{ getStatusText(command.status) }}
            </span>
          </div>
          <!-- 操作按钮区域：执行/删除 -->
          <div class="command-actions">
            <button 
              v-if="command.status === 'pending' || command.status === '已发送'" 
              class="action-btn execute" 
              @click="executeCommand(command.id)"
            >
              <el-icon><VideoPlay /></el-icon> 执行
            </button>
            <button 
              class="action-btn delete" 
              @click="deleteCommand(command.id)"
            >
              <el-icon><Delete /></el-icon>
            </button>
          </div>
        </div>
      </div>
      
      <!-- 分页组件 -->
      <div v-if="total > 0" class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage + 1"
          :page-sizes="[10, 20, 50]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
        />
      </div>
    </div>
    
    <!-- 发送指令弹窗 -->
    <el-dialog 
      title="发送指令" 
      v-model="showAddModal"
      width="480px"
      :close-on-click-modal="false"
    >
      <!-- 指令表单 -->
      <form @submit.prevent="handleSubmit" class="command-form">
        <!-- 指令类型选择 -->
        <div class="form-group">
          <label class="form-label">指令类型 *</label>
          <select class="form-input" v-model="form.action">
            <option value="OPEN_DOOR">开门</option>
            <option value="CLOSE_DOOR">关门</option>
            <option value="DISPENSE_MEDICINE">出药</option>
            <option value="CHECK_STOCK">检查库存</option>
            <option value="CALIBRATE">校准</option>
          </select>
        </div>
        <!-- 设备编号输入 -->
        <div class="form-group">
          <label class="form-label">设备编号 *</label>
          <input type="text" class="form-input" v-model="form.deviceId" placeholder="请输入智能药箱设备编号" />
        </div>
        <!-- 选择药品（仅出药指令时显示） -->
        <div class="form-group" v-if="form.action === 'DISPENSE_MEDICINE'">
          <label class="form-label">选择药品 *</label>
          <el-select
            v-model="form.medicineId"
            placeholder="请选择药品"
            filterable
            clearable
            style="width: 100%"
            :loading="medicinesLoading"
            @visible-change="onMedicineSelectVisible"
          >
            <el-option
              v-for="med in medicines"
              :key="med.id"
              :label="med.name"
              :value="med.id"
            >
              <div class="medicine-option">
                <div class="medicine-option-info">
                  <span class="medicine-option-name">{{ med.name }}</span>
                  <span class="medicine-option-spec" v-if="med.spec">{{ med.spec }}</span>
                </div>
                <span class="medicine-option-stock" :class="{ 'low-stock': med.isLowStock }">
                  库存: {{ med.stock }}
                </span>
              </div>
            </el-option>
          </el-select>
        </div>
        <!-- 参数输入 -->
        <div class="form-group">
          <label class="form-label">参数</label>
          <textarea class="form-input" v-model="form.params" placeholder="请输入指令参数" rows="3"></textarea>
        </div>
      </form>
      
      <!-- 弹窗底部按钮 -->
      <template #footer>
        <button class="btn-secondary" @click="closeModal">取消</button>
        <button class="btn-primary" @click="handleSubmit">发送</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 引入Vue组合式API
import { ref, reactive, onMounted, onActivated, watch } from 'vue'
// 引入Element Plus消息提示组件
import { ElMessage, ElMessageBox } from 'element-plus'
// 引入API接口
import { commandAPI, medicineAPI } from '../utils/api'
// 引入图标组件
import { ArrowRight, Cellphone, VideoPlay, Delete, ArrowRight as DoorOpen, ArrowLeft as DoorClose, TakeawayBox, Box, Setting } from '@element-plus/icons-vue'
// 引入日期处理库
import dayjs from 'dayjs'

// 加载状态
const loading = ref(false)
// 控制发送指令弹窗显示
const showAddModal = ref(false)
// 指令列表数据
const commands = ref([])
// 当前页码（从0开始）
const currentPage = ref(0)
// 每页显示数量
const pageSize = ref(20)
// 总记录数
const total = ref(0)
// 药品列表数据
const medicines = ref([])
// 药品列表加载状态
const medicinesLoading = ref(false)

// 表单响应式数据
const form = reactive({
  action: 'OPEN_DOOR',  // 指令类型，默认为开门
  deviceId: '',         // 设备编号
  medicineId: '',       // 药品ID（出药指令时需要）
  params: ''            // 指令参数
})

// 监听指令类型变化，切换为出药指令时自动加载药品列表
watch(() => form.action, (newAction) => {
  if (newAction === 'DISPENSE_MEDICINE' && medicines.value.length === 0) {
    fetchMedicines()
  }
})

// 获取指令列表数据
async function fetchCommands() {
  loading.value = true
  
  try {
    const response = await commandAPI.getAll(currentPage.value, pageSize.value)
    commands.value = response && response.list ? response.list : []
    total.value = response && response.totalElements ? response.totalElements : 0
  } catch (error) {
    console.error('获取指令列表失败:', error)
    ElMessage.error('获取指令列表失败')
  } finally {
    loading.value = false
  }
}

// 获取药品列表
async function fetchMedicines() {
  medicinesLoading.value = true
  try {
    const response = await medicineAPI.getAll({}, 0, 100)
    medicines.value = response && response.list ? response.list : []
  } catch (error) {
    console.error('获取药品列表失败:', error)
    ElMessage.error('获取药品列表失败')
  } finally {
    medicinesLoading.value = false
  }
}

// 药品下拉框显示时触发，首次打开时加载药品列表
function onMedicineSelectVisible(visible) {
  if (visible && medicines.value.length === 0) {
    fetchMedicines()
  }
}

// 每页数量变化时的处理
function handleSizeChange(val) {
  pageSize.value = val
  currentPage.value = 0
  fetchCommands()
}

// 当前页变化时的处理
function handleCurrentChange(val) {
  currentPage.value = val - 1
  fetchCommands()
}

// 执行指定的指令
async function executeCommand(id) {
  try {
    await commandAPI.execute(id)
    ElMessage.success('指令执行成功')
    fetchCommands()
  } catch (error) {
    ElMessage.error('执行失败')
  }
}

// 删除指令：确认后执行删除
function deleteCommand(id) {
  ElMessageBox.confirm(
    '确定要删除这条指令吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await commandAPI.delete(id)
      ElMessage.success('删除成功')
      fetchCommands()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 关闭弹窗并重置表单
function closeModal() {
  showAddModal.value = false
  form.action = 'OPEN_DOOR'
  form.deviceId = ''
  form.medicineId = ''
  form.params = ''
}

// 提交表单：发送新指令
async function handleSubmit() {
  // 表单校验：设备编号必填
  if (!form.deviceId) {
    ElMessage.warning('请输入设备编号')
    return
  }
  // 表单校验：出药指令必须选择药品
  if (form.action === 'DISPENSE_MEDICINE' && !form.medicineId) {
    ElMessage.warning('请选择药品')
    return
  }
  
  loading.value = true
  
  try {
    // 构造提交数据
    const data = {
      action: form.action,
      deviceId: form.deviceId,
      params: form.params
    }
    // 出药指令时添加药品ID
    if (form.medicineId) {
      data.medicineId = parseInt(form.medicineId)
    }
    
    await commandAPI.create(data)
    ElMessage.success('指令发送成功')
    closeModal()
    fetchCommands()
  } catch (error) {
    ElMessage.error('发送失败')
  } finally {
    loading.value = false
  }
}

// 格式化日期时间字符串
function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss')
}

// 根据指令类型获取对应的图标组件
function getCommandIcon(type) {
  switch (type) {
    case 'OPEN_DOOR': return DoorOpen
    case 'CLOSE_DOOR': return DoorClose
    case 'DISPENSE_MEDICINE': return TakeawayBox
    case 'CHECK_STOCK': return Box
    case 'CALIBRATE': return Setting
    default: return Cellphone
  }
}

// 根据指令类型获取对应的中文文本
function getCommandText(type) {
  switch (type) {
    case 'OPEN_DOOR': return '开门指令'
    case 'CLOSE_DOOR': return '关门指令'
    case 'DISPENSE_MEDICINE': return '出药指令'
    case 'CHECK_STOCK': return '库存检查'
    case 'CALIBRATE': return '校准指令'
    default: return type
  }
}

// 根据指令状态获取对应的样式类名
function getStatusClass(status) {
  switch (status) {
    case 'pending': return 'pending'
    case '已发送': return 'pending'
    case '已执行': return 'success'
    case 'FAILED': return 'danger'
    case '失败': return 'danger'
    default: return 'default'
  }
}

// 根据指令状态获取对应的中文文本
function getStatusText(status) {
  switch (status) {
    case 'pending': return '待执行'
    case '已发送': return '已发送'
    case '已执行': return '已执行'
    case 'FAILED': return '执行失败'
    case '失败': return '执行失败'
    default: return status || '未知'
  }
}

// 组件挂载时获取指令列表
onMounted(() => {
  fetchCommands()
})

// 页面从缓存恢复时重新加载数据（keep-alive）
onActivated(() => {
  fetchCommands()
})
</script>

<style scoped>
/* 页面整体样式 */
.commands-page {
  padding-top: 80px;
}

/* 加载中样式 */
.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;
}

/* 加载动画样式 */
.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(16, 185, 129, 0.1);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* 旋转动画关键帧 */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 页面头部样式 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

/* 指令列表容器样式 */
.commands-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 指令卡片样式 */
.command-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--border-color);
  transition: all var(--transition-normal);
}

/* 指令卡片悬停效果 */
.command-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

/* 指令图标样式 */
.command-icon {
  font-size: 40px;
}

/* 指令信息区域样式 */
.command-info {
  flex: 1;
}

/* 指令类型样式 */
.command-type {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

/* 指令日期样式 */
.command-date {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 指令参数样式 */
.command-params {
  font-size: 14px;
  color: var(--text-muted);
  margin-top: 4px;
}

/* 状态区域样式 */
.command-status {
  flex-shrink: 0;
}

/* 状态标签通用样式 */
.status-tag {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

/* 待执行状态样式 */
.status-tag.pending {
  background: rgba(245, 158, 11, 0.1);
  color: var(--warning-color);
}

/* 成功状态样式 */
.status-tag.success {
  background: rgba(34, 197, 94, 0.1);
  color: var(--success-color);
}

/* 失败状态样式 */
.status-tag.danger {
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger-color);
}

/* 默认状态样式 */
.status-tag.default {
  background: rgba(156, 163, 175, 0.1);
  color: var(--text-muted);
}

/* 操作按钮区域样式 */
.command-actions {
  display: flex;
  gap: 12px;
}

/* 操作按钮通用样式 */
.action-btn {
  padding: 8px 12px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all var(--transition-fast);
}

/* 执行按钮样式 */
.action-btn.execute {
  background: rgba(34, 197, 94, 0.1);
  color: var(--success-color);
}

/* 执行按钮悬停效果 */
.action-btn.execute:hover {
  background: rgba(34, 197, 94, 0.2);
}

/* 删除按钮样式 */
.action-btn.delete {
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger-color);
}

/* 删除按钮悬停效果 */
.action-btn.delete:hover {
  background: rgba(239, 68, 68, 0.2);
}

/* 表单容器样式 */
.command-form {
  display: flex;
  flex-direction: column;
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

/* 表单输入框样式 */
.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 15px;
  transition: all var(--transition-fast);
}

/* 输入框聚焦效果 */
.form-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

/* 空状态样式 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
}

/* 空状态图标样式 */
.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

/* 空状态文本样式 */
.empty-text {
  font-size: 18px;
  color: var(--text-secondary);
  margin-bottom: 24px;
}

/* 分页容器样式 */
.pagination-container {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

/* 移动端响应式样式 */
@media (max-width: 600px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .command-card {
    flex-direction: column;
    align-items: flex-start;
  }
}

/* 药品选择器下拉选项样式 */
.medicine-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

/* 药品信息区域样式 */
.medicine-option-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 药品名称样式 */
.medicine-option-name {
  font-weight: 500;
  color: var(--text-primary);
}

/* 药品规格样式 */
.medicine-option-spec {
  font-size: 12px;
  color: var(--text-muted);
}

/* 库存数量样式 */
.medicine-option-stock {
  font-size: 12px;
  color: var(--success-color);
  flex-shrink: 0;
}

/* 低库存警告样式 */
.medicine-option-stock.low-stock {
  color: var(--danger-color);
}
</style>
