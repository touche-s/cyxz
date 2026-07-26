<template>
  <main class="main-content">
    <div class="page-inner">
      <section class="hero-panel">
        <div class="hero-card">
          <div class="hero-bg-orb hero-bg-orb--1"></div>
          <div class="hero-bg-orb hero-bg-orb--2"></div>
          <div class="hero-bg-orb hero-bg-orb--3"></div>
          <div class="hero-dots">
            <span v-for="i in 12" :key="i" class="hero-dot" :style="dotStyle(i)"></span>
          </div>

          <div class="hero-left">
            <span class="hero-kicker">
              <Icon icon="ph:sparkle" class="kicker-icon" />
              从作品进入同好世界
            </span>
            <h1>
              找到你的<span class="h1-accent">同好圈</span>，从这里开始
            </h1>
            <p>
              选一个你喜欢的 IP，加入圈子，和同好一起讨论、创作、分享属于你们的次元故事。
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

          <div class="hero-right">
            <div class="collage-wrap">
              <div class="collage-card collage-card--main">
                <div class="collage-cover collage-cover--1">
                  <div class="collage-cover-pattern"></div>
                </div>
                <div class="collage-bar">
                  <span class="collage-name">原神</span>
                  <span class="collage-stat">2.4k 成员</span>
                </div>
              </div>
              <div class="collage-card collage-card--top">
                <div class="collage-cover collage-cover--2">
                  <div class="collage-cover-pattern"></div>
                </div>
                <div class="collage-bar">
                  <span class="collage-name">崩坏：星穹铁道</span>
                  <span class="collage-tag">热门</span>
                </div>
              </div>
              <div class="collage-card collage-card--bottom">
                <div class="collage-cover collage-cover--3">
                  <div class="collage-cover-pattern"></div>
                </div>
                <div class="collage-bar">
                  <span class="collage-name">同人创作</span>
                  <span class="collage-tag tag-new">新圈</span>
                </div>
              </div>
            </div>

            <div class="floating-tag floating-tag--1">
              <Icon icon="ph:plus-circle" class="ft-icon" />
              今日新增 12 个圈子
            </div>
            <div class="floating-tag floating-tag--2">
              <Icon icon="ph:tag" class="ft-icon" />
              热门标签：<span class="ft-hashtag">#同人</span><span class="ft-hashtag">#日常</span><span class="ft-hashtag">#COS</span>
            </div>
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

      <section v-if="joinedCircles.length > 0" ref="joinedSectionRef" class="circle-section">
        <div class="section-top">
          <div>
            <span class="block-kicker">
              <Icon icon="ph:heart-straight" class="kicker-icon" />
              我的圈子
            </span>
            <h2>常驻同好地带</h2>
          </div>
          <a class="view-all" @click="to('/square?tab=joined')">查看更多 →</a>
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

      <section v-if="hotCircles.length > 0" class="circle-section">
        <div class="section-top">
          <div>
            <span class="block-kicker">
              <Icon icon="ph:fire" class="kicker-icon" />
              热门推荐
            </span>
            <h2>本周最活跃圈子</h2>
          </div>
          <a class="view-all" @click="to('/square?tab=hot')">查看更多 →</a>
        </div>
        <div class="circle-grid">
          <CircleCard
            v-for="(circle, index) in hotCircles"
            :key="circle.id"
            :circle="circle"
            :rank="index"
            @click="enterCircle(circle)"
            @toggle="toggleJoin(circle)"
          />
        </div>
      </section>

      <section ref="allSectionRef" class="circle-section">
        <div class="section-top">
          <div>
            <span class="block-kicker">
              <Icon icon="ph:planet" class="kicker-icon" />
              全部圈子
            </span>
            <h2>从作品主题进入社区</h2>
          </div>
          <a class="view-all" @click="to('/square?tab=all')">查看更多 →</a>
        </div>
        <div v-if="!loading" class="circle-grid">
          <CircleCard
            v-for="circle in previewCircles"
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
          <a @click="to('/guidelines')" class="footer-link">社区规范</a>
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

