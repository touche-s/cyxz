<template>
  <div class="user-center-page">
    <div class="uc-shell">
      <aside class="uc-sidebar">
        <div class="sidebar-user">
          <div class="sidebar-avatar" @click="activeTab = 'avatar'">
            <img v-if="profile.avatar" :src="profile.avatar" alt="" />
            <span v-else class="sidebar-avatar-fallback">{{ (profile.nickname || 'U').charAt(0) }}</span>
          </div>
          <div class="sidebar-user-text">
            <span class="sidebar-nick">{{ profile.nickname || '未设置昵称' }}</span>
            <span class="sidebar-bio">{{ profile.bio || '还没有个性签名' }}</span>
          </div>
        </div>

        <nav class="sidebar-nav">
          <div class="nav-group">
            <span class="nav-group-label">账户资料</span>
            <a
              class="nav-link"
              :class="{ active: activeTab === 'home' }"
              @click="activeTab = 'home'"
            >
              <Icon icon="ph:house" />
              <span>首页</span>
            </a>
            <a
              class="nav-link"
              :class="{ active: activeTab === 'info' }"
              @click="activeTab = 'info'"
            >
              <Icon icon="ph:pencil-simple-line" />
              <span>我的信息</span>
            </a>
            <a
              class="nav-link"
              :class="{ active: activeTab === 'avatar' }"
              @click="activeTab = 'avatar'"
            >
              <Icon icon="ph:camera" />
              <span>我的头像</span>
            </a>
          </div>

          <div class="nav-group">
            <span class="nav-group-label">偏好设置</span>
            <a
              class="nav-link"
              :class="{ active: activeTab === 'privacy' }"
              @click="activeTab = 'privacy'"
            >
              <Icon icon="ph:shield-check" />
              <span>隐私设置</span>
            </a>
          </div>
        </nav>

        <div class="sidebar-footer">
          <router-link to="/" class="sidebar-footer-link">
            <Icon icon="ph:arrow-left" />
            <span>返回首页</span>
          </router-link>
        </div>
      </aside>

      <main class="uc-main">
        <div class="page-hdr">
          <div class="page-hdr-left">
            <h1 class="page-hdr-title">{{ currentPageTitle }}</h1>
            <p class="page-hdr-sub">{{ currentPageSub }}</p>
          </div>
        </div>

        <div class="mobile-tabs">
          <button
            v-for="tab in allTabs"
            :key="tab.key"
            class="mobile-tab-btn"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            <Icon :icon="tab.icon" class="mobile-tab-icon" />
            {{ tab.label }}
          </button>
        </div>

        <template v-if="activeTab === 'home'">
          <div class="card profile-summary">
            <div class="summary-avatar" @click="activeTab = 'avatar'">
              <img v-if="profile.avatar" :src="profile.avatar" alt="" />
              <span v-else class="summary-avatar-fb">{{ (profile.nickname || 'U').charAt(0) }}</span>
            </div>
            <div class="summary-info">
              <h2>{{ profile.nickname || '未设置昵称' }}</h2>
              <p>{{ profile.bio || '还没有个性签名，介绍一下自己吧~' }}</p>
            </div>
            <button class="summary-edit" @click="activeTab = 'info'">
              <Icon icon="ph:pencil-simple" />
              编辑资料
            </button>
          </div>

          <div class="card">
            <div class="card-hd">
              <Icon icon="ph:shield-check" class="card-hd-icon" />
              <span>隐私设置</span>
            </div>
            <div class="setting-rows">
              <div class="setting-row" v-for="item in privacySettings" :key="item.key">
                <div class="setting-row-left">
                  <span class="setting-row-name">{{ item.label }}</span>
                  <span class="setting-row-desc">{{ privacyDesc[item.key] }}</span>
                </div>
                <el-switch v-model="item.value" class="uc-switch" />
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-hd">
              <Icon icon="ph:bell" class="card-hd-icon" />
              <span>通知设置</span>
            </div>
            <div class="setting-rows">
              <div class="setting-row" v-for="item in notificationSettings" :key="item.key">
                <div class="setting-row-left">
                  <span class="setting-row-name">{{ item.label }}</span>
                  <span class="setting-row-desc">{{ notifDesc[item.key] }}</span>
                </div>
                <el-switch v-model="item.value" class="uc-switch" />
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-hd">
              <Icon icon="ph:lock-key" class="card-hd-icon" />
              <span>账号安全</span>
            </div>
            <div class="setting-rows">
              <div class="setting-row" v-for="item in accountItems" :key="item.label" @click="item.action?.()" :class="{ 'setting-row-clickable': item.action }">
                <div class="setting-row-left">
                  <Icon :icon="item.icon" class="setting-row-icon" />
                  <span class="setting-row-name">{{ item.label }}</span>
                </div>
                <span v-if="item.value" class="setting-row-val">{{ item.value }}</span>
                <span v-else-if="item.pending" class="setting-row-pending">即将支持</span>
                <span v-else class="setting-row-arrow">
                  <Icon icon="ph:caret-right" />
                </span>
              </div>
            </div>
          </div>
        </template>

        <template v-if="activeTab === 'info'">
          <div class="card">
            <div class="card-hd">
              <Icon icon="ph:identification-card" class="card-hd-icon" />
              <span>基本资料</span>
            </div>
            <div class="info-grid">
              <div class="form-field">
                <label class="ff-label">昵称</label>
                <div class="ff-input-wrap">
                  <input v-model="infoForm.nickname" type="text" class="ff-input" maxlength="7" placeholder="输入你的昵称" />
                  <span class="ff-count">{{ infoForm.nickname.length }}/7</span>
                </div>
              </div>
              <div class="form-field">
                <label class="ff-label">生日</label>
                <el-date-picker v-model="infoForm.birthday" type="date" placeholder="选择生日" class="ff-date" />
              </div>
              <div class="form-field form-field-full">
                <label class="ff-label">个性签名</label>
                <div class="ff-input-wrap">
                  <textarea v-model="infoForm.bio" class="ff-input ff-textarea" maxlength="50" rows="3" placeholder="介绍一下自己吧~"></textarea>
                  <span class="ff-count">{{ infoForm.bio.length }}/50</span>
                </div>
              </div>
              <div class="form-field form-field-full">
                <label class="ff-label">性别</label>
                <div class="gender-seg">
                  <button
                    v-for="opt in genderOptions"
                    :key="opt.value"
                    class="gender-seg-btn"
                    :class="{ active: infoForm.gender === opt.value }"
                    @click="infoForm.gender = opt.value"
                  >
                    {{ opt.label }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="form-sticky">
            <span class="form-sticky-hint">修改将立即同步至主页</span>
            <button class="btn-save" @click="saveInfo" :disabled="saving">
              <Icon icon="ph:check" v-if="!saving" />
              <Icon icon="ph:spinner" class="spin" v-else />
              {{ saving ? '保存中...' : '保存修改' }}
            </button>
          </div>
        </template>

        <template v-if="activeTab === 'avatar'">
          <div class="card avatar-display">
            <div class="avatar-showcase">
              <div class="avatar-ring">
                <img v-if="profile.avatar" :src="profile.avatar" alt="" />
                <span v-else class="avatar-ring-fb">{{ (profile.nickname || 'U').charAt(0) }}</span>
                <div class="avatar-badge">当前头像</div>
              </div>
              <div class="avatar-show-info">
                <h3>{{ profile.nickname || '未设置昵称' }}</h3>
                <p>点击下方按钮上传新头像，或从历史头像中选择</p>
                <div class="avatar-show-actions">
                  <button class="btn-upload" @click="triggerUpload">
                    <Icon icon="ph:upload-simple" />
                    上传新头像
                  </button>
                  <span class="upload-extra">支持 PNG / JPG / GIF，最大 10MB</span>
                </div>
              </div>
            </div>
            <input ref="fileInput" type="file" accept="image/png,image/jpeg,image/gif" style="display:none" @change="handleAvatarChange" />
          </div>

          <div v-if="avatarHistory.length > 0" class="card">
            <div class="card-hd">
              <Icon icon="ph:clock-counter-clockwise" class="card-hd-icon" />
              <span>历史头像</span>
              <span class="card-hd-extra">点击历史头像即可快速切换</span>
            </div>
            <div class="history-grid-new">
              <div
                v-for="(url, idx) in avatarHistory"
                :key="idx"
                class="history-item"
                :class="{ active: url === profile.avatar }"
                @click="setAvatar(url)"
              >
                <img :src="url" alt="" />
                <div class="history-overlay">
                  <Icon icon="ph:check-circle" v-if="url === profile.avatar" />
                  <span v-else>设为头像</span>
                </div>
                <button
                  v-if="url !== profile.avatar"
                  class="history-item-del"
                  @click.stop="deleteHistory(url, idx)"
                  title="删除"
                >
                  <Icon icon="ph:x" />
                </button>
              </div>
            </div>
          </div>
          <div v-else class="card empty-card">
            <div class="empty-state">
              <Icon icon="ph:image" class="empty-icon" />
              <p>还没有历史头像，上传第一张吧~</p>
            </div>
          </div>
        </template>

        <template v-if="activeTab === 'privacy'">
          <div class="card">
            <div class="card-hd">
              <Icon icon="ph:eye" class="card-hd-icon" />
              <span>主页可见性</span>
            </div>
            <div class="setting-rows">
              <div class="setting-row" v-for="item in visibilitySettings" :key="item.key">
                <div class="setting-row-left">
                  <span class="setting-row-name">{{ item.label }}</span>
                  <span class="setting-row-desc">{{ privacyDesc[item.key] }}</span>
                </div>
                <el-switch v-model="item.value" class="uc-switch" />
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-hd">
              <Icon icon="ph:chat-circle" class="card-hd-icon" />
              <span>社交互动</span>
            </div>
            <div class="setting-rows">
              <div class="setting-row" v-for="item in socialSettings" :key="item.key">
                <div class="setting-row-left">
                  <span class="setting-row-name">{{ item.label }}</span>
                  <span class="setting-row-desc">{{ privacyDesc[item.key] }}</span>
                </div>
                <el-switch v-model="item.value" class="uc-switch" />
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-hd">
              <Icon icon="ph:bell" class="card-hd-icon" />
              <span>通知设置</span>
            </div>
            <div class="setting-rows">
              <div class="setting-row" v-for="item in notificationSettings" :key="item.key">
                <div class="setting-row-left">
                  <span class="setting-row-name">{{ item.label }}</span>
                  <span class="setting-row-desc">{{ notifDesc[item.key] }}</span>
                </div>
                <el-switch v-model="item.value" class="uc-switch" />
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-hd">
              <Icon icon="ph:lock-key" class="card-hd-icon" />
              <span>账号安全</span>
            </div>
            <div class="setting-rows">
              <div class="setting-row" v-for="item in accountItems" :key="item.label">
                <div class="setting-row-left">
                  <Icon :icon="item.icon" class="setting-row-icon" />
                  <span class="setting-row-name">{{ item.label }}</span>
                </div>
                <span v-if="item.value" class="setting-row-val">{{ item.value }}</span>
                <span v-else-if="item.pending" class="setting-row-pending">即将支持</span>
                <span v-else class="setting-row-arrow">
                  <Icon icon="ph:caret-right" />
                </span>
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

    <!-- 修改密码弹窗 -->
    <Teleport to="body">
      <div class="modal-overlay" v-if="showPasswordModal" @click.self="closePasswordModal">
        <div class="modal-card">
          <div class="modal-header">
            <h3>修改密码</h3>
            <button class="modal-close" @click="closePasswordModal">&times;</button>
          </div>
          <div class="modal-body">
            <div class="password-field">
              <label>旧密码</label>
              <input v-model="passwordForm.oldPassword" type="password" class="form-input" placeholder="输入当前密码" />
            </div>
            <div class="password-field">
              <label>新密码</label>
              <input v-model="passwordForm.newPassword" type="password" class="form-input" placeholder="6-20位新密码" maxlength="20" />
            </div>
            <div class="password-field">
              <label>确认新密码</label>
              <input v-model="passwordForm.confirmPassword" type="password" class="form-input" placeholder="再次输入新密码" maxlength="20" />
            </div>
          </div>
          <div class="modal-footer">
            <button class="cancel-btn" @click="closePasswordModal">取消</button>
            <button class="submit-btn" @click="submitPassword" :disabled="passwordSaving">
              {{ passwordSaving ? '保存中...' : '确认修改' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
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

const allTabs = [
  { key: 'home', label: '首页', icon: 'ph:house' },
  { key: 'info', label: '我的信息', icon: 'ph:pencil-simple-line' },
  { key: 'avatar', label: '我的头像', icon: 'ph:camera' },
  { key: 'privacy', label: '隐私设置', icon: 'ph:shield-check' },
]

const genderOptions = [
  { value: 0, label: '保密' },
  { value: 1, label: '男' },
  { value: 2, label: '女' },
]

const pageTitles: Record<string, string> = {
  home: '账户概览',
  info: '我的信息',
  avatar: '我的头像',
  privacy: '隐私设置',
}

const pageSubs: Record<string, string> = {
  home: '管理你的资料、隐私与通知偏好',
  info: '编辑你的基本资料与个人展示信息',
  avatar: '上传和管理你的头像',
  privacy: '控制你的个人信息可见范围与交互权限',
}

const currentPageTitle = computed(() => pageTitles[activeTab.value] || '账户设置')
const currentPageSub = computed(() => pageSubs[activeTab.value] || '')

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

const visibilitySettings = computed(() =>
  privacySettings.filter(s => s.key.startsWith('public'))
)

const socialSettings = computed(() =>
  privacySettings.filter(s => s.key.startsWith('allow'))
)

const privacyDesc: Record<string, string> = {
  publicFavorites: '其他用户可在你的主页查看收藏内容',
  publicBirthday: '在你的个人资料中展示生日信息',
  publicFollowing: '其他用户可查看你关注的人',
  publicFollowers: '其他用户可查看关注你的人',
  allowMention: '在帖子和评论中可以被@提及',
  allowPrivateMessage: '其他用户可以向你发送私信',
}

const notificationSettings = reactive([
  { key: 'notificationLike', label: '点赞通知', value: true },
  { key: 'notificationComment', label: '评论通知', value: true },
  { key: 'notificationFollow', label: '关注通知', value: true },
])

const notifDesc: Record<string, string> = {
  notificationLike: '有人点赞你的帖子时通知',
  notificationComment: '有人评论你的帖子时通知',
  notificationFollow: '有人关注你时通知',
}

const accountItems = [
  { icon: 'ph:lock-key', label: '修改密码', action: openPasswordModal },
  { icon: 'ph:device-mobile', label: '绑定手机号', pending: true },
  { icon: 'ph:envelope', label: '绑定邮箱', pending: true },
  { icon: 'ph:info', label: '关于次元小站', value: 'v1.0.0' },
]

onMounted(async () => {
  const tab = route.query.tab as string
  if (tab && allTabs.some(t => t.key === tab)) {
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

// ===== 修改密码 =====
const showPasswordModal = ref(false)
const passwordSaving = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

function openPasswordModal() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  showPasswordModal.value = true
}

function closePasswordModal() {
  showPasswordModal.value = false
}

async function submitPassword() {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入旧密码')
    return
  }
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少6位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  passwordSaving.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword,
    })
    ElMessage.success('密码修改成功')
    closePasswordModal()
  } catch {
    ElMessage.error('密码修改失败')
  } finally {
    passwordSaving.value = false
  }
}

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
  } catch { /* */ }
}

