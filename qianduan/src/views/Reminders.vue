<template>
  <!-- 用药提醒页面主容器 -->
  <div class="reminders-page">
    <div class="page-container">
      <!-- 页面头部：标题和操作按钮 -->
      <div class="page-header">
        <h2 class="section-title">用药提醒</h2>
        <div class="header-actions">
          <!-- 导出CSV按钮 -->
          <button class="btn-outline" @click="exportCSV" :disabled="reminders.length === 0">
            <el-icon><Download /></el-icon>
            <span>导出</span>
          </button>
          <!-- 添加提醒按钮 -->
          <button class="btn-primary" @click="showAddModal = true">
            <el-icon><Plus /></el-icon>
            <span>添加提醒</span>
          </button>
        </div>
      </div>

      <!-- 统计卡片区域：总提醒数/已服药/待服药/依从率 -->
      <div class="stats-cards">
        <div class="stat-card total">
          <div class="stat-icon"><el-icon><Clock /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">总提醒</div>
          </div>
        </div>
        <div class="stat-card pending">
          <div class="stat-icon"><el-icon><Bell /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.pending }}</div>
            <div class="stat-label">待服药</div>
          </div>
        </div>
        <div class="stat-card taken">
          <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.taken }}</div>
            <div class="stat-label">已服药</div>
          </div>
        </div>
        <div class="stat-card rate">
          <div class="stat-icon"><el-icon><DataLine /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.complianceRate }}%</div>
            <div class="stat-label">依从率</div>
          </div>
        </div>
      </div>

      <!-- 工具栏：筛选标签 + 排序 + 批量操作 -->
      <div class="toolbar">
        <!-- 筛选标签栏：全部/待服药/已服药 -->
        <div class="filter-tabs">
          <button
            v-for="tab in filterTabs"
            :key="tab.key"
            class="filter-btn"
            :class="{ 'filter-active': activeFilter === tab.key }"
            @click="activeFilter = tab.key"
          >
            {{ tab.label }}
            <span class="filter-count">{{ getFilterCount(tab.key) }}</span>
          </button>
        </div>
        <!-- 排序选择器 -->
        <div class="sort-area">
          <el-select v-model="sortBy" placeholder="排序方式" size="small" style="width: 140px">
            <el-option label="按提醒时间" value="time" />
            <el-option label="按药品名称" value="name" />
            <el-option label="按创建时间" value="id" />
          </el-select>
          <button class="sort-order-btn" @click="toggleSortOrder" :class="{ 'desc': sortOrder === 'desc' }" :title="sortOrder === 'asc' ? '切换为降序' : '切换为升序'">
            <el-icon><ArrowUp /></el-icon>
          </button>
        </div>
      </div>

      <!-- 批量操作栏（选中提醒时显示） -->
      <div v-if="selectedIds.length > 0" class="batch-bar">
        <span class="batch-info">已选择 {{ selectedIds.length }} 项</span>
        <div class="batch-actions">
          <button class="btn-batch take" @click="batchMarkTaken">
            <el-icon><Check /></el-icon> 批量标记已服药
          </button>
          <button class="btn-batch delete" @click="batchDelete">
            <el-icon><Delete /></el-icon> 批量删除
          </button>
          <button class="btn-batch cancel" @click="clearSelection">取消选择</button>
        </div>
      </div>

      <!-- 加载中状态 -->
      <div v-if="loading" class="loading-spinner">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      </div>

      <!-- 空状态 -->
      <div v-else-if="sortedReminders.length === 0" class="empty-state">
        <el-icon class="empty-icon"><Clock /></el-icon>
        <div class="empty-text">暂无用药提醒</div>
        <button class="btn-primary" @click="showAddModal = true">添加提醒</button>
      </div>

      <!-- 提醒列表区域 -->
      <div v-else class="reminders-list">
        <!-- 全选复选框 -->
        <div class="select-all-row">
          <label class="checkbox-label">
            <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" />
            <span>全选</span>
          </label>
        </div>
        <!-- 单个提醒卡片 -->
        <div
          class="reminder-card"
          v-for="(reminder, index) in sortedReminders"
          :key="reminder.id"
          :class="{ 'reminder-done': reminder.taken, 'reminder-selected': selectedIds.includes(reminder.id) }"
          :style="{ animationDelay: `${index * 0.04}s` }"
        >
          <!-- 复选框 -->
          <input
            type="checkbox"
            class="reminder-checkbox"
            :checked="selectedIds.includes(reminder.id)"
            @change="toggleSelect(reminder.id)"
          />
          <!-- 提醒时间区域 -->
          <div class="reminder-time">
            <div class="time-value">{{ formatTime(reminder.time) }}</div>
            <div class="time-label">{{ reminder.frequency }}</div>
          </div>
          <!-- 提醒详细信息 -->
          <div class="reminder-info">
            <h3 class="reminder-medicine">{{ reminder.medicineName }}</h3>
            <p class="reminder-dosage">剂量: {{ reminder.dosage }}</p>
            <div class="reminder-meta">
              <span v-if="reminder.startDate" class="meta-tag">起: {{ formatDate(reminder.startDate) }}</span>
              <span v-if="reminder.endDate" class="meta-tag">止: {{ formatDate(reminder.endDate) }}</span>
              <span v-if="reminder.advanceNotice" class="meta-tag">提前{{ reminder.advanceNotice }}分钟</span>
            </div>
            <p v-if="reminder.remark" class="reminder-remark">{{ reminder.remark }}</p>
          </div>
          <!-- 服药状态标签 -->
          <div class="reminder-status">
            <span v-if="reminder.taken" class="status-tag done">已服药</span>
            <span v-else class="status-tag pending">待服药</span>
          </div>
          <!-- 操作按钮区域 -->
          <div class="reminder-actions">
            <button v-if="!reminder.taken" class="action-btn take" @click="markTaken(reminder.id)">
              <el-icon><Check /></el-icon> 已服药
            </button>
            <button class="action-btn history" @click="showHistory(reminder)" title="服药历史">
              <el-icon><Histogram /></el-icon>
            </button>
            <button class="action-btn edit" @click="editReminder(reminder)">
              <el-icon><Edit /></el-icon>
            </button>
            <button class="action-btn delete" @click="deleteReminder(reminder.id)">
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

    <!-- 添加/编辑提醒弹窗 -->
    <el-dialog
      :title="editingReminder ? '编辑提醒' : '添加提醒'"
      v-model="showAddModal"
      width="520px"
      :close-on-click-modal="false"
      @closed="onDialogClosed"
    >
      <form @submit.prevent="handleSubmit" class="reminder-form">
        <!-- 选择药品 -->
        <div class="form-group">
          <label class="form-label">选择药品 *</label>
          <el-select
            v-model="form.medicineId"
            placeholder="请选择药品"
            filterable
            clearable
            style="width: 100%"
            :loading="medicinesLoading"
            @visible-change="onMedicineSelectVisible"
            @change="onMedicineChange"
          >
            <el-option v-for="med in medicines" :key="med.id" :label="med.name" :value="med.id">
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

        <!-- 提醒时间 + 提前通知 -->
        <div class="form-row">
          <div class="form-group form-group-half">
            <label class="form-label">提醒时间 *</label>
            <input type="time" class="form-input" v-model="form.time" />
          </div>
          <div class="form-group form-group-half">
            <label class="form-label">提前通知</label>
            <select class="form-input" v-model.number="form.advanceNotice">
              <option :value="0">准时通知</option>
              <option :value="5">提前 5 分钟</option>
              <option :value="10">提前 10 分钟</option>
              <option :value="15">提前 15 分钟</option>
              <option :value="30">提前 30 分钟</option>
            </select>
          </div>
        </div>

        <!-- 频率 + 剂量 -->
        <div class="form-row">
          <div class="form-group form-group-half">
            <label class="form-label">频率 *</label>
            <select class="form-input" v-model="form.frequency">
              <option value="每日一次">每日一次</option>
              <option value="每日两次">每日两次</option>
              <option value="每日三次">每日三次</option>
              <option value="每周一次">每周一次</option>
              <option value="每周两次">每周两次</option>
            </select>
          </div>
          <div class="form-group form-group-half">
            <label class="form-label">剂量 *</label>
            <input type="text" class="form-input" v-model="form.dosage" placeholder="如：1片" />
          </div>
        </div>

        <!-- 提醒有效期：日期范围选择 -->
        <div class="form-group">
          <label class="form-label">提醒有效期</label>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            :disabled-date="disabledPastDate"
          />
          <div class="form-hint">不选择则永久有效</div>
        </div>

        <!-- 重复结束条件（仅重复提醒时显示） -->
        <div v-if="isRepeating" class="form-group">
          <label class="form-label">重复结束条件</label>
          <el-radio-group v-model="form.repeatEndType" class="repeat-end-group">
            <el-radio value="never">永不结束</el-radio>
            <el-radio value="endDate">结束于指定日期</el-radio>
            <el-radio value="count">重复指定次数</el-radio>
          </el-radio-group>
          <!-- 结束于日期：日期选择器 -->
          <div v-if="form.repeatEndType === 'endDate'" class="repeat-end-config">
            <el-date-picker
              v-model="form.repeatEndDate"
              type="date"
              placeholder="选择结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              :disabled-date="disabledPastDate"
            />
          </div>
          <!-- 重复N次：次数输入 -->
          <div v-if="form.repeatEndType === 'count'" class="repeat-end-config">
            <input type="number" class="form-input" v-model.number="form.repeatCount" min="1" placeholder="重复次数" />
          </div>
        </div>

        <!-- 提醒音效选择 -->
        <div class="form-group">
          <label class="form-label">提醒音效</label>
          <div class="sound-select-wrapper">
            <select class="form-input" v-model="form.soundEffect">
              <option value="default">默认铃声</option>
              <option value="gentle">轻柔提示</option>
              <option value="urgent">紧急提醒</option>
              <option value="melody">旋律音乐</option>
              <option value="silent">静音</option>
            </select>
            <button class="btn-preview" @click="previewSound" :disabled="form.soundEffect === 'silent'" title="预览音效">
              <el-icon><VideoPlay /></el-icon>
            </button>
          </div>
        </div>

        <!-- 备注输入 -->
        <div class="form-group">
          <label class="form-label">备注</label>
          <textarea class="form-input" v-model="form.remark" placeholder="请输入备注信息" rows="2"></textarea>
        </div>
      </form>

      <!-- 弹窗底部按钮 -->
      <template #footer>
        <button class="btn-secondary" @click="closeModal">取消</button>
        <button class="btn-primary" @click="handleSubmit">保存</button>
      </template>
    </el-dialog>

    <!-- 服药历史记录弹窗 -->
    <el-dialog
      title="服药历史记录"
      v-model="showHistoryModal"
      width="480px"
    >
      <div v-if="historyReminder" class="history-header">
        <h4>{{ historyReminder.medicineName }}</h4>
        <span class="history-dosage">剂量: {{ historyReminder.dosage }} | {{ historyReminder.frequency }}</span>
      </div>
      <div v-if="historyRecords.length > 0" class="history-list">
        <div v-for="(record, idx) in historyRecords" :key="idx" class="history-item">
          <div class="history-time">{{ record.time }}</div>
          <div class="history-status">
            <span class="status-tag" :class="record.taken ? 'done' : 'pending'">
              {{ record.taken ? '已服药' : '未服药' }}
            </span>
          </div>
        </div>
      </div>
      <div v-else class="history-empty">暂无历史记录</div>
      <template #footer>
        <button class="btn-secondary" @click="showHistoryModal = false">关闭</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 引入Vue组合式API
