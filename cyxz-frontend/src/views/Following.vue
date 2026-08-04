<template>
  <main class="main-content">
    <div class="page-inner">

    <!-- 关注的人头像行 -->
    <section v-if="followingUsers.length > 0" class="following-section">
      <div class="following-row">
        <div
          v-for="user in followingUsers"
          :key="user.userId"
          class="following-avatar-item"
          @click="goToUser(user.userId)"
        >
          <div class="following-avatar">
            <UserAvatar :src="user.avatar" :name="user.nickname" :size="56" />
          </div>
          <span class="following-name">{{ user.nickname }}</span>
        </div>
      </div>
    </section>

    <!-- 无关注提示 -->
    <EmptyState v-if="!loading && followingUsers.length === 0" icon="ph:heart-straight" title="还没有关注任何人" hint="去发现更多创作者，关注他们就能在这里看到新作品啦" />

    <!-- 加载中 -->
    <LoadingSpinner v-if="loading && followingUsers.length === 0" />

    <!-- 动态时间线 -->
    <section v-if="posts.length > 0" class="timeline-section">
      <div class="section-label">最新动态</div>
      <div class="timeline">
        <article
          v-for="post in posts"
          :key="post.id"
          class="timeline-item"
          @click="enterPost(post)"
        >
          <!-- 作者信息栏 -->
          <div class="tl-author">
            <div class="tl-avatar">
              <UserAvatar :src="post.authorAvatar" :name="post.authorName" :size="32" />
            </div>
            <span class="tl-name">{{ post.authorName || '匿名用户' }}</span>
            <span class="tl-time">{{ formatTime(post.createTime) }}</span>
          </div>

          <!-- 内容区 -->
          <div class="tl-body">
            <div class="tl-cover" v-if="post.cover">
              <img :src="post.cover" alt="" />
            </div>
            <div class="tl-info">
              <h3 class="tl-title">{{ post.title }}</h3>
              <p class="tl-excerpt">{{ post.content?.replace(/<[^>]+>/g, '').slice(0, 140) || '' }}</p>
              <div class="tl-meta">
                <span v-if="post.circleName" class="tl-circle">{{ post.circleName }}</span>
                <span class="tl-stat">
                  <Icon icon="ph:heart" class="tl-stat-icon" /> {{ post.likes }}
                </span>
                <span class="tl-stat">
                  <Icon icon="ph:chat-circle" class="tl-stat-icon" /> {{ post.comments }}
                </span>
              </div>
            </div>
          </div>
        </article>
      </div>

      <!-- 加载更多 -->
      <div v-if="hasMore" class="load-more" @click="loadMore">
        <LoadingSpinner v-if="loadingMore" inline text="" />
        <span v-else>加载更多</span>
      </div>
    </section>

    <!-- 关注了但无动态 -->
    <EmptyState v-if="!loading && followingUsers.length > 0 && posts.length === 0 && !initialLoading" icon="ph:confetti" title="暂无新动态" hint="你关注的人还没有发布作品" />

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
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { getFollowingList } from '@/api/user'
import { getFollowingPosts } from '@/api/post'
import type { FollowUserVO } from '@/api/user'
import type { PostVO } from '@/api/post'
import { useNavigate } from '@/composables/useNavigate'
import { usePagination } from '@/composables/usePagination'
import { formatTime } from '@/utils/format'

const { open } = useNavigate()
const followingUsers = ref<FollowUserVO[]>([])

const {
  list: posts,
  loading,
  loadingMore,
  initialLoading,
  hasMore,
  load: loadPosts,
  loadMore,
} = usePagination<PostVO>(
  ({ page, size }) => getFollowingPosts({ page, size }),
  { pageSize: 10 }
)

async function loadFollowing() {
  try {
    const res = await getFollowingList({ page: 1, size: 50 })
    followingUsers.value = (res as any).records ?? []
  } catch { /* ignore */ }
}

function goToUser(userId: string) {
  open(`/user/${userId}`)
}

function enterPost(post: PostVO) {
  open(`/post/${post.id}`)
}

onMounted(async () => {
  await loadFollowing()
  await loadPosts(true)
})
</script>

<style scoped>
.main-content {
  padding: 90px 0 60px;
}

.page-inner {
  width: min(720px, calc(100vw - 48px));
  margin: 0 auto;
}

/* ===== 关注头像行 ===== */
.following-section {
  margin-bottom: 28px;
}

.following-row {
  display: flex;
  gap: 20px;
  overflow-x: auto;
  padding: 4px 0 8px;
  scrollbar-width: none;
}

.following-row::-webkit-scrollbar { display: none; }

.following-avatar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  flex-shrink: 0;
  transition: transform 0.2s;
}

.following-avatar-item:hover {
  transform: translateY(-2px);
}

.following-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--gradient-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(255, 107, 157, 0.18);
  border: 2px solid var(--white);
  transition: box-shadow 0.2s;
}

.following-avatar-item:hover .following-avatar {
  box-shadow: 0 4px 20px rgba(255, 107, 157, 0.3);
}

.following-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.following-avatar span {
  color: var(--white);
  font-size: 20px;
  font-weight: 700;
}

.following-name {
  font-size: 12px;
  color: var(--text);
  max-width: 60px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-label {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-dim);
  margin-bottom: 14px;
  padding-left: 2px;
}

/* ===== 时间线 ===== */
.timeline-section {
  margin-bottom: 36px;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.timeline-item {
  background: var(--card);
  border-radius: 14px;
  padding: 16px 18px;
  cursor: pointer;
  transition: all 0.2s;
}

.timeline-item:hover {
  background: var(--bg-soft);
}

.tl-author {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.tl-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--gradient-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.tl-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.tl-avatar span {
  color: var(--white);
  font-size: 13px;
  font-weight: 700;
}

.tl-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.tl-time {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-dim);
}

.tl-body {
  display: flex;
  gap: 14px;
}

.tl-cover {
  width: 120px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--gradient-card);
}

.tl-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.tl-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tl-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
}

.tl-excerpt {
  font-size: 13px;
  color: var(--text-dim);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
}

.tl-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 2px;
}

.tl-circle {
  font-size: 11px;
  color: var(--pink);
  background: var(--pink-bg);
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 600;
}

.tl-stat {
  font-size: 12px;
  color: var(--text-dim);
  display: flex;
  align-items: center;
  gap: 3px;
}

.tl-stat-icon {
  font-size: 14px;
}

/* ===== 加载更多 ===== */
.load-more {
  text-align: center;
  padding: 16px;
  margin-top: 12px;
  font-size: 14px;
  color: var(--pink);
  cursor: pointer;
  border-radius: 12px;
  background: var(--card);
  border: 1.5px solid var(--border-light);
  transition: all 0.2s;
}

.load-more:hover {
  border-color: var(--pink);
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.08);
}

/* ===== Footer ===== */
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
