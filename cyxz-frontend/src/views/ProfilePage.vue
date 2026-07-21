<template>
  <div class="profile-page">
    <!-- ===== 顶部：Banner ===== -->
    <div class="cover-banner"></div>

    <!-- ===== 用户信息主卡 ===== -->
    <div class="profile-card-wrap" v-if="profile">
      <div class="profile-card">
        <div class="pc-top">
          <div class="pc-left">
            <div class="avatar-wrapper" :class="{ clickable: isSelf }" @click="isSelf && router.push('/user-center?tab=avatar')">
              <div class="profile-avatar">
                <img v-if="profile.avatar" :src="profile.avatar" alt="avatar" class="profile-avatar-img" />
                <span v-else>{{ (profile.nickname || 'U').charAt(0) }}</span>
                <div v-if="isSelf" class="avatar-hover-overlay">
                  <span>更换头像</span>
                </div>
              </div>
            </div>
            <div class="pc-info">
              <div class="pc-name-row">
                <h2 class="pc-name">{{ profile.nickname }}</h2>
              </div>
              <p class="pc-bio">{{ profile.bio || '这个人很懒，什么都没写...' }}</p>
            </div>
          </div>
          <div class="pc-right">
            <template v-if="!isSelf">
              <FollowButton
                :following="following"
                :loading="followLoading"
                variant="profile"
                @toggle="toggleFollow"
              />
            </template>
          </div>
        </div>
        <div class="pc-stats-row">
          <span class="pc-stat"><strong>{{ profile?.followingCount ?? 0 }}</strong> 关注</span>
          <span class="pc-stat"><strong>{{ profile?.followerCount ?? 0 }}</strong> 粉丝</span>
          <span class="pc-stat"><strong>{{ postStats?.totalLikes ?? 0 }}</strong> 获赞</span>
          <span class="pc-stat"><strong>{{ postStats?.totalViews ?? 0 }}</strong> 浏览</span>
        </div>
      </div>
    </div>

    <!-- ===== Tab 栏 + 搜索 ===== -->
    <div class="info-bar">
      <div class="info-bar-inner">
        <div class="tab-nav">
          <a href="#" :class="{ active: activeTab === 'works' }" @click.prevent="onTabChange('works')">
            <img src="@/assets/icons/post.svg" alt="post" class="tab-icon" />作品
          </a>
          <a href="#" :class="{ active: activeTab === 'favorites' }" @click.prevent="onTabChange('favorites')">
            <img src="@/assets/icons/favorite.svg" alt="favorite" class="tab-icon" />收藏
          </a>
        </div>
      </div>
    </div>

    <!-- ===== 内容区 ===== -->
    <div class="content-area">
      <div class="content-tools">
        <div class="tab-search">
          <img src="@/assets/icons/search.svg" alt="search" class="search-icon" />
          <input v-model="searchKeyword" :placeholder="activeTab === 'works' ? '搜索作品...' : '搜索收藏...'"></input>
        </div>
      </div>

      <!-- 作品 tab -->
      <div v-if="activeTab === 'works'">
        <div class="content-grid" v-if="filteredPosts.length">
          <PostCard
            v-for="item in filteredPosts"
            :key="item.id"
            :post="item"
            size="small"
            :show-collect="false"
            :show-like="false"
            @click="goToPost"
          />
        </div>
        <EmptyState v-else-if="!postLoading" :title="searchKeyword ? '没有匹配的作品' : '还没有发布任何作品'" :hint="isSelf ? '快去发布你的第一篇帖子吧~' : ''">
          <template v-if="isSelf" #actions>
            <button class="guide-btn guide-btn-primary" @click="goToCreatePost">
              <img src="@/assets/icons/edit.svg" alt="edit" class="btn-icon" />发布帖子
            </button>
          </template>
        </EmptyState>
        <div v-else class="loading-placeholder">加载中...</div>
      </div>

      <!-- 收藏 tab -->
      <div v-if="activeTab === 'favorites'">
        <div class="content-grid" v-if="filteredFavorites.length">
          <PostCard
            v-for="item in filteredFavorites"
            :key="item.id"
            :post="item"
            size="small"
            :show-collect="false"
            :show-like="false"
            @click="goToPost"
          />
        </div>
        <EmptyState v-else-if="!favoriteLoading" :title="searchKeyword ? '没有匹配的收藏' : '还没有收藏任何内容'" hint="发现喜欢的帖子就收藏起来吧~" />
        <div v-else class="loading-placeholder">加载中...</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserProfile } from '@/api/user'
import type { UserInfo } from '@/api/user'
import { getUserPostsByTarget, getUserFavorites } from '@/api/post'
import type { PostVO } from '@/api/post'
import { usePostStats } from '@/composables/usePostStats'
import { useUserStore } from '@/stores/user'
import { useFollow } from '@/composables/useFollow'
import PostCard from '@/components/PostCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import FollowButton from '@/components/FollowButton.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { following, followLoading, checkFollowing, toggleFollow: doFollow } = useFollow()
const { stats: postStats, loadUserStats: loadPostStats } = usePostStats()

