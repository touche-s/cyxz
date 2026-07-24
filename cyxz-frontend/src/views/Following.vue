<template>
  <main class="main-content">
    <div class="page-inner">

    <!-- 我关注的圈子 -->
    <section v-if="joinedCircles.length > 0" class="section">
      <div class="section-label">我关注的圈子</div>
      <div class="joined-circle-row">
        <div v-for="circle in joinedCircles" :key="circle.id" class="mini-circle" @click="enterCircle(circle)">
          <div class="mini-avatar">
            <img v-if="circle.avatar" :src="circle.avatar" :alt="circle.name" />
            <span v-else>{{ circle.name.charAt(0) }}</span>
          </div>
          <span class="mini-name">{{ circle.name }}</span>
        </div>
      </div>
    </section>

    <!-- 关注动态（占位） -->
    <section class="section">
      <div class="section-label">关注动态</div>
      <div class="placeholder-card">
        <Icon icon="ph:heart" class="placeholder-icon" />
        <p>关注动态即将上线，敬请期待</p>
        <div class="placeholder-features">
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>关注创作者，定制专属内容流</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>第一时间看到他们的新作品和动态</span>
          </div>
        </div>
      </div>
    </section>

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
import { ref, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { getJoinedCircles } from '@/api/circle'
import type { CircleVO } from '@/api/circle'
import { useNavigate } from '@/composables/useNavigate'

const { open } = useNavigate()
const joinedCircles = ref<CircleVO[]>([])

async function loadJoined() {
  try {
    joinedCircles.value = await getJoinedCircles()
  } catch { /* ignore */ }
}

function enterCircle(circle: CircleVO) {
  open(`/circle/${circle.id}`)
}

onMounted(loadJoined)
</script>

<style scoped>
.main-content {
  padding: 90px 0 60px;
}

.page-inner {
  width: min(960px, calc(100vw - 48px));
  margin: 0 auto;
}

.section {
  margin-bottom: 36px;
}

.section-label {
  font-size: 18px;
  font-weight: 800;
  color: var(--text);
  margin-bottom: 16px;
}

.joined-circle-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.mini-circle {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--card);
  border: 1.5px solid var(--border-light);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.22s ease-out;
}

.mini-circle:hover {
  border-color: var(--pink);
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.08);
  transform: translateY(-2px);
}

.mini-avatar {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.mini-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mini-avatar span {
  color: white;
  font-size: 14px;
  font-weight: 700;
}

.mini-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.placeholder-card {
  background: var(--card);
  border: 1.5px solid var(--border-light);
  border-radius: 16px;
  padding: 40px;
  text-align: center;
}

.placeholder-icon {
  width: 48px;
  height: 48px;
  color: var(--pink);
  margin-bottom: 12px;
}

.placeholder-card p {
  font-size: 14px;
  color: var(--text-dim);
  margin-bottom: 20px;
}

.placeholder-features {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 320px;
  margin: 0 auto;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--text-dim);
  padding: 8px 14px;
  background: var(--bg-soft);
  border-radius: 10px;
}

.feature-dot {
  width: 6px;
  height: 6px;
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

.footer-links { display: flex; gap: 28px; }
.footer-links a { font-size: 12px; color: var(--text-dim); text-decoration: none; }
.footer-links a:hover { color: var(--pink); }
.footer-copy { font-size: 12px; color: var(--text-dim); }
</style>
