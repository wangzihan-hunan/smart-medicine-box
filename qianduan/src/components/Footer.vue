<template>
  <footer class="footer">
    <div class="footer-container">
      <div class="footer-content">
        <div class="footer-section">
          <div class="footer-brand">
            <span class="brand-icon">💊</span>
            <span class="brand-text">智能药箱管理系统</span>
          </div>
          <p class="footer-desc">
            致力于为家庭提供智能、便捷的用药管理解决方案，让健康管理更简单。
          </p>
        </div>
        
        <div class="footer-section">
          <h4 class="footer-title">快速链接</h4>
          <ul class="footer-links">
            <li><router-link to="/">首页</router-link></li>
            <li><router-link to="/medicines">药品管理</router-link></li>
            <li><router-link to="/prescriptions">药方上传</router-link></li>
            <li><router-link to="/reminders">用药提醒</router-link></li>
          </ul>
        </div>
        
        <div class="footer-section">
          <h4 class="footer-title">支持</h4>
          <ul class="footer-links">
            <li><router-link to="/about">关于我们</router-link></li>
            <li><a href="#" @click.prevent>帮助中心</a></li>
            <li><a href="#" @click.prevent>隐私政策</a></li>
            <li><a href="#" @click.prevent>服务条款</a></li>
          </ul>
        </div>
        
        <div class="footer-section">
          <h4 class="footer-title">联系我们</h4>
          <div class="contact-info">
            <p>📧 contact@medicinebox.com</p>
            <p>📞 400-888-8888</p>
            <p>📍 北京市朝阳区健康大厦</p>
          </div>
        </div>
      </div>
      
      <div class="footer-bottom">
        <p>&copy; 2026 智能药箱管理系统. 保留所有权利.</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
</script>

<style scoped>
.footer {
  background: linear-gradient(135deg, #1f2937 0%, #111827 100%);
  color: white;
  padding: 48px 0 24px;
  margin-top: auto;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.footer-content {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 40px;
  margin-bottom: 32px;
}

.footer-section {
  padding: 16px 0;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.brand-icon {
  font-size: 28px;
}

.brand-text {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary-light), var(--primary-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.footer-desc {
  font-size: 14px;
  color: #9ca3af;
  line-height: 1.6;
}

.footer-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #f3f4f6;
}

.footer-links {
  list-style: none;
  padding: 0;
}

.footer-links li {
  margin-bottom: 10px;
}

.footer-links a {
  color: #9ca3af;
  text-decoration: none;
  font-size: 14px;
  transition: color var(--transition-fast);
}

.footer-links a:hover {
  color: var(--primary-light);
}

.contact-info p {
  font-size: 14px;
  color: #9ca3af;
  margin-bottom: 8px;
}

.footer-bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding-top: 20px;
  text-align: center;
}

.footer-bottom p {
  font-size: 13px;
  color: #6b7280;
}

@media (max-width: 900px) {
  .footer-content {
    grid-template-columns: repeat(2, 1fr);
    gap: 32px;
  }
}

@media (max-width: 600px) {
  .footer-content {
    grid-template-columns: 1fr;
    gap: 24px;
  }
  
  .footer {
    padding: 32px 0 20px;
  }
}
</style>
