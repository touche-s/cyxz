<template>
  <article
    class="circle-card"
    :class="{ 'circle-card--joined': variant === 'joined' }"
    @click="$emit('click')"
  >
    <div class="card-cover">
      <img
        v-if="circle.cover"
        :src="circle.cover"
        :alt="circle.name"
        class="cover-img"
      />
      <div v-else class="cover-fallback"></div>
      <div class="cover-overlay"></div>
      <div v-if="rank !== undefined" class="cover-rank" :class="`cover-rank--${rank + 1}`">
        <Icon v-if="rank === 0" icon="ph:crown-simple" />
        <template v-else>{{ rank + 1 }}</template>
      </div>
    </div>
    <div class="card-body">
      <div class="card-head">
        <div class="circle-avatar">
          <img v-if="circle.avatar" :src="circle.avatar" :alt="circle.name" />
          <span v-else class="avatar-fallback">{{ circle.name.charAt(0) }}</span>
        </div>
        <div class="card-head-info">
          <h3 class="circle-name">{{ circle.name }}</h3>
          <div class="circle-stats">
            <span>{{ circle.postCount }} 帖子</span>
            <span class="stat-sep">·</span>
            <span>{{ circle.memberCount }} 成员</span>
          </div>
        </div>
        <button
          v-if="variant !== 'joined'"
          class="join-btn"
          :class="{ joined: circle.joined }"
          @click.stop="$emit('toggle')"
        >
          {{ circle.joined ? '已加入' : '加入' }}
        </button>
        <span v-else class="joined-tag">已加入</span>
      </div>
      <p class="circle-intro">{{ circle.intro }}</p>
    </div>
  </article>
</template>

<script setup lang="ts">
import type { CircleVO } from '@/api/circle'

defineProps<{
  circle: CircleVO
  variant?: 'default' | 'joined'
  rank?: number
}>()

defineEmits<{
  click: []
  toggle: []
}>()
</script>

<style scoped>
.circle-card {
  background: var(--card);
  border: 1.5px solid var(--border-light);
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.circle-card:hover {
  border-color: rgba(255, 107, 157, 0.35);
  box-shadow: 0 12px 36px rgba(255, 107, 157, 0.08);
  transform: translateY(-4px);
}

.circle-card--joined {
  border-color: rgba(255, 107, 157, 0.18);
}

.card-cover {
  position: relative;
  height: 80px;
  overflow: hidden;
  background: var(--gradient-card);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.4s ease-out;
}

.circle-card:hover .cover-img {
  transform: scale(1.04);
}

.cover-fallback {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #fbd0e8, #e8d5ff);
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    transparent 30%,
    rgba(255, 255, 255, 0.1) 68%,
    rgba(255, 255, 255, 0.42) 100%
  );
  pointer-events: none;
}

.cover-rank {
  position: absolute;
  top: 8px;
  left: 8px;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 800;
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 2;
}

.cover-rank--1 {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
  font-size: 16px;
}

.cover-rank--2 {
  background: linear-gradient(135deg, #a5b4fc, #c4b5fd);
}

.cover-rank--3 {
  background: linear-gradient(135deg, #d97706, #f59e0b);
}

.card-body {
  padding: 12px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.circle-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f9a8d4, #c4b5fd);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.88);
}

.circle-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  color: #fff;
  font-size: 17px;
  font-weight: 800;
}

.card-head-info {
  min-width: 0;
  flex: 1;
}

.circle-name {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.circle-stats {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--text-dim);
  margin-top: 2px;
}

.stat-sep {
  color: var(--border);
}

.circle-intro {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-dim);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.joined-tag {
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 8px;
  background: var(--pink-bg);
  color: var(--pink);
  font-size: 11px;
  font-weight: 600;
}

.join-btn {
  padding: 4px 10px;
  border: 1.5px solid var(--pink);
  background: transparent;
  color: var(--pink);
  flex-shrink: 0;
  font-size: 11px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
}

.join-btn:hover {
  background: var(--pink);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.18);
}

.join-btn.joined {
  background: var(--pink-bg);
  border-color: var(--border);
  color: var(--text-dim);
}

.join-btn.joined:hover {
  border-color: #ef4444;
  color: #ef4444;
  background: #fef2f2;
}

/* ===== Dark mode overrides ===== */
html.dark .cover-overlay {
  background: linear-gradient(
    to bottom,
    transparent 30%,
    rgba(40, 35, 60, 0.1) 68%,
    rgba(40, 35, 60, 0.42) 100%
  );
}

html.dark .join-btn.joined:hover {
  background: rgba(239, 68, 68, 0.1);
}

html.dark .circle-card:hover {
  box-shadow: 0 12px 36px rgba(255, 107, 157, 0.04);
}

html.dark .join-btn:hover {
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.09);
}
</style>
