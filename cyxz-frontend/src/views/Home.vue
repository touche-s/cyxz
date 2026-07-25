<template>
  <main class="main-content">
    <div class="page-inner">
      <!-- ===== Hero ===== -->
      <section class="hero-panel">
        <div class="hero-copy">
          <!-- 装饰层 -->
          <div class="hero-deco-layer">
            <span class="meteor meteor--1"></span>
            <span class="meteor meteor--2"></span>
            <span class="meteor meteor--3"></span>
          </div>
          <div class="hero-sparkles">
            <span class="sparkle sparkle--1"><Icon icon="ph:sparkle" /></span>
            <span class="sparkle sparkle--2"><Icon icon="ph:star-four" /></span>
            <span class="sparkle sparkle--3"><Icon icon="ph:sparkle" /></span>
            <span class="sparkle sparkle--4"><Icon icon="ph:star-four" /></span>
            <span class="sparkle sparkle--5"><Icon icon="ph:sparkle" /></span>
          </div>
          <span class="hero-kicker">
            <Icon icon="ph:sparkle" class="kicker-icon" />
            从作品进入同好世界
          </span>
          <h1>
            找到你的<span class="h1-accent">同好圈</span>，从这里开始
          </h1>
          <p>
            每一部作品都是一个世界。选一个你喜欢的 IP，加入圈子，和同好一起讨论、创作、分享属于你们的次元故事。
          </p>
          <div class="hero-actions">
            <button class="primary-btn" @click="scrollToJoined">
              <Icon icon="ph:circles-three-plus" class="btn-icon" />
              我的圈子
            </button>
            <button class="ghost-btn" @click="scrollToAll">
              探索全部
              <Icon icon="ph:arrow-right" class="btn-icon-arrow" />
            </button>
          </div>
        </div>

        <aside class="hero-side" v-if="hotCircles.length > 0">
          <div class="hero-side-head">
            <span class="hero-side-kicker">
              <Icon icon="ph:fire" class="kicker-icon" />
              热门圈子排行
            </span>
            <strong>本周最活跃</strong>
          </div>
          <div class="hero-rank-list">
            <article
              v-for="(circle, index) in hotCircles"
              :key="circle.id"
              class="hero-rank-card"
              @click="enterCircle(circle)"
            >
              <div class="hero-rank-badge" :class="`hero-rank-badge--${index + 1}`">
                {{ index + 1 }}
              </div>
              <div class="hero-rank-avatar">
                <img v-if="circle.avatar" :src="circle.avatar" :alt="circle.name" />
                <span v-else class="hero-rank-avatar-fallback">{{ circle.name.charAt(0) }}</span>
              </div>
              <div class="hero-rank-main">
                <div class="hero-rank-topline">
                  <strong>{{ circle.name }}</strong>
                  <span class="hero-rank-heat">{{ circle.memberCount }} 成员</span>
                </div>
                <p>{{ circle.intro }}</p>
              </div>
            </article>
          </div>
        </aside>
      </section>

      <!-- 我的圈子 -->
      <section v-if="joinedCircles.length > 0" ref="joinedSectionRef" class="circle-section">
        <div class="section-top">
          <div>
            <span class="block-kicker">
              <Icon icon="ph:heart-straight" class="kicker-icon" />
              我的圈子
            </span>
            <h2>常驻同好地带</h2>
          </div>
          <span class="section-count">{{ joinedCircles.length }}</span>
        </div>
        <div class="circle-grid">
          <CircleCard
            v-for="circle in joinedCircles"
            :key="circle.id"
            :circle="circle"
            variant="joined"
            @click="enterCircle(circle)"
          />
        </div>
      </section>

      <!-- 热门排行 -->
      <section v-if="hotCircles.length > 0" class="circle-section">
        <div class="section-top">
          <div>
            <span class="block-kicker">
              <Icon icon="ph:fire" class="kicker-icon" />
              热门推荐
            </span>
            <h2>本周最活跃圈子</h2>
          </div>
        </div>
        <div class="hot-grid">
          <article
            v-for="(circle, index) in hotCircles"
            :key="circle.id"
            class="hot-card"
            @click="enterCircle(circle)"
          >
            <div class="hot-cover">
              <img
                v-if="circle.cover"
                :src="circle.cover"
                :alt="circle.name"
                class="hot-cover-img"
              />
              <div v-else class="hot-cover-fallback" :class="`hot-cover-fallback--${index + 1}`"></div>
              <div class="hot-cover-overlay"></div>
              <div class="hot-rank" :class="`hot-rank--${index + 1}`">
                <template v-if="index === 0"><Icon icon="ph:crown-simple" /></template>
                <template v-else>{{ index + 1 }}</template>
              </div>
            </div>
            <div class="hot-body hot-body--stack">
              <div class="hot-head-row">
                <div class="hot-avatar">
                  <img v-if="circle.avatar" :src="circle.avatar" :alt="circle.name" />
                  <span v-else class="hot-avatar-fallback">{{ circle.name.charAt(0) }}</span>
                </div>
                <div class="hot-info">
                  <strong>{{ circle.name }}</strong>
                  <p>{{ circle.intro }}</p>
                </div>
              </div>
              <div class="hot-meta">
                <span><Icon icon="ph:note-pencil" /> {{ circle.postCount }} 帖子</span>
                <span><Icon icon="ph:users" /> {{ circle.memberCount }} 成员</span>
              </div>
            </div>
          </article>
        </div>
      </section>

      <!-- 全部圈子 -->
      <section ref="allSectionRef" class="circle-section">
        <div class="section-top">
          <div>
            <span class="block-kicker">
              <Icon icon="ph:planet" class="kicker-icon" />
              全部圈子
            </span>
            <h2>从作品主题进入社区</h2>
          </div>
          <span class="section-count">{{ circles.length }}</span>
        </div>
        <div v-if="!loading" class="circle-grid">
          <CircleCard
            v-for="circle in circles"
            :key="circle.id"
            :circle="circle"
            @click="enterCircle(circle)"
            @toggle="toggleJoin(circle)"
          />
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
import CircleCard from '@/components/CircleCard.vue'

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

