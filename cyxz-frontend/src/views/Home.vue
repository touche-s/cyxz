<template>
  <main class="main-content">
    <div class="page-inner">
    <div class="hero-banner">
      <div class="hero-text">
        <div class="hero-eyebrow">
          <span class="pulse-dot"></span>
          今日推荐
        </div>
        <h1>在<em>这里</em>发现<br />属于你的世界</h1>
        <p>每一次分享都是一场奇遇，每一个灵感都值得被看见</p>
        <div class="hero-stats">
          <div class="hero-stat"><b>128K+</b>创作者</div>
          <div class="hero-stat"><b>2.4M+</b>作品</div>
          <div class="hero-stat"><b>580+</b>今日上新</div>
        </div>
      </div>
      <div class="hero-illust">
        <div class="circle c1"></div>
        <div class="circle c2"></div>
        <div class="circle c3"></div>
        <div class="char-emoji"></div>
      </div>
    </div>

    <div class="cat-row">
      <span class="cat-pill active">⭐ 推荐</span>
      <span class="cat-pill">🎨 绘画</span>
      <span class="cat-pill">📷 摄影</span>
      <span class="cat-pill">🍰 美食</span>
      <span class="cat-pill">👗 穿搭</span>
      <span class="cat-pill">✈️ 旅行</span>
      <span class="cat-pill">🧵 手作</span>
      <span class="cat-pill">📚 读书</span>
      <span class="cat-pill">🎵 音乐</span>
      <span class="cat-pill">🐱 萌宠</span>
      <span class="cat-pill">💄 美妆</span>
      <span class="cat-pill">🎮 游戏</span>
    </div>

    <div class="section-label">
      精选内容
      <a href="#">查看更多 →</a>
    </div>

    <div class="content-grid">
      <div class="card" v-for="item in cards" :key="item.title">
        <div class="card-cover">
          <div class="img" :style="{ background: item.gradient }"></div>
          <span class="card-badge">{{ item.badge }}</span>
          <button class="card-save">♡</button>
        </div>
        <div class="card-body">
          <div class="card-title">{{ item.title }}</div>
          <div class="card-meta">
            <div class="card-author">
              <div class="card-avatar"></div>
              <span class="card-author-name">{{ item.author }}</span>
            </div>
            <div class="card-stats">
              <span>♡ {{ item.likes }}</span>
              <span>💬 {{ item.comments }}</span>
            </div>
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
      <div class="footer-copy">© 2026 次元小站. All rights reserved.</div>
    </footer>
    </div>
  </main>
</template>

<script setup lang="ts">
const cards = [
  { title: '新番推荐｜本季神作盘点，错过一部都是损失', author: '动漫达人', badge: '🔥 热门', likes: '8.2k', comments: '326', gradient: 'linear-gradient(135deg, #ffd4e8, #ffa8c8, #ff8db5)' },
  { title: '插画教程｜从零画出一张好看的氛围感插图', author: '画师小窝', badge: '✨ 精选', likes: '5.4k', comments: '198', gradient: 'linear-gradient(135deg, #d4e8ff, #a8c8ff, #8db5ff)' },
  { title: 'Cosplay分享｜第一次出cos的完整记录', author: 'Coser酱', badge: '🆕 最新', likes: '3.7k', comments: '152', gradient: 'linear-gradient(135deg, #f0d4ff, #d8b0ff, #c084fc)' },
  { title: '周末探店｜藏在巷子里的宝藏咖啡店', author: '小鹿喝咖啡', badge: '🔥 热门', likes: '2.3k', comments: '87', gradient: 'linear-gradient(135deg, #fff0d4, #ffe0a8, #ffd08d)' },
  { title: '穿搭分享｜秋季温柔系日常 look', author: '穿搭日记', badge: '✨ 精选', likes: '5.1k', comments: '213', gradient: 'linear-gradient(135deg, #d4ffe8, #a8ffd0, #8dffb5)' },
  { title: '手作日记｜用黏土做了超可爱的桌面摆件', author: '手工达人', badge: '🆕 最新', likes: '1.6k', comments: '64', gradient: 'linear-gradient(135deg, #ffe8d4, #ffc8a8, #ffa88d)' },
  { title: '游戏评测｜年度最佳独立游戏的魅力', author: '游戏编辑部', badge: '🔥 热门', likes: '6.9k', comments: '278', gradient: 'linear-gradient(135deg, #e8d4ff, #d0b0ff)' },
  { title: '旅行日记｜京都红枫季的美到窒息', author: '旅行者笔记', badge: '✨ 精选', likes: '4.2k', comments: '156', gradient: 'linear-gradient(135deg, #c8ffe8, #a0ffd0)' },
]
</script>

<style scoped>
.main-content {
  padding: 90px 0 60px;
}

.page-inner {
  max-width: 1500px;
  margin: 0 auto;
  padding: 0 32px;
}

.hero-banner {
  position: relative;
  background: linear-gradient(135deg, rgba(255, 182, 215, 0.35), rgba(192, 132, 252, 0.25), rgba(129, 140, 248, 0.15));
  border-radius: 20px;
  padding: 48px 44px;
  margin-bottom: 24px;
  overflow: hidden;
  border: 1.5px solid rgba(255, 182, 215, 0.2);
  display: flex;
  align-items: center;
  gap: 40px;
}

