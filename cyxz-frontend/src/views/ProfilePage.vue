<template>
  <div class="profile-page">
    <!-- ===== 顶部：Banner ===== -->
    <div class="cover-banner"></div>

    <!-- ===== 用户信息主卡 ===== -->
    <div class="profile-card-wrap" v-if="profile">
      <div class="profile-card">
        <div class="pc-top">
          <div class="pc-left">
            <div class="avatar-wrapper" @click="isSelf && startEdit()">
              <div class="profile-avatar">
                <img v-if="profile.avatar" :src="profile.avatar" alt="avatar" class="profile-avatar-img" />
                <span v-else>{{ (profile.nickname || 'U').charAt(0) }}</span>
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
            <template v-if="isSelf">
              <button class="pc-action-btn pc-edit-btn" @click="startEdit">编辑资料</button>
            </template>
            <template v-else>
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
          <input :placeholder="activeTab === 'works' ? '搜索作品...' : '搜索收藏...'"></input>
        </div>
      </div>

      <!-- 作品 tab -->
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
        <EmptyState v-else-if="!postLoading" title="还没有发布任何作品" :hint="isSelf ? '快去发布你的第一篇帖子吧~' : ''">
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
        <EmptyState v-else-if="!favoriteLoading" title="还没有收藏任何内容" hint="发现喜欢的帖子就收藏起来吧~" />
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
import { getUserProfile, updateUserProfile } from '@/api/user'
import type { UserInfo } from '@/api/user'
import { uploadAvatar } from '@/api/upload'
import { getUserPostsByTarget, getUserFavorites } from '@/api/post'
import type { PostVO } from '@/api/post'
import { usePostStats } from '@/composables/usePostStats'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'
import { useFollow } from '@/composables/useFollow'
import { formatDate } from '@/utils/format'
import PostCard from '@/components/PostCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import FollowButton from '@/components/FollowButton.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { requireLogin } = useAuth()
const { following, followLoading, checkFollowing, toggleFollow: doFollow } = useFollow()
const { stats: postStats, loadUserStats: loadPostStats } = usePostStats()

const profile = ref<UserInfo | null>(null)
const loading = ref(true)
const showEdit = ref(false)
const saving = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement>()
const activeTab = ref('works')

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
.avatar-wrapper:hover .avatar-edit-badge {
  transform: translate(50%, -50%) scale(1.15);
  opacity: 1;
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
  box-shadow: 0 4px 16px rgba(255,107,157,0.25);
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
  border: 1px solid rgba(255,107,157,0.1);
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
  background: rgba(255,107,157,0.06);
}
.tab-nav a.active {
  color: #ff5f9a;
  font-weight: 700;
  background: white;
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
  border: 1px solid rgba(255,107,157,0.18);
  border-radius: 999px;
  padding: 8px 16px;
  box-shadow: inset 0 1px 3px rgba(255,107,157,0.06);
  transition: background 0.2s, box-shadow 0.2s, border-color 0.2s;
}
.tab-search:focus-within {
  background: white;
  border-color: rgba(255,107,157,0.32);
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
.tab-search input::placeholder { color: #bbb; }
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
  background: rgba(180,132,255,0.08);
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
  border: 1px solid rgba(255,107,157,0.08);
  box-shadow: 0 2px 12px rgba(180,132,255,0.07);
  transition: all 0.25s ease;
}
.content-grid :deep(.card:hover) {
  transform: translateY(-3px);
  box-shadow: 0 8px 28px rgba(255,107,157,0.14);
  border-color: rgba(255,107,157,0.18);
}
.content-grid :deep(.card-cover) {
  height: 146px;
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
  box-shadow: 0 4px 20px rgba(255,107,157,0.3);
}
.guide-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 28px rgba(255,107,157,0.4);
}
.btn-icon { width: 16px; height: 16px; margin-right: 6px; filter: brightness(0) invert(1); }
.loading-placeholder {
  text-align: center;
  padding: 60px 0;
  color: var(--text-dim);
  font-size: 14px;
}

/* ===== 编辑资料弹窗（保持不变） ===== */
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
.avatar-preview:hover .avatar-upload-mask { opacity: 1; }
.avatar-preview:hover .avatar-upload-camera {
  animation: cameraPop 0.4s ease-out;
}
@keyframes cameraPop {
  0% { transform: scale(0.3); opacity: 0; }
  60% { transform: scale(1.3); }
  100% { transform: scale(1); opacity: 1; }
}
.avatar-upload-hint { font-size: 12px; color: #999; }

.edit-overlay {
  position: fixed;
  inset: 0;
  z-index: 300;
  background: rgba(180,132,255,0.15);
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
    0 8px 30px rgba(180,132,255,0.18),
    0 2px 8px rgba(255,138,200,0.08);
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
.deco-tl { top: 12px; left: 16px; }
.deco-tl::before { content: '✦'; }
.deco-br { bottom: 12px; right: 16px; }
.deco-br::before { content: '♡'; }

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
.edit-modal :deep(.el-form-item) { margin-bottom: 20px; }
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
.edit-modal :deep(.el-input__wrapper:hover) { border-color: rgba(255,138,200,0.45); }
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
.edit-modal :deep(.el-textarea__inner:hover) { border-color: rgba(255,138,200,0.45); }
.edit-modal :deep(.el-textarea__inner:focus) {
  border-color: #B484FF;
  box-shadow: 0 0 0 3px rgba(180,132,255,0.12);
}
.gender-item { margin-bottom: 24px !important; }
.gender-group { display: flex; gap: 24px; }
.birthday-picker { width: 100%; }
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
.edit-modal :deep(.el-radio__input) { display: none; }
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
.edit-save-btn:active { transform: scale(0.98); }

.modal-slide-enter-active { transition: all 0.25s ease-out; }
.modal-slide-leave-active { transition: all 0.2s ease-in; }
.modal-slide-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.97);
}
.modal-slide-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.98);
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