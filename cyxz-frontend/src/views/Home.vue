<template>
  <main class="main-content">
    <div class="page-inner">
      <section class="hero-panel">
        <div class="hero-copy">
          <span class="hero-kicker">首页 · 圈子广场</span>
          <h1>先找到圈子，再开始今天的内容浏览</h1>
          <p>
            首页聚焦圈子入口，把兴趣归属放在前面。先进入熟悉的同好圈，再看讨论、作品和动态，让每次打开都有明确落点。
          </p>
          <div class="hero-actions">
            <button class="primary-btn" @click="scrollToJoined">进入我的圈子</button>
            <button class="ghost-btn" @click="scrollToAll">查看全部圈子</button>
          </div>
        </div>

        <div class="hero-stats">
          <div class="stat-card">
            <strong>{{ circles.length }}</strong>
            <span>可逛圈子</span>
          </div>
          <div class="stat-card">
            <strong>{{ joinedCircles.length }}</strong>
            <span>已加入</span>
          </div>
          <div class="stat-card">
            <strong>{{ hotCircles.length }}</strong>
            <span>本周热门</span>
          </div>
        </div>
      </section>

      <section v-if="joinedCircles.length > 0" ref="joinedSectionRef" class="circle-section">
        <div class="section-top">
          <div>
            <span class="block-kicker">我的圈子</span>
            <h2>优先回到你常驻的同好地带</h2>
          </div>
          <span class="section-count">{{ joinedCircles.length }}</span>
        </div>
        <div class="circle-grid">
          <article
            v-for="circle in joinedCircles"
            :key="circle.id"
            class="circle-card circle-card--joined"
            @click="enterCircle(circle)"
          >
            <div class="circle-avatar">
              <img v-if="circle.avatar" :src="circle.avatar" :alt="circle.name" />
              <span v-else class="avatar-text">{{ circle.name.charAt(0) }}</span>
            </div>
            <div class="circle-info">
              <div class="circle-title-row">
                <h3 class="circle-name">{{ circle.name }}</h3>
                <span class="circle-tag">已加入</span>
              </div>
              <p class="circle-intro">{{ circle.intro }}</p>
              <div class="circle-stats-row">
                <span>{{ circle.postCount }} 帖子</span>
                <span>{{ circle.memberCount }} 成员</span>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section v-if="hotCircles.length > 0" class="circle-section hot-section">
        <div class="section-top">
          <div>
            <span class="block-kicker">热门推荐</span>
            <h2>本周最活跃的圈子</h2>
          </div>
        </div>
        <div class="hot-list">
          <article
            v-for="(circle, index) in hotCircles"
            :key="circle.id"
            class="hot-card"
            @click="enterCircle(circle)"
          >
            <div class="hot-rank">0{{ index + 1 }}</div>
            <div class="hot-main">
              <strong>{{ circle.name }}</strong>
              <p>{{ circle.intro }}</p>
            </div>
            <div class="hot-meta">
              <span>{{ circle.postCount }} 帖子</span>
              <span>{{ circle.memberCount }} 成员</span>
            </div>
          </article>
        </div>
      </section>

      <section ref="allSectionRef" class="circle-section">
        <div class="section-top">
          <div>
            <span class="block-kicker">圈子广场</span>
            <h2>从作品主题进入社区</h2>
          </div>
          <span class="section-count">{{ circles.length }}</span>
        </div>
        <div v-if="!loading" class="circle-grid">
          <article
            v-for="circle in circles"
            :key="circle.id"
            class="circle-card"
            @click="enterCircle(circle)"
          >
            <div class="circle-avatar">
              <img v-if="circle.avatar" :src="circle.avatar" :alt="circle.name" />
              <span v-else class="avatar-text">{{ circle.name.charAt(0) }}</span>
            </div>
            <div class="circle-info">
              <div class="circle-title-row">
                <h3 class="circle-name">{{ circle.name }}</h3>
                <button
                  class="join-btn"
                  :class="{ joined: circle.joined }"
                  @click.stop="toggleJoin(circle)"
                >
                  {{ circle.joined ? '已加入' : '加入' }}
                </button>
              </div>
              <p class="circle-intro">{{ circle.intro }}</p>
              <div class="circle-stats-row">
                <span>{{ circle.postCount }} 帖子</span>
                <span>{{ circle.memberCount }} 成员</span>
              </div>
            </div>
          </article>
        </div>
        <LoadingSpinner v-else text="加载圈子中..." />
        <EmptyState v-if="!loading && circles.length === 0" title="暂无圈子" />
      </section>

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
import { computed, onMounted, ref } from 'vue'
import { getCircleList, getJoinedCircles, joinCircle, leaveCircle } from '@/api/circle'
import type { CircleVO } from '@/api/circle'
import { useNavigate } from '@/composables/useNavigate'
import { useAuth } from '@/composables/useAuth'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'