async function deleteHistory(url: string, idx: number) {
  try {
    await deleteUploadedFile(url)
    avatarHistory.value.splice(idx, 1)
  } catch {
    ElMessage.error('删除失败')
  }
}

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
.user-center-page {
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(170deg, #fff8fc 0%, #fdf4fa 30%, #faf5ff 70%, #fef8ff 100%);
  display: flex;
  justify-content: center;
  padding: calc(var(--header-offset) + 4px) 24px 24px;
}

html.dark .user-center-page {
  background: linear-gradient(170deg, #1a1a30 0%, #1d1a35 30%, #1e1a38 70%, #1c1835 100%);
}

.uc-shell {
  display: flex;
  width: 100%;
  max-width: 1120px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px) saturate(1.6);
  border-radius: 22px;
  border: 1px solid rgba(255, 182, 215, 0.2);
  box-shadow:
    0 4px 40px rgba(255, 107, 157, 0.06),
    0 1px 4px rgba(180, 132, 255, 0.04),
    inset 0 0 0 1px rgba(255, 255, 255, 0.5);
  overflow: hidden;
  height: 100%;
}

html.dark .uc-shell {
  background: rgba(37, 37, 71, 0.75);
  backdrop-filter: blur(24px) saturate(1.6);
  border-color: rgba(255, 182, 215, 0.1);
  box-shadow:
    0 4px 40px rgba(0, 0, 0, 0.3),
    inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}

.uc-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: rgba(255, 246, 251, 0.5);
  border-right: 1px solid rgba(255, 182, 215, 0.15);
  display: flex;
  flex-direction: column;
  padding: 28px 0 20px;
}

