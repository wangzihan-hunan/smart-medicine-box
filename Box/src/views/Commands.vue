<template>
  <div class="commands-container">
    <div class="page-header">
      <h2>指令转发</h2>
    </div>

    <div class="card">
      <h3 class="card-title">发送指令</h3>
      <el-form :model="commandForm" :rules="rules" ref="commandFormRef">
        <el-form-item prop="commandType">
          <el-select v-model="commandForm.commandType" placeholder="选择指令类型">
            <el-option label="开盒" value="open_box" />
            <el-option label="关盒" value="close_box" />
            <el-option label="取药" value="dispense" />
            <el-option label="检查库存" value="check_stock" />
            <el-option label="唤醒设备" value="wake_up" />
          </el-select>
        </el-form-item>
        <el-form-item prop="targetSlot">
          <el-input-number v-model="commandForm.targetSlot" :min="1" :max="12" placeholder="目标药槽（1-12）" />
        </el-form-item>
        <el-form-item prop="quantity">
          <el-input-number v-model="commandForm.quantity" :min="1" placeholder="数量" />
        </el-form-item>
        <el-form-item prop="priority">
          <el-select v-model="commandForm.priority" placeholder="优先级">
            <el-option label="低" value="low" />
            <el-option label="中" value="medium" />
            <el-option label="高" value="high" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="sendCommand" :loading="sending">发送指令</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <h3 class="card-title">指令历史</h3>
      <el-table :data="commandHistory" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="commandType" label="指令类型">
          <template #default="{ row }">
            <el-tag type="info">{{ getCommandText(row.commandType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetSlot" label="目标药槽" />
        <el-table-column prop="quantity" label="数量" />
        <el-table-column prop="priority" label="优先级">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)">
              {{ getPriorityText(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发送时间" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { commandApi } from '../utils/api'

const commandFormRef = ref(null)
const sending = ref(false)
const commandHistory = ref([])

const commandForm = reactive({
  commandType: '',
  targetSlot: null,
  quantity: null,
  priority: 'medium'
})

const rules = {
  commandType: [
    { required: true, message: '请选择指令类型', trigger: 'blur' }
  ]
}

const getCommandText = (type) => {
  const map = {
    open_box: '开盒',
    close_box: '关盒',
    dispense: '取药',
    check_stock: '检查库存',
    wake_up: '唤醒设备'
  }
  return map[type] || type
}

const getPriorityText = (priority) => {
  const map = {
    low: '低',
    medium: '中',
    high: '高'
  }
  return map[priority] || priority
}

const getPriorityType = (priority) => {
  const map = {
    low: 'info',
    medium: 'warning',
    high: 'danger'
  }
  return map[priority] || 'info'
}

const getStatusText = (status) => {
  const map = {
    pending: '待处理',
    processing: '处理中',
    completed: '已完成',
    failed: '失败'
  }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = {
    pending: 'warning',
    processing: 'info',
    completed: 'success',
    failed: 'danger'
  }
  return map[status] || 'info'
}

const sendCommand = async () => {
  if (!commandFormRef.value) return
  
  await commandFormRef.value.validate(async (valid) => {
    if (valid) {
      sending.value = true
      try {
        await commandApi.forward(commandForm)
        ElMessage.success('指令发送成功')
        commandForm.commandType = ''
        commandForm.targetSlot = null
        commandForm.quantity = null
        commandForm.priority = 'medium'
        fetchHistory()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '指令发送失败')
      } finally {
        sending.value = false
      }
    }
  })
}

const fetchHistory = async () => {
  try {
    commandHistory.value = await commandApi.history()
  } catch (error) {
    console.error('获取指令历史失败:', error)
  }
}

onMounted(() => {
  fetchHistory()
})
</script>

<style scoped>
.commands-container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.card-title {
  font-size: 18px;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}
</style>
