<template>
  <div class="user-center">
    <div class="user-center-inner">
    <!-- 左侧导航 -->
    <aside class="sidebar">
      <nav class="sidebar-nav">
        <a
          v-for="tab in tabs"
          :key="tab.key"
          class="nav-item"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          <Icon v-if="tab.key === 'home'" icon="ph:house" class="nav-icon pink-icon" />
          <Icon v-else-if="tab.key === 'avatar'" icon="ph:camera" class="nav-icon pink-icon" />
          <Icon v-else icon="ph:pencil-simple-line" class="nav-icon pink-icon" />
          <span>{{ tab.label }}</span>
        </a>
      </nav>
    </aside>

    <!-- 右侧主内容 -->
    <main class="main-content">
      <!-- 首页 -->
      <template v-if="activeTab === 'home'">
        <!-- 个人卡片 -->
        <div class="profile-hero">
          <div class="hero-avatar">
            <img v-if="profile.avatar" :src="profile.avatar" alt="" class="hero-avatar-img" />
            <span v-else class="hero-avatar-text">{{ (profile.nickname || 'U').charAt(0) }}</span>
          </div>
          <div class="hero-info">
            <h2>{{ profile.nickname }}</h2>
            <p>{{ profile.bio || '还没有个性签名，去「我的信息」完善吧~' }}</p>
          </div>
        </div>

        <!-- 隐私设置 -->
        <div class="content-card">
          <h3 class="section-title">隐私设置</h3>
          <div class="settings-grid">
            <div class="setting-item" v-for="item in privacySettings" :key="item.key">
              <span class="setting-label">{{ item.label }}</span>
              <el-switch v-model="item.value" />
            </div>
          </div>
        </div>

        <!-- 通知设置 -->
        <div class="content-card">
          <h3 class="section-title">通知设置</h3>
          <div class="settings-grid">
            <div class="setting-item" v-for="item in notificationSettings" :key="item.key">
              <span class="setting-label">{{ item.label }}</span>
              <el-switch v-model="item.value" />
            </div>
          </div>
        </div>

        <!-- 账号安全 -->
        <div class="content-card">
          <h3 class="section-title">账号安全</h3>
          <div class="settings-list">
            <div class="setting-row" v-for="item in accountItems" :key="item.label">
              <Icon :icon="item.icon" class="setting-row-icon pink-icon" />
              <span class="setting-row-label">{{ item.label }}</span>
              <span v-if="item.value" class="setting-row-value">{{ item.value }}</span>
              <span v-else class="setting-row-arrow">›</span>
            </div>
          </div>
        </div>
      </template>

      <!-- 我的头像 -->
      <template v-if="activeTab === 'avatar'">
        <div class="content-card">
          <h3 class="section-title">当前头像</h3>
          <div class="current-avatar">
            <div class="avatar-preview-lg">
              <img v-if="profile.avatar" :src="profile.avatar" alt="" />
              <span v-else class="avatar-placeholder-lg">{{ (profile.nickname || 'U').charAt(0) }}</span>
            </div>
            <div class="avatar-actions">
              <button class="upload-btn" @click="triggerUpload">
                <Icon icon="ph:upload-simple" class="upload-icon" />
                上传新头像
              </button>
              <p class="upload-hint">支持 PNG / JPG / GIF，最大 10MB</p>
            </div>
            <input ref="fileInput" type="file" accept="image/png,image/jpeg,image/gif" style="display:none" @change="handleAvatarChange" />
          </div>
        </div>

        <div v-if="avatarHistory.length > 0" class="content-card">
          <h3 class="section-title">历史头像</h3>
          <div class="history-grid">
            <div
              v-for="(url, idx) in avatarHistory"
              :key="idx"
              class="history-avatar"
              :class="{ current: url === profile.avatar }"
              @click="setAvatar(url)"
            >
              <img :src="url" alt="" />
              <button class="history-delete" v-if="url !== profile.avatar" @click.stop="deleteHistory(url, idx)">×</button>
            </div>
          </div>
        </div>
        <div v-else class="content-card">
          <p class="empty-hint">还没有历史头像，上传第一张吧~</p>
        </div>
      </template>

      <!-- 我的信息 -->
      <template v-if="activeTab === 'info'">
        <div class="content-card">
          <h3 class="section-title">基础信息</h3>
          <div class="info-form">
            <div class="form-item">
              <label class="form-label">昵称</label>
              <div class="input-with-count">
                <input v-model="infoForm.nickname" type="text" class="form-input" maxlength="7" placeholder="输入你的昵称" />
                <span class="char-count">{{ infoForm.nickname.length }}/7</span>
              </div>
            </div>
            <div class="form-item">
              <label class="form-label">个性签名</label>
              <div class="input-with-count">
                <textarea v-model="infoForm.bio" class="form-textarea" maxlength="50" rows="3" placeholder="介绍一下自己吧~"></textarea>
                <span class="char-count">{{ infoForm.bio.length }}/50</span>
              </div>
            </div>
            <div class="form-row">
              <div class="form-item form-item-half">
                <label class="form-label">生日</label>
                <el-date-picker v-model="infoForm.birthday" type="date" placeholder="选择生日" class="birthday-picker" />
              </div>
              <div class="form-item form-item-half">
                <label class="form-label">性别</label>
                <el-radio-group v-model="infoForm.gender" class="gender-group">
                  <el-radio :value="0">保密</el-radio>
                  <el-radio :value="1">男</el-radio>
                  <el-radio :value="2">女</el-radio>
                </el-radio-group>
              </div>
            </div>
            <div class="form-actions">
              <el-button class="save-btn" type="primary" @click="saveInfo" :loading="saving">保存修改</el-button>
            </div>
          </div>
        </div>
      </template>
    </main>
    </div>

    <ImageCropper
      ref="avatarCropperRef"
      :visible="showAvatarCropper"
      title="裁剪头像"
      :aspect-ratio="1"
      :circular="true"
      @crop="onAvatarCrop"
      @cancel="showAvatarCropper = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'