html.dark .uc-sidebar {
  background: rgba(30, 28, 48, 0.5);
  border-right-color: rgba(255, 182, 215, 0.08);
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 20px 24px;
  margin-bottom: 4px;
  border-bottom: 1px solid rgba(255, 182, 215, 0.12);
}

.sidebar-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--gradient-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  border: 2px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 10px rgba(255, 107, 157, 0.16);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.sidebar-avatar:hover {
  transform: scale(1.06);
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.24);
}

.sidebar-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sidebar-avatar-fallback {
  color: #fff;
  font-size: 20px;
  font-weight: 800;
}

.sidebar-user-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.sidebar-nick {
  font-size: 14px;
  font-weight: 700;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-bio {
  font-size: 12px;
  color: var(--text-dim);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-nav {
  flex: 1;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 8px;
}

.nav-group-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 8px 14px 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-dim);
  cursor: pointer;
  text-decoration: none;
  transition: all 0.18s ease;
  position: relative;
}

.nav-link:hover {
  background: rgba(255, 107, 157, 0.05);
  color: var(--pink);
}

.nav-link:focus-visible {
  outline: 2px solid var(--pink);
  outline-offset: -2px;
}

.nav-link.active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(180, 132, 255, 0.1));
  color: var(--pink);
  font-weight: 600;
}

