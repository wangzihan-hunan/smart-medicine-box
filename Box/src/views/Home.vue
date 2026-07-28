<template>
  <div class="home-container">
    <section class="hero-section">
      <div class="hero-content">
        <h1>智能药箱助手</h1>
        <p>让用药更安全、更便捷、更智能</p>
        <div class="hero-buttons">
          <router-link to="/medicines" class="btn-primary">开始使用</router-link>
          <router-link to="/about" class="btn-secondary">了解更多</router-link>
        </div>
      </div>
    </section>

    <section class="features-section">
      <div class="section-header">
        <h2>核心功能</h2>
        <p>全方位的用药管理解决方案</p>
      </div>
      <div class="features-grid">
        <div class="feature-card">
          <div class="feature-icon">
            <el-icon><FirstAidKit /></el-icon>
          </div>
          <h3>药品管理</h3>
          <p>智能识别药品信息，实时监控库存状态</p>
          <router-link to="/medicines" class="feature-link">查看详情</router-link>
        </div>
        <div class="feature-card">
          <div class="feature-icon">
            <el-icon><Document /></el-icon>
          </div>
          <h3>药方上传</h3>
          <p>支持多种格式药方上传，自动解析用药信息</p>
          <router-link to="/prescriptions" class="feature-link">查看详情</router-link>
        </div>
        <div class="feature-card">
          <div class="feature-icon">
            <el-icon><Clock /></el-icon>
          </div>
          <h3>用药提醒</h3>
          <p>智能用药提醒，确保按时按量服药</p>
          <router-link to="/reminders" class="feature-link">查看详情</router-link>
        </div>
        <div class="feature-card">
          <div class="feature-icon">
            <el-icon class="el-icon-send"></el-icon>
          </div>
          <h3>指令转发</h3>
          <p>远程控制药箱，智能化分发药品</p>
          <router-link to="/commands" class="feature-link">查看详情</router-link>
        </div>
      </div>
    </section>

    <section class="today-reminders" v-if="isLoggedIn">
      <div class="section-header">
        <h2>今日提醒</h2>
        <p>今日待服用的药品</p>
      </div>
      <div class="card" v-if="reminders.length > 0">
        <el-timeline>
          <el-timeline-item
            v-for="(reminder, index) in reminders"
            :key="index"
            :timestamp="reminder.time"
            placement="top"
          >
            <el-card shadow="hover">
              <div class="reminder-content">
                <h4>{{ reminder.medicineName }}</h4>
                <p>剂量：{{ reminder.dosage }}</p>
                <p>备注：{{ reminder.note || '无' }}</p>
                <el-button
                  v-if="!reminder.taken"
                  type="success"
                  size="small"
                  @click="markAsTaken(index)"
                >
                  已服用
                </el-button>
                <span v-else class="taken-badge">已服用</span>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
      <div class="card empty-card" v-else>
        <div class="empty-state">
          <el-icon :size="48"><Calendar /></el-icon>
          <p>今日暂无用药提醒</p>
        </div>
      </div>
    </section>

    <section class="quick-access">
      <div class="section-header">
        <h2>快捷入口</h2>
      </div>
      <div class="quick-grid">
        <router-link to="/medicines" class="quick-item">
          <el-icon><FirstAidKit /></el-icon>
          <span>药品管理</span>
        </router-link>
        <router-link to="/prescriptions" class="quick-item">
          <el-icon><Upload /></el-icon>
          <span>上传药方</span>
        </router-link>
        <router-link to="/reminders" class="quick-item">
          <el-icon><Bell /></el-icon>
          <span>提醒设置</span>
        </router-link>
        <router-link to="/commands" class="quick-item">
          <el-icon><Setting /></el-icon>
          <span>设备控制</span>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { reminderApi } from '../utils/api'
import {
  FirstAidKit, Document, Clock, Upload,
  Bell, Setting, User, Pointer
} from '@element-plus/icons-vue'

const isLoggedIn = ref(false)
const reminders = ref([])

const checkLoginStatus = () => {
  isLoggedIn.value = localStorage.getItem('isLoggedIn') === 'true'
}

const fetchTodayReminders = async () => {
  if (!isLoggedIn.value) return
  try {
    reminders.value = await reminderApi.getAll()
  } catch (error) {
    console.error('获取今日提醒失败:', error)
  }
}

const markAsTaken = (index) => {
  reminders.value[index].taken = true
  ElMessage.success('已标记为服用')
}

onMounted(() => {
  checkLoginStatus()
  fetchTodayReminders()
})
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
}

.hero-section {
  background: linear-gradient(135deg, #4CAF50 0%, #81C784 100%);
  color: white;
  padding: 80px 20px;
  text-align: center;
  border-radius: 12px;
  margin-bottom: 40px;
}

.hero-content h1 {
  font-size: 48px;
  margin-bottom: 20px;
}

.hero-content p {
  font-size: 20px;
  margin-bottom: 30px;
  opacity: 0.9;
}

.hero-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.section-header {
  text-align: center;
  margin-bottom: 30px;
}

.section-header h2 {
  font-size: 28px;
  margin-bottom: 10px;
}

.section-header p {
  color: #666;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
}

.feature-card {
  background: #fff;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.feature-icon {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #4CAF50 0%, #81C784 100%);
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
}

.feature-icon el-icon {
  font-size: 32px;
  color: white;
}

.feature-card h3 {
  font-size: 18px;
  margin-bottom: 10px;
}

.feature-card p {
  color: #666;
  margin-bottom: 15px;
}

.feature-link {
  color: #4CAF50;
  text-decoration: none;
  font-weight: 500;
}

.feature-link:hover {
  text-decoration: underline;
}

.today-reminders {
  margin-bottom: 40px;
}

.reminder-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.taken-badge {
  background-color: #E8F5E9;
  color: #4CAF50;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  display: inline-block;
  width: fit-content;
}

.empty-card {
  text-align: center;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 20px;
}

.quick-item {
  background: #fff;
  border-radius: 12px;
  padding: 25px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  text-decoration: none;
  color: #333;
  transition: all 0.3s ease;
}

.quick-item:hover {
  background: rgba(76, 175, 80, 0.05);
  color: #4CAF50;
}

.quick-item el-icon {
  font-size: 32px;
  margin-bottom: 10px;
}
</style>
