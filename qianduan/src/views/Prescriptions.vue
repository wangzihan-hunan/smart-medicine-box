<!-- 药方上传页面模板 -->
<template>
  <!-- 药方上传页面主容器 -->
  <div class="prescriptions-page">
    <div class="page-container">
      <!-- 页面头部：标题和操作按钮 -->
      <div class="page-header">
        <h2 class="section-title">药方上传</h2>
        <div class="header-actions">
          <!-- 智能识别按钮 -->
          <button class="btn-accent" @click="openSmartRecognition">
            <el-icon><Camera /></el-icon>
            <span>智能识别</span>
          </button>
          <!-- 上传药方按钮 -->
          <button class="btn-primary" @click="showUploadModal = true">
            <el-icon><Upload /></el-icon>
            <span>上传药方</span>
          </button>
        </div>
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

    <!-- 智能识别弹窗 -->
    <el-dialog
      title="智能识别药品"
      v-model="showSmartRecognition"
      width="720px"
      :close-on-click-modal="false"
      @closed="closeSmartRecognition"
    >
      <!-- 步骤1：选择识别方式 -->
      <div v-if="smartStep === 1" class="smart-step">
        <div class="smart-methods">
          <!-- 图片识别 -->
          <div class="method-card" @click="handleImageRecognition">
            <el-icon class="method-icon"><Picture /></el-icon>
            <div class="method-title">拍照/图片识别</div>
            <div class="method-desc">上传药品包装或药方照片，自动识别药品信息</div>
          </div>
          <!-- 条形码扫描 -->
          <div class="method-card" @click="startBarcodeScan">
            <el-icon class="method-icon"><Camera /></el-icon>
            <div class="method-title">条形码扫描</div>
            <div class="method-desc">扫描药品包装上的条形码，快速识别药品</div>
          </div>
        </div>
        <!-- 隐藏的文件输入框 -->
        <input
          type="file"
          ref="smartFileInput"
          class="hidden-input"
          accept=".jpg,.jpeg,.png,.bmp"
          @change="handleSmartFileSelect"
        />
      </div>

      <!-- 步骤2：条形码扫描 -->
      <div v-else-if="smartStep === 2" class="smart-step">
        <BarcodeScanner
          v-if="showBarcodeScanner"
          @scan-result="handleBarcodeResult"
          @close="smartStep = 1"
          @error="handleScannerError"
        />
      </div>

      <!-- 步骤3：识别结果 -->
      <div v-else-if="smartStep === 3" class="smart-step">
        <!-- 加载中 -->
        <div v-if="recognizing" class="recognizing-state">
          <div class="spinner"></div>
          <span>正在识别中...</span>
        </div>

        <!-- 识别失败 -->
        <div v-else-if="recognitionError" class="recognition-error">
          <el-icon class="error-icon"><WarningFilled /></el-icon>
          <div class="error-text">{{ recognitionError }}</div>
          <button class="btn-primary" @click="resetRecognition">重新识别</button>
        </div>

        <!-- 识别结果展示 -->
        <div v-else-if="matchResults.length > 0" class="match-results">
          <div class="match-summary">
            识别到 <strong>{{ matchResults.length }}</strong> 种药品，
            匹配到 <strong>{{ matchedCount }}</strong> 种已有药品
          </div>

          <div class="match-list">
            <div
              v-for="(item, index) in matchResults"
              :key="index"
              class="match-item"
              :class="{ matched: item.matched, unmatched: !item.matched, skipped: item.skip }"
            >
              <div class="match-info">
                <div class="match-name">
                  {{ item.recognizedInfo?.name || '未知药品' }}
                  <span v-if="item.recognizedInfo?.spec" class="match-spec">{{ item.recognizedInfo.spec }}</span>
                </div>
                <div v-if="item.matched" class="match-detail">
                  <el-icon class="match-icon"><CircleCheck /></el-icon>
                  匹配到：
                  <strong>{{ item.matchedMedicine?.name }}</strong>
                  <span v-if="item.matchedMedicine?.spec" class="match-existing-spec">
                    ({{ item.matchedMedicine.spec }})
                  </span>
                  <span class="match-similarity">
                    相似度 {{ (item.similarity * 100).toFixed(0) }}%
                  </span>
                  <span class="confidence-tag" :class="item.confidence">
                    {{ item.confidence === 'HIGH' ? '高置信' : '中置信' }}
                  </span>
                </div>
                <div v-else class="match-detail unmatched-detail">
                  <el-icon class="match-icon"><Warning /></el-icon>
                  未匹配到已有药品，将作为新药品入库
                </div>
              </div>
              <div class="match-actions">
                <select v-model="item.quantity" class="quantity-select" v-if="!item.skip">
                  <option :value="1">+1</option>
                  <option :value="2">+2</option>
                  <option :value="3">+3</option>
                  <option :value="5">+5</option>
                  <option :value="10">+10</option>
                </select>
                <button
                  class="action-btn-small skip-btn"
                  @click="item.skip = !item.skip"
                  :class="{ active: item.skip }"
                >
                  {{ item.skip ? '已跳过' : '跳过' }}
                </button>
              </div>
            </div>
          </div>

          <div class="batch-actions">
            <button class="btn-secondary" @click="resetRecognition">重新识别</button>
            <button
              class="btn-primary"
              @click="handleBatchAdd"
              :disabled="allSkipped"
            >
              一键入库 ({{ toAddCount }}种)
            </button>
          </div>
        </div>
      </div>

      <!-- 步骤4：入库结果 -->
      <div v-else-if="smartStep === 4" class="smart-step">
        <div class="batch-result">
          <el-icon class="result-icon success"><CircleCheckFilled /></el-icon>
          <div class="result-title">入库完成</div>
          <div class="result-detail">
            新增 <strong>{{ batchResult.newCount }}</strong> 种药品，
            更新 <strong>{{ batchResult.updateCount }}</strong> 种库存
            <span v-if="batchResult.skipCount > 0">，跳过 {{ batchResult.skipCount }} 种</span>
          </div>
          <button class="btn-primary" @click="finishRecognition">完成</button>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