import { ref, computed, reactive, onMounted, onActivated } from 'vue'
// 引入Element Plus消息提示和弹窗组件
import { ElMessage, ElMessageBox } from 'element-plus'
// 引入API接口
import { reminderAPI, medicineAPI } from '../utils/api'
// 引入图标组件
import { Plus, Clock, Check, Edit, Delete, Bell, CircleCheck, DataLine, Download, Histogram, Loading, VideoPlay, ArrowUp } from '@element-plus/icons-vue'

// ==================== 状态变量 ====================

// 加载状态
const loading = ref(false)
// 控制添加/编辑弹窗显示
const showAddModal = ref(false)
// 提醒列表数据
const reminders = ref([])
// 当前激活的筛选标签
const activeFilter = ref('all')
// 当前编辑的提醒对象
const editingReminder = ref(null)
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
// 排序方式：time(按提醒时间) / name(按药品名称) / id(按创建时间)
const sortBy = ref('time')
// 排序方向：asc(升序) / desc(降序)
const sortOrder = ref('asc')
// 选中的提醒ID列表（用于批量操作）
const selectedIds = ref([])

// 服药历史弹窗状态
const showHistoryModal = ref(false)
// 当前查看历史的提醒对象
const historyReminder = ref(null)
// 历史记录列表
const historyRecords = ref([])

