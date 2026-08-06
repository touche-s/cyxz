<template>
  <div class="circle-admin-container">
    <!-- 左侧：我管理的圈子选择器 -->
    <aside class="ca-sidebar">
      <div class="ca-sidebar-user" v-if="userStore.userInfo">
        <div class="ca-user-avatar">
          <img v-if="userStore.userInfo.avatar" :src="userStore.userInfo.avatar" />
          <span v-else class="ca-user-fb">{{ (userStore.userInfo.nickname || '?').charAt(0) }}</span>
        </div>
        <div class="ca-user-info">
          <span class="ca-user-hi">欢迎回来</span>
          <span class="ca-user-name">{{ userStore.userInfo.nickname || userStore.userInfo.userId }}</span>
          <span class="ca-user-role tag-gray">{{ roleLabel(userStore.role) }}</span>
        </div>
        <button class="ca-dark-btn" @click="toggleDarkMode" :title="isDark ? '切到日间模式' : '切到夜间模式'">
          <Icon :icon="isDark ? 'ph:sun' : 'ph:moon'" />
        </button>
      </div>
      <div class="ca-circle-list">
        <LoadingSpinner v-if="circleLoading" text="加载中..." />
        <div v-else-if="managedCircles.length === 0" class="ca-empty">
          <Icon icon="ph:circles-three-plus" class="ca-empty-icon" />
          <p>你还没有管理的圈子</p>
          <p class="ca-empty-hint">成为圈主或被任命为圈子管理员后可在此管理</p>
        </div>
        <button
          v-for="c in managedCircles"
          :key="c.id"
          class="ca-circle-item"
          :class="{ active: currentCircleId === c.id }"
          @click="selectCircle(c.id)"
        >
          <div class="ca-circle-avatar">
            <img v-if="c.avatar" :src="c.avatar" />
            <span v-else class="ca-avatar-fb">{{ c.name?.charAt(0) }}</span>
          </div>
          <div class="ca-circle-info">
            <span class="ca-circle-name">{{ c.name }}</span>
            <span class="ca-circle-stat">{{ c.memberCount }} 成员 · {{ c.postCount }} 帖子</span>
          </div>
        </button>
      </div>
      <div class="ca-sidebar-footer">
        <button class="ca-back-btn" @click="to('/')">
          <Icon icon="ph:arrow-left" />
          返回前台
        </button>
      </div>
    </aside>

    <!-- 右侧：管理区 -->
    <main class="ca-main" v-if="currentCircle">
      <div class="ca-main-hd">
        <div class="ca-hd-left">
          <div class="ca-hd-avatar">
            <img v-if="currentCircle.avatar" :src="currentCircle.avatar" />
            <span v-else class="ca-hd-fb">{{ currentCircle.name?.charAt(0) }}</span>
          </div>
          <div>
            <h1 class="ca-hd-name">{{ currentCircle.name }}</h1>
            <p class="ca-hd-intro">{{ currentCircle.intro || '暂无简介' }}</p>
          </div>
        </div>
      </div>

      <!-- Tab 切换 -->
      <div class="ca-tabs">
        <button
          v-for="t in tabs"
          :key="t.key"
          class="ca-tab"
          :class="{ active: activeTab === t.key }"
          @click="activeTab = t.key"
        >
          <Icon :icon="t.icon" class="ca-tab-icon" />
          <span>{{ t.label }}</span>
        </button>
      </div>

      <!-- 圈子资料 -->
      <section v-if="activeTab === 'profile'" class="ca-section">
        <div class="ca-form-card">
          <h3>圈子资料</h3>
          <div class="ca-form-row">
            <label>圈子名称</label>
            <input v-model="profileForm.name" class="ca-input" placeholder="输入圈子名称" />
          </div>
          <div class="ca-form-row">
            <label>简介</label>
            <textarea v-model="profileForm.intro" class="ca-textarea" rows="3" placeholder="一句话简介"></textarea>
          </div>
          <div class="ca-form-row">
            <label>头像</label>
            <div class="ca-upload-row">
              <div class="ca-upload-preview ca-avatar-preview">
                <img v-if="profileAvatarPreview || profileForm.avatar" :src="profileAvatarPreview || profileForm.avatar" />
                <span v-else class="ca-preview-fb">{{ profileForm.name?.charAt(0) || '?' }}</span>
              </div>
              <label class="ca-upload-btn">
                选择图片
                <input type="file" accept="image/*" hidden @change="onAvatarFile" />
              </label>
            </div>
          </div>
          <div class="ca-form-row">
            <label>封面</label>
            <div class="ca-upload-row">
              <div class="ca-upload-preview ca-cover-preview">
                <img v-if="profileCoverPreview || profileForm.cover" :src="profileCoverPreview || profileForm.cover" />
                <div v-else class="ca-cover-fb"></div>
              </div>
              <label class="ca-upload-btn">
                选择图片
                <input type="file" accept="image/*" hidden @change="onCoverFile" />
              </label>
            </div>
          </div>
          <div class="ca-form-actions">
            <button class="ca-submit-btn" :disabled="profileSaving" @click="saveProfile">
              {{ profileSaving ? '保存中...' : '保存修改' }}
            </button>
          </div>
        </div>
      </section>

      <!-- 板块管理 -->
      <section v-if="activeTab === 'sections'" class="ca-section">
        <div class="ca-section-head">
          <div>
            <h2>板块配置</h2>
            <p class="ca-section-desc">勾选该圈子要启用的板块，并选择一个作为默认板块</p>
          </div>
          <button class="ca-toolbar-btn" @click="loadSections" title="刷新"><Icon icon="ph:arrows-clockwise" /></button>
        </div>
        <LoadingSpinner v-if="sectionLoading" text="加载中..." />
        <div v-else class="ca-section-list">
          <label
            v-for="t in sectionTemplates"
            :key="t.id"
            class="ca-section-item"
            :class="{ disabled: !sectionChecked.has(t.id) }"
          >
            <span class="ca-section-left">
              <input type="checkbox" :checked="sectionChecked.has(t.id)" @change="toggleSection(t.id)" />
              <span class="ca-section-name">{{ t.name }}</span>
              <span class="ca-section-type">{{ typeLabel(t.applicableType) }}</span>
            </span>
            <span class="ca-section-right">
              <input type="radio" name="defaultSection" :checked="sectionDefault === t.id"
                :disabled="!sectionChecked.has(t.id)" @change="sectionDefault = t.id" />
              <span class="ca-default-label">默认</span>
            </span>
          </label>
          <div class="ca-form-actions" v-if="sectionTemplates.length > 0">
            <button class="ca-submit-btn" :disabled="sectionSaving" @click="saveSections">
              {{ sectionSaving ? '保存中...' : '保存配置' }}
            </button>
          </div>
          <EmptyState v-if="sectionTemplates.length === 0" title="暂无板块模板，请联系平台管理员" />
        </div>
      </section>

      <!-- 帖子审核 -->
      <section v-if="activeTab === 'review'" class="ca-section">
        <div class="ca-section-head">
          <div>
            <h2>帖子审核</h2>
            <p class="ca-section-desc">共 {{ reviewPosts.length }} 条待审核</p>
          </div>
          <button class="ca-toolbar-btn" @click="loadReviewPosts" title="刷新"><Icon icon="ph:arrows-clockwise" /></button>
        </div>
        <LoadingSpinner v-if="reviewLoading" text="加载中..." />
        <div v-else class="ca-table-wrap">
          <table v-if="reviewPosts.length > 0" class="ca-table">
            <thead>
              <tr>
                <th style="width: 100px;">ID</th>
                <th>标题</th>
                <th style="width: 110px;">作者</th>
                <th style="width: 110px;">创建时间</th>
                <th style="width: 140px;">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in reviewPosts" :key="p.id">
                <td class="ca-td-id">{{ p.id }}</td>
                <td class="ca-td-name" :title="p.title">{{ p.title }}</td>
                <td>{{ p.authorName }}</td>
                <td class="ca-td-time">{{ p.createTime?.slice(0, 10) }}</td>
                <td class="ca-td-actions">
                  <button class="ca-op-link ca-op-green" @click="approvePost(p)">通过</button>
                  <button class="ca-op-link ca-op-danger" @click="openReject(p)">拒绝</button>
                </td>
              </tr>
            </tbody>
          </table>
          <EmptyState v-if="reviewPosts.length === 0" title="暂无待审核内容" />
        </div>
      </section>
    </main>

    <!-- 未选择圈子时的占位 -->
    <main class="ca-main ca-main-empty" v-else-if="!circleLoading">
      <div class="ca-placeholder">
        <Icon icon="ph:hand-pointing" class="ca-placeholder-icon" />
        <p>请从左侧选择要管理的圈子</p>
      </div>
    </main>

    <!-- 拒绝原因弹窗 -->
    <Teleport to="body">
      <div class="ca-modal-overlay" v-if="showRejectModal" @click.self="showRejectModal = false">
        <div class="ca-modal-card">
          <div class="ca-modal-header">
            <h3>拒绝帖子 - {{ rejectTarget?.title }}</h3>
            <button class="ca-modal-close" @click="showRejectModal = false">&times;</button>
          </div>
          <div class="ca-modal-body">
            <div class="ca-form-row">
              <label>拒绝原因</label>
              <textarea v-model="rejectReason" class="ca-textarea" rows="3" placeholder="请填写拒绝原因，会展示给作者"></textarea>
            </div>
          </div>
          <div class="ca-modal-footer">
            <button class="ca-cancel-btn" @click="showRejectModal = false">取消</button>
            <button class="ca-submit-btn ca-btn-danger" @click="confirmReject">确认拒绝</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Icon } from '@iconify/vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useNavigate } from '@/composables/useNavigate'