.nav-link.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  border-radius: 0 4px 4px 0;
  background: linear-gradient(180deg, var(--pink), var(--purple));
}

.nav-link svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  opacity: 0.65;
}

.nav-link.active svg {
  opacity: 1;
}

.sidebar-footer {
  padding: 16px 20px 0;
  border-top: 1px solid rgba(255, 182, 215, 0.12);
}

.sidebar-footer-link {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-dim);
  text-decoration: none;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.18s ease;
}

.sidebar-footer-link:hover {
  background: rgba(255, 107, 157, 0.05);
  color: var(--pink);
}

.sidebar-footer-link svg {
  width: 16px;
  height: 16px;
}

.uc-main {
  flex: 1;
  min-width: 0;
  padding: 32px 40px 44px;
  overflow-y: auto;
}

.page-hdr {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 28px;
}

.page-hdr-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--text);
  margin: 0 0 4px;
  letter-spacing: -0.3px;
}

.page-hdr-sub {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0;
}

.mobile-tabs {
  display: none;
  gap: 6px;
  margin-bottom: 22px;
  padding: 4px;
  background: rgba(255, 107, 157, 0.04);
  border-radius: 14px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.mobile-tabs::-webkit-scrollbar { display: none; }

.mobile-tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  border-radius: 11px;
  border: none;
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-dim);
  cursor: pointer;
  transition: all 0.18s ease;
  font-family: inherit;
  white-space: nowrap;
  flex-shrink: 0;
}