// 引入Vue核心API：响应式引用、响应式对象、生命周期钩子
import { ref, reactive, computed, onMounted, onActivated } from 'vue'
// 引入Element Plus消息提示和确认对话框组件
import { ElMessage, ElMessageBox } from 'element-plus'
// 引入药方相关的API接口
import { prescriptionAPI, medicineAPI } from '../utils/api'
// 引入Element Plus图标组件
import { Upload, Document, View, Edit, Delete, FolderOpened, Camera, Picture, CircleCheck, Warning, WarningFilled, CircleCheckFilled } from '@element-plus/icons-vue'
// 引入条形码扫描组件
import BarcodeScanner from '../components/BarcodeScanner.vue'
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

// ==================== 智能识别相关状态 ====================

// 智能识别弹窗显示状态
const showSmartRecognition = ref(false)
// 智能识别步骤：1-选择方式, 2-条形码扫描, 3-识别结果, 4-入库结果
const smartStep = ref(1)
// 是否显示条形码扫描组件
const showBarcodeScanner = ref(false)
// 是否正在识别中
const recognizing = ref(false)
// 识别错误信息
const recognitionError = ref('')
// 匹配结果列表
const matchResults = ref([])
// 批量入库结果
const batchResult = ref({ newCount: 0, updateCount: 0, skipCount: 0 })
// 隐藏文件输入框引用
const smartFileInput = ref(null)

// 计算属性：匹配到的数量
const matchedCount = computed(() => matchResults.value.filter(r => r.matched).length)

// 计算属性：待入库数量
const toAddCount = computed(() => matchResults.value.filter(r => !r.skip).length)

// 计算属性：是否全部跳过
const allSkipped = computed(() => matchResults.value.length > 0 && toAddCount.value === 0)

// 打开智能识别弹窗
function openSmartRecognition() {
  showSmartRecognition.value = true
  smartStep.value = 1
  recognitionError.value = ''
}

// 关闭智能识别弹窗并重置
function closeSmartRecognition() {
  showSmartRecognition.value = false
  resetRecognition()
}

// 触发图片识别文件选择
function handleImageRecognition() {
  smartFileInput.value?.click()
}

// 启动条形码扫描
function startBarcodeScan() {
  smartStep.value = 2
  showBarcodeScanner.value = true
}

// 处理智能识别文件选择
async function handleSmartFileSelect(event) {
  const file = event.target.files?.[0]
  if (!file) return

  recognizing.value = true
  smartStep.value = 3
  recognitionError.value = ''

  try {
    const formData = new FormData()
    formData.append('image', file)

    // 步骤1：调用 OCR 识别
    const recognitionResult = await medicineAPI.recognize(formData)
    if (!recognitionResult || !recognitionResult.success) {
      recognitionError.value = recognitionResult?.message || '识别失败，请尝试重新上传'
      recognizing.value = false
      return
    }

    // 步骤2：调用智能匹配
    const matchResult = await medicineAPI.match(recognitionResult)
    matchResults.value = (matchResult || []).map(item => ({
      ...item,
      quantity: 1,
      skip: false
    }))

    recognizing.value = false
  } catch (error) {
    console.error('智能识别失败:', error)
    recognitionError.value = error.message || '识别服务暂不可用，请稍后重试'
    recognizing.value = false
  } finally {
    // 重置文件输入框
    if (smartFileInput.value) {
      smartFileInput.value.value = ''
    }
  }
}