const { open, to } = useNavigate()
const { requireLogin } = useAuth()

const circles = ref<CircleVO[]>([])
const joinedCircles = ref<CircleVO[]>([])
const loading = ref(false)
const joinLoading = ref<Record<number, boolean>>({})
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

const previewCircles = computed(() => circles.value.slice(0, 8))

function dotStyle(i: number) {
  const positions = [
    { top: '8%', left: '72%' },
    { top: '14%', left: '82%' },
    { top: '22%', left: '92%' },
    { top: '36%', left: '68%' },
    { top: '46%', left: '88%' },
    { top: '58%', left: '78%' },
    { top: '68%', left: '94%' },
    { top: '78%', left: '70%' },
    { top: '84%', left: '86%' },
    { top: '10%', left: '62%' },
    { top: '30%', left: '60%' },
    { top: '52%', left: '62%' },
  ]
  const delays = [0, 0.8, 1.6, 0.4, 1.2, 2.0, 0.6, 1.4, 0.2, 1.0, 1.8, 0.9]
  const sizes = [4, 3, 5, 3, 4, 3, 2, 4, 3, 5, 3, 2]
  const idx = i - 1
  return {
    top: positions[idx].top,
    left: positions[idx].left,
    width: sizes[idx] + 'px',
    height: sizes[idx] + 'px',
    animationDelay: delays[idx] + 's',
  }
}

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
  if (joinLoading.value[circle.id]) return
  const prevJoined = circle.joined
  const prevMemberCount = circle.memberCount
  joinLoading.value[circle.id] = true
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
    circle.joined = prevJoined
    circle.memberCount = prevMemberCount
  } finally {
    joinLoading.value[circle.id] = false
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

/* ==================== HERO PANEL ==================== */
.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(320px, 0.9fr);
  gap: 20px;
  margin-bottom: 40px;
}

/* ==================== HERO CARD (LEFT) ==================== */
.hero-card {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 0.92fr;
  gap: 20px;
  min-height: 348px;
  padding: 28px 0 28px 32px;
  border-radius: 24px;
  overflow: hidden;
  background: linear-gradient(145deg, #fff6fb 0%, #ffeef6 28%, #f6efff 68%, #fdf5ff 100%);
  border: 1.5px solid rgba(255, 163, 200, 0.35);
  box-shadow:
    0 12px 40px rgba(255, 107, 157, 0.06),
    0 2px 8px rgba(180, 132, 255, 0.05),
    inset 0 0 0 1px rgba(255, 255, 255, 0.6);
}

html.dark .hero-card {
  background: linear-gradient(145deg, #211d35 0%, #252042 28%, #221f40 68%, #1f1c38 100%);
  border-color: rgba(255, 163, 200, 0.15);
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.25),
    inset 0 0 0 1px rgba(255, 255, 255, 0.03);
}

/* Background orbs */
.hero-bg-orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
}

.hero-bg-orb--1 {
  width: 220px;
  height: 220px;
  top: -60px;
  right: 10%;
  background: radial-gradient(circle, rgba(255, 163, 200, 0.15) 0%, transparent 70%);
  animation: orbFloat1 6s ease-in-out infinite;
}

.hero-bg-orb--2 {
  width: 160px;
  height: 160px;
  bottom: -40px;
  right: 40%;
  background: radial-gradient(circle, rgba(180, 132, 255, 0.12) 0%, transparent 70%);
  animation: orbFloat2 7s ease-in-out infinite;
}

.hero-bg-orb--3 {
  width: 120px;
  height: 120px;
  top: 30%;
  right: 60%;
  background: radial-gradient(circle, rgba(255, 200, 220, 0.1) 0%, transparent 70%);
  animation: orbFloat3 5s ease-in-out infinite;
}

@keyframes orbFloat1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(12px, -10px) scale(1.06); }
}

@keyframes orbFloat2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-8px, 14px) scale(1.08); }
}

@keyframes orbFloat3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(10px, 6px) scale(1.05); }
}