.mobile-tab-btn:hover {
  color: var(--pink);
}

.mobile-tab-btn.active {
  background: var(--card);
  color: var(--pink);
  box-shadow: 0 2px 8px rgba(255, 107, 157, 0.1);
}

.mobile-tab-icon {
  width: 16px;
  height: 16px;
}

.card {
  background: rgba(255, 255, 255, 0.7);
  border-radius: 16px;
  border: 1px solid rgba(255, 182, 215, 0.15);
  box-shadow: 0 2px 12px rgba(255, 107, 157, 0.04), 0 1px 3px rgba(180, 132, 255, 0.03);
  padding: 24px;
  margin-bottom: 20px;
  transition: box-shadow 0.2s ease;
}

html.dark .card {
  background: rgba(37, 37, 71, 0.55);
  border-color: rgba(255, 182, 215, 0.08);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

.card:hover {
  box-shadow: 0 4px 20px rgba(255, 107, 157, 0.06), 0 1px 4px rgba(180, 132, 255, 0.05);
}

.card-hd {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(255, 182, 215, 0.1);
}

.card-hd-icon {
  width: 20px;
  height: 20px;
  color: var(--pink);
  flex-shrink: 0;
}

.card-hd-extra {
  margin-left: auto;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-dim);
}

.profile-summary {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 28px;
  background: linear-gradient(135deg, rgba(255, 240, 247, 0.6), rgba(245, 235, 255, 0.6));
  border: 1px solid rgba(255, 182, 215, 0.2);
}