// ==================== 表单数据 ====================

// 日期范围（el-date-picker daterange 绑定值，[startDate, endDate]）
const dateRange = ref([])

// 表单响应式数据
const form = reactive({
  medicineId: '',        // 药品ID
  medicineName: '',      // 药品名称
  time: '',              // 提醒时间（HH:mm 格式）
  frequency: '每日一次', // 服药频率
  dosage: '',            // 剂量
  remark: '',            // 备注
  advanceNotice: 0,      // 提前通知时间（分钟），0表示准时通知
  soundEffect: 'default',// 提醒音效：default/gentle/urgent/melody/silent
  repeatEndType: 'never',// 重复结束类型：never(永不结束)/endDate(结束于日期)/count(重复N次)
  repeatEndDate: '',     // 重复结束日期（repeatEndType='endDate'时使用）
  repeatCount: 1         // 重复次数（repeatEndType='count'时使用）
})

// ==================== 计算属性 ====================

/**
 * isRepeating：判断当前表单是否为重复提醒
 * 逻辑：频率不为"每日一次"时视为重复提醒
 * 对应后端字段 isRepeat，前端使用 isRepeating 命名更语义化
 */
const isRepeating = computed(() => {
  return form.frequency !== '每日一次'
})

/**
 * 统计数据：根据提醒列表计算总提醒数、已服药数、待服药数、依从率
 */
