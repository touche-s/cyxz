<template>
  <main class="main-content">
    <div class="profile-header" v-if="profile">
      <div class="header-left">
        <div class="avatar">{{ (profile.nickname || 'U').charAt(0) }}</div>
        <div class="info">
          <h2>{{ profile.nickname }}</h2>
          <p>{{ profile.bio || '这个人很懒，什么都没写...' }}</p>
          <div class="meta">
            <span>ID: {{ profile.userId }}</span>
          </div>
        </div>
      </div>
      <el-button class="edit-btn" type="primary" round @click="startEdit" v-if="isSelf">
        编辑资料
      </el-button>
    </div>

    <div class="section-label">我的内容</div>
    <el-empty v-if="!loading" description="暂无内容" />

    <div v-if="showEdit" class="overlay" @click.self="cancelEdit">
      <div class="edit-card">
        <h3>编辑资料</h3>
        <el-form :model="editForm" label-position="top">
          <el-form-item label="昵称">
            <el-input v-model="editForm.nickname" maxlength="20" />
          </el-form-item>
          <el-form-item label="简介">
            <el-input v-model="editForm.bio" type="textarea" :rows="3" maxlength="200" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="editForm.gender">
              <el-radio :value="0">保密</el-radio>
              <el-radio :value="1">男</el-radio>
              <el-radio :value="2">女</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <div class="edit-actions">
          <el-button @click="cancelEdit">取消</el-button>
          <el-button type="primary" @click="saveEdit" :loading="saving">保存</el-button>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserProfile, updateUserProfile } from '@/api/user'
import type { UserInfo } from '@/api/user'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const profile = ref<UserInfo | null>(null)
const loading = ref(true)
const showEdit = ref(false)
const saving = ref(false)

const editForm = reactive({
  nickname: '',
  bio: '',
  gender: 0,
})

const isSelf = computed(() => {
  return String(profile.value?.userId) === String(userStore.userInfo?.id)
})

onMounted(async () => {
  const userId = Number(route.params.id)
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
  editForm.gender = profile.value.gender || 0
  showEdit.value = true
}

function cancelEdit() {
  showEdit.value = false
}

async function saveEdit() {
  saving.value = true
  try {
    const res = await updateUserProfile(editForm)
    const data = (res.data as any).data || res.data
    if (profile.value) Object.assign(profile.value, data)
    if (isSelf.value) {
      userStore.setUserInfo({ ...userStore.userInfo!, ...data } as any)
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
.main-content {
  padding: 90px 44px 60px;
}

.profile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: white;
  border-radius: var(--radius);
  padding: 32px;
  border: 1.5px solid var(--border);
  box-shadow: var(--shadow);
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 28px;
  font-weight: 800;
  flex-shrink: 0;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.2);
}

.info h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 6px;
}

.info p {
  font-size: 14px;
  color: var(--text-dim);
  margin-bottom: 8px;
}

.meta { font-size: 12px; color: var(--text-dim); }

.edit-btn {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  border: none;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.25);
}

.edit-btn:hover {
  box-shadow: 0 6px 24px rgba(255, 107, 157, 0.35);
}

.section-label {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 16px;
}

.overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}

.edit-card {
  background: white;
  border-radius: var(--radius);
  padding: 28px;
  width: 440px;
  max-width: 90vw;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.edit-card h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 20px;
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .main-content { padding: 90px 16px 60px; }
  .profile-header { flex-direction: column; gap: 16px; text-align: center; }
  .header-left { flex-direction: column; }
}
</style>
