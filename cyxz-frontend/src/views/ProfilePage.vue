<template>
  <div class="profile-page">
    <div class="cover-banner">
      <span class="cover-deco star-1"></span>
      <img src="@/assets/icons/favorite.svg" alt="star" class="cover-deco star-2" />
      <span class="cover-deco star-3"></span>
      <span class="cover-deco star-4"></span>
      <img src="@/assets/icons/sparkle.svg" alt="sparkle" class="cover-deco star-5" />

      <div class="profile-on-banner" v-if="profile">
        <div class="avatar-wrapper" @click="isSelf && startEdit()">
          <div class="profile-avatar">
            <img v-if="profile.avatar" :src="profile.avatar" alt="avatar" class="profile-avatar-img" />
            <span v-else>{{ (profile.nickname || 'U').charAt(0) }}</span>
          </div>
          <div v-if="isSelf" class="avatar-edit-badge" title="编辑资料">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
          </div>
        </div>
        <div class="profile-detail">
          <h2>{{ profile.nickname }}</h2>
          <p class="profile-bio">{{ profile.bio || '这个人很懒，什么都没写...' }}</p>
        </div>
        <button v-if="!isSelf"
                class="follow-btn"
                :class="{ followed: following }"
                :disabled="followLoading"
                @click="toggleFollow">
          {{ following ? '已关注' : '关注' }}
        </button>
      </div>
    </div>

    <div class="info-bar">
      <div class="info-bar-inner">
        <div class="tab-nav">
          <a href="#" :class="{ active: activeTab === 'home' }" @click.prevent="onTabChange('home')">
            <img src="@/assets/icons/home.svg" alt="home" class="tab-icon" />主页
          </a>
          <a href="#" :class="{ active: activeTab === 'works' }" @click.prevent="onTabChange('works')">
            <img src="@/assets/icons/post.svg" alt="post" class="tab-icon" />作品
          </a>
          <a href="#" :class="{ active: activeTab === 'favorites' }" @click.prevent="onTabChange('favorites')">
            <img src="@/assets/icons/favorite.svg" alt="favorite" class="tab-icon" />收藏
          </a>
        </div>
        <div class="info-bar-right">
          <div class="tab-search">
            <img src="@/assets/icons/search.svg" alt="search" class="search-icon" />
            <input type="text" placeholder="搜索作品、动态" />
          </div>
          <div class="profile-stats">
            <div class="profile-stat">
              <div class="num">{{ profile?.followingCount ?? 0 }}</div>
              <div class="label">关注</div>
            </div>
            <div class="profile-stat">
              <div class="num">{{ profile?.followerCount ?? 0 }}</div>
              <div class="label">粉丝</div>
            </div>
            <div class="profile-stat">
              <div class="num">{{ postStats?.totalLikes ?? 0 }}</div>
              <div class="label">获赞</div>
            </div>
            <div class="profile-stat">
              <div class="num">{{ postStats?.totalViews ?? 0 }}</div>
              <div class="label">浏览</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="content-area">
      <!-- 主页 tab：作品 + 收藏 -->
      <div v-if="activeTab === 'home'">
        <!-- 作品 -->
        <div class="section-block">
          <div class="section-block-title">
            <img src="@/assets/icons/post.svg" alt="post" class="section-block-icon" />
            作品
            <span class="section-block-count">{{ posts.length }}</span>
          </div>
          <div class="content-grid" v-if="posts.length">
            <PostCard
              v-for="item in posts"
              :key="item.id"
              :post="item"
              size="small"
              :show-collect="false"
              :show-like="false"
              @click="goToPost"
            />
          </div>
          <div v-else-if="!postLoading" class="empty-state">
            <p class="empty-text">这里还没有任何内容</p>
            <p class="empty-hint">发布你的第一条动态，让大家认识你吧~</p>
            <div class="empty-actions" v-if="isSelf">
              <button class="guide-btn guide-btn-primary" @click="goToCreatePost">
                <img src="@/assets/icons/edit.svg" alt="edit" class="btn-icon" />投稿作品
              </button>
            </div>
          </div>
          <div v-else class="loading-placeholder">加载中...</div>
        </div>

        <!-- 收藏 -->
        <div class="section-block">
          <div class="section-block-title">
            <img src="@/assets/icons/favorite.svg" alt="favorite" class="section-block-icon" />
            收藏
            <span class="section-block-count">{{ favorites.length }}</span>
          </div>
          <div class="content-grid" v-if="favorites.length">
            <PostCard
              v-for="item in favorites"
              :key="item.id"
              :post="item"
              size="small"
              :show-collect="false"
              :show-like="false"
              @click="goToPost"
            />
          </div>
          <div v-else-if="!favoriteLoading" class="empty-state">
            <p class="empty-text">还没有收藏任何内容</p>
            <p class="empty-hint">发现喜欢的帖子就收藏起来吧~</p>
          </div>
          <div v-else class="loading-placeholder">加载中...</div>
        </div>
      </div>

      <!-- 作品 tab：显示作品 -->
      <div v-if="activeTab === 'works'">
        <div class="content-grid" v-if="posts.length">
          <PostCard
            v-for="item in posts"
            :key="item.id"
            :post="item"
            size="small"
            :show-collect="false"
            :show-like="false"
            @click="goToPost"
          />
        </div>
        <div v-else-if="!postLoading" class="empty-state">
          <p class="empty-text">你还没有发布任何作品</p>
          <p class="empty-hint">快去发布你的第一篇帖子吧~</p>
          <div class="empty-actions" v-if="isSelf">
            <button class="guide-btn guide-btn-primary" @click="goToCreatePost">
              <img src="@/assets/icons/edit.svg" alt="edit" class="btn-icon" />发布帖子
            </button>
          </div>
        </div>
        <div v-else class="loading-placeholder">加载中...</div>
      </div>

      <!-- 收藏 tab：显示收藏 -->
      <div v-if="activeTab === 'favorites'">
        <div class="content-grid" v-if="favorites.length">
          <PostCard
            v-for="item in favorites"
            :key="item.id"
            :post="item"
            size="small"
            :show-collect="false"
            :show-like="false"
            @click="goToPost"
          />
        </div>
        <div v-else-if="!favoriteLoading" class="empty-state">
          <p class="empty-text">你还没有收藏任何内容</p>
          <p class="empty-hint">发现喜欢的帖子就收藏起来吧~</p>
        </div>
        <div v-else class="loading-placeholder">加载中...</div>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="modal-slide">
        <div v-if="showEdit" class="edit-overlay" @click.self="cancelEdit">
          <div class="edit-modal">
            <span class="modal-deco deco-tl"></span>
            <span class="modal-deco deco-br"></span>

            <button class="edit-close" @click="cancelEdit">✕</button>
            <h3 class="edit-title">编辑资料</h3>
            <div class="edit-divider"></div>

            <div class="avatar-upload-row">
              <div class="avatar-preview" @click="triggerUpload">
                <img v-if="editForm.avatar" :src="editForm.avatar" alt="avatar" />
                <span v-else class="avatar-placeholder">{{ (editForm.nickname || 'U').charAt(0) }}</span>
                <div class="avatar-upload-mask">
                  <svg class="avatar-upload-camera" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
                    <circle cx="12" cy="13" r="4"/>
                  </svg>
                  <span>更换头像</span>
                </div>
              </div>
              <input
                ref="fileInput"
                type="file"
                accept="image/png,image/jpeg,image/gif"
                style="display:none"
                @change="handleAvatarChange"
              />
              <div class="avatar-upload-hint">支持 PNG / JPG / GIF，最大 10MB</div>
            </div>

            <el-form :model="editForm" label-position="top">
              <el-form-item label="昵称">
                <el-input v-model="editForm.nickname" maxlength="20" placeholder="输入你的昵称" />
              </el-form-item>
              <el-form-item label="简介">
                <el-input v-model="editForm.bio" type="textarea" :rows="3" maxlength="200" placeholder="介绍一下自己吧~" />
              </el-form-item>
              <el-form-item label="生日">
                <el-date-picker
                  v-model="editForm.birthday"
                  type="date"
                  placeholder="选择你的生日"
                  class="birthday-picker"
                />
              </el-form-item>
              <el-form-item label="性别" class="gender-item">
                <el-radio-group v-model="editForm.gender" class="gender-group">
                  <el-radio :value="0">保密</el-radio>
                  <el-radio :value="1">男</el-radio>
                  <el-radio :value="2">女</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-form>
            <div class="edit-actions">
              <el-button class="edit-cancel-btn" @click="cancelEdit">取消</el-button>
              <el-button class="edit-save-btn" type="primary" @click="saveEdit" :loading="saving">保存</el-button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserProfile, updateUserProfile, followUser, unfollowUser, isFollowing } from '@/api/user'