const { open } = useNavigate()
const { requireLogin } = useAuth()

const circles = ref<CircleVO[]>([])
const joinedCircles = ref<CircleVO[]>([])
const loading = ref(false)
const joinedSectionRef = ref<HTMLElement | null>(null)
const allSectionRef = ref<HTMLElement | null>(null)

const hotCircles = computed(() => {
  return [...circles.value]
    .sort((a, b) => {
      if (b.postCount !== a.postCount) return b.postCount - a.postCount
      return b.memberCount - a.memberCount
    })
    .slice(0, 3)
})

async function loadCircles() {
  loading.value = true
  try {
    circles.value = await getCircleList()
  } catch (e) {
    console.error('加载圈子失败:', e)
  } finally {
    loading.value = false
  }
}

async function loadJoined() {
  try {
    joinedCircles.value = await getJoinedCircles()
  } catch {
    joinedCircles.value = []
  }
}

function enterCircle(circle: CircleVO) {
  open(`/circle/${circle.id}`)
}

function scrollToJoined() {
  if (joinedCircles.value.length > 0) {
    joinedSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    return
  }
  allSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollToAll() {
  allSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function toggleJoin(circle: CircleVO) {
  if (!requireLogin()) return
  try {
    if (circle.joined) {
      await leaveCircle(circle.id)
      circle.joined = false
      circle.memberCount = Math.max(circle.memberCount - 1, 0)
      joinedCircles.value = joinedCircles.value.filter(c => c.id !== circle.id)
    } else {
      await joinCircle(circle.id)
      circle.joined = true
      circle.memberCount = circle.memberCount + 1
      joinedCircles.value.push({ ...circle, joined: true })
    }
  } catch (e) {
    console.error('操作失败:', e)
  }
}

onMounted(() => {
  loadCircles()
  loadJoined()
})
</script>

<style scoped>
.main-content {
  padding: 90px 0 60px;
}

.page-inner {
  width: min(1368px, calc(100vw - 48px));
  margin: 0 auto;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.9fr);
  gap: 20px;
  margin-bottom: 32px;
}

.hero-copy,
.hero-stats,
.circle-card,
.hot-card {
  background: var(--card);
  border: 1.5px solid var(--border-light);
  border-radius: 24px;
}

.hero-copy {
  position: relative;
  overflow: hidden;
  padding: 34px;
  background: linear-gradient(135deg, #fff7fb 0%, #fff1f7 52%, #fff9fd 100%);
  border: 1px solid rgba(255, 182, 213, 0.7);
  box-shadow: 0 18px 38px rgba(255, 107, 157, 0.1);
}

.hero-ribbon {
  position: absolute;
  top: -48px;
  left: -120px;
  width: 240px;
  height: calc(100% + 96px);
  background: linear-gradient(90deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.92), rgba(255, 194, 224, 0.65), rgba(255, 255, 255, 0));
  transform: skewX(-18deg);
  opacity: 0.95;
  pointer-events: none;
  animation: heroRibbonSweep 2.8s linear infinite;
}

.hero-copy::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.38), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.hero-copy::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  background: linear-gradient(90deg, rgba(255, 125, 182, 0), rgba(255, 125, 182, 0.95), rgba(255, 125, 182, 0));
  pointer-events: none;
}

.hero-kicker,
.block-kicker {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--pink);
  letter-spacing: 0.08em;
}

.hero-copy h1 {
  margin: 14px 0 12px;
  font-size: 34px;
  line-height: 1.2;
  color: var(--text);
}