const stats = computed(() => {
  const total = reminders.value.length
  const taken = reminders.value.filter(r => r.taken).length
  const pending = total - taken
  const complianceRate = total > 0 ? ((taken / total) * 100).toFixed(1) : '0.0'
  return { total, taken, pending, complianceRate }
})

/**
 * 排序后的提醒列表：先按筛选条件过滤，再按排序方式排序
 */
const sortedReminders = computed(() => {
  let list = reminders.value
  // 按筛选条件过滤
  if (activeFilter.value === 'pending') {
    list = list.filter(r => !r.taken)
  } else if (activeFilter.value === 'taken') {
    list = list.filter(r => r.taken)
  }
  // 按排序方式排序
  const sorted = [...list]
  let comparator
  if (sortBy.value === 'time') {
    comparator = (a, b) => (a.time || '').localeCompare(b.time || '')
  } else if (sortBy.value === 'name') {
    comparator = (a, b) => (a.medicineName || '').localeCompare(b.medicineName || '')
  } else if (sortBy.value === 'id') {
    comparator = (a, b) => (a.id || 0) - (b.id || 0)
  } else {
    comparator = () => 0
  }
  sorted.sort((a, b) => {
    const result = comparator(a, b)
    return sortOrder.value === 'desc' ? -result : result
  })
  return sorted
})

/**
 * 是否全选当前列表
 */
const isAllSelected = computed(() => {
  if (sortedReminders.value.length === 0) return false
  return sortedReminders.value.every(r => selectedIds.value.includes(r.id))
})

// ==================== 筛选标签配置 ====================

const filterTabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待服药' },
  { key: 'taken', label: '已服药' }
]

// ==================== 数据获取 ====================

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

// 药品选择变化时，同步更新药品名称
function onMedicineChange(id) {
  const med = medicines.value.find(m => m.id === id)
  form.medicineName = med ? med.name : ''
}

// 获取提醒列表数据
async function fetchReminders() {
  loading.value = true
  try {
    const response = await reminderAPI.getAll(currentPage.value, pageSize.value)
    reminders.value = response && response.list ? response.list : []
    total.value = response && response.totalElements ? response.totalElements : 0
  } catch (error) {
    console.error('获取提醒列表失败:', error)
    ElMessage.error(error.message || '获取提醒列表失败')
  } finally {
    loading.value = false
  }
}

// 每页数量变化
function handleSizeChange(val) {
  pageSize.value = val
  currentPage.value = 0
  fetchReminders()
}

// 当前页变化
function handleCurrentChange(val) {
  currentPage.value = val - 1
  fetchReminders()
}

// ==================== 统计与筛选 ====================

// 获取指定筛选条件下的提醒数量
function getFilterCount(key) {
  if (key === 'pending') return reminders.value.filter(r => !r.taken).length
  if (key === 'taken') return reminders.value.filter(r => r.taken).length
  return reminders.value.length
}

// 切换排序方向
function toggleSortOrder() {
  sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
}

// ==================== 单个操作 ====================

// 标记提醒为已服药，同时记录到本地历史
async function markTaken(id) {
  try {
    await reminderAPI.markTaken(id)
    ElMessage.success('已标记为已服药')
    // 记录服药历史到本地存储
    addHistoryRecord(id, true)
    fetchReminders()
  } catch (error) {
    ElMessage.error('标记失败')
  }
}

