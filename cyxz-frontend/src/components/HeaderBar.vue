<template>
  <header class="header-bar">
    <div class="header-left">
      <div class="logo-wrap">
        <div class="logo-icon">
          <img src="/favicon.svg" alt="logo" class="logo-img" />
        </div>
        <span class="logo-text">次元小站</span>
      </div>
    </div>
    <div class="header-center" :class="{ 'header-center--hidden': $route.path === '/search' }">
      <SearchInput v-model="searchText" variant="header" placeholder="搜索感兴趣的内容..." @search="goToSearch" />
    </div>
    <nav class="nav">
      <router-link to="/" :class="{ active: $route.path === '/' }">发现</router-link>
      <router-link to="/following" :class="{ active: $route.path === '/following' }">关注</router-link>
      <router-link to="/community" :class="{ active: $route.path === '/community' }">社区</router-link>
      <a href="javascript:;" class="nav-disabled">热门</a>
      <a href="javascript:;" class="nav-disabled">商城</a>
      <a href="javascript:;" :class="{ active: $route.path === '/creator' }" @click="goCreator">创作中心</a>
    </nav>
    <div class="header-right">
      <div v-if="userStore.isLoggedIn">
        <div class="user-dropdown" :class="{ open: dropdownOpen }">
          <div class="avatar-trigger" @click.stop="goToProfile" @mouseenter="dropdownOpen = true" @mouseleave="dropdownOpen = false">
            <img v-if="userStore.userInfo?.avatar" :src="userStore.userInfo.avatar" alt="avatar" class="avatar-img" />
            <span v-else class="avatar-placeholder">{{ (userStore.userInfo?.nickname || 'U').charAt(0) }}</span>
          </div>
          <Transition name="drop">
            <div v-if="dropdownOpen" class="dropdown-panel" @mouseenter="dropdownOpen = true" @mouseleave="dropdownOpen = false">
              <div class="panel-top">
                <div class="panel-user-info">
                  <span class="panel-nickname">{{ userStore.userInfo?.nickname || '用户' }}</span>
                  <span class="panel-uid">UID: {{ userStore.userInfo?.id || '-' }}</span>
                </div>
              </div>
              <div class="panel-stats">
                <div class="stat-item" @click="goFans('following')">
                  <span class="stat-num">{{ followStats.following }}</span>
                  <span class="stat-label">关注</span>
                </div>
                <div class="stat-item" @click="goFans('followers')">
                  <span class="stat-num">{{ followStats.followers }}</span>
                  <span class="stat-label">粉丝</span>
                </div>
              </div>
              <div class="panel-menu">
                <div class="menu-item" @click="handleCommand('user-center')">
                  <Icon icon="ph:user" class="menu-icon" />
                  <span>个人中心</span>
                  <Icon icon="ph:caret-right" class="menu-arrow" />
                </div>
                <div class="menu-divider"></div>
                <div class="menu-item logout" @click="handleCommand('logout')">
                  <Icon icon="ph:sign-out" class="menu-icon" />
                  <span>退出登录</span>
                </div>
              </div>
            </div>
          </Transition>
        </div>
      </div>
      <div v-else class="login-circle" @click="userStore.openLoginModal()">
        <span>登录</span>
      </div>
      <div class="header-icons">
        <button class="header-action" @click="goPrivateMessages">
          <Icon icon="ph:chat-circle-text" class="action-iconify" />
          <span class="action-label">私信</span>
        </button>
        <button class="header-action" @click="goMessages">
          <Icon icon="ph:bell" class="action-iconify" />
          <span class="action-label">通知</span>
        </button>
        <button class="header-action" @click="goFavorites">
          <Icon icon="ph:star" class="action-iconify" />
          <span class="action-label">收藏</span>
        </button>
        <button class="header-action" @click="toggleDarkMode">
          <Icon icon="ph:moon" v-if="!isDark" class="action-iconify" />
          <Icon icon="ph:sun" v-else class="action-iconify" />
        </button>
      </div>
      <button class="btn-create" @click="goPublish">
        <Icon icon="ph:pencil-simple" class="btn-iconify" />
        发布
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import SearchInput from '@/components/SearchInput.vue'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'
import { useNavigate } from '@/composables/useNavigate'
import { logout } from '@/api/auth'
import { getFollowStats } from '@/api/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const { requireLogin } = useAuth()
const { open, openWithQuery, to } = useNavigate()