import { useUserStore } from '@/stores/user'
import { getManagedCircles, getCircleDetail, updateCircle, getCircleSections } from '@/api/circle'
import { uploadCircleResource } from '@/api/upload'
import { listPendingReview, approvePost, rejectPost } from '@/api/post'
import type { CircleVO } from '@/api/circle'
import type { PostVO } from '@/api/post'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import request from '@/utils/request'

const route = useRoute()
const { to } = useNavigate()
const userStore = useUserStore()

// 暗色模式
const isDark = ref(false)
function toggleDarkMode() {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('darkMode', isDark.value ? '1' : '0')
}

function roleLabel(role: string) {
  if (role === 'SITE_OWNER') return '站主'
  if (role === 'PLATFORM_ADMIN') return '平台管理员'
  if (role === 'USER') return '普通用户'
  return role || '普通用户'
}

interface SectionTemplate {
  id: number; name: string; applicableType: string; description: string; sortOrder: number
}

const tabs = [
  { key: 'profile', label: '圈子资料', icon: 'ph:gear' },
  { key: 'sections', label: '板块管理', icon: 'ph:stack' },
  { key: 'review', label: '帖子审核', icon: 'ph:shield-check' },
] as const

const managedCircles = ref<CircleVO[]>([])
const circleLoading = ref(false)
const currentCircleId = ref<number | null>(null)
const currentCircle = ref<CircleVO | null>(null)
const activeTab = ref<'profile' | 'sections' | 'review'>('profile')