/* Dot decoration */
.hero-dots {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.hero-dot {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 163, 200, 0.22);
  animation: dotPulse 2.5s ease-in-out infinite;
}

html.dark .hero-dot {
  background: rgba(255, 163, 200, 0.15);
}

@keyframes dotPulse {
  0%, 100% { opacity: 0.25; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.4); }
}

/* Left text area */
.hero-left {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-right: 4px;
}

.hero-kicker {
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
  width: fit-content;
}

.kicker-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.hero-left h1 {
  margin: 14px 0 8px;
  font-size: 32px;
  line-height: 1.22;
  font-weight: 800;
  color: var(--text);
}

.h1-accent {
  background: linear-gradient(135deg, #ff5ea0, #b07cf0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-left p {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.7;
  color: rgba(90, 70, 108, 0.72);
  max-width: 480px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

html.dark .hero-left p {
  color: rgba(200, 190, 220, 0.65);
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.primary-btn,
.ghost-btn {
  border-radius: 14px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 46px;
  padding: 0 24px;
  font-family: inherit;
}

.primary-btn {
  border: none;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  box-shadow: 0 6px 20px rgba(255, 107, 157, 0.2);
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(180, 132, 255, 0.26);
}

.primary-btn:active {
  transform: scale(0.97);
}

.ghost-btn {
  border: 1.5px solid rgba(255, 163, 200, 0.35);
  background: rgba(255, 255, 255, 0.7);
  color: var(--text);
}

html.dark .ghost-btn {
  background: rgba(37, 37, 71, 0.5);
}

.ghost-btn:hover {
  border-color: var(--pink);
  color: var(--pink);
  background: rgba(255, 107, 157, 0.05);
  transform: translateY(-2px);
}

.ghost-btn:hover .btn-icon-arrow {
  transform: translateX(3px);
}

.btn-icon {
  width: 18px;
  height: 18px;
}

.btn-icon-arrow {
  width: 16px;
  height: 16px;
  transition: transform 0.22s ease-out;
}

/* ==================== HERO RIGHT - COLLAGE ==================== */
.hero-right {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-right: 8px;
}

.collage-wrap {
  position: relative;
  width: 100%;
  height: 240px;
}

.collage-card {
  position: absolute;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 28px rgba(255, 107, 157, 0.1), 0 2px 6px rgba(180, 132, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.6);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.3s ease;
  cursor: default;
}

.collage-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 14px 36px rgba(255, 107, 157, 0.15), 0 4px 12px rgba(180, 132, 255, 0.12);
}

.collage-card--main {
  width: 68%;
  height: 148px;
  top: 50%;
  left: 10%;
  transform: translateY(-50%);
  z-index: 3;
}

.collage-card--main:hover {
  transform: translateY(calc(-50% - 3px));
}

.collage-card--top {
  width: 52%;
  height: 115px;
  top: 0;
  right: 2%;
  z-index: 2;
  border-radius: 14px;
}

.collage-card--bottom {
  width: 48%;
  height: 110px;
  bottom: 4px;
  left: 0;
  z-index: 4;
  border-radius: 14px;
}

.collage-cover {
  position: relative;
  width: 100%;
  height: calc(100% - 36px);
  overflow: hidden;
}

.collage-cover--1 {
  background: linear-gradient(135deg, #a8d8ea, #e8c4f8, #fcd5e8);
}

.collage-cover--2 {
  background: linear-gradient(135deg, #1a1a3e, #3d2e6e, #6b4faa);
}

.collage-cover--3 {
  background: linear-gradient(135deg, #fce4ec, #f8d0e0, #e8c8f0);
}

.collage-cover-pattern {
  position: absolute;
  inset: 0;
  opacity: 0.12;
  background-image:
    radial-gradient(circle at 20% 30%, rgba(255, 255, 255, 0.8) 1px, transparent 1px),
    radial-gradient(circle at 60% 70%, rgba(255, 255, 255, 0.6) 1px, transparent 1px),
    radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.7) 1.5px, transparent 1.5px),
    radial-gradient(circle at 40% 50%, rgba(255, 255, 255, 0.5) 2px, transparent 2px);
  background-size: 60px 60px, 40px 40px, 50px 50px, 70px 70px;
}

.collage-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  height: 36px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(8px);
}

html.dark .collage-bar {
  background: rgba(30, 28, 48, 0.75);
}

.collage-name {
  font-size: 12px;
  font-weight: 800;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.collage-stat {
  font-size: 10px;
  font-weight: 600;
  color: var(--text-dim);
  flex-shrink: 0;
}

.collage-tag {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.15), rgba(180, 132, 255, 0.1));
  color: var(--pink);
  flex-shrink: 0;
}

.tag-new {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.15), rgba(16, 185, 129, 0.1));
  color: var(--success);
}