// 编辑提醒：填充表单数据并打开弹窗
function editReminder(reminder) {
  editingReminder.value = reminder
  form.medicineId = reminder.medicineId || ''
  form.medicineName = reminder.medicineName
  form.time = reminder.time
  form.frequency = reminder.frequency
  form.dosage = reminder.dosage
  form.remark = reminder.remark || ''
  form.advanceNotice = reminder.advanceNotice || 0
  form.soundEffect = reminder.soundEffect || 'default'
  form.repeatEndType = reminder.repeatEndType || 'never'
  form.repeatEndDate = reminder.repeatEndDate || ''
  form.repeatCount = reminder.repeatCount || 1
  // 设置日期范围
  dateRange.value = []
  if (reminder.startDate) {
    dateRange.value[0] = reminder.startDate
  }
  if (reminder.endDate) {
    dateRange.value[1] = reminder.endDate
  }
  showAddModal.value = true
}

// 删除提醒
function deleteReminder(id) {
  ElMessageBox.confirm('确定要删除这个提醒吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await reminderAPI.delete(id)
      ElMessage.success('删除成功')
      // 从选中列表中移除
      selectedIds.value = selectedIds.value.filter(sid => sid !== id)
      fetchReminders()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// ==================== 批量操作 ====================

// 切换单个选中状态
function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx > -1) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

// 切换全选/取消全选
function toggleSelectAll() {
  if (isAllSelected.value) {
    // 取消全选：移除当前列表中的所有ID
    const currentIds = sortedReminders.value.map(r => r.id)
    selectedIds.value = selectedIds.value.filter(id => !currentIds.includes(id))
  } else {
    // 全选：添加当前列表中的所有ID（去重）
    const currentIds = sortedReminders.value.map(r => r.id)
    selectedIds.value = [...new Set([...selectedIds.value, ...currentIds])]
  }
}

// 清空选择
function clearSelection() {
  selectedIds.value = []
}

// 批量标记已服药
function batchMarkTaken() {
  ElMessageBox.confirm(
    `确定要将选中的 ${selectedIds.value.length} 个提醒标记为已服药吗？`,
    '批量操作',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    let successCount = 0
    let failCount = 0
    for (const id of selectedIds.value) {
      try {
        await reminderAPI.markTaken(id)
        addHistoryRecord(id, true)
        successCount++
      } catch {
        failCount++
      }
    }
    ElMessage.success(`批量标记完成：成功 ${successCount} 个${failCount > 0 ? `，失败 ${failCount} 个` : ''}`)
    clearSelection()
    fetchReminders()
  }).catch(() => {})
}