// 处理条形码扫描结果
async function handleBarcodeResult(result) {
  showBarcodeScanner.value = false
  smartStep.value = 3
  recognizing.value = true
  recognitionError.value = ''

  try {
    // 条形码识别：将条形码作为药品名称，先尝试匹配
    const recognitionResult = {
      success: true,
      medicines: [{
        name: result.barcode,
        spec: '',
        dosage: '',
        frequency: '',
        quantity: '1'
      }]
    }

    const matchResult = await medicineAPI.match(recognitionResult)
    matchResults.value = (matchResult || []).map(item => ({
      ...item,
      quantity: 1,
      skip: false
    }))

    recognizing.value = false
  } catch (error) {
    console.error('条形码匹配失败:', error)
    recognitionError.value = '条形码匹配失败，请尝试图片识别'
    recognizing.value = false
  }
}

// 处理扫描器错误
function handleScannerError(msg) {
  ElMessage.warning(msg || '扫描失败')
  smartStep.value = 1
}

// 批量入库
async function handleBatchAdd() {
  const items = matchResults.value
    .filter(item => !item.skip)
    .map(item => ({
      name: item.recognizedInfo?.name || '',
      spec: item.recognizedInfo?.spec || '',
      category: item.recognizedInfo?.category || '',
      manufacturer: item.recognizedInfo?.manufacturer || '',
      quantity: item.quantity || 1,
      matchedMedicineId: item.matched ? item.matchedMedicine?.id : null
    }))

  if (items.length === 0) {
    ElMessage.warning('请选择要入库的药品')
    return
  }

  try {
    const result = await medicineAPI.batchAdd(items)
    batchResult.value = result
    smartStep.value = 4
    ElMessage.success('入库成功')
  } catch (error) {
    console.error('批量入库失败:', error)
    ElMessage.error(error.message || '入库失败')
  }
}

// 重置识别状态
function resetRecognition() {
  smartStep.value = 1
  recognizing.value = false
  recognitionError.value = ''
  matchResults.value = []
  showBarcodeScanner.value = false
}

// 完成识别，关闭弹窗并刷新列表
function finishRecognition() {
  showSmartRecognition.value = false
  resetRecognition()
  fetchPrescriptions()
}

// ==================== 药方列表相关方法 ====================

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

/* ==================== 智能识别样式 ==================== */

.header-actions {
  display: flex;
  gap: 12px;
}

.btn-accent {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  transition: all var(--transition-normal);
}

.btn-accent:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.smart-step {
  min-height: 200px;
}

.smart-methods {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.method-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 20px;
  border: 2px solid var(--border-color);
  border-radius: 16px;
  cursor: pointer;
  transition: all var(--transition-normal);
  text-align: center;
}

.method-card:hover {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.04);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.1);
}

.method-icon {
  font-size: 48px;
  color: #667eea;
  margin-bottom: 16px;
}

.method-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.method-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.recognizing-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  gap: 16px;
  color: var(--text-secondary);
}

.recognition-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  gap: 16px;
}

.error-icon {
  font-size: 48px;
  color: var(--danger-color);
}

.error-text {
  font-size: 15px;
  color: var(--text-secondary);
  text-align: center;
}

.match-results {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.match-summary {
  font-size: 14px;
  color: var(--text-secondary);
  padding: 12px 16px;
  background: rgba(102, 126, 234, 0.06);
  border-radius: 10px;
  text-align: center;
}

.match-summary strong {
  color: #667eea;
}

.match-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  overflow-y: auto;
}

.match-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  transition: all var(--transition-fast);
}

.match-item.matched {
  border-color: rgba(34, 197, 94, 0.3);
  background: rgba(34, 197, 94, 0.04);
}

.match-item.unmatched {
  border-color: rgba(245, 158, 11, 0.3);
  background: rgba(245, 158, 11, 0.04);
}

.match-item.skipped {
  opacity: 0.5;
}

.match-info {
  flex: 1;
  min-width: 0;
}

.match-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.match-spec {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 400;
}

.match-detail {
  font-size: 13px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.match-icon {
  font-size: 16px;
}

.match-item.matched .match-icon {
  color: var(--success-color);
}

.match-item.unmatched .match-icon {
  color: var(--warning-color);
}

.match-existing-spec {
  font-size: 12px;
  color: var(--text-muted);
}

.match-similarity {
  font-size: 12px;
  color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  padding: 2px 8px;
  border-radius: 10px;
}

.confidence-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 8px;
  font-weight: 500;
}

.confidence-tag.HIGH {
  background: rgba(34, 197, 94, 0.15);
  color: var(--success-color);
}

.confidence-tag.MEDIUM {
  background: rgba(245, 158, 11, 0.15);
  color: var(--warning-color);
}

.match-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.quantity-select {
  padding: 6px 10px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 14px;
  background: white;
  cursor: pointer;
}

.action-btn-small {
  padding: 6px 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  background: white;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.action-btn-small:hover {
  border-color: var(--text-muted);
}

.action-btn-small.active {
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger-color);
  border-color: rgba(239, 68, 68, 0.3);
}

.batch-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.batch-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  gap: 12px;
  text-align: center;
}

.result-icon {
  font-size: 56px;
}

.result-icon.success {
  color: var(--success-color);
}

.result-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.result-detail {
  font-size: 15px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.result-detail strong {
  color: #667eea;
  font-size: 18px;
}
</style>