/* Floating tags */
.floating-tag {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 700;
  padding: 5px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 163, 200, 0.2);
  color: var(--text);
  box-shadow: 0 4px 14px rgba(255, 107, 157, 0.06);
  white-space: nowrap;
  z-index: 5;
  pointer-events: none;
  animation: tagFloat 3.5s ease-in-out infinite;
}

html.dark .floating-tag {
  background: rgba(37, 37, 71, 0.78);
  border-color: rgba(255, 163, 200, 0.12);
}

.floating-tag--1 {
  top: -2px;
  right: -8px;
  animation-delay: 0s;
}

.floating-tag--2 {
  bottom: -6px;
  right: -6px;
  animation-delay: 1.2s;
}

@keyframes tagFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

.ft-icon {
  width: 14px;
  height: 14px;
  color: var(--pink);
  flex-shrink: 0;
}

.ft-hashtag {
  color: var(--pink);
  margin-left: 2px;
}

/* ==================== HERO SIDE RANK ==================== */
.hero-side {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(255, 249, 253, 0.94));
  border: 1.5px solid rgba(255, 163, 200, 0.2);
  border-radius: 24px;
  padding: 18px;
  box-shadow: 0 12px 32px rgba(255, 107, 157, 0.04);
}

html.dark .hero-side {
  background: linear-gradient(180deg, rgba(37, 37, 71, 0.75), rgba(32, 30, 55, 0.85));
  border-color: rgba(255, 163, 200, 0.1);
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

.hero-side-kicker {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 700;
  color: var(--pink);
  padding: 4px 12px;
  background: rgba(255, 107, 157, 0.08);
  border-radius: 999px;
  width: fit-content;
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

.block-kicker {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 800;
  color: var(--pink);
  letter-spacing: 0.06em;
  margin-bottom: 4px;
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

.view-all {
  font-size: 13px;
  font-weight: 600;
  color: var(--pink);
  text-decoration: none;
  cursor: pointer;
  transition: color 0.22s ease-out;
}

.view-all:hover {
  color: var(--purple);
}

/* ==================== GRID ==================== */
.circle-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
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
  cursor: pointer;
}

.footer-links a:hover { color: var(--pink); }

.footer-copy {
  font-size: 11px;
  color: var(--text-dim);
}

/* ==================== RESPONSIVE ==================== */
@media (max-width: 1100px) {
  .hero-panel { grid-template-columns: 1fr; }

  .hero-card {
    grid-template-columns: 1fr 0.85fr;
    min-height: 320px;
    padding: 24px 0 24px 24px;
  }

  .hero-left h1 { font-size: 28px; }

  .circle-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .page-inner { width: min(100vw - 24px, 1368px); }

  .hero-card {
    grid-template-columns: 1fr;
    min-height: auto;
    padding: 24px;
    gap: 0;
  }

  .hero-left {
    padding-right: 0;
  }

  .hero-left h1 { font-size: 24px; }

  .hero-right {
    display: none;
  }

  .hero-dots {
    display: none;
  }

  .hero-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .floating-tag {
    display: none;
  }

  .circle-grid { grid-template-columns: 1fr; }
}
</style>
