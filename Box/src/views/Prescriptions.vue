<template>
  <div class="prescriptions-container">
    <div class="page-header">
      <h2>药方上传</h2>
    </div>

    <div class="card">
      <h3 class="card-title">上传药方</h3>
      <div class="upload-area" @click="triggerFileInput" @dragover.prevent @drop.prevent="handleDrop">
        <el-icon :size="48"><Upload /></el-icon>
        <p>点击或拖拽文件到此处上传</p>
        <p class="hint">支持 PDF、图片等格式</p>
        <input type="file" ref="fileInput" class="file-input" @change="handleFileChange" />
      </div>
      <div v-if="selectedFile" class="file-info">
        <span>{{ selectedFile.name }}</span>
        <el-button type="danger" size="small" @click="clearFile">移除</el-button>
      </div>
      <el-button type="primary" @click="uploadPrescription" :disabled="!selectedFile">上传</el-button>
    </div>

    <div class="card">
      <h3 class="card-title">历史记录</h3>
      <el-table :data="prescriptions" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="uploadTime" label="上传时间" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'warning'">
              {{ row.status === 'success' ? '已解析' : '处理中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="medicinesCount" label="药品数量" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="viewPrescription(row)">查看</el-button>
            <el-button size="small" type="danger" @click="deletePrescription(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog title="药方详情" v-model="showDetailModal">
      <div v-if="selectedPrescription">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="文件名">{{ selectedPrescription.fileName }}</el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ selectedPrescription.uploadTime }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedPrescription.status === 'success' ? 'success' : 'warning'">
              {{ selectedPrescription.status === 'success' ? '已解析' : '处理中' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="药品数量">{{ selectedPrescription.medicinesCount }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { prescriptionApi } from '../utils/api'
import { Upload } from '@element-plus/icons-vue'

const fileInput = ref(null)
const selectedFile = ref(null)
const prescriptions = ref([])
const showDetailModal = ref(false)
const selectedPrescription = ref(null)

const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileChange = (event) => {
  const file = event.target.files?.[0]
  if (file) {
    selectedFile.value = file
  }
}

const handleDrop = (event) => {
  const file = event.dataTransfer.files?.[0]
  if (file) {
    selectedFile.value = file
  }
}

const clearFile = () => {
  selectedFile.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const uploadPrescription = async () => {
  if (!selectedFile.value) return
  
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  
  try {
    await prescriptionApi.upload(formData)
    ElMessage.success('上传成功')
    clearFile()
    fetchPrescriptions()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '上传失败')
  }
}

const fetchPrescriptions = async () => {
  try {
    prescriptions.value = await prescriptionApi.history()
  } catch (error) {
    console.error('获取药方历史失败:', error)
  }
}

const viewPrescription = (prescription) => {
  selectedPrescription.value = prescription
  showDetailModal.value = true
}

const deletePrescription = async (id) => {
  try {
    await prescriptionApi.delete(id)
    ElMessage.success('删除成功')
    fetchPrescriptions()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  fetchPrescriptions()
})
</script>

<style scoped>
.prescriptions-container {
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

.upload-area {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-area:hover {
  border-color: #4CAF50;
  background-color: rgba(76, 175, 80, 0.05);
}

.upload-area el-icon {
  color: #4CAF50;
  margin-bottom: 10px;
}

.upload-area p {
  margin: 5px 0;
}

.hint {
  color: #999;
  font-size: 14px;
}

.file-input {
  display: none;
}

.file-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.file-info span {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