import { getMyProfile, updateUserProfile } from '@/api/user'
import type { UserInfo } from '@/api/user'
import { uploadAvatar, getAvatarHistory, deleteUploadedFile } from '@/api/upload'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/format'
import ImageCropper from '@/components/ImageCropper.vue'

const userStore = useUserStore()
const route = useRoute()
const saving = ref(false)
const fileInput = ref<HTMLInputElement>()
const avatarCropperRef = ref<InstanceType<typeof ImageCropper> | null>(null)
const showAvatarCropper = ref(false)
const avatarHistory = ref<string[]>([])

const activeTab = ref('home')

const tabs = [
  { key: 'home', label: '首页' },
  { key: 'avatar', label: '我的头像' },
  { key: 'info', label: '我的信息' },
]

const profile = reactive<UserInfo>({
  userId: '',
  nickname: '',
  avatar: '',
  bio: '',
  gender: 0,
  birthday: '',
})

const infoForm = reactive({
  nickname: '',
  bio: '',
  gender: 0,
  birthday: '',
})

const privacySettings = reactive([
  { key: 'publicFavorites', label: '公开我的收藏', value: false },
  { key: 'publicBirthday', label: '公开我的生日', value: true },
  { key: 'publicFollowing', label: '公开我的关注列表', value: true },
  { key: 'publicFollowers', label: '公开我的粉丝列表', value: true },
  { key: 'allowMention', label: '允许他人@我', value: true },
  { key: 'allowPrivateMessage', label: '允许他人私信我', value: true },
])

const notificationSettings = reactive([
  { key: 'notificationLike', label: '点赞通知', value: true },
  { key: 'notificationComment', label: '评论通知', value: true },
  { key: 'notificationFavorite', label: '收藏通知', value: true },
  { key: 'notificationFollow', label: '关注通知', value: true },
])

const accountItems = [
  { icon: 'ph:lock-key', label: '修改密码' },
  { icon: 'ph:device-mobile', label: '绑定手机号' },
  { icon: 'ph:envelope', label: '绑定邮箱' },
  { icon: 'ph:info', label: '关于次元小站', value: 'v1.0.0' },
]

onMounted(async () => {
  const tab = route.query.tab as string
  if (tab && tabs.some(t => t.key === tab)) {
    activeTab.value = tab
  }

  const userId = userStore.userInfo?.id
  if (!userId) return
  try {
    const data = await getMyProfile()
    Object.assign(profile, data)
    syncInfoForm()
  } catch {
    ElMessage.error('加载用户信息失败')
  }
})

watch(activeTab, (tab) => {
  if (tab === 'avatar' && avatarHistory.value.length === 0) {
    loadHistory()
  }
})

function syncInfoForm() {
  infoForm.nickname = profile.nickname
  infoForm.bio = profile.bio || ''
  infoForm.gender = profile.gender ?? 0
  infoForm.birthday = profile.birthday || ''
}

// ========== 我的头像 ==========
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
  avatarCropperRef.value?.loadImage(file)
  showAvatarCropper.value = true
  input.value = ''
}