import type { UserInfo } from '@/api/user'
import { uploadAvatar } from '@/api/upload'
import { getUserPostsByTarget, getUserFavorites, getUserPostStats } from '@/api/post'
import type { PostVO, PostStatsVO } from '@/api/post'
import { useUserStore } from '@/stores/user'
import PostCard from '@/components/PostCard.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const profile = ref<UserInfo | null>(null)
const loading = ref(true)
const showEdit = ref(false)
const saving = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement>()
const activeTab = ref('home')
const following = ref(false)
const followLoading = ref(false)

const editForm = reactive({
  nickname: '',
  bio: '',
  gender: 0,
  avatar: '',
  birthday: '',
})

const posts = ref<PostVO[]>([])
const favorites = ref<PostVO[]>([])
const postLoading = ref(false)
const favoriteLoading = ref(false)
const postStats = ref<PostStatsVO | null>(null)

const isSelf = computed(() => {
  return String(profile.value?.userId) === String(userStore.userInfo?.id)
})

onMounted(async () => {
  const userId = String(route.params.id)
  if (!userId) { loading.value = false; return }
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
      try {
        const followRes = await isFollowing(userId)
        following.value = ((followRes.data as any).data) === true
      } catch {
        // 忽略关注状态查询失败
      }
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

async function loadPostStats(userId: string) {
  try {
    const res = await getUserPostStats(userId)
    const data = (res.data as any).data || res.data
    if (data) {
      postStats.value = {
        totalPosts: Number(data.totalPosts) || 0,
        totalViews: Number(data.totalViews) || 0,
        totalLikes: Number(data.totalLikes) || 0,
        totalCollections: Number(data.totalCollections) || 0,
      }
    }
  } catch {
    // 忽略统计加载失败，保留 null 由模板兜底为 0
  }
}

// tab 切换时加载对应数据
function onTabChange(tab: string) {
  activeTab.value = tab
  const userId = String(route.params.id)
  if (tab === 'works' && posts.value.length === 0) {
    loadPosts(userId)
  } else if (tab === 'favorites' && favorites.value.length === 0) {
    loadFavorites(userId)
  }
}

function startEdit() {
  if (!profile.value) return
  editForm.nickname = profile.value.nickname
  editForm.bio = profile.value.bio || ''
  editForm.gender = profile.value.gender ?? 0
  editForm.avatar = profile.value.avatar || ''
  editForm.birthday = profile.value.birthday || ''
  showEdit.value = true
}

function triggerUpload() {
  fileInput.value?.click()
}

async function handleAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 10MB')
    return
  }

  uploading.value = true
  try {
    const res = await uploadAvatar(file)
    const data = (res.data as any).data || res.data
    const url = typeof data === 'string' ? data : data?.url
    if (url) {
      editForm.avatar = url
      ElMessage.success('头像上传成功')
    }
  } catch {
    ElMessage.error('头像上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

function cancelEdit() {
  showEdit.value = false
}

function formatDate(date: Date | string): string {
  if (!date) return ''
  const d = typeof date === 'string' ? new Date(date) : date
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

async function saveEdit() {
  saving.value = true
  try {
    const payload = {
      ...editForm,
      birthday: editForm.birthday ? formatDate(editForm.birthday) : '',
    }
    await updateUserProfile(payload)
    if (profile.value) {
      profile.value.nickname = editForm.nickname
      profile.value.bio = editForm.bio
      profile.value.gender = editForm.gender
      profile.value.avatar = editForm.avatar
      profile.value.birthday = editForm.birthday
    }
    if (isSelf.value && userStore.userInfo) {
      userStore.setUserInfo({ ...userStore.userInfo, nickname: editForm.nickname, avatar: editForm.avatar } as any)
    }
    ElMessage.success('保存成功')
    showEdit.value = false
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleFollow() {
  if (!userStore.isLoggedIn) {
    userStore.openLoginModal()
    return
  }
  const targetUserId = String(route.params.id)
  followLoading.value = true
  const oldFollowing = following.value
  following.value = !oldFollowing
  try {
    if (oldFollowing) {
      await unfollowUser(targetUserId)
      ElMessage.success('已取消关注')
    } else {
      await followUser(targetUserId)
      ElMessage.success('关注成功')
    }
    // 本地更新粉丝数
    if (profile.value) {
      profile.value.followerCount = oldFollowing
        ? Math.max((profile.value.followerCount || 0) - 1, 0)
        : (profile.value.followerCount || 0) + 1
    }
  } catch {
    following.value = oldFollowing
    ElMessage.error('操作失败')
  } finally {
    followLoading.value = false
  }
}

function goToCreatePost() {
  router.push('/creator')
}

function goToPost(post: PostVO) {
  router.push(`/post/${post.id}`)
}
</script>

<style scoped>
.profile-page {
  padding-bottom: 60px;
}

.cover-banner {
  width: 100%;
  height: 260px;
  background: linear-gradient(135deg, #fce4ec 0%, #e8eaf6 40%, #f3e5f5 70%, #fce4ec 100%);
  position: relative;
  overflow: hidden;
}
.cover-banner::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 50%, rgba(255,107,157,0.15) 0%, transparent 50%),
    radial-gradient(circle at 80% 30%, rgba(192,132,252,0.15) 0%, transparent 50%),
    radial-gradient(circle at 60% 80%, rgba(96,165,250,0.1) 0%, transparent 40%);
}

.cover-deco {
  position: absolute;
  font-size: 60px;
  opacity: 0.12;
}
.star-1 {
  top: 20px; left: 10%;
  width: 30px; height: 30px;
  background: radial-gradient(circle, rgba(255,182,193,0.5), transparent 70%);
  border-radius: 50%;
  animation: twinkle 3s ease-in-out infinite;
}
.star-2 {
  top: 60px; left: 30%;
  font-size: 40px;
  animation: float 4s ease-in-out infinite;
}
.star-3 {
  bottom: 10px; left: 55%;
  font-size: 50px;
  animation: float 5s ease-in-out infinite 1s;
}
.star-4 {
  top: 30px; right: 15%;
  width: 24px; height: 24px;
  background: radial-gradient(circle, rgba(192,132,252,0.4), transparent 70%);
  border-radius: 50%;
  animation: twinkle 4s ease-in-out infinite 0.5s;
}
.star-5 {
  bottom: 20px; right: 30%;
  font-size: 35px;
  animation: float 3.5s ease-in-out infinite 0.8s;
}

@keyframes twinkle {
  0%, 100% { opacity: 0.1; transform: scale(1); }
  50% { opacity: 0.25; transform: scale(1.3); }
}
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.profile-on-banner {
  position: absolute;
  bottom: 20px;
  left: calc((100vw - 1500px) / 2 + 32px);
  display: flex;
  align-items: flex-end;
  gap: 20px;
  z-index: 10;
}

.avatar-wrapper {
  position: relative;
  cursor: default;
  flex-shrink: 0;
}
.avatar-wrapper:hover .avatar-edit-badge {
  transform: translate(50%, -50%) scale(1.15);
  opacity: 1;
}
.profile-avatar {
  width: 86px;
  height: 86px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 34px;
  font-weight: 800;
  border: 4px solid white;
  box-shadow: 0 4px 20px rgba(255,107,157,0.3);
  transition: transform 0.2s;
  overflow: hidden;
}
.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-wrapper:hover .profile-avatar {
  transform: scale(1.03);
}
.avatar-edit-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  border: 2.5px solid white;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.7;
  transform: translate(50%, -50%) scale(1);
  transition: all 0.25s ease;
  box-shadow: 0 2px 8px rgba(255,107,157,0.3);
}

.profile-detail { padding-bottom: 10px; }
.profile-detail h2 {
  font-size: 22px;
  font-weight: 700;
  color: white;
  text-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.profile-bio {
  font-size: 13px;
  color: rgba(255,255,255,0.9);
  margin-top: 4px;
  text-shadow: 0 1px 4px rgba(0,0,0,0.1);
}

.follow-btn {
  padding: 8px 24px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  border: none;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  box-shadow: 0 4px 16px rgba(255,107,157,0.3);
  transition: all 0.25s ease;
  margin-left: 20px;
  margin-bottom: 12px;
  align-self: flex-end;
  flex-shrink: 0;
}
.follow-btn:hover:not(:disabled) {
  transform: translateY(-2px) scale(1.03);
  box-shadow: 0 6px 24px rgba(255,107,157,0.4);
}
.follow-btn.followed {
  background: rgba(255,255,255,0.9);
  color: var(--text-dim);
  border: 1.5px solid rgba(255,255,255,0.6);
  box-shadow: none;
}
.follow-btn.followed:hover:not(:disabled) {
  border-color: var(--pink);
  color: var(--pink);
  background: white;
}
.follow-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.info-bar {
  background: white;
  border-bottom: 1px solid var(--border);
}
.info-bar-inner {
  max-width: 1500px;
  margin: 0 auto;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}
.tab-nav { display: flex; gap: 4px; }
.tab-nav a {
  text-decoration: none;
  color: var(--text-dim);
  font-size: 14px;
  font-weight: 600;
  padding: 18px 20px;
  border-bottom: 3px solid transparent;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}
.tab-nav a:first-child { padding-left: 0; }
.tab-nav a:hover { color: var(--pink); }
.tab-nav a.active {
  color: var(--pink);
  border-bottom-color: var(--pink);
}
.info-bar-right {
  display: flex;
  align-items: center;
  gap: 24px;
}
.tab-search {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f5f0f7;
  border-radius: 20px;
  padding: 8px 16px;
}
.tab-search input {
  border: none;
  background: none;
  outline: none;
  font-size: 12px;
  width: 140px;
  color: var(--text);
}
.tab-search input::placeholder { color: #bbb; }
.tab-search .search-icon { width: 14px; height: 14px; opacity: 0.5; }

.tab-icon { width: 16px; height: 16px; }
.btn-icon { width: 16px; height: 16px; margin-right: 6px; filter: brightness(0) invert(1); }

.profile-stats {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-shrink: 0;
}
.profile-stat {
  text-align: center;
  cursor: pointer;
  padding: 6px 16px;
  border-radius: 12px;
  transition: all 0.25s ease;
}
.profile-stat:hover {
  background: rgba(255,107,157,0.06);
  transform: translateY(-2px);
}
.profile-stat .num {
  font-size: 20px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.profile-stat .label {
  font-size: 11px;
  color: var(--text-dim);
  margin-top: 2px;
}

.content-area {
  max-width: 1500px;
  margin: 0 auto;
  padding: 24px 32px 60px;
}

/* ===== Section Block (主页 tab 分块) ===== */
.section-block {
  margin-bottom: 36px;
  padding-bottom: 36px;
  border-bottom: 1px solid var(--border);
}
.section-block:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}
.section-block-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 16px;
}
.section-block-icon {
  width: 18px;
  height: 18px;
}
.section-block-count {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-dim);
  background: rgba(180, 132, 255, 0.08);
  padding: 2px 8px;
  border-radius: 10px;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0 40px;
}
.empty-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 6px;
}
.empty-hint {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0 0 24px;
}
.empty-actions {
  display: flex;
  gap: 12px;
}
.guide-btn {
  display: flex;
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
  box-shadow: 0 4px 20px rgba(255,107,157,0.3);
}
.guide-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 28px rgba(255,107,157,0.4);
}

.loading-placeholder {
  text-align: center;
  padding: 60px 0;
  color: var(--text-dim);
  font-size: 14px;
}

.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  position: relative;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
  border: 3px solid rgba(255,138,200,0.15);
}
.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FF8AC8, #B484FF);
  color: white;
  font-size: 28px;
  font-weight: 800;
}
.avatar-upload-mask {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.35);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.25s ease;
}
.avatar-upload-mask span {
  color: white;
  font-size: 12px;
  font-weight: 600;
}
.avatar-upload-camera {
  width: 24px;
  height: 24px;
  transition: transform 0.3s ease;
}
.avatar-preview:hover .avatar-upload-mask {
  opacity: 1;
}
.avatar-preview:hover .avatar-upload-camera {
  animation: cameraPop 0.4s ease-out;
}