const profile = ref<UserInfo | null>(null)
const loading = ref(true)
const activeTab = ref('works')

const posts = ref<PostVO[]>([])
const favorites = ref<PostVO[]>([])
const postLoading = ref(false)
const favoriteLoading = ref(false)
const searchKeyword = ref('')

const filteredPosts = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return posts.value
  return posts.value.filter(p => p.title?.toLowerCase().includes(kw))
})
const filteredFavorites = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return favorites.value
  return favorites.value.filter(p => p.title?.toLowerCase().includes(kw))
})

const isSelf = computed(() => {
  return String(profile.value?.userId) === String(userStore.userInfo?.id)
})

onMounted(async () => {
  const userId = String(route.params.id)
  if (!userId) { loading.value = false; return }
  // 支持 ?tab=favorites 参数
  if (route.query.tab === 'favorites') {
    activeTab.value = 'favorites'
  }
  try {
    const res = await getUserProfile(userId)
    const data = (res.data as any).data || res.data
    profile.value = data
    // 加载作品列表
    await loadPosts(userId)
    // 加载收藏列表（主页 tab 也需要展示）
    await loadFavorites(userId)
    // 加载帖子统计（获赞、浏览）
    await loadPostStats(userId)
    // 非本人时查询关注状态
    if (!isSelf.value) {
      await checkFollowing(userId)
    }
  } catch {
    ElMessage.error('加载用户信息失败')
  } finally {
    loading.value = false
  }
})

async function loadPosts(userId: string) {
  postLoading.value = true
  try {
    const res = await getUserPostsByTarget(userId, { page: 1, size: 20 })
    const data = (res.data as any).data || res.data
    posts.value = data?.records || []
  } catch {
    posts.value = []
  } finally {
    postLoading.value = false
  }
}

async function loadFavorites(userId: string) {
  favoriteLoading.value = true
  try {
    const res = await getUserFavorites(userId, { page: 1, size: 20 })
    const data = (res.data as any).data || res.data
    favorites.value = data?.records || []
  } catch {
    favorites.value = []
  } finally {
    favoriteLoading.value = false
  }
}

// tab 切换时加载对应数据
function onTabChange(tab: string) {
  activeTab.value = tab
  searchKeyword.value = ''
  const userId = String(route.params.id)
  if (tab === 'works' && posts.value.length === 0) {
    loadPosts(userId)
  } else if (tab === 'favorites' && favorites.value.length === 0) {
    loadFavorites(userId)
  }
}

function toggleFollow() {
  doFollow(String(route.params.id), (nowFollowing) => {
    if (profile.value) {
      profile.value.followerCount = nowFollowing
        ? (profile.value.followerCount || 0) + 1
        : Math.max((profile.value.followerCount || 0) - 1, 0)
    }
  })
}

function goToCreatePost() {
  router.push('/creator')
}

function goToPost(post: PostVO) {
  const url = router.resolve(`/post/${post.id}`).href
  window.open(url, '_blank')
}
</script>

<style scoped>
.profile-page {
  padding-bottom: 60px;
  background: var(--bg);
}

/* ===== Banner ===== */
.cover-banner {
  width: 100%;
  height: 280px;
  background-image:
    linear-gradient(180deg, rgba(255,255,255,0) 50%, rgba(0,0,0,0.22) 100%),
    url('@/assets/images/post-detail-bg.svg');
  background-size: cover;
  background-position: center;
}

/* ===== 用户信息主卡 ===== */
.profile-card-wrap {
  max-width: 1220px;
  margin: -160px auto 0;
  padding: 0 24px;
  position: relative;
  z-index: 10;
}
.profile-card {
  padding: 0 4px 10px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  border-radius: 20px 20px 0 0;
}
.pc-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}
.pc-left {
  display: flex;
  align-items: center;
  gap: 20px;
}
.pc-info { display: flex; flex-direction: column; gap: 6px; }
.pc-name-row { display: flex; align-items: center; gap: 10px; }
.pc-name {
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
  text-shadow: 0 1px 4px rgba(255,255,255,0.6);
}
.pc-bio {
  font-size: 13px;
  color: #fff;
  margin: 0;
  line-height: 1.5;
  text-shadow: 0 1px 3px rgba(0,0,0,0.18);
}

/* stats row */
.pc-stats-row {
  display: flex;
  gap: 24px;
  padding: 14px 4px 8px;
  flex-wrap: wrap;
}
.pc-stat {
  font-size: 13px;
  color: rgba(255,255,255,0.9);
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: baseline;
  gap: 5px;
}
.pc-stat strong {
  font-size: 20px;
  font-weight: 700;
  color: white;
}
.pc-stat:hover {
  opacity: 0.85;
}

/* right actions */
.pc-right { flex-shrink: 0; }
.pc-action-btn {
  padding: 8px 24px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid rgba(255,255,255,0.3);
  background: rgba(255,255,255,0.18);
  color: white;
}
.pc-action-btn:hover {
  background: rgba(255,255,255,0.35);
  transform: translateY(-1px);
}