// 批量删除
function batchDelete() {
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedIds.value.length} 个提醒吗？此操作不可恢复。`,
    '批量删除',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'error' }
  ).then(async () => {
    let successCount = 0
    let failCount = 0
    for (const id of selectedIds.value) {
      try {
        await reminderAPI.delete(id)
        successCount++
      } catch {
        failCount++
      }
    }
    ElMessage.success(`批量删除完成：成功 ${successCount} 个${failCount > 0 ? `，失败 ${failCount} 个` : ''}`)
    clearSelection()
    fetchReminders()
  }).catch(() => {})
}

// ==================== 音效预览功能 ====================

// 音效频率映射表
const SOUND_FREQUENCIES = {
  default: { start: 800, end: 800, duration: 0.3 },
  gentle: { start: 523, end: 659, duration: 0.4 },
  urgent: { start: 880, end: 1100, duration: 0.2 },
  melody: { start: 523, end: 784, duration: 0.5 }
}

// 预览音效（使用Web Audio API）
function previewSound() {
  try {
    const audioContext = new (window.AudioContext || window.webkitAudioContext)()
    const sound = SOUND_FREQUENCIES[form.soundEffect] || SOUND_FREQUENCIES.default
    
    const oscillator = audioContext.createOscillator()
    const gainNode = audioContext.createGain()
    
    oscillator.connect(gainNode)
    gainNode.connect(audioContext.destination)
    
    oscillator.type = form.soundEffect === 'melody' ? 'sine' : 'square'
    oscillator.frequency.setValueAtTime(sound.start, audioContext.currentTime)
    oscillator.frequency.linearRampToValueAtTime(sound.end, audioContext.currentTime + sound.duration)
    
    gainNode.gain.setValueAtTime(0.3, audioContext.currentTime)
    gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + sound.duration)
    
    oscillator.start(audioContext.currentTime)
    oscillator.stop(audioContext.currentTime + sound.duration)
    
    ElMessage.success('音效预览成功')
  } catch (e) {
    console.error('音效预览失败:', e)
    ElMessage.warning('浏览器不支持音效预览')
  }
}

// ==================== 服药历史记录 ====================

// 本地存储key前缀
const HISTORY_KEY = 'reminder_history'

// 获取本地存储的服药历史
function getLocalHistory() {
  try {
    const data = localStorage.getItem(HISTORY_KEY)
    return data ? JSON.parse(data) : {}
  } catch {
    return {}
  }
}

// 添加服药历史记录到本地存储
function addHistoryRecord(reminderId, taken) {
  const history = getLocalHistory()
  if (!history[reminderId]) {
    history[reminderId] = []
  }
  history[reminderId].push({
    time: new Date().toLocaleString('zh-CN'),
    taken: taken
  })
  // 最多保留50条记录
  if (history[reminderId].length > 50) {
    history[reminderId] = history[reminderId].slice(-50)
  }
  localStorage.setItem(HISTORY_KEY, JSON.stringify(history))
}

// 显示服药历史弹窗
function showHistory(reminder) {
  historyReminder.value = reminder
  const history = getLocalHistory()
  const records = history[reminder.id] || []
  // 倒序显示，最新的在前
  historyRecords.value = records.slice().reverse()
  showHistoryModal.value = true
}

// ==================== 表单提交 ====================

// 关闭弹窗后的回调
function onDialogClosed() {
  resetForm()
}

// 重置表单
function resetForm() {
  editingReminder.value = null
  form.medicineId = ''
  form.medicineName = ''
  form.time = ''
  form.frequency = '每日一次'
  form.dosage = ''
  form.remark = ''
  form.advanceNotice = 0
  form.soundEffect = 'default'
  form.repeatEndType = 'never'
  form.repeatEndDate = ''
  form.repeatCount = 1
  dateRange.value = []
}

// 关闭弹窗
function closeModal() {
  showAddModal.value = false
}

// 提交表单：新增或更新提醒
async function handleSubmit() {
  // 表单校验
  if (!form.medicineId || !form.time || !form.dosage) {
    ElMessage.warning('请填写必填项')
    return
  }

  // 重复结束条件校验
  if (isRepeating.value) {
    if (form.repeatEndType === 'endDate' && !form.repeatEndDate) {
      ElMessage.warning('请选择重复结束日期')
      return
    }
    if (form.repeatEndType === 'count' && (!form.repeatCount || form.repeatCount < 1)) {
      ElMessage.warning('请输入有效的重复次数')
      return
    }
  }

  // 从日期范围提取 startDate / endDate
  const startDate = dateRange.value && dateRange.value[0] ? dateRange.value[0] : null
  const endDate = dateRange.value && dateRange.value[1] ? dateRange.value[1] : null

  // 构造提交数据
  // isRepeating 对应后端字段 isRepeat，表示是否为周期性重复提醒
  const submitData = {
    medicineId: form.medicineId,
    medicineName: form.medicineName,
    time: form.time,
    frequency: form.frequency,
    dosage: form.dosage,
    remark: form.remark,
    isRepeat: isRepeating.value,    // 后端字段名 isRepeat，值由频率判断得出
    startDate: startDate,            // 提醒开始日期
    endDate: endDate,               // 提醒结束日期
    advanceNotice: form.advanceNotice,    // 提前通知时间（分钟）
    soundEffect: form.soundEffect,        // 提醒音效
    repeatEndType: form.repeatEndType,    // 重复结束类型
    repeatEndDate: form.repeatEndDate,    // 重复结束日期
    repeatCount: form.repeatCount         // 重复次数
  }

  // 如果重复结束类型为 endDate，将结束日期也写入 endDate 字段
  if (isRepeating.value && form.repeatEndType === 'endDate' && form.repeatEndDate) {
    submitData.endDate = form.repeatEndDate
  }

  loading.value = true
  try {
    if (editingReminder.value) {
      await reminderAPI.update(editingReminder.value.id, submitData)
      ElMessage.success('更新成功')
    } else {
      await reminderAPI.create(submitData)
      ElMessage.success('添加成功')
    }
    closeModal()
    fetchReminders()
  } catch (error) {
    ElMessage.error(editingReminder.value ? '更新失败' : '添加失败')
  } finally {
    loading.value = false
  }
}

// ==================== CSV导出 ====================

// 音效名称映射表
const SOUND_LABELS = {
  default: '默认铃声',
  gentle: '轻柔提示',
  urgent: '紧急提醒',
  melody: '旋律音乐',
  silent: '静音'
}

// 导出提醒数据为CSV文件
function exportCSV() {
  if (reminders.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }

  // CSV表头（包含音效、提前通知时间等字段）
  const headers = ['药品名称', '提醒时间', '频率', '剂量', '提前通知', '提醒音效', '状态', '开始日期', '结束日期', '备注']
  // CSV行数据
  const rows = sortedReminders.value.map(r => [
    r.medicineName || '',
    formatTime(r.time) || '',
    r.frequency || '',
    r.dosage || '',
    r.advanceNotice ? `提前${r.advanceNotice}分钟` : '准时',
    SOUND_LABELS[r.soundEffect] || '默认铃声',
    r.taken ? '已服药' : '待服药',
    r.startDate || '',
    r.endDate || '',
    r.remark || ''
  ])

  // 拼接CSV字符串（添加BOM头解决中文乱码）
  const csvContent = '\uFEFF' + [headers, ...rows]
    .map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    .join('\n')

  // 创建下载链接
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', `用药提醒_${new Date().toISOString().slice(0, 10)}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