@keyframes cameraPop {
  0% { transform: scale(0.3); opacity: 0; }
  60% { transform: scale(1.3); }
  100% { transform: scale(1); opacity: 1; }
}
.avatar-upload-hint {
  font-size: 12px;
  color: #999;
}

.edit-overlay {
  position: fixed;
  inset: 0;
  z-index: 300;
  background: rgba(180, 132, 255, 0.15);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
}
.edit-modal {
  background: linear-gradient(180deg, #FFF9FD 0%, #ffffff 100%);
  border-radius: 20px;
  padding: 32px 40px 24px;
  width: 480px;
  max-width: 92vw;
  box-shadow:
    0 8px 30px rgba(180, 132, 255, 0.18),
    0 2px 8px rgba(255, 138, 200, 0.08);
  position: relative;
  overflow: hidden;
}

.modal-deco {
  position: absolute;
  font-size: 28px;
  opacity: 0.12;
  pointer-events: none;
  user-select: none;
}
.deco-tl {
  top: 12px;
  left: 16px;
}
.deco-tl::before {
  content: '✦';
}
.deco-br {
  bottom: 12px;
  right: 16px;
}
.deco-br::before {
  content: '♡';
}

.edit-title {
  font-size: 22px;
  font-weight: 800;
  margin: 0 0 12px;
  background: linear-gradient(135deg, #FF8AC8, #B484FF);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.edit-divider {
  height: 2px;
  background: linear-gradient(90deg, #FF8AC8, #B484FF, transparent);
  border-radius: 1px;
  margin-bottom: 20px;
  opacity: 0.25;
}

.edit-close {
  position: absolute;
  top: 14px;
  right: 18px;
  background: none;
  border: none;
  font-size: 15px;
  color: #bbb;
  cursor: pointer;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease;
}
.edit-close:hover {
  background: rgba(255,138,200,0.1);
  color: #FF8AC8;
  transform: scale(1.1);
}

.edit-modal :deep(.el-form-item) {
  margin-bottom: 20px;
}
.edit-modal :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 600;
  color: #555;
  padding-bottom: 8px;
}

.edit-modal :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: none;
  border: 1.5px solid rgba(255,138,200,0.25);
  padding: 4px 14px;
  height: 44px;
  background: white;
  transition: all 0.22s ease;
}
.edit-modal :deep(.el-input__wrapper:hover) {
  border-color: rgba(255,138,200,0.45);
}
.edit-modal :deep(.el-input__wrapper.is-focus) {
  border-color: #B484FF;
  box-shadow: 0 0 0 3px rgba(180,132,255,0.12);
}

.edit-modal :deep(.el-textarea__inner) {
  border-radius: 12px;
  box-shadow: none;
  border: 1.5px solid rgba(255,138,200,0.25);
  background: white;
  transition: all 0.22s ease;
  min-height: 80px;
}
.edit-modal :deep(.el-textarea__inner:hover) {
  border-color: rgba(255,138,200,0.45);
}
.edit-modal :deep(.el-textarea__inner:focus) {
  border-color: #B484FF;
  box-shadow: 0 0 0 3px rgba(180,132,255,0.12);
}

.gender-item {
  margin-bottom: 24px !important;
}
.gender-group {
  display: flex;
  gap: 24px;
}
.birthday-picker {
  width: 100%;
}
.edit-modal :deep(.el-radio) {
  margin-right: 0;
  padding: 6px 20px;
  border-radius: 10px;
  border: 1.5px solid rgba(255,138,200,0.2);
  transition: all 0.22s ease;
}
.edit-modal :deep(.el-radio:hover) {
  border-color: rgba(255,138,200,0.4);
  background: rgba(255,138,200,0.03);
}
.edit-modal :deep(.el-radio__input) {
  display: none;
}
.edit-modal :deep(.el-radio__label) {
  font-size: 14px;
  font-weight: 500;
  color: #666;
  padding-left: 0 !important;
  transition: color 0.2s;
}
.edit-modal :deep(.el-radio.is-checked) {
  border-color: #B484FF;
  background: rgba(180,132,255,0.04);
  box-shadow: 0 0 0 3px rgba(180,132,255,0.08);
}
.edit-modal :deep(.el-radio.is-checked .el-radio__label) {
  background: linear-gradient(135deg, #FF8AC8, #B484FF);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 700;
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 4px;
  padding-bottom: 4px;
}
.edit-cancel-btn {
  border-radius: 12px;
  font-weight: 600;
  font-size: 14px;
  padding: 10px 28px;
  background: white;
  border: 1.5px solid rgba(180,132,255,0.3) !important;
  color: #B484FF;
  transition: all 0.22s ease;
}
.edit-cancel-btn:hover {
  background: rgba(180,132,255,0.04);
  transform: translateY(-1px);
}
.edit-save-btn {
  border-radius: 12px;
  font-weight: 700;
  font-size: 14px;
  padding: 10px 36px;
  background: linear-gradient(135deg, #FF8AC8, #B484FF);
  border: none;
  box-shadow:
    0 4px 16px rgba(255,138,200,0.25),
    inset 0 1px 0 rgba(255,255,255,0.2);
  transition: all 0.22s ease;
}
.edit-save-btn:hover {
  box-shadow:
    0 6px 24px rgba(180,132,255,0.3),
    inset 0 1px 0 rgba(255,255,255,0.2);
  transform: scale(1.03);
}
.edit-save-btn:active {
  transform: scale(0.98);
}

.modal-slide-enter-active {
  transition: all 0.25s ease-out;
}
.modal-slide-leave-active {
  transition: all 0.2s ease-in;
}
.modal-slide-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.97);
}
.modal-slide-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.98);
}

@media (max-width: 768px) {
  .cover-banner { height: 200px; }
  .profile-on-banner { left: 20px; }
  .info-bar-inner { flex-direction: column; height: auto; padding: 12px 16px; gap: 12px; }
  .tab-nav { overflow-x: auto; }
  .tab-search { display: none; }
  .profile-stats { gap: 12px; }
  .profile-stat { padding: 4px 10px; }
  .profile-stat .num { font-size: 16px; }
  .content-area { padding: 16px; }
  .content-grid { grid-template-columns: repeat(2, 1fr); }
  .empty-actions { flex-direction: column; width: 100%; max-width: 280px; }
  .guide-btn { justify-content: center; }
}
</style>