// ===== 圈子资料 =====
const profileForm = ref({ name: '', intro: '', avatar: '', cover: '' })
const profileAvatarPreview = ref('')
const profileCoverPreview = ref('')
const profileSaving = ref(false)

// ===== 板块管理 =====
const sectionTemplates = ref<SectionTemplate[]>([])
const sectionChecked = ref(new Set<number>())
const sectionDefault = ref<number | null>(null)
const sectionLoading = ref(false)
const sectionSaving = ref(false)

// ===== 帖子审核 =====
const reviewPosts = ref<PostVO[]>([])
const reviewLoading = ref(false)
const showRejectModal = ref(false)
const rejectTarget = ref<PostVO | null>(null)
const rejectReason = ref('')

async function loadManagedCircles() {
  circleLoading.value = true
  try {
    managedCircles.value = await getManagedCircles()
    // 如果 URL 带了圈子 ID 且在管理列表中，自动选中
    const routeId = Number(route.params.id)
    if (routeId && managedCircles.value.some((c) => c.id === routeId)) {
      await selectCircle(routeId)
    } else if (managedCircles.value.length > 0) {
      await selectCircle(managedCircles.value[0].id)
    }
  } catch {
    managedCircles.value = []
  } finally {
    circleLoading.value = false
  }
}

