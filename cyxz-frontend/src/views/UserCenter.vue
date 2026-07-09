<template>
  <div class="user-center">
    <!-- Cover Banner (全宽) -->
    <div class="cover-banner">
      <span class="cover-deco star-1"></span>
      <span class="cover-deco star-2">⭐</span>
      <span class="cover-deco star-3"></span>
      <span class="cover-deco star-4"></span>
      <span class="cover-deco star-5">✨</span>

      <!-- Profile info (和内容区左对齐) -->
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
      </div>
    </div>

    <!-- Tab Bar + Stats (同一行) -->
    <div class="info-bar">
      <div class="info-bar-inner">
        <div class="tab-nav">
          <a href="#" class="active" @click.prevent>🏠 主页</a>
          <a href="#" @click.prevent> 动态</a>
          <a href="#" @click.prevent>📝 投稿</a>
          <a href="#" @click.prevent> 合集和系列</a>
          <a href="#" @click.prevent>⭐ 收藏</a>
          <a href="#" @click.prevent>❤️ 追番追剧</a>
        </div>
        <div class="info-bar-right">
          <div class="tab-search">
            <span class="icon">🔍</span>
            <input type="text" placeholder="搜索视频、动态" />
          </div>
          <div class="profile-stats">
            <div class="profile-stat">
              <div class="num">89</div>
              <div class="label">关注</div>
            </div>
            <div class="profile-stat">
              <div class="num">256</div>
              <div class="label">粉丝</div>
            </div>
            <div class="profile-stat">
              <div class="num">1.2k</div>
              <div class="label">获赞</div>
            </div>
            <div class="profile-stat">
              <div class="num">3.4k</div>
              <div class="label">浏览</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Content Area -->
    <div class="content-area">
      <!-- Content Grid -->
      <div class="content-grid" v-if="contents.length">
        <div class="content-card" v-for="item in contents" :key="item.id">
          <div class="card-cover">{{ item.emoji }}</div>
          <div class="card-body">
            <div class="card-title">{{ item.title }}</div>
            <div class="card-meta">
              <span>❤️ {{ item.likes }}</span>
              <span>💬 {{ item.comments }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State with Guide Buttons -->
      <div v-else-if="!loading" class="empty-state">
        <p class="empty-text">这里还没有任何内容</p>
        <p class="empty-hint">发布你的第一条动态，让大家认识你吧~</p>
        <div class="empty-actions" v-if="isSelf">
          <button class="guide-btn guide-btn-primary">
            <span>✏️</span> 投稿作品
          </button>
        </div>
      </div>

      <div v-else class="loading-placeholder">加载中...</div>
    </div>

    <!-- Edit Modal -->
    <Teleport to="body">
      <Transition name="modal-slide">
        <div v-if="showEdit" class="edit-overlay" @click.self="cancelEdit">
          <div class="edit-modal">
            <!-- Corner decorations -->
            <span class="modal-deco deco-tl"></span>
            <span class="modal-deco deco-br"></span>

            <button class="edit-close" @click="cancelEdit">✕</button>
            <h3 class="edit-title">编辑资料</h3>
            <div class="edit-divider"></div>

            <!-- Avatar Upload -->
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
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserProfile, updateUserProfile } from '@/api/user'
import type { UserInfo } from '@/api/user'
import { uploadAvatar } from '@/api/upload'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const profile = ref<UserInfo | null>(null)
const loading = ref(true)
const showEdit = ref(false)
const saving = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement>()

const editForm = reactive({
  nickname: '',
  bio: '',
  gender: 0,
  avatar: '',
  birthday: '',
})

// 示例内容（后续对接真实接口）
const contents = ref<any[]>([])

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
  } catch {
    ElMessage.error('加载用户信息失败')
  } finally {
    loading.value = false
  }
})

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
</script>

<style scoped>
.user-center {
  padding-bottom: 60px;
}

/* ===== Cover Banner (全宽) ===== */
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

/* Decorative elements with animations */
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

/* Profile info on banner - 和内容区左对齐 */
.profile-on-banner {
  position: absolute;
  bottom: 20px;
  left: calc((100vw - 1500px) / 2 + 32px);
  display: flex;
  align-items: flex-end;
  gap: 20px;
  z-index: 10;
}

/* Avatar with edit badge */
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

/* ===== Info Bar (Tab + Stats 同一行) ===== */
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
.tab-search .icon { color: #bbb; font-size: 14px; }

/* Stats */
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

/* ===== Content Area ===== */
.content-area {
  max-width: 1500px;
  margin: 0 auto;
  padding: 24px 32px 60px;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.content-card {
  background: white;
  border-radius: var(--radius);
  border: 1.5px solid var(--border);
  box-shadow: var(--shadow);
  overflow: hidden;
  transition: all 0.2s;
  cursor: pointer;
}
.content-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 30px rgba(192,132,252,0.15);
}
.card-cover {
  width: 100%;
  aspect-ratio: 16/10;
  background: linear-gradient(135deg, #fce4ec, #e8eaf6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
}
.card-body { padding: 12px 14px; }
.card-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-meta { font-size: 11px; color: var(--text-dim); display: flex; gap: 12px; }

/* Empty State */
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
.guide-btn-secondary {
  background: white;
  color: var(--pink);
  border: 1.5px solid var(--pink) !important;
  box-shadow: 0 2px 12px rgba(255,107,157,0.1);
}
.guide-btn-secondary:hover {
  background: rgba(255,107,157,0.05);
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(255,107,157,0.15);
}

.loading-placeholder {
  text-align: center;
  padding: 60px 0;
  color: var(--text-dim);
  font-size: 14px;
}

/* Avatar upload */
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

/* Edit Modal */
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

/* Corner decorations */
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

/* Title */
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

/* Close button */
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

/* Form items */
.edit-modal :deep(.el-form-item) {
  margin-bottom: 20px;
}
.edit-modal :deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: 600;
  color: #555;
  padding-bottom: 8px;
}

/* Input fields */
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
.edit-modal :deep(.el-input__inner) {
  color: #333;
}
.edit-modal :deep(.el-input__inner::placeholder) {
  color: #BB99CC;
}

/* Textarea */
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
.edit-modal :deep(.el-textarea__inner::placeholder) {
  color: #BB99CC;
}

/* Gender radio group */
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
.edit-modal :deep(.el-radio:hover .el-radio__label) {
  color: #B484FF;
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

/* Action buttons */
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

/* Modal slide animation */
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