.hero-banner::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 6px;
  background: linear-gradient(180deg, var(--pink), var(--purple), var(--blue));
  border-radius: 0 3px 3px 0;
}

.hero-text { flex: 1; z-index: 1; }

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.75);
  border: 1.5px solid rgba(255, 182, 215, 0.3);
  font-size: 11px;
  font-weight: 700;
  color: var(--pink);
  margin-bottom: 16px;
  letter-spacing: 2px;
}

.pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--pink);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255, 107, 157, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(255, 107, 157, 0); }
}

.hero-text h1 {
  font-size: 38px;
  font-weight: 900;
  line-height: 1.2;
  margin-bottom: 12px;
  color: var(--text);
}

.hero-text h1 em {
  font-style: normal;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-text p {
  font-size: 14px;
  color: var(--text-dim);
  line-height: 1.7;
  max-width: 400px;
}

.hero-stats { display: flex; gap: 28px; margin-top: 20px; }

.hero-stat { font-size: 12px; color: var(--text-dim); }

.hero-stat b {
  display: block;
  font-size: 20px;
  font-weight: 900;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-illust {
  flex-shrink: 0;
  z-index: 1;
  width: 280px;
  height: 200px;
  position: relative;
}

.circle {
  position: absolute;
  width: 140px;
  height: 140px;
  border-radius: 50%;
  opacity: 0.15;
  animation: circleFloat 4s ease-in-out infinite;
}

.c1 { background: var(--pink); top: 10px; left: 40px; }
.c2 { background: var(--purple); bottom: 0; right: 30px; animation-delay: 1s; }
.c3 { background: var(--blue); top: 60px; right: 0; width: 80px; height: 80px; animation-delay: 2s; }

.char-emoji {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 80px;
  animation: charBounce 3s ease-in-out infinite;
  filter: drop-shadow(0 4px 12px rgba(255, 107, 157, 0.3));
}

.char-emoji::before { content: ''; }

@keyframes circleFloat {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-12px) scale(1.05); }
}

@keyframes charBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.cat-row {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  overflow-x: auto;
  padding: 4px 0;
}

.cat-row::-webkit-scrollbar { display: none; }

.cat-pill {
  padding: 8px 18px;
  border-radius: 14px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  border: 1.5px solid var(--border);
  background: white;
  color: var(--text-dim);
  transition: all 0.25s;
  flex-shrink: 0;
}

.cat-pill.active {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  border-color: transparent;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.25);
}

.cat-pill:hover:not(.active) {
  border-color: rgba(255, 107, 157, 0.3);
  color: var(--pink);
  background: rgba(255, 240, 247, 0.7);
}

.section-label {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-label a {
  font-size: 12px;
  font-weight: 500;
  color: var(--pink);
  text-decoration: none;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  margin-bottom: 32px;
}

.card {
  background: white;
  border-radius: var(--radius);
  overflow: hidden;
  border: 1.5px solid var(--border);
  transition: all 0.3s;
  cursor: pointer;
}

.card:hover {
  transform: translateY(-5px);
  border-color: rgba(255, 107, 157, 0.35);
  box-shadow: 0 12px 36px rgba(255, 107, 157, 0.12);
}

.card-cover {
  position: relative;
  height: 180px;
  overflow: hidden;
}

.card-cover .img {
  width: 100%;
  height: 100%;
  transition: transform 0.4s;
}

.card:hover .card-cover .img { transform: scale(1.05); }

.card-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  font-size: 10px;
  font-weight: 700;
  color: var(--pink);
  border: 1px solid rgba(255, 182, 215, 0.3);
}

.card-save {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
  color: var(--text-dim);
}

.card-save:hover { color: var(--pink); background: white; }

.card-body { padding: 14px 16px; }

.card-title {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 12px;
  color: var(--text);
}

.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-author { display: flex; align-items: center; gap: 8px; }

.card-avatar {
  width: 26px;
  height: 26px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  border: 2px solid white;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.card-author-name { font-size: 12px; color: var(--text-dim); font-weight: 500; }

.card-stats { display: flex; gap: 12px; font-size: 12px; color: var(--text-dim); }
.card-stats span { display: flex; align-items: center; gap: 3px; }

.footer {
  text-align: center;
  padding: 40px 0 20px;
  border-top: 1px solid var(--border);
  margin-top: 20px;
}

.footer-links { display: flex; justify-content: center; gap: 24px; margin-bottom: 12px; }

.footer-links a {
  font-size: 12px;
  color: var(--text-dim);
  text-decoration: none;
}

.footer-links a:hover { color: var(--pink); }

.footer-copy { font-size: 11px; color: #c4a0b8; }

@media (max-width: 1200px) {
  .content-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 900px) {
  .content-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .hero-banner { flex-direction: column; padding: 32px 24px; gap: 24px; }
  .hero-illust { width: 200px; height: 160px; }
  .hero-text h1 { font-size: 28px; }
  .main-content { padding: 90px 0 60px; }
  .content-grid { grid-template-columns: 1fr; }
}
</style>