// ==================== 工具函数 ====================

// 格式化时间字符串，只保留 HH:mm
function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.substring(0, 5)
}

// 格式化日期字符串
function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.length > 10 ? dateStr.substring(0, 10) : dateStr
}

// 禁用过去日期（日期选择器用）
function disabledPastDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

// ==================== 生命周期 ====================

// 组件挂载时获取提醒列表和药品列表
onMounted(() => {
  fetchReminders()
})

// 页面从缓存恢复时重新加载数据
onActivated(() => {
  fetchReminders()
})
</script>

<style scoped>
/* 页面整体样式 */
.reminders-page {
  padding-top: 80px;
}

/* 页面头部样式 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 统计卡片区域 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: var(--border-radius-lg);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color);
  transition: transform var(--transition-fast);
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.stat-card.total .stat-icon { background: var(--primary-50); color: var(--primary-color); }
.stat-card.pending .stat-icon { background: var(--warning-50); color: var(--warning-color); }
.stat-card.taken .stat-icon { background: var(--success-50); color: var(--success-color); }
.stat-card.rate .stat-icon { background: #ede9fe; color: #7c3aed; }

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 2px;
}

/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-tabs {
  display: flex;
  gap: 8px;
}

.filter-btn {
  padding: 10px 20px;
  border: 1px solid var(--border-color);
  border-radius: 24px;
  background: white;
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-fast);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.filter-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.filter-active {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: white;
}

.filter-count {
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.filter-active .filter-count {
  background: rgba(255, 255, 255, 0.2);
}

/* 批量操作栏 */
.batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--primary-50);
  border: 1px solid var(--primary-color);
  border-radius: var(--border-radius-sm);
  padding: 12px 20px;
  margin-bottom: 16px;
}

.batch-info {
  font-size: 14px;
  font-weight: 500;
  color: var(--primary-color);
}

.batch-actions {
  display: flex;
  gap: 8px;
}

.btn-batch {
  padding: 8px 16px;
  border: none;
  border-radius: var(--border-radius-sm);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn-batch.take { background: var(--success-color); color: white; }
.btn-batch.take:hover { opacity: 0.9; }
.btn-batch.delete { background: var(--danger-color); color: white; }
.btn-batch.delete:hover { opacity: 0.9; }
.btn-batch.cancel { background: white; color: var(--text-secondary); border: 1px solid var(--border-color); }
.btn-batch.cancel:hover { background: var(--bg-secondary); }

/* 提醒列表 */
.reminders-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 全选行 */
.select-all-row {
  padding: 4px 0;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-secondary);
}

.checkbox-label input {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

/* 提醒卡片 */
.reminder-card {
  background: white;
  border-radius: var(--border-radius-lg);
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--border-color);
  border-left: 4px solid var(--warning-color);
  transition: all var(--transition-normal);
  animation: slideUp 0.4s ease forwards;
  opacity: 0;
}

.reminder-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--card-shadow-hover);
}

.reminder-card.reminder-done {
  opacity: 0.6;
  border-left-color: var(--success-color);
}

.reminder-card.reminder-selected {
  border-color: var(--primary-color);
  background: var(--primary-50);
}

