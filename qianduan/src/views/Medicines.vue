<template>
  <!-- 药品管理页面根容器 -->
  <div class="medicines-page">
    <!-- 页面主容器 -->
    <div class="page-container">
      <!-- 页面头部：标题和添加按钮 -->
      <div class="page-header">
        <h2 class="section-title">药品管理</h2>
        <button class="btn-primary" @click="showAddModal = true">
          <el-icon><Plus /></el-icon>
          <span>添加药品</span>
        </button>
      </div>
      
      <!-- 搜索和筛选栏 -->
      <div class="search-bar">
        <!-- 搜索输入框 -->
        <div class="search-wrapper">
          <el-icon class="search-icon"><Search /></el-icon>
          <input 
            type="text" 
            class="search-input" 
            v-model="searchQuery" 
            placeholder="搜索药品名称..."
            @input="handleSearch"
          />
        </div>
        <!-- 筛选和导出按钮行 -->
        <div class="filter-row">
          <!-- 分类筛选下拉框 -->
          <el-select v-model="filterCategory" placeholder="选择分类" clearable @change="handleSearch">
            <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
          </el-select>
          <!-- 状态筛选下拉框（低库存/已过期） -->
          <el-select v-model="filterStatus" placeholder="状态筛选" clearable @change="handleSearch">
            <el-option label="低库存" value="lowStock" />
            <el-option label="已过期" value="expired" />
          </el-select>
          <!-- 导出CSV按钮 -->
          <button class="btn-secondary" @click="exportMedicines">
            <el-icon><Download /></el-icon>
            <span>导出CSV</span>
          </button>
        </div>
      </div>
      
      <!-- 加载中状态 -->
      <div v-if="loading" class="loading-spinner">
        <el-spinner type="circle" size="40" />
      </div>
      
      <!-- 空数据状态 -->
      <div v-else-if="medicines.length === 0" class="empty-state">
        <el-icon class="empty-icon"><TakeawayBox /></el-icon>
        <div class="empty-text">暂无药品信息</div>
        <button class="btn-primary" @click="showAddModal = true">添加药品</button>
      </div>
      
      <!-- 药品列表网格 -->
      <div v-else class="medicines-grid">
        <!-- 单个药品卡片 -->
        <div 
          class="medicine-card" 
          v-for="(medicine, index) in medicines" 
          :key="medicine.id"
          :class="{ 
            'low-stock': medicine.isLowStock,
            'expired': medicine.isExpired 
          }"
          :style="{ animationDelay: `${index * 0.05}s` }"
        >
          <!-- 药品卡片头部：图标、名称、分类、状态标签 -->
          <div class="medicine-header">
            <el-icon class="medicine-icon"><TakeawayBox /></el-icon>
            <div class="medicine-info">
              <h3 class="medicine-name">{{ medicine.name }}</h3>
              <p class="medicine-category">{{ medicine.category || '未分类' }}</p>
            </div>
            <!-- 状态标签组 -->
            <div class="status-badges">
              <el-tag v-if="medicine.isExpired" type="danger" size="small">已过期</el-tag>
              <el-tag v-else-if="medicine.isLowStock" type="warning" size="small">低库存</el-tag>
            </div>
          </div>
          <!-- 药品元信息：库存、规格、厂家、有效期 -->
          <div class="medicine-meta">
            <div class="meta-item">
              <span class="meta-label">库存</span>
              <span class="meta-value" :class="{ 'danger': medicine.isLowStock }">
                {{ medicine.stock }}
              </span>
            </div>
            <div class="meta-item">
              <span class="meta-label">规格</span>
              <span class="meta-value">{{ medicine.spec || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">生产厂家</span>
              <span class="meta-value">{{ medicine.manufacturer || '-' }}</span>
            </div>
            <div class="meta-item" v-if="medicine.expireDate">
              <span class="meta-label">有效期至</span>
              <span class="meta-value" :class="{ 'danger': medicine.isExpired }">
                {{ formatDate(medicine.expireDate) }}
              </span>
            </div>
          </div>
          <!-- 药品操作按钮：编辑、删除 -->
          <div class="medicine-actions">
            <button class="action-btn edit" @click="editMedicine(medicine)">
              <el-icon><Edit /></el-icon> 编辑
            </button>
            <button class="action-btn delete" @click="deleteMedicine(medicine.id)">
              <el-icon><Delete /></el-icon> 删除
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
    
    <!-- 添加/编辑药品弹窗 -->
    <el-dialog 
      :title="editingMedicine ? '编辑药品' : '添加药品'" 
      v-model="showAddModal"
      width="520px"
      :close-on-click-modal="false"
    >
      <!-- 添加药品时的Tab切换：手动输入 / 图片识别 -->
      <div v-if="!editingMedicine" class="add-tabs">
        <button 
          class="tab-btn" 
          :class="{ active: addTab === 'manual' }"
          @click="addTab = 'manual'"
        >
          手动输入
        </button>
        <button 
          class="tab-btn" 
          :class="{ active: addTab === 'recognize' }"
          @click="addTab = 'recognize'"
        >
          图片识别
        </button>
      </div>

      <!-- 图片识别区域 -->
      <div v-if="addTab === 'recognize'" class="recognize-section">
        <!-- 图片上传组件 -->
        <el-upload
          class="image-uploader"
          action=""
          :show-file-list="false"
          :auto-upload="false"
          :on-change="handleImageSelect"
          accept="image/jpeg,image/png,image/gif,image/webp"
        >
          <div class="upload-area">
            <el-icon class="upload-icon"><Camera /></el-icon>
            <div class="upload-text">{{ recognizeLoading ? '识别中...' : '点击上传药品图片' }}</div>
            <div class="upload-hint">支持拍照或从相册选择</div>
          </div>
        </el-upload>
        
        <!-- 识别到的原始文本展示 -->
        <div v-if="recognizedText" class="recognized-text">
          <div class="recognized-label">识别到的文本：</div>
          <div class="recognized-content">{{ recognizedText }}</div>
        </div>

        <!-- 识别结果表单（可修改） -->
        <div v-if="recognizedInfo" class="recognized-info">
          <div class="recognized-info-label">识别结果（可修改）：</div>
          <div class="recognized-info-form">
            <div class="form-group">
              <label class="form-label">药品名称 *</label>
              <input type="text" class="form-input" v-model="form.name" placeholder="请输入药品名称" />
            </div>
            <div class="form-group">
              <label class="form-label">分类</label>
              <input type="text" class="form-input" v-model="form.category" placeholder="请输入药品分类" />
            </div>
            <div class="form-group">
              <label class="form-label">规格</label>
              <input type="text" class="form-input" v-model="form.spec" placeholder="请输入药品规格" />
            </div>
            <div class="form-group">
              <label class="form-label">生产厂家</label>
              <input type="text" class="form-input" v-model="form.manufacturer" placeholder="请输入生产厂家" />
            </div>
            <div class="form-group">
              <label class="form-label">数量 *</label>
              <input type="number" class="form-input" v-model="form.stock" placeholder="请输入数量" min="0" />
            </div>
          </div>
        </div>
      </div>

      <!-- 手动输入表单 -->
      <form v-else @submit.prevent="handleSubmit" class="medicine-form">
        <div class="form-group">
          <label class="form-label">药品名称 *</label>
          <input type="text" class="form-input" v-model="form.name" placeholder="请输入药品名称" />
        </div>
        <div class="form-group">
          <label class="form-label">分类</label>
          <input type="text" class="form-input" v-model="form.category" placeholder="请输入药品分类" />
        </div>
        <div class="form-group">
          <label class="form-label">规格</label>
          <input type="text" class="form-input" v-model="form.spec" placeholder="请输入药品规格" />
        </div>
        <div class="form-group">
          <label class="form-label">生产厂家</label>
          <input type="text" class="form-input" v-model="form.manufacturer" placeholder="请输入生产厂家" />
        </div>
        <div class="form-group">
          <label class="form-label">数量 *</label>
          <input type="number" class="form-input" v-model="form.stock" placeholder="请输入数量" min="0" />
        </div>
        <div class="form-group">
          <label class="form-label">有效期至</label>
          <input type="date" class="form-input" v-model="form.expireDate" />
        </div>
        <div class="form-group">
          <label class="form-label">低库存阈值（默认5）</label>
          <input type="number" class="form-input" v-model="form.lowStockThreshold" placeholder="5" min="0" />
        </div>
      </form>
      
      <!-- 弹窗底部按钮 -->
      <template #footer>
        <button class="btn-secondary" @click="closeModal">取消</button>
        <button class="btn-primary" @click="handleSubmit">保存</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 导入Vue组合式API
import { ref, reactive, onMounted, onActivated } from 'vue'
// 导入Element Plus消息提示和确认对话框组件
import { ElMessage, ElMessageBox } from 'element-plus'
// 导入药品API工具
import { medicineAPI } from '../utils/api'
// 导入Element Plus图标组件
import { Plus, Search, TakeawayBox, Edit, Delete, Camera, Download } from '@element-plus/icons-vue'

// 页面加载状态
const loading = ref(false)
// 添加/编辑弹窗显示状态
const showAddModal = ref(false)
// 搜索关键词
const searchQuery = ref('')
// 药品列表数据
const medicines = ref([])
// 当前编辑的药品对象
const editingMedicine = ref(null)
// 添加药品的Tab类型：manual手动输入 / recognize图片识别
const addTab = ref('manual')
// 图片识别加载状态
const recognizeLoading = ref(false)
// 识别到的原始文本
const recognizedText = ref('')
// 识别出的药品信息对象
const recognizedInfo = ref(null)
// 当前页码（从0开始）
const currentPage = ref(0)
// 每页显示数量
const pageSize = ref(20)
// 数据总条数
const total = ref(0)
// 药品分类列表
const categories = ref([])
// 选中的分类筛选条件
const filterCategory = ref('')
// 选中的状态筛选条件
const filterStatus = ref('')

// 药品表单数据
const form = reactive({
  // 药品名称
  name: '',
  // 药品分类
  category: '',
  // 药品规格
  spec: '',
  // 生产厂家
  manufacturer: '',
  // 库存数量
  stock: 0,
  // 有效期至
  expireDate: '',
  // 低库存阈值
  lowStockThreshold: 5
})

// 获取药品列表数据
async function fetchMedicines() {
  // 设置加载状态
  loading.value = true
  
  try {
    // 构造查询参数
    const response = await medicineAPI.getAll({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchQuery.value || null,
      category: filterCategory.value || null,
      lowStock: filterStatus.value === 'lowStock' ? 999 : null,
      expired: filterStatus.value === 'expired' ? true : null
    })
    // 保存药品列表
    medicines.value = response && response.list ? response.list : []
    // 保存总条数
    total.value = response && response.totalElements ? response.totalElements : 0
  } catch (error) {
    console.error('获取药品列表失败:', error)
    ElMessage.error(error.message || '获取药品列表失败')
  } finally {
    // 关闭加载状态
    loading.value = false
  }
}

// 获取药品分类列表
async function fetchCategories() {
  try {
    const response = await medicineAPI.getCategories()
    categories.value = response || []
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

// 格式化日期为本地格式
function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

// 每页条数改变时的处理
function handleSizeChange(val) {
  pageSize.value = val
  currentPage.value = 0
  fetchMedicines()
}

// 当前页码改变时的处理
function handleCurrentChange(val) {
  currentPage.value = val - 1
  fetchMedicines()
}

// 搜索/筛选处理
function handleSearch() {
  currentPage.value = 0
  fetchMedicines()
}

// 编辑药品，填充表单数据并打开弹窗
function editMedicine(medicine) {
  editingMedicine.value = medicine
  form.name = medicine.name
  form.category = medicine.category || ''
  form.spec = medicine.spec || ''
  form.manufacturer = medicine.manufacturer || ''
  form.stock = medicine.stock
  form.expireDate = medicine.expireDate ? new Date(medicine.expireDate).toISOString().split('T')[0] : ''
  form.lowStockThreshold = medicine.lowStockThreshold || 5
  showAddModal.value = true
}

// 删除药品，带确认对话框
function deleteMedicine(id) {
  ElMessageBox.confirm(
    '确定要删除这个药品吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await medicineAPI.delete(id)
      ElMessage.success('删除成功')
      fetchMedicines()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 关闭弹窗并重置表单
function closeModal() {
  showAddModal.value = false
  editingMedicine.value = null
  addTab.value = 'manual'
  recognizedText.value = ''
  recognizedInfo.value = null
  recognizeLoading.value = false
  form.name = ''
  form.category = ''
  form.spec = ''
  form.manufacturer = ''
  form.stock = 0
  form.expireDate = ''
  form.lowStockThreshold = 5
}

/** 支持的图片文件类型 */
const SUPPORTED_IMAGE_TYPES = ['jpg', 'jpeg', 'png', 'gif', 'webp']
const SUPPORTED_IMAGE_TYPES_TEXT = SUPPORTED_IMAGE_TYPES.map(t => t.toUpperCase()).join('、')

/** 验证文件扩展名是否在支持列表中 */
function isValidImageFile(file) {
  if (!file || !file.name) return false
  const ext = file.name.split('.').pop().toLowerCase()
  return SUPPORTED_IMAGE_TYPES.includes(ext)
}

// 处理图片选择和识别
async function handleImageSelect(file) {
  // 验证是否为支持的图片格式
  if (!file.raw || !isValidImageFile(file.raw)) {
    ElMessage.warning(`请选择有效的图片文件，支持格式：${SUPPORTED_IMAGE_TYPES_TEXT}`)
    return
  }

  recognizeLoading.value = true
  recognizedText.value = ''
  recognizedInfo.value = null

  try {
    // 构造FormData上传图片
    const formData = new FormData()
    formData.append('image', file.raw)

    // 调用图片识别API
    const response = await medicineAPI.recognize(formData)

    if (response && response.success) {
      // 保存识别到的原始文本
      recognizedText.value = response.recognizedText || ''

      // 保存识别出的药品信息并填充表单
      const info = response.medicineInfo
      if (info) {
        form.name = info.name || ''
        form.category = info.category || ''
        form.spec = info.spec || ''
        form.manufacturer = info.manufacturer || ''
        recognizedInfo.value = info
      }

      ElMessage.success(response.message || '识别成功')
    } else {
      ElMessage.warning(response?.message || '未识别到药品信息')
    }
  } catch (error) {
    console.error('图片识别失败:', error)
    // 优先使用后端返回的具体错误信息，其次使用通用提示
    const errMsg = error.message || '图片识别失败，请检查网络或重试'
    // 仅在错误信息不包含格式提示时，补充支持格式
    if (errMsg.includes('格式') || errMsg.includes('JPG') || errMsg.includes('PNG')) {
      ElMessage.error(errMsg)
    } else {
      ElMessage.error(`${errMsg}（支持格式：${SUPPORTED_IMAGE_TYPES_TEXT}）`)
    }
  } finally {
    recognizeLoading.value = false
  }
}

// 提交表单（新增或更新）
async function handleSubmit() {
  // 验证药品名称必填
  if (!form.name) {
    ElMessage.warning('请输入药品名称')
    return
  }
  
  loading.value = true
  
  try {
    if (editingMedicine.value) {
      // 编辑模式：调用更新API
      await medicineAPI.update(editingMedicine.value.id, form)
      ElMessage.success('更新成功')
    } else {
      // 新增模式：调用创建API
      await medicineAPI.create(form)
      ElMessage.success('添加成功')
    }
    // 关闭弹窗并刷新列表
    closeModal()
    fetchMedicines()
  } catch (error) {
    ElMessage.error(editingMedicine.value ? '更新失败' : '添加失败')
  } finally {
    loading.value = false
  }
}

// 导出药品数据为CSV文件
async function exportMedicines() {
  try {
    const csv = await medicineAPI.exportMedicines()
    
    // 创建Blob并触发下载
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    link.setAttribute('href', url)
    link.setAttribute('download', `medicines_${new Date().toISOString().split('T')[0]}.csv`)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 组件挂载时初始化数据
onMounted(() => {
  fetchMedicines()
  fetchCategories()
})

// 页面从缓存恢复时重新加载数据（keep-alive）
onActivated(() => {
  fetchMedicines()
  fetchCategories()
})
</script>

<style scoped>
/* 药品管理页面整体样式 */
.medicines-page {
  padding-top: 80px;
}

/* 页面头部样式 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

/* 搜索栏样式 */
.search-bar {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

/* 筛选按钮行样式 */
.filter-row {
  display: flex;
  gap: 12px;
}

/* 筛选下拉框宽度 */
.filter-row :deep(.el-select) {
  width: 160px;
}

/* 搜索输入框包裹层 */
.search-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

/* 搜索图标样式 */
.search-icon {
  position: absolute;
  left: 14px;
  font-size: 16px;
  z-index: 1;
}

/* 搜索输入框样式 */
.search-input {
  width: 100%;
  padding: 12px 16px 12px 44px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-sm);
  font-size: 15px;
  background: white;
  outline: none;
  transition: all var(--transition-fast);
}

/* 搜索输入框聚焦状态 */
.search-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* 药品列表网格布局 */
.medicines-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

/* 药品卡片样式 */
.medicine-card {
  background: white;
  border-radius: var(--border-radius-lg);
  padding: 24px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color);
  transition: all var(--transition-normal);
  animation: slideUp 0.4s ease forwards;
  opacity: 0;
}

/* 药品卡片悬停效果 */
.medicine-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--card-shadow-hover);
}

/* 低库存药品卡片左边框标识 */
.medicine-card.low-stock {
  border-left: 4px solid var(--warning-color);
}

/* 已过期药品卡片左边框标识 */
.medicine-card.expired {
  border-left: 4px solid var(--danger-color);
  opacity: 0.85;
}

/* 药品卡片头部样式 */
.medicine-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
  justify-content: space-between;
}

/* 状态标签组样式 */
.status-badges {
  display: flex;
  gap: 8px;
}

/* 药品图标样式 */
.medicine-icon {
  font-size: 40px;
  flex-shrink: 0;
}

/* 药品信息容器 */
.medicine-info {
  flex: 1;
}

/* 药品名称样式 */
.medicine-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
  letter-spacing: -0.01em;
}

/* 药品分类样式 */
.medicine-category {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 库存警报样式（未使用但保留） */
.stock-alert {
  font-size: 20px;
  animation: pulse 2s ease-in-out infinite;
}

/* 药品元信息网格 */
.medicine-meta {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

/* 单个元信息项 */
.meta-item {
  display: flex;
  flex-direction: column;
}

/* 元信息标签样式 */
.meta-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 4px;
}

/* 元信息值样式 */
.meta-value {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

/* 危险状态文字颜色 */
.meta-value.danger {
  color: var(--danger-color);
}

/* 药品操作按钮容器 */
.medicine-actions {
  display: flex;
  gap: 12px;
}

/* 操作按钮基础样式 */
.action-btn {
  flex: 1;
  padding: 10px 12px;
  border: none;
  border-radius: var(--border-radius-sm);
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

/* 编辑按钮样式 */
.action-btn.edit {
  background: var(--primary-50);
  color: var(--primary-color);
}

/* 编辑按钮悬停效果 */
.action-btn.edit:hover {
  background: rgba(37, 99, 235, 0.15);
}

/* 删除按钮样式 */
.action-btn.delete {
  background: var(--danger-50);
  color: var(--danger-color);
}

/* 删除按钮悬停效果 */
.action-btn.delete:hover {
  background: rgba(239, 68, 68, 0.15);
}

/* 药品表单样式 */
.medicine-form {
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
  border-radius: var(--border-radius-sm);
  font-size: 15px;
  transition: all var(--transition-fast);
  outline: none;
}

/* 表单输入框聚焦状态 */
.form-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
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

/* 空状态文字样式 */
.empty-text {
  font-size: 18px;
  color: var(--text-secondary);
  margin-bottom: 24px;
}

/* 添加Tab切换栏样式 */
.add-tabs {
  display: flex;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

/* Tab按钮样式 */
.tab-btn {
  flex: 1;
  padding: 10px 16px;
  border: none;
  background: transparent;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all var(--transition-fast);
}

/* Tab按钮悬停效果 */
.tab-btn:hover {
  color: var(--primary-color);
}

/* Tab按钮激活状态 */
.tab-btn.active {
  color: var(--primary-color);
  border-bottom-color: var(--primary-color);
}

/* 图片识别区域样式 */
.recognize-section {
  margin-top: 8px;
}

/* 图片上传组件样式 */
.image-uploader {
  margin-bottom: 16px;
}

/* 上传区域样式 */
.upload-area {
  border: 2px dashed var(--border-color);
  border-radius: var(--border-radius-lg);
  padding: 32px 24px;
  text-align: center;
  cursor: pointer;
  transition: all var(--transition-fast);
}

/* 上传区域悬停效果 */
.upload-area:hover {
  border-color: var(--primary-color);
  background: rgba(37, 99, 235, 0.03);
}

/* 上传图标样式 */
.upload-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

/* 上传文字样式 */
.upload-text {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

/* 上传提示文字样式 */
.upload-hint {
  font-size: 13px;
  color: var(--text-muted);
}

/* 识别文本展示区域 */
.recognized-text {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--primary-50);
  border-radius: var(--border-radius-sm);
}

/* 识别文本标签 */
.recognized-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--primary-color);
  margin-bottom: 8px;
}

/* 识别文本内容 */
.recognized-content {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.6;
  word-break: break-all;
}

/* 识别结果信息区域 */
.recognized-info {
  margin-top: 16px;
}

/* 识别结果标签 */
.recognized-info-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 12px;
  padding: 8px 12px;
  background: rgba(34, 197, 94, 0.08);
  border-radius: var(--border-radius-sm);
  display: inline-block;
}

/* 识别结果表单区域 */
.recognized-info-form {
  margin-top: 8px;
}

/* 分页容器样式 */
.pagination-container {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

/* 响应式布局 - 移动端适配 */
@media (max-width: 600px) {
  /* 移动端页面头部纵向排列 */
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  /* 移动端搜索栏宽度限制 */
  .search-bar {
    max-width: 100%;
  }
  
  /* 移动端药品元信息单列布局 */
  .medicine-meta {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  /* 移动端操作按钮纵向排列 */
  .medicine-actions {
    flex-direction: column;
  }
  
  /* 移动端分页内边距调整 */
  .pagination-container {
    padding: 0 12px;
  }
}
</style>