async function selectCircle(circleId: number) {
  if (currentCircleId.value === circleId && currentCircle.value) return
  currentCircleId.value = circleId
  activeTab.value = 'profile'
  try {
    currentCircle.value = await getCircleDetail(circleId)
    profileForm.value = {
      name: currentCircle.value.name,
      intro: currentCircle.value.intro || '',
      avatar: currentCircle.value.avatar || '',
      cover: currentCircle.value.cover || '',
    }
    profileAvatarPreview.value = ''
    profileCoverPreview.value = ''
  } catch {
    ElMessage.error('加载圈子详情失败')
  }
}

// 资料编辑
async function onAvatarFile(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  profileAvatarPreview.value = URL.createObjectURL(file)
  try {
    const url = await uploadCircleResource(file, currentCircleId.value!, 'avatar') as string
    profileForm.value.avatar = url
  } catch { ElMessage.error('头像上传失败') }
}

async function onCoverFile(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  profileCoverPreview.value = URL.createObjectURL(file)
  try {
    const url = await uploadCircleResource(file, currentCircleId.value!, 'cover') as string
    profileForm.value.cover = url
  } catch { ElMessage.error('封面上传失败') }
}

async function saveProfile() {
  if (!currentCircleId.value) return
  if (!profileForm.value.name) { ElMessage.warning('请输入圈子名称'); return }
  profileSaving.value = true
  try {
    await updateCircle(currentCircleId.value, {
      name: profileForm.value.name,
      intro: profileForm.value.intro || undefined,
      avatar: profileForm.value.avatar || undefined,
      cover: profileForm.value.cover || undefined,
    })
    ElMessage.success('保存成功')
    if (currentCircle.value) {
      currentCircle.value = { ...currentCircle.value, ...profileForm.value }
    }
    // 同步左侧选择器中的名称
    const item = managedCircles.value.find((c) => c.id === currentCircleId.value)
    if (item) item.name = profileForm.value.name
  } catch { ElMessage.error('保存失败') }
  finally { profileSaving.value = false }
}

// 板块管理
function typeLabel(type: string) {
  if (type === 'ALL') return '全部'
  if (type === 'NORMAL') return '图文'
  if (type === 'ARTICLE') return '长文'
  return type
}

function toggleSection(templateId: number) {
  const next = new Set(sectionChecked.value)
  if (next.has(templateId)) {
    next.delete(templateId)
    if (sectionDefault.value === templateId) sectionDefault.value = null
  } else {
    next.add(templateId)
    if (sectionDefault.value === null) sectionDefault.value = templateId
  }
  sectionChecked.value = next
}

async function loadSections() {
  if (!currentCircleId.value) return
  sectionLoading.value = true
  try {
    if (sectionTemplates.value.length === 0) {
      sectionTemplates.value = await request.get('/admin/section-template/list') as any || []
    }
    const sections = await getCircleSections(currentCircleId.value)
    sectionChecked.value = new Set(sections.map((s) => s.templateId))
    sectionDefault.value = sections.find((s) => s.isDefault === 1)?.templateId ?? null
  } catch {
    sectionChecked.value = new Set()
    sectionDefault.value = null
  } finally {
    sectionLoading.value = false
  }
}

