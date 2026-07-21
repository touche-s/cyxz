<template>
  <main class="main-content">
    <div class="page-inner">
      <div class="page-hero">
        <h1>社区</h1>
        <p>这里是次元小站的社区广场，发现话题、参与讨论、结识同好</p>
      </div>

      <div class="community-grid">
        <div class="community-section">
          <div class="section-header">
            <div class="section-icon">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
            </div>
            <h3>热门话题</h3>
          </div>
          <div class="placeholder-list">
            <div class="placeholder-item skeleton" v-for="i in 4" :key="'topic-' + i">
              <div class="skeleton-avatar"></div>
              <div class="skeleton-lines">
                <div class="skeleton-line w-60"></div>
                <div class="skeleton-line w-40"></div>
              </div>
            </div>
            <div class="coming-soon-badge">讨论区即将上线</div>
          </div>
        </div>

        <div class="community-section">
          <div class="section-header">
            <div class="section-icon icon-pink">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            </div>
            <h3>活跃创作者</h3>
          </div>
          <div class="creator-grid">
            <div class="creator-card skeleton" v-for="i in 3" :key="'creator-' + i">
              <div class="skeleton-circle"></div>
              <div class="skeleton-line w-50" style="margin: 12px auto 0;"></div>
              <div class="skeleton-line w-30" style="margin: 8px auto 0;"></div>
            </div>
          </div>
        </div>

        <div class="community-section">
          <div class="section-header">
            <div class="section-icon icon-purple">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="4 7 4 4 20 4 20 7"/>
                <line x1="9" y1="20" x2="15" y2="20"/>
                <line x1="12" y1="4" x2="12" y2="20"/>
              </svg>
            </div>
            <h3>标签墙</h3>
          </div>
          <div class="tag-cloud">
            <span class="tag skeleton-tag" v-for="tag in tags" :key="tag">{{ tag }}</span>
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
const tags = ['二次元', '绘画', 'Cosplay', '手办', '动画', '漫画', '轻小说', '游戏', '同人', '声优', '漫展', '宅舞']
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

.community-grid {
  display: flex;
  flex-direction: column;
  gap: 28px;
  padding: 0 0 60px;
}

.community-section {
  background: var(--card);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  border: 1px solid var(--border);
  padding: 28px 32px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.section-header h3 {
  font-size: 17px;
  font-weight: 700;
  color: var(--text);
}

.section-icon {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.12), rgba(255, 141, 181, 0.08));
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--pink);
}

.section-icon.icon-pink {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.12), rgba(255, 182, 193, 0.12));
  color: var(--pink-light);
}

.section-icon.icon-purple {
  background: linear-gradient(135deg, rgba(192, 132, 252, 0.12), rgba(212, 168, 255, 0.08));
  color: var(--purple);
}

.placeholder-list {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.placeholder-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: var(--radius-sm);
  background: var(--bg);
}

.coming-soon-badge {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: 10px 24px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  font-size: 14px;
  font-weight: 700;
  color: var(--pink);
  border: 1.5px solid var(--border);
  box-shadow: var(--shadow);
}

.creator-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.creator-card {
  text-align: center;
  padding: 24px 16px;
  border-radius: var(--radius-sm);
  background: var(--bg);
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag {
  padding: 8px 18px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  background: var(--bg);
  color: var(--text);
  border: 1px solid var(--border);
  transition: all 0.22s ease-out;
}

.tag:hover {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(192, 132, 252, 0.1));
  color: var(--pink);
  border-color: var(--border);
}

.skeleton {
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% { opacity: 0.4; }
  50% { opacity: 0.7; }
  100% { opacity: 0.4; }
}

.skeleton-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.15), rgba(192, 132, 252, 0.1));
  flex-shrink: 0;
}

.skeleton-circle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.15), rgba(192, 132, 252, 0.1));
  margin: 0 auto;
}

.skeleton-lines {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.skeleton-line {
  height: 12px;
  border-radius: 6px;
  background: linear-gradient(90deg, rgba(255, 107, 157, 0.1), rgba(192, 132, 252, 0.08));
}

.w-60 { width: 60%; }
.w-50 { width: 50%; }
.w-40 { width: 40%; }
.w-30 { width: 30%; }

.skeleton-tag {
  cursor: default;
}

.footer {
  border-top: 1px solid var(--border);
  padding: 32px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
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

@media (max-width: 768px) {
  .page-inner { padding: 0 16px; }
  .creator-grid { grid-template-columns: 1fr 1fr; }
}
</style>