html.dark .profile-summary {
  background: linear-gradient(135deg, rgba(45, 42, 70, 0.5), rgba(42, 40, 68, 0.5));
}

.summary-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--gradient-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  border: 3px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 0 20px rgba(255, 107, 157, 0.18), 0 4px 12px rgba(180, 132, 255, 0.1);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.summary-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 0 28px rgba(255, 107, 157, 0.25), 0 6px 18px rgba(180, 132, 255, 0.15);
}

.summary-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.summary-avatar-fb {
  color: #fff;
  font-size: 28px;
  font-weight: 800;
}

.summary-info {
  flex: 1;
  min-width: 0;
}

.summary-info h2 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-info p {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-edit {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 10px;
  border: 1px solid rgba(255, 107, 157, 0.2);
  background: rgba(255, 255, 255, 0.6);
  color: var(--text);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
  flex-shrink: 0;
  font-family: inherit;
}

html.dark .summary-edit {
  background: rgba(37, 37, 71, 0.5);
}

.summary-edit:hover {
  border-color: var(--pink);
  color: var(--pink);
  background: rgba(255, 107, 157, 0.06);
}

.summary-edit:focus-visible {
  outline: 2px solid var(--pink);
  outline-offset: 2px;
}

.summary-edit svg {
  width: 15px;
  height: 15px;
}

.setting-rows {
  display: flex;
  flex-direction: column;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
  min-height: 72px;
}

.setting-row + .setting-row {
  border-top: 1px solid rgba(255, 182, 215, 0.08);
}

.setting-row-left {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.setting-row-icon {
  width: 20px;
  height: 20px;
  color: var(--text-dim);
  flex-shrink: 0;
  margin-right: 4px;
}

.setting-row-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.setting-row-desc {
  font-size: 12px;
  color: var(--text-dim);
  line-height: 1.4;
}

.setting-row-val {
  font-size: 13px;
  color: var(--text-dim);
  flex-shrink: 0;
}

.setting-row-pending {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-dim);
  background: rgba(255, 107, 157, 0.06);
  padding: 4px 10px;
  border-radius: 8px;
  flex-shrink: 0;
}

.setting-row-arrow {
  font-size: 18px;
  color: var(--text-dim);
  opacity: 0.35;
  flex-shrink: 0;
}

.setting-row-arrow svg {
  width: 16px;
  height: 16px;
}

:deep(.uc-switch) {
  --el-switch-on-color: #FF8AC8;
  --el-switch-off-color: #e4d8e8;
  flex-shrink: 0;
}

html.dark :deep(.uc-switch) {
  --el-switch-off-color: #4a4460;
}

:deep(.uc-switch .el-switch__core) {
  border-radius: 12px;
  height: 22px;
  min-width: 44px;
}

:deep(.uc-switch .el-switch__core .el-switch__action) {
  width: 16px;
  height: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px 28px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-field-full {
  grid-column: 1 / -1;
}

.ff-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--text);
}

.ff-input-wrap {
  position: relative;
}

.ff-input {
  width: 100%;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid rgba(255, 182, 215, 0.25);
  font-size: 14px;
  color: var(--text);
  outline: none;
  background: rgba(255, 246, 251, 0.4);
  transition: all 0.2s ease;
  box-sizing: border-box;
  font-family: inherit;
}

html.dark .ff-input {
  background: rgba(30, 28, 48, 0.5);
  border-color: rgba(255, 182, 215, 0.12);
}

.ff-input:focus {
  border-color: var(--pink);
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.08);
}

html.dark .ff-input:focus {
  background: rgba(45, 42, 70, 0.7);
}

.ff-textarea {
  resize: vertical;
  min-height: 88px;
  line-height: 1.5;
}

.ff-count {
  position: absolute;
  right: 10px;
  bottom: -18px;
  font-size: 11px;
  color: var(--text-dim);
}