const dropdownOpen = ref(false)
const followStats = ref({ following: 0, followers: 0 })
const searchText = ref('')
const isDark = ref(false)

function toggleDarkMode() {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('darkMode', isDark.value ? '1' : '0')
}

function goCreator() {
  if (!requireLogin()) return
  dropdownOpen.value = false
  to('/creator')
}

function goToSearch() {
  const kw = searchText.value.trim()
  if (!kw) return
  openWithQuery('/search', { q: kw })
  searchText.value = ''
}

function goFavorites() {
  if (!requireLogin()) return
  const uid = userStore.userInfo?.id
  if (uid) {
    openWithQuery(`/user/${uid}`, { tab: 'favorites' })
  }
}

function goMessages() {
  if (!requireLogin()) return
  to('/messages')
}

function goPrivateMessages() {
  if (!requireLogin()) return
  to('/messages')
}

function goPublish() {
  if (!requireLogin()) return
  userStore.creatorActiveNav = 'publish'
  to('/creator')
}

/** 进入粉丝管理页面对应 tab */
function goFans(tab: 'followers' | 'following') {
  dropdownOpen.value = false
  if (!requireLogin()) return
  userStore.creatorActiveNav = 'fans'
  userStore.creatorFansTab = tab
  to('/creator')
}

/** 点击头像进入个人空间（新标签页） */
function goToProfile() {
  const uid = userStore.userInfo?.id
  if (uid) {
    open(`/user/${uid}`)
  }
  dropdownOpen.value = false
}

async function handleCommand(cmd: string) {
  dropdownOpen.value = false
  if (cmd === 'user-center') {
    open('/user-center')
  } else if (cmd === 'logout') {
    try { await logout() } catch { /* ignore */ }
    userStore.clearAuth()
    to('/')
    ElMessage.success('已退出登录')
  }
}

async function loadFollowStats() {
  if (!userStore.userInfo?.id) return
  try {
    const d = await getFollowStats()
    followStats.value = {
      following: d?.followingCount ?? 0,
      followers: d?.followerCount ?? 0,
    }
  } catch { /* ignore */ }
}

onMounted(() => {
  isDark.value = localStorage.getItem('darkMode') === '1'
  document.documentElement.classList.toggle('dark', isDark.value)
  loadFollowStats()
})
</script>

<style scoped>
.header-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: var(--header-bg);
  backdrop-filter: blur(24px) saturate(1.4);
  border-bottom: 1px solid var(--border);
  padding: 0 44px;
  height: 66px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.header-center {
  display: flex;
  align-items: center;
}

.header-center--hidden {
  visibility: hidden;
  pointer-events: none;
}

.header-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.logo-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.3);
  transition: all 0.22s ease-out;
}
.logo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.logo-wrap:hover .logo-icon {
  box-shadow: 0 4px 24px rgba(255, 107, 157, 0.45);
  transform: scale(1.05);
}

.logo-text {
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 1px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 20px rgba(255, 107, 157, 0.15);
}

.nav {
  display: flex;
  gap: 4px;
  align-items: center;
}

.nav a {
  text-decoration: none;
  color: var(--text-dim);
  font-size: 13px;
  font-weight: 600;
  padding: 8px 16px;
  border-radius: 12px;
  transition: all 0.22s ease-out;
  position: relative;
}

.nav a:hover {
  color: var(--purple);
}

.nav a.active {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 700;
}
.nav a.active::after {
  content: '';
  position: absolute;
  bottom: 2px;
  left: 16px;
  right: 16px;
  height: 3px;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--pink), var(--purple));
}

.nav a.nav-disabled {
  cursor: default;
  opacity: 0.45;
}

.nav a.nav-disabled:hover {
  color: var(--text-dim);
}

.btn-create {
  padding: 9px 20px;
  border-radius: 14px;
  border: none;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.25);
  transition: all 0.22s ease-out;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-create:hover {
  transform: scale(1.04);
  box-shadow: 0 6px 24px rgba(180, 132, 255, 0.35);
}
.btn-create:active {
  transform: scale(0.97);
}
.btn-iconify {
  width: 16px;
  height: 16px;
  color: white;
}

.login-circle {
   width: 40px;
   height: 40px;
   border-radius: 50%;
   cursor: pointer;
   border: 2px solid rgba(255, 107, 157, 0.35);
   background: var(--card);
   display: flex;
   align-items: center;
   justify-content: center;
   transition: all 0.22s ease-out;
   flex-shrink: 0;
   margin: 0 18px;
 }

