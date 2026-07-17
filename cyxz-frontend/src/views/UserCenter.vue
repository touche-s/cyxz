<template>
  <div class="user-center">
    <div class="page-header">
      <button class="back-btn" @click="$router.back()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M15 18l-6-6 6-6"/>
        </svg>
        返回
      </button>
      <h1 class="page-title">个人中心</h1>
      <div class="placeholder"></div>
    </div>

    <div class="center-content">
      <div class="settings-container">
        <div class="settings-section profile-section">
          <div class="profile-header">
            <div class="avatar-wrapper" @click="startEdit()">
              <div class="profile-avatar">
                <img v-if="profile.avatar" :src="profile.avatar" alt="avatar" class="profile-avatar-img" />
                <span v-else>{{ (profile.nickname || 'U').charAt(0) }}</span>
              </div>
              <div class="avatar-edit-badge">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="white" stroke-width="2.5">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                </svg>
              </div>
            </div>
            <div class="profile-info">
              <h2>{{ profile.nickname }}</h2>
              <p class="profile-bio">{{ profile.bio || '点击编辑资料完善你的个人信息~' }}</p>
            </div>
          </div>
          <button class="edit-profile-btn" @click="startEdit()">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
            编辑资料
          </button>
        </div>

        <div class="settings-section">
          <h3 class="settings-section-title">隐私设置</h3>
          <div class="settings-grid">
            <div class="setting-item">
              <span class="setting-label">公开我的收藏</span>
              <el-switch v-model="settings.publicFavorites" />
            </div>
            <div class="setting-item">
              <span class="setting-label">公开我的生日</span>
              <el-switch v-model="settings.publicBirthday" />
            </div>
            <div class="setting-item">
              <span class="setting-label">公开我的关注列表</span>
              <el-switch v-model="settings.publicFollowing" />
            </div>
            <div class="setting-item">
              <span class="setting-label">公开我的粉丝列表</span>
              <el-switch v-model="settings.publicFollowers" />
            </div>
            <div class="setting-item">
              <span class="setting-label">允许他人@我</span>
              <el-switch v-model="settings.allowMention" />
            </div>
            <div class="setting-item">
              <span class="setting-label">允许他人私信我</span>
              <el-switch v-model="settings.allowPrivateMessage" />
            </div>
          </div>
        </div>

        <div class="settings-section">
          <h3 class="settings-section-title">通知设置</h3>
          <div class="settings-grid">
            <div class="setting-item">
              <span class="setting-label">点赞通知</span>
              <el-switch v-model="settings.notificationLike" />
            </div>
            <div class="setting-item">
              <span class="setting-label">评论通知</span>
              <el-switch v-model="settings.notificationComment" />
            </div>
            <div class="setting-item">
              <span class="setting-label">收藏通知</span>
              <el-switch v-model="settings.notificationFavorite" />
            </div>
            <div class="setting-item">
              <span class="setting-label">关注通知</span>
              <el-switch v-model="settings.notificationFollow" />
            </div>
          </div>
        </div>

        <div class="settings-section">
          <h3 class="settings-section-title">账号安全</h3>
          <div class="settings-list">
            <div class="setting-row">
              <img src="@/assets/icons/shield.svg" alt="shield" class="setting-row-icon" />
              <span class="setting-row-label">修改密码</span>
              <span class="setting-row-arrow">›</span>
            </div>
            <div class="setting-row">
              <img src="@/assets/icons/info.svg" alt="info" class="setting-row-icon" />
              <span class="setting-row-label">绑定手机号</span>
              <span class="setting-row-arrow">›</span>
            </div>
            <div class="setting-row">
              <img src="@/assets/icons/mail.svg" alt="mail" class="setting-row-icon" />
              <span class="setting-row-label">绑定邮箱</span>
              <span class="setting-row-arrow">›</span>
            </div>
          </div>
        </div>

        <div class="settings-section">
          <h3 class="settings-section-title">关于</h3>
          <div class="settings-list">
            <div class="setting-row">
              <img src="@/assets/icons/info.svg" alt="info" class="setting-row-icon" />
              <span class="setting-row-label">关于次元小站</span>
              <span class="setting-row-value">v1.0.0</span>
            </div>
            <div class="setting-row">
              <img src="@/assets/icons/info.svg" alt="info" class="setting-row-icon" />
              <span class="setting-row-label">用户协议</span>
              <span class="setting-row-arrow">›</span>
            </div>
            <div class="setting-row">
              <img src="@/assets/icons/shield.svg" alt="shield" class="setting-row-icon" />
              <span class="setting-row-label">隐私政策</span>
              <span class="setting-row-arrow">›</span>
            </div>
          </div>
        </div>
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
                  <svg class="avatar-upload-camera" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="white" stroke-width="2">
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyProfile, updateUserProfile } from '@/api/user'
import type { UserInfo } from '@/api/user'
import { uploadAvatar } from '@/api/upload'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const profile = reactive<UserInfo>({
  userId: 0,
  nickname: '',
  avatar: '',
  bio: '',
  gender: 0,
  birthday: '',
})