/* ==================== HERO ==================== */
.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.95fr);
  gap: 20px;
  margin-bottom: 40px;
}

.hero-copy {
  position: relative;
  overflow: hidden;
  padding: 30px 32px 28px;
  background: var(--gradient-hero);
  background-size: 200% 200%;
  animation: heroBgShift 8s ease-in-out infinite;
  border: 1.5px solid rgba(255, 163, 200, 0.45);
  border-radius: 24px;
  box-shadow:
    0 14px 32px rgba(255, 107, 157, 0.04),
    inset 0 0 0 1px rgba(255, 255, 255, 0.68);
}

@keyframes heroBgShift {
  0%, 100% { background-position: 0% 50%; }
  25% { background-position: 50% 30%; }
  50% { background-position: 100% 60%; }
  75% { background-position: 30% 80%; }
}

/* ========== 流星装饰 ========== */
.hero-deco-layer {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 1;
}

.meteor {
  position: absolute;
  top: -30%;
  width: 2.5px;
  height: 60px;
  border-radius: 3px;
  background: linear-gradient(to bottom, #fff, rgba(255, 107, 157, 0.6), rgba(180, 132, 255, 0));
  box-shadow: 0 0 8px 2px rgba(255, 163, 200, 0.5);
  opacity: 0;
  animation: meteorFall var(--meteor-dur, 3s) ease-in infinite;
  animation-delay: var(--meteor-delay, 0s);
}

.meteor--1 {
  left: 15%;
  --meteor-dur: 3s;
  --meteor-delay: 0s;
}

.meteor--2 {
  left: 48%;
  --meteor-dur: 3.6s;
  --meteor-delay: 1.4s;
  height: 48px;
  width: 2px;
}

.meteor--3 {
  left: 70%;
  --meteor-dur: 2.8s;
  --meteor-delay: 2.8s;
  height: 68px;
}

@keyframes meteorFall {
  0% {
    opacity: 0;
    transform: translate(0, 0) rotate(-22deg);
  }
  8% {
    opacity: 0.9;
  }
  30% {
    opacity: 0;
    transform: translate(120px, 280px) rotate(-22deg);
  }
  100% {
    opacity: 0;
    transform: translate(120px, 280px) rotate(-22deg);
  }
}

/* ========== 闪光星点 ========== */
.hero-sparkles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

.sparkle {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: sparklePulse 2s ease-in-out infinite;
}

.sparkle--1 {
  top: 8%;
  right: 12%;
  font-size: 22px;
  color: #ffb3d0;
  animation-delay: 0s;
  filter: drop-shadow(0 0 4px rgba(255, 107, 157, 0.4));
}

.sparkle--2 {
  top: 48%;
  right: 6%;
  font-size: 16px;
  color: #c4b5fd;
  animation-delay: 0.5s;
  animation-duration: 2.5s;
  filter: drop-shadow(0 0 3px rgba(180, 132, 255, 0.45));
}

.sparkle--3 {
  top: 14%;
  right: 36%;
  font-size: 14px;
  color: #ffd1e3;
  animation-delay: 1s;
  animation-duration: 1.8s;
  filter: drop-shadow(0 0 3px rgba(255, 163, 200, 0.35));
}

.sparkle--4 {
  top: 60%;
  right: 28%;
  font-size: 20px;
  color: #e0c8ff;
  animation-delay: 1.6s;
  animation-duration: 2.3s;
  filter: drop-shadow(0 0 4px rgba(180, 132, 255, 0.4));
}

.sparkle--5 {
  top: 5%;
  right: 52%;
  font-size: 12px;
  color: #ffb3d0;
  animation-delay: 2s;
  animation-duration: 2.1s;
  filter: drop-shadow(0 0 2px rgba(255, 107, 157, 0.35));
}

@keyframes sparklePulse {
  0%, 100% {
    opacity: 0.35;
    transform: scale(0.85);
  }
  50% {
    opacity: 1;
    transform: scale(1.15);
  }
}

.hero-kicker,
.block-kicker,
.hero-side-kicker {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 700;
  color: var(--pink);
  letter-spacing: 0.06em;
  padding: 5px 14px;
  background: rgba(255, 107, 157, 0.1);
  border-radius: 999px;
  border: 1px solid rgba(255, 107, 157, 0.18);
}

.block-kicker {
  background: transparent;
  border: none;
  padding: 0;
  font-weight: 800;
  margin-bottom: 4px;
}

.hero-side-kicker {
  padding: 4px 12px;
}

.kicker-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.hero-copy h1 {
  margin: 12px 0 8px;
  font-size: 32px;
  line-height: 1.25;
  font-weight: 800;
  color: var(--text);
}

.h1-accent {
  background: linear-gradient(135deg, #ff5ea0, #b07cf0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-copy p {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: rgba(90, 70, 108, 0.75);
  max-width: 620px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.primary-btn,
.ghost-btn {
  border-radius: 14px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.primary-btn {
  border: none;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  padding: 12px 22px;
  box-shadow: 0 6px 20px rgba(255, 107, 157, 0.18);
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(180, 132, 255, 0.24);
}

.ghost-btn {
  border: 1.5px solid rgba(255, 163, 200, 0.4);
  background: rgba(255, 255, 255, 0.78);
  color: var(--text);
  padding: 12px 22px;
}

.ghost-btn:hover {
  border-color: var(--pink);
  color: var(--pink);
  background: rgba(255, 107, 157, 0.04);
}

.ghost-btn:hover .btn-icon-arrow {
  transform: translateX(3px);
}

.btn-icon-arrow {
  transition: transform 0.22s ease-out;
}

/* ==================== HERO SIDE RANK ==================== */
.hero-side {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(255, 248, 252, 0.98));
  border: 1.5px solid rgba(255, 163, 200, 0.22);
  border-radius: 24px;
  padding: 16px;
  box-shadow: 0 14px 32px rgba(255, 107, 157, 0.04);
}

.hero-side-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}

.hero-side-head strong {
  font-size: 18px;
  color: var(--text);
  font-weight: 800;
}

.hero-rank-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hero-rank-card {
  display: grid;
  grid-template-columns: auto auto minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 14px;
  background: var(--card);
  border: 1px solid rgba(255, 163, 200, 0.14);
  cursor: pointer;
  transition: all 0.22s ease;
}

.hero-rank-card:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 107, 157, 0.3);
  box-shadow: 0 10px 20px rgba(255, 107, 157, 0.06);
}