.login-circle span {
  font-size: 12px;
  font-weight: 700;
  color: var(--pink);
  line-height: 1;
}

.login-circle:hover {
   border-color: var(--pink);
   box-shadow: 0 4px 20px rgba(255, 107, 157, 0.2);
   transform: scale(1.08);
 }

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.header-icons {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 0 8px;
}

.header-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  width: 46px;
  height: 54px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 14px;
  padding: 0;
  transition: background 0.2s ease;
}

.header-action:hover {
}

.action-iconify {
  width: 19px;
  height: 19px;
  color: var(--text-dim);
  line-height: 1;
  transition: color 0.2s ease, transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.header-action:hover .action-iconify {
  color: var(--pink);
  transform: translateY(-2px);
}

.action-label {
  font-size: 11px;
  line-height: 1;
  color: var(--text-dim);
  transition: color 0.2s ease;
  white-space: nowrap;
}

.header-action:hover .action-label {
  color: var(--pink);
}

.user-dropdown {
  position: relative;
  margin: 0 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-trigger {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  cursor: pointer;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.95);
  transition: transform 0.24s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.24s ease, border-color 0.24s ease;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 301;
  flex-shrink: 0;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.22);
}

.user-dropdown.open .avatar-trigger {
  transform: translateY(18px) scale(1.56);
  border-color: #fff;
  box-shadow: 0 10px 24px rgba(255, 107, 157, 0.24);
}

.avatar-placeholder {
  color: white;
  font-size: 14px;
  font-weight: 800;
}

.dropdown-panel {
  position: absolute;
  top: calc(100% - 8px);
  left: 50%;
  transform: translateX(-50%);
  width: 260px;
  background: var(--card);
  border-radius: 18px;
  border: 1.5px solid var(--border-light);
  box-shadow: 0 18px 48px rgba(255, 107, 157, 0.10), 0 2px 10px rgba(255, 107, 157, 0.06);
  z-index: 200;
  overflow: visible;
  padding-top: 38px;
}

.drop-enter-active {
  transition: all 0.24s cubic-bezier(0.4, 0, 0.2, 1);
}
.drop-leave-active {
  transition: all 0.16s ease;
}
.drop-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px) scale(0.97);
}
.drop-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-6px) scale(0.98);
}

.panel-top {
  padding: 0 16px 12px;
  text-align: center;
}

.panel-user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.panel-nickname {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.panel-uid {
  font-size: 11px;
  color: var(--text-dim);
}

.panel-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  padding: 12px 12px 10px;
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.stat-item:hover .stat-num,
.stat-item:hover .stat-label {
  color: var(--pink);
}

.stat-num {
  font-size: 17px;
  line-height: 1;
  font-weight: 700;
  color: var(--text);
  transition: color 0.2s;
}

.stat-label {
  font-size: 12px;
  color: var(--text-dim);
  transition: color 0.2s;
}

.panel-menu {
  padding: 6px 0 8px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
  color: var(--text);
  font-size: 14px;
  line-height: 1;
}

.menu-item:hover {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.08), rgba(180, 132, 255, 0.06));
  color: var(--pink);
}

.menu-item:hover .menu-icon,
.menu-item:hover .menu-arrow {
  stroke: var(--pink);
}

.menu-icon {
  width: 21px;
  height: 21px;
  flex-shrink: 0;
  stroke: var(--text-dim);
  transition: stroke 0.2s;
}

.menu-arrow {
  width: 16px;
  height: 16px;
  margin-left: auto;
  stroke: var(--text-dim);
  flex-shrink: 0;
  transition: stroke 0.2s;
}

.menu-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 107, 157, 0.2), transparent);
  margin: 8px 16px;
}

.menu-item.logout {
  color: #e85b7d;
}

.menu-item.logout .menu-icon {
  color: #e85b7d;
}

.menu-item.logout:hover {
  background: rgba(232, 91, 125, 0.08);
  color: #e85b7d;
}

@media (max-width: 768px) {
  .header-bar {
    padding: 0 16px;
    height: auto;
    min-height: 66px;
    flex-wrap: wrap;
    gap: 10px;
  }
  .header-center {
    order: 3;
    width: 100%;
  }
  .nav { display: none; }
  .header-right {
    gap: 6px;
  }
  .header-icons {
    margin: 0;
    gap: 6px;
  }
  .btn-create {
    padding: 8px 14px;
  }
}
</style>
