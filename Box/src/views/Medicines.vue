<template>
  <div class="medicines-container">
    <div class="page-header">
      <h2>药品管理</h2>
      <el-button type="primary" @click="showAddModal = true">添加药品</el-button>
    </div>

    <div class="card">
      <el-table :data="medicines" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="药品名称" />
        <el-table-column prop="spec" label="规格" />
        <el-table-column prop="stock" label="库存" />
        <el-table-column prop="expiryDate" label="有效期" />
        <el-table-column prop="location" label="位置" />
        <el-table-column prop="notes" label="备注" />
        <el-table-column prop="createdAt" label="添加时间" />
      </el-table>
    </div>

    <el-dialog title="添加药品" v-model="showAddModal">
      <el-form :model="medicineForm" :rules="rules" ref="medicineFormRef">
        <el-form-item prop="name">
          <el-input v-model="medicineForm.name" placeholder="请输入药品名称" />
        </el-form-item>
        <el-form-item label="规格" prop="spec">
          <el-input v-model="medicineForm.spec" placeholder="请输入规格" />
        </el-form-item>
        <el-form-item prop="stock">
          <el-input-number v-model="medicineForm.stock" :min="0" />
        </el-form-item>
        <el-form-item prop="expiryDate">
          <el-date-picker v-model="medicineForm.expiryDate" type="date" placeholder="选择有效期" />
        </el-form-item>
        <el-form-item prop="location">
          <el-input v-model="medicineForm.location" placeholder="请输入存放位置" />
        </el-form-item>
        <el-form-item prop="notes">
          <el-input v-model="medicineForm.notes" type="textarea" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddModal = false">取消</el-button>
        <el-button type="primary" @click="addMedicine">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { medicineApi } from '../utils/api'

const showAddModal = ref(false)
const medicineFormRef = ref(null)
const medicines = ref([])

const medicineForm = reactive({
  name: '',
  spec: '',
  stock: 0,
  expiryDate: '',
  location: '',
  notes: ''
})

const rules = {
  name: [
    { required: true, message: '请输入药品名称', trigger: 'blur' }
  ],
  stock: [
    { required: true, message: '请输入库存数量', trigger: 'blur' }
  ]
}

const fetchMedicines = async () => {
  try {
    medicines.value = await medicineApi.getAll()
  } catch (error) {
    console.error('获取药品列表失败:', error)
  }
}

const addMedicine = async () => {
  if (!medicineFormRef.value) return
  
  await medicineFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await medicineApi.add(medicineForm)
        ElMessage.success('添加成功')
        showAddModal.value = false
        medicineForm.name = ''
        medicineForm.specification = ''
        medicineForm.stock = 0
        medicineForm.expiryDate = ''
        medicineForm.location = ''
        medicineForm.notes = ''
        fetchMedicines()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '添加失败')
      }
    }
  })
}

onMounted(() => {
  fetchMedicines()
})
</script>

<style scoped>
.medicines-container {
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
