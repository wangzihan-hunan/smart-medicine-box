<!-- 药方上传页面模板 -->
<template>
  <!-- 药方上传页面主容器 -->
  <div class="prescriptions-page">
    <div class="page-container">
      <!-- 页面头部：标题和上传按钮 -->
      <div class="page-header">
        <h2 class="section-title">药方上传</h2>
        <!-- 上传药方按钮：点击打开上传弹窗 -->
        <button class="btn-primary" @click="showUploadModal = true">
          <el-icon><Upload /></el-icon>
          <span>上传药方</span>
        </button>
      </div>
      
      <!-- 加载中状态 -->
      <div v-if="loading" class="loading-spinner">
        <div class="spinner"></div>
        <span>加载中...</span>
      </div>
      
      <!-- 空状态：没有药方记录时显示 -->
      <div v-else-if="prescriptions.length === 0" class="empty-state">
        <el-icon class="empty-icon"><Document /></el-icon>
        <div class="empty-text">暂无药方记录</div>
        <button class="btn-primary" @click="showUploadModal = true">上传药方</button>
      </div>
      
      <!-- 药方列表区域 -->
      <div v-else class="prescriptions-list">
        <!-- 单个药方卡片 -->
        <div 
          class="prescription-card" 
          v-for="prescription in prescriptions" 
          :key="prescription.id"
        >
          <!-- 药方图标 -->
          <el-icon class="prescription-icon"><Document /></el-icon>
          <!-- 药方基本信息 -->
          <div class="prescription-info">
            <h3 class="prescription-name">{{ prescription.fileName || '药方' }}</h3>
            <p class="prescription-date">上传时间: {{ prescription.uploadDate || '-' }}</p>
            <p v-if="prescription.diagnosis" class="prescription-diagnosis">诊断: {{ prescription.diagnosis }}</p>
            <p v-if="prescription.dosageInstructions" class="prescription-instructions">用药说明: {{ prescription.dosageInstructions }}</p>
          </div>
          <!-- 药方处理状态标签 -->
          <div class="prescription-status">
            <span 
              class="status-tag" 
              :class="getStatusClass(prescription.status)"
            >
              {{ getStatusText(prescription.status) }}
            </span>
          </div>
          <!-- 药方操作按钮：查看、编辑、删除 -->
          <div class="prescription-actions">
            <button class="action-btn view" @click="viewPrescription(prescription)">
              <el-icon><View /></el-icon>
            </button>
            <button class="action-btn edit" @click="openEditModal(prescription)">
              <el-icon><Edit /></el-icon>
            </button>
            <button class="action-btn delete" @click="deletePrescription(prescription.id)">
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
    
    <!-- 上传药方弹窗 -->
    <el-dialog 
      title="上传药方" 
      v-model="showUploadModal"
      width="480px"
      :close-on-click-modal="false"
    >
      <!-- 文件上传区域：支持点击和拖拽上传 -->
      <div class="upload-area" @click="triggerFileInput" @dragover.prevent @drop.prevent="handleDrop">
        <el-icon class="upload-icon"><FolderOpened /></el-icon>
        <div class="upload-text">点击或拖拽文件到此处上传</div>
        <div class="upload-hint">支持 PDF、图片等格式</div>
      </div>
      <!-- 隐藏的文件输入框 -->
      <input 
        type="file" 
        ref="fileInput" 
        class="hidden-input" 
        accept=".pdf,.jpg,.jpeg,.png"
        @change="handleFileSelect"
      />
      
      <!-- 已选文件预览 -->
      <div v-if="selectedFile" class="file-preview">
        <div class="file-info">
          <span class="file-name">{{ selectedFile.name }}</span>
          <span class="file-size">{{ formatFileSize(selectedFile.size) }}</span>
        </div>
        <button class="remove-btn" @click="selectedFile = null">✕</button>
      </div>
      
      <!-- 药方表单：诊断和用药说明 -->
      <div class="prescription-form">
        <div class="form-group">
          <label class="form-label">诊断</label>
          <input type="text" class="form-input" v-model="form.diagnosis" placeholder="请输入诊断信息" />
        </div>
        <div class="form-group">
          <label class="form-label">用药说明</label>
          <textarea class="form-textarea" v-model="form.dosageInstructions" placeholder="请输入用药说明" rows="3"></textarea>
        </div>
      </div>
      
      <!-- 弹窗底部按钮 -->
      <template #footer>
        <button class="btn-secondary" @click="closeModal">取消</button>
        <button class="btn-primary" @click="handleUpload" :disabled="!selectedFile">上传</button>
      </template>
    </el-dialog>

    <!-- 编辑药方弹窗 -->
    <el-dialog 
      title="编辑药方" 
      v-model="showEditModal"
      width="480px"
      :close-on-click-modal="false"
    >
      <!-- 编辑表单：诊断和用药说明 -->
      <div class="prescription-form">
        <div class="form-group">
          <label class="form-label">诊断</label>
          <input type="text" class="form-input" v-model="editForm.diagnosis" placeholder="请输入诊断信息" />
        </div>
        <div class="form-group">
          <label class="form-label">用药说明</label>
          <textarea class="form-textarea" v-model="editForm.dosageInstructions" placeholder="请输入用药说明" rows="3"></textarea>
        </div>
      </div>
      
      <!-- 弹窗底部按钮 -->
      <template #footer>
        <button class="btn-secondary" @click="closeEditModal">取消</button>
        <button class="btn-primary" @click="handleEdit">保存</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 引入Vue核心API：响应式引用、响应式对象、生命周期钩子
