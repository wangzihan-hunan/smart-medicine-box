<!-- 关于我们页面模板 -->
<template>
  <!-- 关于我们页面主容器 -->
  <div class="about-page">
    <!-- 页面首屏英雄区域 -->
    <section class="about-hero">
      <div class="page-container">
        <div class="hero-content">
          <el-icon class="brand-icon"><TakeawayBox /></el-icon>
          <h1 class="hero-title">关于智能药箱</h1>
          <p class="hero-desc">致力于为家庭提供智能、便捷的用药管理解决方案</p>
        </div>
      </div>
    </section>
    
    <!-- 公司使命介绍区域 -->
    <section class="about-intro">
      <div class="page-container">
        <div class="intro-content">
          <!-- 使命介绍文字 -->
          <div class="intro-text">
            <h2>我们的使命</h2>
            <p>
              智能药箱管理系统旨在通过物联网技术和人工智能，为家庭提供智能化的用药管理服务。我们相信，技术可以让健康管理变得更加简单、高效和贴心。
            </p>
            <p>
              无论是药品库存管理、用药提醒还是远程控制，智能药箱都能帮助您轻松应对，让家人的健康管理不再成为负担。
            </p>
          </div>
          <!-- 平台数据统计 -->
          <div class="intro-stats">
            <div class="stat-item">
              <div class="stat-value">10万+</div>
              <div class="stat-label">家庭用户</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">500万+</div>
              <div class="stat-label">用药提醒</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">99.9%</div>
              <div class="stat-label">系统稳定</div>
            </div>
          </div>
        </div>
      </div>
    </section>
    
    <!-- 核心功能展示区域 -->
    <section class="features-intro">
      <div class="page-container">
        <h2 class="section-title">核心功能</h2>
        <!-- 功能卡片网格 -->
        <div class="features-grid">
          <!-- 智能药方识别功能 -->
          <div class="feature-card">
            <el-icon class="feature-icon"><Camera /></el-icon>
            <h3>智能药方识别</h3>
            <p>拍照上传药方，AI自动识别药品信息，快速录入系统，减少手动操作。</p>
          </div>
          <!-- 药品库存管理功能 -->
          <div class="feature-card">
            <el-icon class="feature-icon"><TakeawayBox /></el-icon>
            <h3>药品库存管理</h3>
            <p>实时追踪药品库存，低库存预警提醒，避免断药风险，智能管理药品有效期。</p>
          </div>
          <!-- 智能用药提醒功能 -->
          <div class="feature-card">
            <el-icon class="feature-icon"><Clock /></el-icon>
            <h3>智能用药提醒</h3>
            <p>根据药方自动生成用药提醒，支持多种提醒方式，确保按时服药。</p>
          </div>
          <!-- 远程设备控制功能 -->
          <div class="feature-card">
            <el-icon class="feature-icon"><Cellphone /></el-icon>
            <h3>远程设备控制</h3>
            <p>远程控制智能药箱，一键发送取药指令，便捷又安全，随时随地管理药箱。</p>
          </div>
        </div>
      </div>
    </section>
    
    <!-- 团队介绍区域 -->
    <section class="about-team">
      <div class="page-container">
        <h2 class="section-title">团队介绍</h2>
        <!-- 团队成员卡片网格 -->
        <div class="team-grid">
          <!-- 创始人 -->
          <div class="team-card">
            <div class="team-avatar">👨‍💻</div>
            <h3>张三</h3>
            <p>创始人 & CEO</p>
            <p class="team-desc">10年医疗健康行业经验</p>
          </div>
          <!-- 技术总监 -->
          <div class="team-card">
            <div class="team-avatar">👩‍💻</div>
            <h3>李四</h3>
            <p>技术总监</p>
            <p class="team-desc">8年软件开发经验</p>
          </div>
          <!-- 医学顾问 -->
          <div class="team-card">
            <div class="team-avatar">👨‍⚕️</div>
            <h3>王五</h3>
            <p>医学顾问</p>
            <p class="team-desc">15年临床经验</p>
          </div>
        </div>
      </div>
    </section>
    
    <!-- 联系方式区域 -->
    <section class="contact-info">
      <div class="page-container">
        <h2 class="section-title">联系我们</h2>
        <!-- 联系方式卡片网格 -->
        <div class="contact-grid">
          <!-- 邮箱 -->
          <div class="contact-item">
            <el-icon class="contact-icon"><Message /></el-icon>
            <h3>邮箱</h3>
            <p>contact@medicinebox.com</p>
          </div>
          <!-- 电话 -->
          <div class="contact-item">
            <el-icon class="contact-icon"><Phone /></el-icon>
            <h3>电话</h3>
            <p>400-888-8888</p>
          </div>
          <!-- 地址 -->
          <div class="contact-item">
            <el-icon class="contact-icon"><MapLocation /></el-icon>
            <h3>地址</h3>
            <p>北京市朝阳区健康大厦</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