.ff-date {
  width: 100%;
}

:deep(.ff-date .el-input__wrapper) {
  background: rgba(255, 246, 251, 0.4);
  border-radius: 10px;
  border: 1px solid rgba(255, 182, 215, 0.25);
  box-shadow: none;
  padding: 4px 12px;
  height: 46px;
}

html.dark :deep(.ff-date .el-input__wrapper) {
  background: rgba(30, 28, 48, 0.5);
  border-color: rgba(255, 182, 215, 0.12);
}

:deep(.ff-date .el-input__wrapper:hover) {
  border-color: var(--pink);
}

.gender-seg {
  display: flex;
  gap: 2px;
  background: rgba(255, 107, 157, 0.04);
  border-radius: 10px;
  padding: 3px;
  width: fit-content;
}

.gender-seg-btn {
  padding: 8px 20px;
  border-radius: 8px;
  border: none;
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-dim);
  cursor: pointer;
  transition: all 0.18s ease;
  font-family: inherit;
}

.gender-seg-btn:hover {
  color: var(--pink);
}

.gender-seg-btn.active {
  background: var(--gradient-brand);
  color: #fff;
  box-shadow: 0 2px 10px rgba(255, 107, 157, 0.2);
}

.form-sticky {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  border: 1px solid rgba(255, 182, 215, 0.15);
  box-shadow: 0 -1px 10px rgba(255, 107, 157, 0.04);
  position: sticky;
  bottom: 0;
  margin-top: 8px;
}

html.dark .form-sticky {
  background: rgba(37, 37, 71, 0.7);
  border-color: rgba(255, 182, 215, 0.08);
}

.form-sticky-hint {
  font-size: 12px;
  color: var(--text-dim);
  margin-right: auto;
}

.btn-save {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 11px 32px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #FF8AC8, #B484FF);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(255, 138, 200, 0.25);
  transition: all 0.22s ease;
  font-family: inherit;
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(180, 132, 255, 0.32);
}

.btn-save:active:not(:disabled) {
  transform: scale(0.97);
}

.btn-save:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-save:focus-visible {
  outline: 2px solid var(--pink);
  outline-offset: 2px;
}

.btn-save svg {
  width: 16px;
  height: 16px;
}