async function saveSections() {
  if (!currentCircleId.value) return
  if (sectionChecked.value.size === 0) { ElMessage.warning('请至少启用一个板块'); return }
  if (sectionDefault.value === null) { ElMessage.warning('请选择一个默认板块'); return }
  sectionSaving.value = true
  try {
    const configs = sectionTemplates.value
      .filter((t) => sectionChecked.value.has(t.id))
      .map((t, i) => ({ templateId: t.id, isDefault: t.id === sectionDefault.value ? 1 : 0, sortOrder: i, status: 1 }))
    await request.put(`/circle/${currentCircleId.value}/sections`, configs)
    ElMessage.success('板块配置保存成功')
  } catch { ElMessage.error('保存失败') }
  finally { sectionSaving.value = false }
}

// 帖子审核
async function loadReviewPosts() {
  if (!currentCircleId.value) return
  reviewLoading.value = true
  try {
    const res = await listPendingReview({ page: 1, size: 100 })
    // 按当前圈子过滤（审核列表是全局的，圈子管理员只看自己圈子）
    reviewPosts.value = (res.records || []).filter((p) => p.circleId === currentCircleId.value)
  } catch {
    reviewPosts.value = []
  } finally {
    reviewLoading.value = false
  }
}

async function approvePostAction(p: PostVO) {
  try { await approvePost(p.id); ElMessage.success('审核通过'); await loadReviewPosts() }
  catch { ElMessage.error('操作失败') }
}

function openReject(p: PostVO) {
  rejectTarget.value = p
  rejectReason.value = ''
  showRejectModal.value = true
}

async function confirmReject() {
  if (!rejectTarget.value || !rejectReason.value.trim()) { ElMessage.warning('请填写拒绝原因'); return }
  try {
    await rejectPost(rejectTarget.value.id, rejectReason.value.trim())
    ElMessage.success('已拒绝')
    showRejectModal.value = false
    rejectTarget.value = null
    await loadReviewPosts()
  } catch { ElMessage.error('操作失败') }
}

// 切换 tab 时懒加载对应数据
watch(activeTab, (tab) => {
  if (tab === 'sections' && sectionTemplates.value.length === 0) loadSections()
  if (tab === 'review' && reviewPosts.value.length === 0) loadReviewPosts()
})

// 切换圈子时重置各 tab 数据
watch(currentCircleId, () => {
  sectionTemplates.value = []
  sectionChecked.value = new Set()
  sectionDefault.value = null
  reviewPosts.value = []
  if (activeTab.value === 'sections') loadSections()
  if (activeTab.value === 'review') loadReviewPosts()
})

onMounted(() => {
  isDark.value = localStorage.getItem('darkMode') === '1'
  document.documentElement.classList.toggle('dark', isDark.value)
  loadManagedCircles()
})
</script>

<style scoped>
.circle-admin-container {
  display: flex;
  min-height: 100vh;
  background: var(--bg);
}

/* 左侧圈子选择器 */
.ca-sidebar {
  width: 260px;
  background: var(--card);
  border-right: 1px solid var(--border-light);
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  z-index: 100;
}

.ca-sidebar-user {
  padding: 20px 16px;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  gap: 12px;
}

.ca-user-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--gradient-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ca-user-avatar img { width: 100%; height: 100%; object-fit: cover; }
.ca-user-fb { font-size: 18px; font-weight: 700; color: #fff; }

.ca-user-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.ca-user-hi { font-size: 11px; color: var(--text-dim); }
.ca-user-name { font-size: 14px; font-weight: 700; color: var(--text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ca-user-role { font-size: 11px; padding: 1px 8px; border-radius: 6px; font-weight: 500; width: fit-content; }

.ca-circle-list {
  padding: 12px;
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ca-circle-item {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  transition: all 0.2s;
}

.ca-circle-item:hover {
  background: var(--pink-bg);
}

.ca-circle-item.active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(180, 132, 255, 0.1));
}

.ca-circle-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--gradient-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ca-circle-avatar img { width: 100%; height: 100%; object-fit: cover; }

.ca-avatar-fb {
  font-size: 14px;
  font-weight: 700;
  color: #fff;
}

.ca-circle-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}

.ca-circle-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ca-circle-item.active .ca-circle-name {
  color: var(--pink);
}

.ca-circle-stat {
  font-size: 11px;
  color: var(--text-dim);
}

.ca-dark-btn {
  margin-left: auto;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: var(--text-dim);
  transition: all 0.2s;
  padding: 0;
}
.ca-dark-btn:hover { border-color: var(--pink); color: var(--pink); }

.ca-sidebar-footer {
  padding: 12px;
  border-top: 1px solid var(--border-light);
}

.ca-back-btn {
  width: 100%;
  padding: 8px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-dim);
  transition: all 0.2s;
}

