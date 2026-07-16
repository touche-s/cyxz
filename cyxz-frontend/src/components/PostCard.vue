<template>
  <div class="card" :class="{ 'card--small': size === 'small' }" @click="$emit('click', post)">
    <div class="card-cover">
      <img v-if="post.cover" :src="post.cover" class="img" alt="cover" />
      <div v-else class="img" :style="{ background: getGradient(post.id) }"></div>
      <span class="card-badge" v-if="post.categoryName">{{ post.categoryName }}</span>
      <button
        v-if="showCollect"
        class="card-save"
        :class="{ active: post.collected }"
        @click.stop="$emit('collect', post)"
      >
        <img :src="post.collected ? favoriteIconSrc : favoriteOutlineIconSrc" alt="collect" class="collect-icon" />
      </button>
    </div>
    <div class="card-body">
      <div class="card-title">{{ post.title }}</div>
      <div class="card-tags" v-if="post.tags && post.tags.length > 0">
        <span class="card-tag" v-for="tag in post.tags.slice(0, 3)" :key="tag">{{ tag }}</span>
      </div>
      <div class="card-meta">
        <div class="card-author">
          <div class="card-avatar" v-if="!post.authorAvatar"></div>
          <img v-else :src="post.authorAvatar" class="card-avatar" alt="avatar" />
          <span class="card-author-name">{{ post.authorName || '匿名用户' }}</span>
        </div>
        <div class="card-stats">
          <button
            v-if="showLike"
            class="like-btn"
            :class="{ active: post.liked }"
            @click.stop="$emit('like', post)"
          >
            <img :src="post.liked ? likeIconSrc : likeOutlineIconSrc" alt="like" class="stat-icon" />
            {{ formatNumber(post.likes) }}
          </button>
          <span><img src="@/assets/icons/eye.svg" alt="eye" class="stat-icon" /> {{ formatNumber(post.views) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PostVO } from '@/api/post'
import likeIconSrc from '@/assets/icons/like.svg'
import likeOutlineIconSrc from '@/assets/icons/like-outline.svg'
import favoriteIconSrc from '@/assets/icons/favorite.svg'
import favoriteOutlineIconSrc from '@/assets/icons/favorite-outline.svg'
import { formatNumber } from '@/utils/format'

defineProps<{
  post: PostVO
  showCollect?: boolean
  showLike?: boolean
  size?: 'normal' | 'small'
}>()

defineEmits<{
  click: [post: PostVO]
  like: [post: PostVO]
  collect: [post: PostVO]
}>()

const gradients = [
  'linear-gradient(135deg, #ffd4e8, #ffa8c8, #ff8db5)',
  'linear-gradient(135deg, #d4e8ff, #a8c8ff, #8db5ff)',
  'linear-gradient(135deg, #f0d4ff, #d8b0ff, #c084fc)',
  'linear-gradient(135deg, #fff0d4, #ffe0a8, #ffd08d)',
  'linear-gradient(135deg, #d4ffe8, #a8ffd0, #8dffb5)',
  'linear-gradient(135deg, #ffe8d4, #ffc8a8, #ffa88d)',
  'linear-gradient(135deg, #e8d4ff, #d0b0ff)',
  'linear-gradient(135deg, #c8ffe8, #a0ffd0)',
]

const getGradient = (id: string | number) => {
  const source = String(id)
  const lastDigit = Number(source[source.length - 1] || '0')
  return gradients[lastDigit % gradients.length]
}
</script>

<style scoped>
.card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  border: 1.5px solid var(--border);
  box-shadow: 0 4px 12px rgba(180, 132, 255, 0.08);
  transition: all 0.22s ease-out;
  cursor: pointer;
}

.card:hover {
  transform: translateY(-4px) scale(1.03);
  border-color: rgba(180, 132, 255, 0.3);
  box-shadow: 0 12px 36px rgba(180, 132, 255, 0.15);
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
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(255, 138, 200, 0.9), rgba(180, 132, 255, 0.9));
  font-size: 10px;
  font-weight: 700;
  color: white;
  backdrop-filter: blur(8px);
}

.card-save {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: none;
  transition: all 0.22s ease-out;
  padding: 0;
  line-height: 0;
}

.card-save .collect-icon {
  width: 16px;
  height: 16px;
  display: block;
}

.card-save:hover {
  background: white;
  transform: scale(1.15);
  box-shadow: 0 2px 12px rgba(180, 132, 255, 0.2);
}

.card-body { padding: 14px 16px; }

.card-title {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 8px;
  color: var(--text);
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.card-tag {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 500;
  color: var(--pink);
  background: rgba(255, 107, 157, 0.08);
  border: 1px solid rgba(255, 107, 157, 0.15);
  white-space: nowrap;
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
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  border: 2px solid white;
  box-shadow: 0 1px 4px rgba(180, 132, 255, 0.15);
}

.card-author-name {
  font-size: 12px;
  color: var(--text-dim);
  font-weight: 500;
  transition: color 0.22s ease-out;
}
.card-author-name:hover {
  color: var(--pink);
}

.card-stats {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: var(--text-dim);
  line-height: 1;
}

.card-stats span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  line-height: 1;
  transition: color 0.22s ease-out;
}

.card-stats span:hover {
  color: var(--text);
}

.card-stats .stat-icon {
  width: 14px;
  height: 14px;
  display: block;
  flex-shrink: 0;
}

.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 12px;
  color: var(--text-dim);
  padding: 0;
  transition: color 0.22s ease-out;
  line-height: 1;
  font-family: inherit;
}

.like-btn .stat-icon {
  transition: transform 0.22s ease-out;
}

.like-btn:hover .stat-icon {
  transform: scale(1.35);
}

/* ===== Small Size ===== */
.card--small .card-cover {
  height: 120px;
}

.card--small .card-body {
  padding: 10px 12px;
}

.card--small .card-title {
  font-size: 13px;
  margin-bottom: 8px;
}

.card--small .card-avatar {
  width: 22px;
  height: 22px;
}

.card--small .card-author-name {
  font-size: 11px;
}

.card--small .card-stats {
  gap: 10px;
  font-size: 11px;
}

.card--small .card-stats .stat-icon {
  width: 12px;
  height: 12px;
}

.card--small .card-badge {
  padding: 3px 8px;
  font-size: 9px;
}

.card--small .card-save {
  width: 28px;
  height: 28px;
}

.card--small .card-save .collect-icon {
  width: 14px;
  height: 14px;
}

.like-btn:hover {
  color: var(--pink);
}

.like-btn.active {
  color: var(--pink);
}
</style>