import { ref, reactive, onMounted, onActivated } from 'vue'
// 引入Element Plus消息提示和确认对话框组件
import { ElMessage, ElMessageBox } from 'element-plus'
// 引入药方相关的API接口
import { prescriptionAPI } from '../utils/api'
// 引入Element Plus图标组件
import { Upload, Document, View, Edit, Delete, FolderOpened } from '@element-plus/icons-vue'
// 引入dayjs日期处理库
import dayjs from 'dayjs'

// 列表加载状态
const loading = ref(false)
// 上传弹窗显示状态
const showUploadModal = ref(false)
// 编辑弹窗显示状态
const showEditModal = ref(false)
// 药方列表数据
const prescriptions = ref([])
// 当前选中的待上传文件
const selectedFile = ref(null)
// 文件输入框的DOM引用
const fileInput = ref(null)
// 当前编辑的药方ID
const editingId = ref(null)
// 当前页码（从0开始）
const currentPage = ref(0)
// 每页条数
const pageSize = ref(20)
// 药方总条数
const total = ref(0)

// 上传表单数据
const form = reactive({
  diagnosis: '',
  dosageInstructions: ''
})

// 编辑表单数据
const editForm = reactive({
  diagnosis: '',
  dosageInstructions: ''
})

// 获取药方列表数据
async function fetchPrescriptions() {
  loading.value = true
  
  try {
    const response = await prescriptionAPI.getAll(currentPage.value, pageSize.value)
    prescriptions.value = response && response.list ? response.list : []
    total.value = response && response.totalElements ? response.totalElements : 0
  } catch (error) {
    console.error('获取药方列表失败:', error)
    ElMessage.error('获取药方列表失败')
  } finally {
    loading.value = false
  }
}

// 处理每页条数变化
function handleSizeChange(val) {
  pageSize.value = val
  currentPage.value = 0
  fetchPrescriptions()
}

// 处理页码变化
function handleCurrentChange(val) {
  currentPage.value = val - 1
  fetchPrescriptions()
}

// 触发文件选择对话框
function triggerFileInput() {
  fileInput.value?.click()
}

// 处理文件选择
function handleFileSelect(event) {
  const file = event.target.files?.[0]
  if (file) {
    selectedFile.value = file
  }
}

// 处理拖拽上传文件
function handleDrop(event) {
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    selectedFile.value = file
  }
}

// 处理药方上传
async function handleUpload() {
  if (!selectedFile.value) return
  
  loading.value = true
  
  // 构建FormData表单数据
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  if (form.diagnosis) {
    formData.append('diagnosis', form.diagnosis)
  }
  if (form.dosageInstructions) {
    formData.append('dosageInstructions', form.dosageInstructions)
  }
  
  try {
    await prescriptionAPI.upload(formData)
    ElMessage.success('上传成功')
    closeModal()
    fetchPrescriptions()
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    loading.value = false
  }
}