.ca-back-btn:hover { border-color: var(--pink); color: var(--pink); }

.ca-empty {
  padding: 40px 20px;
  text-align: center;
  color: var(--text-dim);
}

.ca-empty-icon {
  width: 40px;
  height: 40px;
  margin-bottom: 12px;
  opacity: 0.4;
}

.ca-empty p { margin: 4px 0; font-size: 13px; }
.ca-empty-hint { font-size: 11px !important; opacity: 0.7; }

/* 右侧主区 */
.ca-main {
  flex: 1;
  margin-left: 260px;
  padding: 32px;
  min-height: 100vh;
}

.ca-main-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.ca-placeholder {
  text-align: center;
  color: var(--text-dim);
}

.ca-placeholder-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 12px;
  opacity: 0.4;
}

.ca-placeholder p { font-size: 14px; }

/* 头部 */
.ca-main-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.ca-hd-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.ca-hd-avatar {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  overflow: hidden;
  background: var(--gradient-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ca-hd-avatar img { width: 100%; height: 100%; object-fit: cover; }

.ca-hd-fb {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
}

.ca-hd-name {
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

.ca-hd-intro {
  font-size: 13px;
  color: var(--text-dim);
  margin: 4px 0 0;
}

/* Tabs */
.ca-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 0;
}

.ca-tab {
  padding: 10px 18px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-dim);
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  margin-bottom: -1px;
}

.ca-tab:hover { color: var(--pink); }

.ca-tab.active {
  color: var(--pink);
  border-bottom-color: var(--pink);
  font-weight: 600;
}

.ca-tab-icon { width: 16px; height: 16px; }

/* Section 通用 */
.ca-section { }

.ca-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.ca-section-head h2 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

.ca-section-desc {
  font-size: 12px;
  color: var(--text-dim);
  margin: 6px 0 0;
}

.ca-toolbar-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--card);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: var(--text-dim);
  transition: all 0.15s;
}

.ca-toolbar-btn:hover { border-color: var(--pink); color: var(--pink); }

/* 表单卡片 */
.ca-form-card {
  background: var(--card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  padding: 24px;
  max-width: 640px;
}

.ca-form-card h3 {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 18px;
}

.ca-form-row { margin-bottom: 16px; }

.ca-form-row label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 6px;
}

.ca-input, .ca-textarea {
  width: 100%;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid var(--border);
  font-size: 14px;
  color: var(--text);
  background: var(--bg);
  transition: border-color 0.2s;
  box-sizing: border-box;
  font-family: inherit;
}

.ca-textarea { resize: vertical; }
.ca-input:focus, .ca-textarea:focus { outline: none; border-color: var(--pink); }

.ca-form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding-top: 8px;
}

.ca-submit-btn {
  padding: 8px 24px;
  border-radius: 8px;
  background: var(--gradient-brand);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.ca-submit-btn:hover { opacity: 0.9; transform: translateY(-1px); }
.ca-submit-btn:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

.ca-btn-danger { background: var(--error) !important; }

.ca-cancel-btn {
  padding: 8px 20px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-dim);
}

/* 上传 */
.ca-upload-row { display: flex; align-items: center; gap: 12px; }