async function onAvatarCrop(blob: Blob) {
  showAvatarCropper.value = false
  try {
    const file = new File([blob], 'avatar.jpg', { type: 'image/jpeg' })
    const data = await uploadAvatar(file)
    const url = typeof data === 'string' ? data : data?.url
    if (url) {
      profile.avatar = url
      if (userStore.userInfo) {
        userStore.setUserInfo({ ...userStore.userInfo, avatar: url } as any)
      }
      ElMessage.success('头像更换成功')
      loadHistory()
    }
  } catch {
    ElMessage.error('头像上传失败')
  }
}

function setAvatar(url: string) {
  if (url === profile.avatar) return
  profile.avatar = url
  updateUserProfile({ avatar: url }).then(() => {
    if (userStore.userInfo) {
      userStore.setUserInfo({ ...userStore.userInfo, avatar: url } as any)
    }
    ElMessage.success('头像已更换')
    loadHistory()
  }).catch(() => {
    ElMessage.error('更换失败')
  })
}

async function loadHistory() {
  try {
    const data = await getAvatarHistory()
    avatarHistory.value = Array.isArray(data) ? [...data].reverse() : []
  } catch { /* 静默失败 */ }
}

async function deleteHistory(url: string, idx: number) {
  try {
    await deleteUploadedFile(url)
    avatarHistory.value.splice(idx, 1)
  } catch {
    ElMessage.error('删除失败')
  }
}

// ========== 我的信息 ==========
async function saveInfo() {
  saving.value = true
  try {
    const payload = {
      ...infoForm,
      birthday: infoForm.birthday ? formatDate(infoForm.birthday) : '',
    }
    await updateUserProfile(payload)
    Object.assign(profile, {
      nickname: infoForm.nickname,
      bio: infoForm.bio,
      gender: infoForm.gender,
      birthday: infoForm.birthday,
    })
    if (userStore.userInfo) {
      userStore.setUserInfo({ ...userStore.userInfo, nickname: infoForm.nickname, avatar: profile.avatar } as any)
    }
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.user-center {
  display: flex;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(180deg, var(--bg-soft) 0%, var(--bg) 100%);
}

.user-center-inner {
  display: flex;
  width: 100%;
  max-width: 1220px;
  padding-top: 90px;
}

/* ===== 左侧导航 ===== */
.sidebar {
  width: 200px;
  background: var(--card);
  border-right: 1.5px solid var(--border);
  padding: 20px 0;
  flex-shrink: 0;
  border-radius: 16px 0 0 16px;
  overflow-y: auto;
}

.sidebar-nav {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  text-decoration: none;
  transition: all 0.22s ease-out;
}

.nav-item:hover {
  background: var(--pink-bg);
  color: var(--pink);
}

.nav-item.active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(180, 132, 255, 0.1));
  color: var(--pink);
  font-weight: 600;
}

.nav-item svg {
  flex-shrink: 0;
  opacity: 0.7;
}

.nav-item.active svg {
  opacity: 1;
}

/* ===== 主内容区 ===== */
.main-content {
  flex: 1;
  min-width: 0;
  background: var(--card);
  padding: 28px 32px 40px;
  border-radius: 0 16px 16px 0;
  border: 1.5px solid var(--border);
  border-left: none;
}

/* ===== 个人卡片 Hero ===== */
.profile-hero {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 28px;
  padding: 32px;
  background: var(--card);
  border-radius: 16px;
  border: 1.5px solid var(--border);
  box-shadow: var(--shadow);
}

.hero-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  border: 4px solid var(--card);
  box-shadow: var(--shadow);
}

.hero-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-avatar-text {
  color: var(--white);
  font-size: 32px;
  font-weight: 800;
}

.hero-info h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

.hero-info p {
  font-size: 14px;
  color: var(--text-dim);
  margin: 6px 0 0;
}

/* ===== 内容卡片 ===== */
.content-card {
  background: var(--card);
  border-radius: 16px;
  padding: 28px 32px;
  border: 1.5px solid var(--border);
  box-shadow: 0 2px 12px rgba(180, 132, 255, 0.05);
  margin-bottom: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 20px;
  padding-bottom: 14px;
  border-bottom: 1.5px solid var(--border);
}

/* ===== 设置网格 ===== */
.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px 32px;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
}

.setting-label {
  font-size: 15px;
  color: var(--text-secondary);
}

.settings-list {
  display: flex;
  flex-direction: column;
}

.setting-row {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-radius: 10px;
}

.setting-row:hover { background: var(--pink-bg); }

.setting-row-icon {
  width: 20px;
  height: 20px;
  margin-right: 14px;
  opacity: 0.7;
}

.setting-row-label {
  flex: 1;
  font-size: 15px;
  color: var(--text-secondary);
}

.setting-row-arrow {
  font-size: 20px;
  color: var(--text-dim);
}