/* 复选框 */
.reminder-checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
  flex-shrink: 0;
}

/* 时间区域 */
.reminder-time {
  text-align: center;
  min-width: 90px;
}

.time-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.time-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

/* 信息区域 */
.reminder-info {
  flex: 1;
  min-width: 0;
}

.reminder-medicine {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.reminder-dosage {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.reminder-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.meta-tag {
  font-size: 12px;
  color: var(--text-muted);
  background: var(--bg-secondary);
  padding: 2px 8px;
  border-radius: 4px;
}

.reminder-remark {
  font-size: 13px;
  color: var(--text-muted);
  font-style: italic;
  margin-top: 4px;
}

/* 状态区域 */
.reminder-status {
  flex-shrink: 0;
}

.status-tag {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.status-tag.pending {
  background: var(--warning-50);
  color: var(--warning-color);
}

.status-tag.done {
  background: var(--success-50);
  color: var(--success-color);
}

/* 操作按钮 */
.reminder-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.action-btn {
  padding: 8px 12px;
  border: none;
  border-radius: var(--border-radius-sm);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-btn.take { background: var(--success-50); color: var(--success-color); }
.action-btn.take:hover { background: rgba(16, 185, 129, 0.15); }
.action-btn.history { background: #ede9fe; color: #7c3aed; }
.action-btn.history:hover { background: rgba(124, 58, 237, 0.15); }
.action-btn.edit { background: var(--primary-50); color: var(--primary-color); }
.action-btn.edit:hover { background: rgba(37, 99, 235, 0.15); }
.action-btn.delete { background: var(--danger-50); color: var(--danger-color); }
.action-btn.delete:hover { background: rgba(239, 68, 68, 0.15); }

/* 表单 */
.reminder-form {
  display: flex;
  flex-direction: column;
}

.form-group {
  margin-bottom: 18px;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-group-half {
  flex: 1;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-sm);
  font-size: 14px;
  transition: all var(--transition-fast);
  outline: none;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.form-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.repeat-end-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}

.repeat-end-config {
  margin-top: 8px;
}

/* 音效选择器包装 */
.sound-select-wrapper {
  display: flex;
  gap: 8px;
}

.sound-select-wrapper .form-input {
  flex: 1;
}

.btn-preview {
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-sm);
  background: white;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
}

.btn-preview:hover:not(:disabled) {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.btn-preview:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 排序方向按钮 */
.sort-area {
  display: flex;
  align-items: center;
  gap: 4px;
}

.sort-order-btn {
  padding: 6px 10px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-sm);
  background: white;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
}

.sort-order-btn:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.sort-order-btn.desc {
  transform: rotate(180deg);
}

/* 按钮样式 */
.btn-primary {
  padding: 10px 20px;
  background: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--border-radius-sm);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-primary:hover {
  background: var(--primary-dark);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 10px 20px;
  background: white;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-sm);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-secondary:hover {
  background: var(--bg-secondary);
}

.btn-outline {
  padding: 10px 16px;
  background: white;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-sm);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-outline:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.btn-outline:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: var(--text-muted);
}

.empty-text {
  font-size: 18px;
  color: var(--text-secondary);
  margin-bottom: 24px;
}

/* 加载状态 */
.loading-spinner {
  text-align: center;
  padding: 80px 20px;
  color: var(--primary-color);
}

/* 分页 */
.pagination-container {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

/* 服药历史弹窗 */
.history-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.history-header h4 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.history-dosage {
  font-size: 13px;
  color: var(--text-muted);
}

.history-list {
  max-height: 400px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-color);
}

.history-item:last-child {
  border-bottom: none;
}

.history-time {
  font-size: 14px;
  color: var(--text-primary);
}

.history-empty {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-muted);
  font-size: 14px;
}

/* 药品选择器选项 */
.medicine-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.medicine-option-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.medicine-option-name {
  font-weight: 500;
  color: var(--text-primary);
}

.medicine-option-spec {
  font-size: 12px;
  color: var(--text-muted);
}

.medicine-option-stock {
  font-size: 12px;
  color: var(--success-color);
  flex-shrink: 0;
}

.medicine-option-stock.low-stock {
  color: var(--danger-color);
}

/* 移动端响应式 */
@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-actions {
    width: 100%;
  }

  .reminder-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .reminder-time {
    min-width: auto;
  }

  .reminder-actions {
    width: 100%;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }

  .batch-bar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .batch-actions {
    flex-wrap: wrap;
  }
}
</style>