/* avatar */
.avatar-wrapper {
  position: relative;
  cursor: default;
  flex-shrink: 0;
  align-self: flex-end;
}
.avatar-wrapper.clickable {
  cursor: pointer;
}
.profile-avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 36px;
  font-weight: 800;
  border: 4px solid var(--bg);
  box-shadow: var(--shadow);
  transition: transform 0.2s;
  overflow: hidden;
}
.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-wrapper:hover .profile-avatar {
  transform: scale(1.04);
}
.avatar-wrapper.clickable:hover .profile-avatar-img {
  filter: blur(4px) brightness(0.6);
}
.avatar-wrapper.clickable:hover .avatar-hover-overlay {
  opacity: 1;
}
.avatar-hover-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.22s ease;
  z-index: 1;
}
.avatar-hover-overlay span {
  color: var(--pink);
  font-size: 13px;
  font-weight: 700;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.3));
}

/* ===== Tab 栏 ===== */
.info-bar {
  background: transparent;
  border-bottom: none;
  position: sticky;
  top: 78px;
  z-index: 20;
  padding-top: 6px;
}
.info-bar-inner {
  max-width: 1220px;
  margin: 0 auto;
  padding: 4px 24px 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  min-height: 48px;
}
.tab-nav {
  display: inline-flex;
  gap: 8px;
  margin: 0 auto;
  padding: 6px;
  background: rgba(255,244,250,0.96);
  border: 1px solid var(--border-light);
  border-radius: 999px;
  box-shadow: 0 4px 14px rgba(255,107,157,0.06);
}
.tab-nav a {
  text-decoration: none;
  color: var(--text-dim);
  font-size: 13px;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: 999px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}
.tab-nav a:hover {
  color: var(--pink);
  background: var(--pink-bg);
}
.tab-nav a.active {
  color: var(--pink);
  font-weight: 700;
  background: var(--card);
  box-shadow:
    0 4px 12px rgba(255,107,157,0.1),
    inset 0 0 0 1px rgba(255,107,157,0.16);
}
.tab-icon { width: 16px; height: 16px; }

.tab-search {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255,248,252,0.95);
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 8px 16px;
  box-shadow: inset 0 1px 3px rgba(255,107,157,0.06);
  transition: background 0.2s, box-shadow 0.2s, border-color 0.2s;
}
.tab-search:focus-within {
  background: var(--card);
  border-color: var(--border);
  box-shadow:
    inset 0 1px 3px rgba(255,107,157,0.06),
    0 0 0 2px rgba(255,107,157,0.12);
}
.tab-search input {
  border: none;
  background: none;
  outline: none;
  font-size: 13px;
  width: 150px;
  color: var(--text);
}
.tab-search input::placeholder { color: var(--text-dim); }
.tab-search .search-icon { width: 14px; height: 14px; opacity: 0.4; }

/* ===== 内容区 ===== */
.content-area {
  max-width: 1220px;
  margin: 0 auto;
  padding: 0 24px 60px;
}
.content-tools {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 18px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 16px;
}
.section-icon { width: 18px; height: 18px; }
.section-count {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-dim);
  background: var(--purple-bg);
  padding: 2px 8px;
  border-radius: 10px;
}

/* 内容网格：四列 */
.content-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 270px));
  gap: 16px;
  justify-content: start;
}
.content-grid :deep(.card) {
  width: 270px;
  border-radius: 14px;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow);
  transition: all 0.25s ease;
}
.content-grid :deep(.card:hover) {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
  border-color: var(--border);
}
.content-grid :deep(.card-cover) {
  /* 使用 PostCard 默认的 4:3 比例，不做覆盖 */
}

/* 通用 */
.guide-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  border: none;
}
.guide-btn-primary {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  box-shadow: var(--shadow);
}
.guide-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}
.btn-icon { width: 16px; height: 16px; margin-right: 6px; filter: brightness(0) invert(1); }
.loading-placeholder {
  text-align: center;
  padding: 60px 0;
  color: var(--text-dim);
  font-size: 14px;
}

/* 响应式 */
@media (max-width: 768px) {
  .cover-banner { height: 124px; }
  .profile-card {
    gap: 16px;
    padding: 20px;
  }
  .pc-top {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .pc-left { gap: 14px; }
  .pc-name { font-size: 18px; }
  .pc-stats-row { gap: 14px; flex-wrap: wrap; }
  .content-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  .content-grid :deep(.card) {
    width: 100%;
  }
  .info-bar-inner {
    padding: 8px 16px;
    justify-content: center;
  }
  .tab-nav {
    width: 100%;
    justify-content: center;
    overflow-x: auto;
  }
  .tab-nav a { padding: 10px 12px; font-size: 13px; }
  .tab-search { display: none; }
  .content-area { padding: 20px 16px 40px; }
  .section-block { padding: 16px; }
}
</style>