.hero-rank-badge {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 800;
  line-height: 1;
  color: #fff;
  flex-shrink: 0;
}

.hero-rank-badge :deep(svg) {
  width: 18px;
  height: 18px;
}

.hero-rank-badge--1 {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
}

.hero-rank-badge--2 {
  background: linear-gradient(135deg, #a5b4fc, #c4b5fd);
}

.hero-rank-badge--3 {
  background: linear-gradient(135deg, #fb923c, #fda4af);
}

.hero-rank-avatar {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--gradient-tag);
  box-shadow: 0 3px 10px rgba(255, 107, 157, 0.12);
}

.hero-rank-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-rank-avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 800;
}

.hero-rank-main {
  min-width: 0;
}

.hero-rank-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-bottom: 2px;
}

.hero-rank-topline strong {
  font-size: 13px;
  color: var(--text);
  font-weight: 800;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hero-rank-heat {
  flex-shrink: 0;
  padding: 2px 7px;
  border-radius: 999px;
  background: rgba(255, 107, 157, 0.08);
  color: var(--pink);
  font-size: 10px;
  font-weight: 700;
}

.hero-rank-main p {
  margin: 0;
  font-size: 11px;
  line-height: 1.4;
  color: var(--text-dim);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}


/* ==================== SECTIONS ==================== */
.circle-section {
  margin-bottom: 48px;
}

.section-top {
  margin-bottom: 20px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.section-top h2 {
  margin: 6px 0 0;
  font-size: 22px;
  font-weight: 800;
  color: var(--text);
}

.section-count {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-dim);
  background: var(--bg-alt);
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid var(--border-light);
}

/* ==================== GRID ==================== */
.circle-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

/* ==================== HOT RANKING ==================== */
.hot-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.hot-card {
  background: var(--card);
  border: 1.5px solid var(--border-light);
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.hot-card:hover {
  border-color: rgba(255, 107, 157, 0.35);
  box-shadow: 0 10px 28px rgba(255, 107, 157, 0.08);
  transform: translateY(-3px);
}

.hot-cover {
  position: relative;
  height: 110px;
  overflow: hidden;
  background: var(--gradient-card);
}

.hot-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.4s ease-out;
}

.hot-card:hover .hot-cover-img {
  transform: scale(1.04);
}

.hot-cover-fallback {
  width: 100%;
  height: 100%;
}

.hot-cover-fallback--1 {
  background: linear-gradient(135deg, #fde8e8, #f5e0ff);
}
.hot-cover-fallback--2 {
  background: linear-gradient(135deg, #e8e8fd, #e0f0ff);
}
.hot-cover-fallback--3 {
  background: linear-gradient(135deg, #fdf0e8, #ffe8f0);
}

.hot-cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    transparent 35%,
    rgba(255, 255, 255, 0.12) 65%,
    rgba(255, 255, 255, 0.45) 100%
  );
  pointer-events: none;
}

.hot-rank {
  position: absolute;
  top: 12px;
  left: 12px;
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 800;
  color: white;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.15);
  z-index: 2;
}

.hot-rank--1 {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
  font-size: 18px;
}

.hot-rank--2 {
  background: linear-gradient(135deg, #94a3b8, #cbd5e1);
  color: #475569;
}

.hot-rank--3 {
  background: linear-gradient(135deg, #d97706, #f59e0b);
}

.hot-body {
  padding: 16px;
}

.hot-body--stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hot-head-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hot-avatar {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  background: var(--gradient-tag);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.88);
  box-shadow: 0 3px 10px rgba(255, 107, 157, 0.14);
}

.hot-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hot-avatar-fallback {
  color: white;
  font-size: 18px;
  font-weight: 800;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}

.hot-info {
  flex: 1;
  min-width: 0;
}

.hot-info strong {
  display: block;
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hot-info p {
  margin: 0;
  font-size: 11px;
  line-height: 1.6;
  color: var(--text-dim);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: var(--text-dim);
}

.hot-meta span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

/* ==================== FOOTER ==================== */
.footer {
  text-align: center;
  padding: 40px 0 20px;
  border-top: 1px solid var(--border);
  margin-top: 48px;
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

.footer-links a:hover { color: var(--pink); }

.footer-copy {
  font-size: 11px;
  color: var(--text-dim);
}

/* ==================== RESPONSIVE ==================== */
@media (max-width: 1100px) {
  .hero-panel { grid-template-columns: 1fr; }
  .hot-grid,
  .circle-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .page-inner { width: min(100vw - 24px, 1368px); }

  .hero-copy { padding: 28px 24px; }
  .hero-copy h1 { font-size: 26px; }

  .hot-grid,
  .circle-grid { grid-template-columns: 1fr; }

  .hero-actions,
  .section-top {
    flex-direction: column;
    align-items: flex-start;
  }

  .hot-head-row {
    align-items: flex-start;
  }
}
</style>