// 引入Element Plus图标组件
import { TakeawayBox, Camera, Clock, Cellphone, Message, Phone, MapLocation } from '@element-plus/icons-vue'
</script>

<style scoped>
/* 关于我们页面主容器样式 */
.about-page {
  padding-top: 64px;
}

/* 首屏英雄区域样式 */
.about-hero {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  padding: 80px 20px;
  text-align: center;
  color: white;
}

/* 英雄区域内容容器 */
.hero-content {
  max-width: 600px;
  margin: 0 auto;
}

/* 品牌图标样式 */
.brand-icon {
  font-size: 72px;
  margin-bottom: 20px;
}

/* 英雄区域标题样式 */
.hero-title {
  font-size: 42px;
  font-weight: 700;
  margin-bottom: 16px;
}

/* 英雄区域描述样式 */
.hero-desc {
  font-size: 18px;
  opacity: 0.9;
}

/* 公司介绍区域样式 */
.about-intro {
  padding: 60px 0;
}

/* 介绍内容布局 */
.intro-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  align-items: center;
}

/* 介绍文字标题样式 */
.intro-text h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 20px;
}

/* 介绍文字段落样式 */
.intro-text p {
  font-size: 16px;
  color: var(--text-secondary);
  line-height: 1.8;
  margin-bottom: 16px;
}

/* 数据统计区域样式 */
.intro-stats {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 统计项卡片样式 */
.stat-item {
  text-align: center;
  padding: 24px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

/* 统计数值样式 */
.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: var(--primary-color);
  margin-bottom: 8px;
}

/* 统计标签样式 */
.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 核心功能区域样式 */
.features-intro {
  padding: 60px 0;
}

/* 功能卡片网格布局 */
.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

/* 功能卡片样式 */
.feature-card {
  background: white;
  border-radius: 16px;
  padding: 32px 24px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

/* 功能卡片图标样式 */
.feature-card .feature-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

/* 功能卡片标题样式 */
.feature-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

/* 功能卡片描述样式 */
.feature-card p {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

/* 团队介绍区域样式 */
.about-team {
  padding: 60px 0;
}

/* 团队成员卡片网格布局 */
.team-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

/* 团队成员卡片样式 */
.team-card {
  background: white;
  border-radius: 16px;
  padding: 32px 24px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

/* 团队成员头像样式 */
.team-avatar {
  font-size: 56px;
  margin-bottom: 16px;
}

/* 团队成员姓名样式 */
.team-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

/* 团队成员职位样式 */
.team-card p:first-of-type {
  font-size: 14px;
  color: var(--primary-color);
  margin-bottom: 8px;
}

/* 团队成员描述样式 */
.team-desc {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 联系信息区域样式 */
.contact-info {
  padding: 60px 0;
}

/* 联系方式卡片网格布局 */
.contact-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

/* 联系方式卡片样式 */
.contact-item {
  background: white;
  border-radius: 16px;
  padding: 32px 24px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

/* 联系方式图标样式 */
.contact-icon {
  font-size: 36px;
  margin-bottom: 16px;
}

/* 联系方式标题样式 */
.contact-item h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

/* 联系方式内容样式 */
.contact-item p {
  font-size: 15px;
  color: var(--text-secondary);
}

/* 平板响应式：两列布局 */
@media (max-width: 900px) {
  .intro-content {
    grid-template-columns: 1fr;
  }
  
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .team-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .contact-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .hero-title {
    font-size: 32px;
  }
}

/* 手机响应式：单列布局 */
@media (max-width: 600px) {
  .features-grid {
    grid-template-columns: 1fr;
  }
  
  .team-grid {
    grid-template-columns: 1fr;
  }
  
  .contact-grid {
    grid-template-columns: 1fr;
  }
  
  .hero-title {
    font-size: 26px;
  }
}
</style>