.setting-row-value {
  font-size: 14px;
  color: var(--text-dim);
}

/* ===== 我的头像 ===== */
.current-avatar {
  display: flex;
  align-items: center;
  gap: 28px;
}

.avatar-preview-lg {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  border: 4px solid var(--card);
  box-shadow: var(--shadow);
}

.avatar-preview-lg img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder-lg {
  color: var(--white);
  font-size: 48px;
  font-weight: 800;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: var(--white);
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease;
  box-shadow: var(--shadow);
  width: fit-content;
}

.upload-icon {
  width: 18px;
  height: 18px;
  color: var(--white);
}

.upload-btn:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-lg);
}

.upload-hint {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0;
}

/* ===== 历史头像 ===== */
.history-grid {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.history-avatar {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  cursor: pointer;
  border: 3px solid transparent;
  transition: all 0.2s;
  opacity: 0.65;
}

.history-avatar:hover { opacity: 1; border-color: rgba(255, 107, 157, 0.35); }

.history-avatar.current {
  opacity: 1;
  border-color: var(--pink);
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.18);
}

.history-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.history-delete {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid var(--white);
  background: var(--overlay);
  color: var(--white);
  font-size: 12px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s;
  z-index: 2;
}

.history-avatar:hover .history-delete { opacity: 1; }
.history-delete:hover { background: var(--error); }

.empty-hint {
  text-align: center;
  color: var(--text-dim);
  font-size: 14px;
  padding: 24px 0;
  margin: 0;
}

/* ===== 我的信息 ===== */
.info-form {
  display: flex;
  flex-direction: column;
}

.form-item {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-dim);
  margin-bottom: 8px;
}

.input-with-count {
  position: relative;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1.5px solid var(--border);
  font-size: 15px;
  color: var(--text);
  outline: none;
  background: var(--card);
  transition: all 0.22s ease;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: var(--pink);
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.1);
}

.form-textarea {
  width: 100%;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1.5px solid var(--border);
  font-size: 15px;
  color: var(--text);
  outline: none;
  background: var(--card);
  resize: vertical;
  min-height: 90px;
  font-family: inherit;
  transition: all 0.22s ease;
  box-sizing: border-box;
}

.form-textarea:focus {
  border-color: var(--pink);
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.1);
}

.char-count {
  position: absolute;
  right: 4px;
  bottom: -20px;
  font-size: 12px;
  color: var(--text-dim);
}

.form-row {
  display: flex;
  gap: 32px;
}

.form-item-half {
  flex: 1;
  min-width: 0;
}

.birthday-picker {
  width: 100%;
}

.gender-group {
  display: flex;
  gap: 20px;
  padding-top: 4px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
  border-top: 1.5px solid var(--border);
  margin-top: 4px;
}

.save-btn {
  border-radius: 12px;
  font-weight: 700;
  font-size: 15px;
  padding: 12px 40px;
  background: linear-gradient(135deg, #FF8AC8, #B484FF) !important;
  border: none !important;
  box-shadow: 0 4px 16px rgba(255, 138, 200, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transition: all 0.22s ease;
}

.save-btn:hover {
  box-shadow: 0 6px 24px rgba(180, 132, 255, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transform: scale(1.03);
}

/* ===== Element Plus 覆盖 ===== */
:deep(.el-radio) {
  margin-right: 0;
  padding: 8px 24px;
  border-radius: 10px;
  border: 1.5px solid var(--border);
  transition: all 0.22s ease;
  height: auto;
}

:deep(.el-radio:hover) {
  border-color: var(--border);
  background: var(--pink-bg);
}

:deep(.el-radio__input) { display: none; }

:deep(.el-radio__label) {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-dim);
  padding-left: 0 !important;
}

:deep(.el-radio.is-checked) {
  border-color: var(--pink);
  background: var(--pink-bg);
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.08);
}

:deep(.el-radio.is-checked .el-radio__label) {
  background: linear-gradient(135deg, #FF8AC8, #B484FF);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 700;
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .sidebar {
    width: 60px;
  }

  .sidebar-nav { padding: 10px 8px; }

  .nav-item {
    justify-content: center;
    padding: 10px;
  }

  .nav-item span { display: none; }

  .settings-grid { grid-template-columns: 1fr; }

  .form-row { flex-direction: column; gap: 0; }
}

@media (max-width: 768px) {
  .sidebar { display: none; }

  .main-content {
    border-radius: 16px;
    border-left: 1.5px solid var(--border);
  }

  .profile-hero {
    flex-direction: column;
    text-align: center;
    padding: 24px;
  }

  .settings-grid { grid-template-columns: 1fr; }

  .form-row { flex-direction: column; gap: 0; }
}
</style>
