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
    </div>
    <div class="card-body-row card-body-row--stack">
      <div class="card-head-row">
        <div
          class="circle-avatar"
          :class="{ 'circle-avatar--lg': variant === 'joined' }"
        >
          <img v-if="circle.avatar" :src="circle.avatar" :alt="circle.name" />
          <span v-else class="avatar-fallback">{{ circle.name.charAt(0) }}</span>
        </div>
        <div class="circle-head-copy">
          <h3 class="circle-name">{{ circle.name }}</h3>
          <span v-if="variant === 'joined'" class="joined-badge">已加入</span>
        </div>
        <button
          v-if="variant !== 'joined'"
          class="join-btn"
          :class="{ joined: circle.joined }"
          @click.stop="$emit('toggle')"
        >
          <Icon v-if="!circle.joined" icon="ph:plus" />
          <Icon v-else icon="ph:check" />
          {{ circle.joined ? '已加入' : '加入' }}
        </button>
      </div>
      <div class="circle-info circle-info--full">
        <p class="circle-intro">{{ circle.intro }}</p>
        <div class="circle-stats-row">
          <span class="stat-item">
            <Icon icon="ph:note-pencil" class="stat-icon" />
            {{ circle.postCount }} 帖子
          </span>
          <span class="stat-item">
            <Icon icon="ph:users" class="stat-icon" />
            {{ circle.memberCount }} 成员
          </span>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import type { CircleVO } from '@/api/circle'

defineProps<{
  circle: CircleVO
  variant?: 'default' | 'joined'
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
  height: 122px;
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

.card-body-row {
  padding: 16px 18px 18px;
}

.card-body-row--stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-head-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.circle-avatar {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f9a8d4, #c4b5fd);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  box-shadow: 0 4px 14px rgba(255, 107, 157, 0.16);
  border: 2px solid rgba(255, 255, 255, 0.88);
}

.circle-avatar--lg {
  width: 62px;
  height: 62px;
  border-radius: 18px;
}

.circle-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  color: var(--white);
  font-size: 22px;
  font-weight: 800;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
}

.circle-avatar--lg .avatar-fallback {
  font-size: 26px;
}

.circle-head-copy {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.circle-info--full {
  min-width: 0;
}

.circle-name {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.circle-intro {
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 1.65;
  color: var(--text-dim);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.circle-stats-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  font-size: 11px;
  color: var(--text-dim);
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.stat-icon {
  width: 13px;
  height: 13px;
  color: var(--text-dim);
}

.joined-badge {
  flex-shrink: 0;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255, 107, 157, 0.08);
  color: var(--pink);
  font-size: 10px;
  font-weight: 700;
  border: 1px solid rgba(255, 107, 157, 0.18);
}

.join-btn {
  margin-left: auto;
  padding: 6px 13px;
  border: 1.5px solid var(--pink);
  background: transparent;
  color: var(--pink);
  flex-shrink: 0;
  font-size: 12px;
  gap: 4px;
  border-radius: 10px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  font-weight: 700;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
}

.join-btn:hover {
  background: var(--pink);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.18);
}

.join-btn.joined {
  background: rgba(255, 107, 157, 0.04);
  border-color: var(--border);
  color: var(--text-dim);
}

.join-btn.joined:hover {
  border-color: #ef4444;
  color: #ef4444;
  background: #fef2f2;
}
</style>
