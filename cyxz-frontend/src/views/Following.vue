<template>
  <main class="main-content">
    <div class="page-inner">
      <div class="page-hero">
        <h1>关注</h1>
        <p>关注你感兴趣的创作者，第一时间看到他们的新作品</p>
      </div>

      <div class="placeholder-section">
        <div class="placeholder-card">
          <div class="placeholder-icon">
            <Icon icon="ph:heart" class="placeholder-iconify" />
          </div>
          <h3>关注动态即将上线</h3>
          <p>在这里你可以看到你关注的创作者的动态内容流。发现有趣的灵魂，建立你的次元圈~</p>
          <div class="placeholder-features">
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>关注创作者，定制专属内容流</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>第一时间看到他们的新作品和动态</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot"></div>
              <span>和同好交流互动</span>
            </div>
          </div>
        </div>
      </div>

      <footer class="footer">
        <div class="footer-links">
          <a href="#">关于我们</a>
          <a href="#">社区规范</a>
          <a href="#">帮助中心</a>
          <a href="#">意见反馈</a>
        </div>
        <div class="footer-copy">&copy; 2026 次元小站. All rights reserved.</div>
      </footer>
    </div>
  </main>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
</script>

<style scoped>
.main-content {
  padding-top: 86px;
  min-height: 100vh;
}

.page-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 44px;
}

.page-hero {
  text-align: center;
  padding: 60px 0 40px;
}

.page-hero h1 {
  font-size: 32px;
  font-weight: 900;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 12px;
}

.page-hero p {
  color: var(--text-dim);
  font-size: 15px;
}

.placeholder-section {
  display: flex;
  justify-content: center;
  padding: 20px 0 60px;
}

.placeholder-card {
  background: var(--card);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  border: 1px solid var(--border);
  padding: 48px 40px;
  text-align: center;
  max-width: 520px;
  width: 100%;
}

.placeholder-icon {
  width: 88px;
  height: 88px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.12), rgba(192, 132, 252, 0.12));
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  color: var(--pink);
}
.placeholder-iconify {
  width: 48px;
  height: 48px;
  color: var(--pink);
}

.placeholder-card h3 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 10px;
}

.placeholder-card > p {
  font-size: 14px;
  color: var(--text-dim);
  line-height: 1.7;
  margin-bottom: 28px;
}

.placeholder-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
  text-align: left;
  padding: 0 20px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--bg);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-dim);
  transition: all 0.22s ease-out;
}

.feature-item:hover {
  background: var(--pink-bg);
  color: var(--text);
  transform: translateX(4px);
}

.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  flex-shrink: 0;
}

.footer {
  border-top: 1px solid var(--border);
  padding: 32px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-top: 40px;
}

.footer-links {
  display: flex;
  gap: 28px;
}

.footer-links a {
  font-size: 12px;
  color: var(--text-dim);
  transition: color 0.22s;
}

.footer-links a:hover {
  color: var(--pink);
}

.footer-copy {
  font-size: 12px;
  color: var(--text-dim);
}
</style>