const showEdit = ref(false)
const saving = ref(false)
const fileInput = ref<HTMLInputElement>()

const editForm = reactive({
  nickname: '',
  bio: '',
  gender: 0,
  avatar: '',
  birthday: '',
})

const settings = reactive({
  publicFavorites: false,
  publicBirthday: true,
  publicFollowing: true,
  publicFollowers: true,
  allowMention: true,
  allowPrivateMessage: true,
  notificationLike: true,
  notificationComment: true,
  notificationFavorite: true,
  notificationFollow: true,
})

onMounted(async () => {
  const userId = userStore.userInfo?.id
  if (!userId) return
  try {
    const res = await getMyProfile()
    const data = (res.data as any).data || res.data
    Object.assign(profile, data)
  } catch {
    ElMessage.error('加载用户信息失败')
  }
})

function startEdit() {
  editForm.nickname = profile.nickname
  editForm.bio = profile.bio || ''
  editForm.gender = profile.gender ?? 0
  editForm.avatar = profile.avatar || ''
  editForm.birthday = profile.birthday || ''
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
    Object.assign(profile, {
      nickname: editForm.nickname,
      bio: editForm.bio,
      gender: editForm.gender,
      avatar: editForm.avatar,
      birthday: editForm.birthday,
    })
    if (userStore.userInfo) {
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
  padding: 120px 0 60px;
  min-height: 100vh;
  background: linear-gradient(180deg, #fff5f9 0%, #f8f9ff 100%);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 700px;
  margin: 0 auto;
  padding: 0 24px;
  margin-bottom: 24px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 12px;
  background: rgba(255, 107, 157, 0.1);
  color: var(--pink);
  font-size: 14px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
  width: 80px;
}

.back-btn svg {
  width: 16px;
  height: 16px;
}

.back-btn:hover {
  background: rgba(255, 107, 157, 0.2);
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--text);
}

.placeholder {
  width: 80px;
}

.center-content {
  max-width: 700px;
  margin: 0 auto;
  padding: 0 24px;
}

.settings-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.settings-section {
  background: white;
  border-radius: 16px;
  padding: 20px 24px;
  border: 1.5px solid var(--border);
  box-shadow: 0 2px 12px rgba(180, 132, 255, 0.05);
}

.profile-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
}

.profile-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 28px;
  font-weight: 800;
  border: 3px solid white;
  box-shadow: 0 4px 16px rgba(255,107,157,0.3);
  overflow: hidden;
}

.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-edit-badge {
  position: absolute;
  top: 0;
  right: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  border: 2px solid white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(255,107,157,0.3);
}

.profile-info {
  flex: 1;
}

.profile-info h2 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

.profile-bio {
  font-size: 13px;
  color: var(--text-dim);
  margin: 4px 0 0;
}

.edit-profile-btn {
  align-self: flex-end;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 12px;
  background: rgba(255, 107, 157, 0.08);
  color: var(--pink);
  font-size: 13px;
  font-weight: 600;
  border: 1.5px solid rgba(255, 107, 157, 0.2);
  cursor: pointer;
  transition: all 0.22s ease-out;
}

.edit-profile-btn:hover {
  background: rgba(255, 107, 157, 0.15);
}

.settings-section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 1.5px solid var(--border);
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.setting-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.settings-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.setting-row {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-radius: 8px;
}

.setting-row:hover {
  background: rgba(255, 107, 157, 0.05);
}

.setting-row-icon {
  font-size: 16px;
  width: 16px;
  height: 16px;
  margin-right: 12px;
}

.setting-row-label {
  flex: 1;
  font-size: 14px;
  color: var(--text-secondary);
}

.setting-row-arrow {
  font-size: 18px;
  color: var(--text-dim);
}

.setting-row-value {
  font-size: 13px;
  color: var(--text-dim);
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

.avatar-preview:hover .avatar-upload-mask {
  opacity: 1;
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
  font-weight: 700;
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 4px;
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
  .center-content {
    padding: 0 16px;
  }

  .page-header {
    padding: 0 16px;
  }

  .profile-header {
    flex-direction: column;
    text-align: center;
  }

  .edit-profile-btn {
    align-self: center;
  }

  .settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>