// 关闭上传弹窗并重置表单
function closeModal() {
  showUploadModal.value = false
  selectedFile.value = null
  form.diagnosis = ''
  form.dosageInstructions = ''
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// 查看药方详情
function viewPrescription(prescription) {
  ElMessage.info('查看药方功能开发中')
}

// 打开编辑弹窗并填充数据
function openEditModal(prescription) {
  editingId.value = prescription.id
  editForm.diagnosis = prescription.diagnosis || ''
  editForm.dosageInstructions = prescription.dosageInstructions || ''
  showEditModal.value = true
}

// 处理药方编辑保存
async function handleEdit() {
  if (!editingId.value) return
  
  loading.value = true
  
  try {
    await prescriptionAPI.update(editingId.value, {
      diagnosis: editForm.diagnosis,
      dosageInstructions: editForm.dosageInstructions
    })
    ElMessage.success('修改成功')
    closeEditModal()
    fetchPrescriptions()
  } catch (error) {
    ElMessage.error('修改失败')
  } finally {
    loading.value = false
  }
}

// 关闭编辑弹窗并重置数据
function closeEditModal() {
  showEditModal.value = false
  editingId.value = null
  editForm.diagnosis = ''
  editForm.dosageInstructions = ''
}

// 删除药方（带确认提示）
function deletePrescription(id) {
  ElMessageBox.confirm(
    '确定要删除这个药方吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await prescriptionAPI.delete(id)
      ElMessage.success('删除成功')
      fetchPrescriptions()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 格式化日期时间
function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss')
}

// 格式化文件大小（B/KB/MB）
function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// 根据状态获取对应的CSS类名
function getStatusClass(status) {
  switch (status) {
    case 'PENDING': return 'pending'
    case 'PROCESSED': return 'success'
    case 'FAILED': return 'danger'
    default: return 'default'
  }
}

// 根据状态获取对应的显示文本
function getStatusText(status) {
  switch (status) {
    case 'PENDING': return '处理中'
    case 'PROCESSED': return '已处理'
    case 'FAILED': return '失败'
    default: return status
  }
}

// 组件挂载时获取药方列表
onMounted(() => {
  fetchPrescriptions()
})

// 页面从缓存恢复时重新加载数据（keep-alive）
onActivated(() => {
  fetchPrescriptions()
})
</script>

<style scoped>
/* 药方上传页面主容器样式 */
.prescriptions-page {
  padding-top: 80px;
}

/* 加载中状态样式 */
.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;
}

/* 加载旋转动画样式 */
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

/* 药方列表容器样式 */
.prescriptions-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 药方卡片样式 */
.prescription-card {
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

/* 药方卡片悬停效果 */
.prescription-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

/* 药方图标样式 */
.prescription-icon {
  font-size: 40px;
}

/* 药方信息区域样式 */
.prescription-info {
  flex: 1;
}

/* 药方名称样式 */
.prescription-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

/* 药方上传日期样式 */
.prescription-date {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 药方状态区域样式 */
.prescription-status {
  flex-shrink: 0;
}

/* 状态标签通用样式 */
.status-tag {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

/* 处理中状态样式 */
.status-tag.pending {
  background: rgba(245, 158, 11, 0.1);
  color: var(--warning-color);
}

/* 已处理状态样式 */
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

/* 药方操作按钮区域样式 */
.prescription-actions {
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

/* 查看按钮样式 */
.action-btn.view {
  background: rgba(59, 130, 246, 0.1);
  color: var(--info-color);
}

/* 查看按钮悬停效果 */
.action-btn.view:hover {
  background: rgba(59, 130, 246, 0.2);
}

/* 编辑按钮样式 */
.action-btn.edit {
  background: rgba(245, 158, 11, 0.1);
  color: var(--warning-color);
}

/* 编辑按钮悬停效果 */
.action-btn.edit:hover {
  background: rgba(245, 158, 11, 0.2);
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

/* 文件上传区域样式 */
.upload-area {
  border: 2px dashed var(--border-color);
  border-radius: 16px;
  padding: 48px 24px;
  text-align: center;
  cursor: pointer;
  transition: all var(--transition-normal);
}

/* 文件上传区域悬停效果 */
.upload-area:hover {
  border-color: var(--primary-color);
  background: rgba(16, 185, 129, 0.02);
}

/* 上传图标样式 */
.upload-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

/* 上传提示文字样式 */
.upload-text {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 8px;
}

/* 上传格式提示样式 */
.upload-hint {
  font-size: 14px;
  color: var(--text-muted);
}

/* 隐藏的文件输入框 */
.hidden-input {
  display: none;
}

/* 文件预览区域样式 */
.file-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 8px;
}

/* 文件信息区域样式 */
.file-info {
  display: flex;
  flex-direction: column;
}

/* 文件名称样式 */
.file-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

/* 文件大小样式 */
.file-size {
  font-size: 13px;
  color: var(--text-muted);
}

/* 移除文件按钮样式 */
.remove-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: var(--text-muted);
  padding: 4px 8px;
  border-radius: 4px;
  transition: all var(--transition-fast);
}

/* 移除按钮悬停效果 */
.remove-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger-color);
}

/* 药方表单区域样式 */
.prescription-form {
  margin-top: 20px;
}

/* 表单组样式 */
.form-group {
  margin-bottom: 16px;
}

/* 表单标签样式 */
.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 8px;
}

/* 表单输入框和文本域通用样式 */
.form-input, .form-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-sm);
  font-size: 15px;
  transition: all var(--transition-fast);
  outline: none;
  box-sizing: border-box;
}

/* 表单输入框聚焦效果 */
.form-input:focus, .form-textarea:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* 文本域样式 */
.form-textarea {
  resize: vertical;
}

/* 诊断信息样式 */
.prescription-diagnosis {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* 用药说明样式（最多显示2行） */
.prescription-instructions {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 空状态容器样式 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
}

/* 空状态图标样式 */
.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

/* 空状态文字样式 */
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

/* 手机端响应式调整 */
@media (max-width: 600px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .prescription-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