.hero-copy p {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: rgba(92, 74, 110, 0.82);
  max-width: 680px;
  position: relative;
  z-index: 1;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.primary-btn,
.ghost-btn,
.join-btn {
  border-radius: 14px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.22s ease-out;
}

.primary-btn {
  border: none;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  padding: 12px 20px;
  box-shadow: 0 10px 24px rgba(255, 107, 157, 0.18);
}

.primary-btn:hover {
  transform: translateY(-2px);
}

.ghost-btn {
  border: 1.5px solid var(--border);
  background: rgba(255, 255, 255, 0.72);
  color: var(--text);
  padding: 12px 20px;
}

.ghost-btn:hover {
  border-color: var(--pink);
  color: var(--pink);
}

.hero-stats {
  padding: 20px;
  display: grid;
  gap: 14px;
}

.stat-card {
  padding: 18px 20px;
  border-radius: 18px;
  background: var(--bg-soft);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-card strong {
  font-size: 28px;
  line-height: 1;
  color: var(--text);
}

.stat-card span {
  font-size: 13px;
  color: var(--text-dim);
}

.circle-section {
  margin-bottom: 36px;
}

.section-top {
  margin-bottom: 18px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.section-top h2 {
  margin: 8px 0 0;
  font-size: 24px;
  color: var(--text);
}

.section-count {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-dim);
  background: var(--bg-soft);
  padding: 5px 12px;
  border-radius: 999px;
}

.circle-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.circle-card {
  padding: 20px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.circle-card:hover,
.hot-card:hover {
  border-color: var(--pink);
  box-shadow: 0 10px 28px rgba(255, 107, 157, 0.1);
  transform: translateY(-3px);
}

.circle-card--joined {
  background: linear-gradient(180deg, rgba(255, 250, 252, 0.96), rgba(255, 255, 255, 0.98));
}

.circle-avatar {
  width: 60px;
  height: 60px;
  border-radius: 18px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  box-shadow: 0 6px 16px rgba(255, 107, 157, 0.2);
}

.circle-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-text {
  color: white;
  font-size: 24px;
  font-weight: 800;
}

.circle-info {
  flex: 1;
  min-width: 0;
}

.circle-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.circle-name {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--text);
}

.circle-tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 107, 157, 0.12);
  color: var(--pink);
  font-size: 11px;
  font-weight: 700;
}

.circle-intro {
  margin: 0 0 10px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-dim);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.circle-stats-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 12px;
  color: var(--text-dim);
}

.join-btn {
  padding: 6px 14px;
  border: 1.5px solid var(--pink);
  background: transparent;
  color: var(--pink);
  flex-shrink: 0;
  white-space: nowrap;
}

.join-btn:hover {
  background: var(--pink);
  color: white;
}

.join-btn.joined {
  background: var(--bg-soft);
  border-color: var(--border);
  color: var(--text-dim);
}

.hot-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.hot-card {
  padding: 18px 20px;
  cursor: pointer;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 14px;
  align-items: center;
}

.hot-rank {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 800;
}

.hot-main strong {
  display: block;
  margin-bottom: 4px;
  font-size: 16px;
  color: var(--text);
}

.hot-main p {
  margin: 0;
  font-size: 12px;
  color: var(--text-dim);
  line-height: 1.6;
}

.hot-meta {
  grid-column: 2;
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: var(--text-dim);
}

.footer {
  text-align: center;
  padding: 40px 0 20px;
  border-top: 1px solid var(--border);
  margin-top: 40px;
}

.footer-links {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 12px;
}

.footer-links a {
  font-size: 12px;
  color: var(--text-dim);
  text-decoration: none;
}

.footer-links a:hover {
  color: var(--pink);
}

.footer-copy {
  font-size: 11px;
  color: var(--text-dim);
}

@keyframes heroGlassSweep {
  0% {
    left: -48%;
    opacity: 0;
  }
  16% {
    opacity: 0.7;
  }
  52% {
    left: 112%;
    opacity: 0.28;
  }
  100% {
    left: 112%;
    opacity: 0;
  }
}

@media (max-width: 1100px) {
  .hero-panel,
  .hot-list,
  .circle-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .hero-stats {
    grid-column: 1 / -1;
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .page-inner {
    width: min(100vw - 24px, 1368px);
  }

  .hero-panel,
  .hot-list,
  .circle-grid,
  .hero-stats {
    grid-template-columns: 1fr;
  }

  .hero-copy {
    padding: 24px;
  }

  .hero-copy h1 {
    font-size: 28px;
  }

  .hero-actions,
  .circle-title-row,
  .section-top {
    flex-direction: column;
    align-items: flex-start;
  }

  .join-btn {
    width: 100%;
  }
}
</style>