/* ===== 改密弹窗 ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-card {
  background: var(--card);
  border-radius: 16px;
  width: 420px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border-light);
}

.modal-header h3 {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  font-size: 22px;
  color: var(--text-dim);
  cursor: pointer;
  line-height: 1;
}

.modal-body {
  padding: 20px 24px;
}

.password-field {
  margin-bottom: 16px;
}

.password-field label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 6px;
}

.modal-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding: 16px 24px;
  border-top: 1px solid var(--border-light);
}

.cancel-btn {
  padding: 8px 20px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-dim);
  font-family: inherit;
}

.submit-btn {
  padding: 8px 24px;
  border-radius: 8px;
  background: var(--gradient-brand);
  color: var(--white);
  font-size: 13px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  font-family: inherit;
}

.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.setting-row-clickable {
  cursor: pointer;
}

.setting-row-clickable:hover {
  background: rgba(255, 107, 157, 0.04);
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.avatar-display {
  padding: 32px 28px;
}

.avatar-showcase {
  display: flex;
  align-items: center;
  gap: 32px;
}

.avatar-ring {
  position: relative;
  width: 132px;
  height: 132px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-brand);
  box-shadow:
    0 0 0 4px rgba(255, 255, 255, 0.8),
    0 0 0 6px rgba(255, 107, 157, 0.08),
    0 0 40px rgba(255, 107, 157, 0.15),
    0 8px 24px rgba(180, 132, 255, 0.12);
  overflow: visible;
  cursor: default;
}

html.dark .avatar-ring {
  box-shadow:
    0 0 0 4px rgba(37, 37, 71, 0.8),
    0 0 0 6px rgba(255, 107, 157, 0.06),
    0 0 40px rgba(255, 107, 157, 0.1);
}

.avatar-ring img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-ring-fb {
  color: #fff;
  font-size: 48px;
  font-weight: 800;
}

.avatar-badge {
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: var(--gradient-brand);
  padding: 3px 14px;
  border-radius: 10px;
  white-space: nowrap;
  z-index: 2;
  box-shadow: 0 2px 8px rgba(255, 107, 157, 0.2);
}

.avatar-show-info {
  flex: 1;
  min-width: 0;
}

.avatar-show-info h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 6px;
}

.avatar-show-info p {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0 0 18px;
  line-height: 1.5;
}

.avatar-show-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.btn-upload {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 11px 26px;
  border-radius: 12px;
  background: var(--gradient-brand);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease;
  box-shadow: 0 4px 14px rgba(255, 107, 157, 0.24);
  font-family: inherit;
}

.btn-upload:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(180, 132, 255, 0.32);
}

.btn-upload:focus-visible {
  outline: 2px solid var(--pink);
  outline-offset: 2px;
}

.btn-upload svg {
  width: 18px;
  height: 18px;
}

.upload-extra {
  font-size: 12px;
  color: var(--text-dim);
}

.history-grid-new {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(84px, 1fr));
  gap: 16px;
}

.history-item {
  position: relative;
  width: 84px;
  height: 84px;
  border-radius: 50%;
  overflow: visible;
  cursor: pointer;
  border: 3px solid rgba(255, 182, 215, 0.2);
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.history-item:hover {
  border-color: var(--pink);
  transform: translateY(-2px);
}

.history-item.active {
  border-color: var(--pink);
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.18);
}

.history-item img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.history-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.18s ease;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.history-item:hover .history-overlay {
  opacity: 1;
}

.history-overlay svg {
  width: 28px;
  height: 28px;
  color: #4ade80;
}

.history-item-del {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid #fff;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.15s, background 0.15s;
  z-index: 3;
  padding: 0;
  font-family: inherit;
}

.history-item:hover .history-item-del {
  opacity: 1;
}

.history-item-del:hover {
  background: var(--error);
}

.history-item-del svg {
  width: 10px;
  height: 10px;
}

.empty-card {
  padding: 40px 24px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--text-dim);
}

.empty-icon {
  width: 48px;
  height: 48px;
  opacity: 0.4;
}

.empty-state p {
  font-size: 14px;
  margin: 0;
}

@media (max-width: 1024px) {
  .uc-sidebar {
    width: 64px;
  }

  .sidebar-user {
    justify-content: center;
    padding: 0 8px 16px;
    flex-direction: column;
    gap: 0;
  }

  .sidebar-user-text,
  .nav-group-label,
  .nav-link span,
  .sidebar-footer-link span {
    display: none;
  }

  .nav-link {
    justify-content: center;
    padding: 10px;
  }

  .nav-link.active::before {
    left: -4px;
    height: 14px;
  }

  .sidebar-footer {
    display: flex;
    justify-content: center;
    padding: 16px 8px 0;
  }

  .sidebar-footer-link {
    justify-content: center;
    padding: 8px;
  }
}

@media (max-width: 768px) {
  .user-center-page {
    padding: calc(var(--header-offset) + 4px) 12px 40px;
  }

  .uc-shell {
    flex-direction: column;
    border-radius: 20px;
  }

  .uc-sidebar {
    display: none;
  }

  .uc-main {
    padding: 22px 18px 32px;
  }

  .mobile-tabs {
    display: flex;
  }

  .page-hdr {
    margin-bottom: 20px;
  }

  .page-hdr-title {
    font-size: 20px;
  }

  .info-grid {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .profile-summary {
    flex-wrap: wrap;
    gap: 14px;
    padding: 20px;
  }

  .summary-edit {
    width: 100%;
    justify-content: center;
  }

  .avatar-showcase {
    flex-direction: column;
    text-align: center;
    gap: 24px;
  }

  .avatar-show-actions {
    align-items: center;
  }

  .history-grid-new {
    grid-template-columns: repeat(auto-fill, minmax(72px, 1fr));
    gap: 12px;
  }

  .history-item {
    width: 72px;
    height: 72px;
  }

  .form-sticky {
    flex-direction: column;
    gap: 12px;
    padding: 14px 18px;
  }

  .form-sticky-hint {
    margin-right: 0;
    text-align: center;
  }

  .btn-save {
    width: 100%;
    justify-content: center;
  }

  .gender-seg {
    width: 100%;
  }

  .gender-seg-btn {
    flex: 1;
    text-align: center;
  }
}
</style>
