<template>
  <div class="about-container">
    <div class="about-header">
      <h1>关于智能药箱助手</h1>
      <p>让用药更安全、更便捷、更智能</p>
    </div>

    <div class="about-content">
      <el-card class="about-card">
        <template #header>
          <div class="card-header">
            <el-icon><InfoFilled /></el-icon>
            <span>项目简介</span>
          </div>
        </template>
        <p>智能药箱助手是一款面向家庭用户的智能用药管理应用，旨在帮助用户更好地管理药品、用药提醒和药方信息。</p>
        <p>通过 OCR 技术识别药品和药方信息，结合智能提醒功能，确保用户按时按量服药，提升用药安全性。</p>
      </el-card>

      <el-card class="about-card">
        <template #header>
          <div class="card-header">
            <el-icon><FirstAidKit /></el-icon>
            <span>核心功能</span>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="12" :xs="24">
            <div class="feature-item">
              <h4>药品管理</h4>
              <p>智能识别药品信息，实时监控库存状态</p>
            </div>
          </el-col>
          <el-col :span="12" :xs="24">
            <div class="feature-item">
              <h4>药方上传</h4>
              <p>支持多种格式药方上传，自动解析用药信息</p>
            </div>
          </el-col>
          <el-col :span="12" :xs="24">
            <div class="feature-item">
              <h4>用药提醒</h4>
              <p>智能用药提醒，确保按时按量服药</p>
            </div>
          </el-col>
          <el-col :span="12" :xs="24">
            <div class="feature-item">
              <h4>设备控制</h4>
              <p>远程控制药箱，智能化分发药品</p>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <el-card class="about-card">
        <template #header>
          <div class="card-header">
            <el-icon><Monitor /></el-icon>
            <span>技术栈</span>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="后端框架">Spring Boot 2.7.x</el-descriptions-item>
          <el-descriptions-item label="前端框架">Vue 3 + Vite</el-descriptions-item>
          <el-descriptions-item label="UI 组件库">Element Plus</el-descriptions-item>
          <el-descriptions-item label="状态管理">Pinia</el-descriptions-item>
          <el-descriptions-item label="数据库">PostgreSQL</el-descriptions-item>
          <el-descriptions-item label="安全认证">Spring Security + JWT</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="about-card">
        <template #header>
          <div class="card-header">
            <el-icon><Message /></el-icon>
            <span>联系我们</span>
          </div>
        </template>
        <p>如有问题或建议，欢迎通过以下方式联系我们：</p>
        <ul class="contact-list">
          <li>邮箱：support@medicinebox.example.com</li>
          <li>电话：400-123-4567</li>
          <li>地址：示例市科技园区智能医疗大厦</li>
        </ul>
      </el-card>
    </div>

    <div class="about-footer">
      <p> 智能药箱助手. All rights reserved.</p>
      <p class="version">Version 1.0.0</p>
    </div>
  </div>
</template>

<script setup>
import { InfoFilled, FirstAidKit, Monitor, Message } from '@element-plus/icons-vue'
</script>

<style scoped>
.about-container {
  max-width: 900px;
  margin: 0 auto;
}

.about-header {
  text-align: center;
  margin-bottom: 40px;
}

.about-header h1 {
  font-size: 32px;
  color: #4CAF50;
  margin-bottom: 10px;
}

.about-header p {
  font-size: 16px;
  color: #666;
}

.about-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 40px;
}

.about-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.feature-item {
  padding: 15px;
  margin-bottom: 10px;
  background: #f5f7fa;
  border-radius: 8px;
}

.feature-item h4 {
  margin: 0 0 8px 0;
  color: #4CAF50;
}

.feature-item p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.contact-list {
  list-style: none;
  padding: 0;
}

.contact-list li {
  padding: 8px 0;
  color: #666;
}

.about-footer {
  text-align: center;
  color: #999;
  font-size: 14px;
}

.about-footer .version {
  margin-top: 5px;
  font-size: 12px;
}
</style>