.ca-upload-preview {
  border-radius: 10px;
  overflow: hidden;
  background: var(--gradient-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ca-avatar-preview { width: 72px; height: 72px; border-radius: 50%; }
.ca-cover-preview { width: 160px; height: 80px; border-radius: 10px; }
.ca-upload-preview img { width: 100%; height: 100%; object-fit: cover; }
.ca-preview-fb { font-size: 24px; font-weight: 700; color: #fff; }
.ca-cover-fb { width: 100%; height: 100%; background: var(--gradient-brand); }

.ca-upload-btn {
  display: inline-block;
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid var(--border);
  font-size: 13px;
  color: var(--text-dim);
  cursor: pointer;
  transition: all 0.2s;
  background: transparent;
}

.ca-upload-btn:hover { border-color: var(--pink); color: var(--pink); }

/* 板块配置列表 */
.ca-section-list { display: flex; flex-direction: column; gap: 4px; max-width: 640px; }

.ca-section-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid var(--border-light);
  cursor: pointer;
  transition: border-color 0.15s;
  background: var(--card);
}

.ca-section-item:hover { border-color: var(--purple); background: rgba(180, 132, 255, 0.04); }
.ca-section-item.disabled { opacity: 0.5; }

.ca-section-left { display: flex; align-items: center; gap: 10px; }
.ca-section-left input[type="checkbox"] { accent-color: var(--purple); }

.ca-section-name { font-size: 14px; font-weight: 600; color: var(--text); }

.ca-section-type {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--bg-secondary, #f3f4f6);
  color: var(--text-dim);
}

.ca-section-right { display: flex; align-items: center; gap: 6px; }
.ca-section-right input[type="radio"] { accent-color: var(--purple); }
.ca-default-label { font-size: 12px; color: var(--text-dim); }

/* 表格 */
.ca-table-wrap {
  background: var(--card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  overflow: hidden;
}

.ca-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 13px;
}

.ca-table thead th {
  padding: 14px 16px;
  text-align: left;
  font-weight: 600;
  color: #fff !important;
  font-size: 12px;
  background: var(--brand) !important;
  white-space: nowrap;
}

.ca-table td {
  padding: 12px 16px;
  color: var(--text);
  border-bottom: 1px solid var(--border-light);
  white-space: nowrap;
}

.ca-table tbody tr:hover { background: rgba(255, 107, 157, 0.06); }
.ca-table tbody tr:last-child td { border-bottom: none; }

.ca-td-id { color: var(--text-dim); font-size: 12px; font-family: 'SF Mono', 'Menlo', monospace; }
.ca-td-name { font-weight: 600; max-width: 240px; overflow: hidden; text-overflow: ellipsis; }
.ca-td-time { color: var(--text-dim); font-size: 12px; }
.ca-td-actions { display: flex; gap: 12px; }

.ca-op-link {
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: opacity 0.12s;
}

.ca-op-link:hover { opacity: 0.8; }
.ca-op-green { color: var(--success); }
.ca-op-danger { color: var(--error); }

/* 弹窗 */
.ca-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.ca-modal-card {
  background: var(--card);
  border-radius: 16px;
  width: 420px;
  max-width: 90vw;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.ca-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border-light);
}

.ca-modal-header h3 { font-size: 16px; font-weight: 700; color: var(--text); margin: 0; }

.ca-modal-close {
  background: none;
  border: none;
  font-size: 22px;
  color: var(--text-dim);
  cursor: pointer;
  line-height: 1;
}

.ca-modal-body { padding: 20px 24px; }

.ca-modal-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding: 16px 24px;
  border-top: 1px solid var(--border-light);
}

/* 夜间模式 */
html.dark .ca-modal-overlay { background: rgba(0, 0, 0, 0.6); }
html.dark .ca-sidebar { border-right-color: var(--border); }
html.dark .ca-dark-btn:hover { border-color: var(--pink-light); color: var(--pink-light); }
html.dark .ca-cancel-btn:hover { border-color: var(--text-dim); color: var(--text); }
html.dark .ca-upload-btn:hover { border-color: var(--pink); color: var(--pink); }
html.dark .ca-avatar-fb { color: #fff; }
html.dark .ca-hd-fb { color: #fff; }
</style>
