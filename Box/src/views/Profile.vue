<template>
  <div class="profile-container">
    <div class="card profile-card">
      <div class="profile-header">
        <div class="avatar">
          <el-icon :size="48"><User /></el-icon>
        </div>
        <div class="profile-info">
          <h2>{{ userInfo.name || '用户' }}</h2>
          <p>{{ userInfo.phone }}</p>
        </div>
        <el-button type="primary" @click="toggleEdit">
          {{ isEditing ? '取消' : '编辑资料' }}
        </el-button>
      </div>
    </div>

    <div class="card">
      <h3 class="card-title">个人信息</h3>
      <el-form :model="userInfo" class="profile-form">
        <el-form-item label="姓名">
          <el-input v-model="userInfo.name" :disabled="!isEditing" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userInfo.phone" disabled />
        </el-form-item>
        <el-form-item label="注册时间">
          <el-input :value="formatDate(userInfo.createdAt)" disabled />
        </el-form-item>
        <el-form-item v-if="isEditing">
          <el-button type="primary" @click="saveProfile">保存修改</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <h3 class="card-title">修改密码</h3>
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" class="profile-form">
        <el-form-item prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请确认新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="changePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi } from '../utils/api'
import { User } from '@element-plus/icons-vue'

const isEditing = ref(false)
const passwordFormRef = ref(null)
const loading = ref(false)

const userInfo = reactive({
  name: '',
  phone: '',
  createdAt: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' },
    { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/, message: '密码必须包含大小写字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const fetchUserInfo = async () => {
  try {
    const response = await authApi.getUserInfo()
    userInfo.name = response.name || ''
    userInfo.phone = response.phone || localStorage.getItem('userPhone') || ''
    userInfo.createdAt = response.createdAt || ''
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

const toggleEdit = () => {
  isEditing.value = !isEditing.value
}

const saveProfile = async () => {
  try {
    await authApi.updateUserInfo({ name: userInfo.name })
    ElMessage.success('修改成功')
    isEditing.value = false
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '修改失败')
  }
}

const changePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await authApi.updateUserInfo({
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        })
        ElMessage.success('密码修改成功')
        passwordForm.oldPassword = ''
        passwordForm.newPassword = ''
        passwordForm.confirmPassword = ''
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '密码修改失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
.profile-container {
  max-width: 800px;
  margin: 0 auto;
}

.profile-card {
  text-align: center;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #4CAF50 0%, #81C784 100%);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
}

.profile-info h2 {
  margin-bottom: 5px;
}

.profile-info p {
  color: #666;
}

.card-title {
  font-size: 18px;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.profile-form {
  max-width: 500px;
}
</style>
