<template>
  <div class="reminders-container">
    <div class="page-header">
      <h2>用药提醒</h2>
      <el-button type="primary" @click="showAddModal = true">添加提醒</el-button>
    </div>

    <div class="card">
      <el-table :data="reminders" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="medicineName" label="药品名称" />
        <el-table-column prop="time" label="提醒时间" />
        <el-table-column prop="dosage" label="剂量" />
        <el-table-column prop="frequency" label="频率">
          <template #default="{ row }">
            <el-tag type="info">{{ getFrequencyText(row.frequency) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="备注" />
        <el-table-column prop="enabled" label="状态">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="toggleReminder(row)" />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog title="添加用药提醒" v-model="showAddModal">
      <el-form :model="reminderForm" :rules="rules" ref="reminderFormRef">
        <el-form-item prop="medicineName">
          <el-input v-model="reminderForm.medicineName" placeholder="请输入药品名称" />
        </el-form-item>
        <el-form-item prop="time">
          <el-time-picker v-model="reminderForm.time" type="time" placeholder="选择提醒时间" />
        </el-form-item>
        <el-form-item prop="dosage">
          <el-input v-model="reminderForm.dosage" placeholder="请输入剂量" />
        </el-form-item>
        <el-form-item prop="frequency">
          <el-select v-model="reminderForm.frequency" placeholder="选择频率">
            <el-option label="每天" value="daily" />
            <el-option label="每周" value="weekly" />
            <el-option label="每月" value="monthly" />
            <el-option label="一次性" value="once" />
          </el-select>
        </el-form-item>
        <el-form-item prop="note">
          <el-input v-model="reminderForm.note" type="textarea" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddModal = false">取消</el-button>
        <el-button type="primary" @click="addReminder">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { reminderApi } from '../utils/api'

const showAddModal = ref(false)
const reminderFormRef = ref(null)
const reminders = ref([])

const reminderForm = reactive({
  medicineName: '',
  time: '',
  dosage: '',
  frequency: 'daily',
  note: ''
})

const rules = {
  medicineName: [
    { required: true, message: '请输入药品名称', trigger: 'blur' }
  ],
  time: [
    { required: true, message: '请选择提醒时间', trigger: 'blur' }
  ],
  dosage: [
    { required: true, message: '请输入剂量', trigger: 'blur' }
  ]
}

const getFrequencyText = (frequency) => {
  const map = {
    daily: '每天',
    weekly: '每周',
    monthly: '每月',
    once: '一次性'
  }
  return map[frequency] || frequency
}

const fetchReminders = async () => {
  try {
    reminders.value = await reminderApi.getAll()
  } catch (error) {
    console.error('获取提醒列表失败:', error)
  }
}

const addReminder = async () => {
  if (!reminderFormRef.value) return
  
  await reminderFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await reminderApi.add(reminderForm)
        ElMessage.success('添加成功')
        showAddModal.value = false
        reminderForm.medicineName = ''
        reminderForm.time = ''
        reminderForm.dosage = ''
        reminderForm.frequency = 'daily'
        reminderForm.note = ''
        fetchReminders()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '添加失败')
      }
    }
  })
}

const toggleReminder = (row) => {
  ElMessage.success(row.enabled ? '提醒已开启' : '提醒已关闭')
}

onMounted(() => {
  fetchReminders()
})
</script>

<style scoped>
.reminders-container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
