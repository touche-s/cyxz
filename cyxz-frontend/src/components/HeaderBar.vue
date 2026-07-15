<template>
  <header class="header-bar">
    <div class="header-left">
      <div class="logo-wrap">
        <div class="logo-icon">
          <img src="@/assets/logo.png" alt="logo" class="logo-img" />
        </div>
        <span class="logo-text">次元小站</span>
      </div>
    </div>
    <div class="header-center">
      <div class="search-wrap">
        <el-icon><Search /></el-icon>
        <input type="text" placeholder="搜索感兴趣的内容..." />
      </div>
    </div>
    <div class="header-right">
      <nav class="nav">
        <router-link to="/" :class="{ active: $route.path === '/' }">首页</router-link>
        <router-link to="/discover" :class="{ active: $route.path === '/discover' }">发现</router-link>
        <router-link to="/following" :class="{ active: $route.path === '/following' }">关注</router-link>
        <router-link to="/community" :class="{ active: $route.path === '/community' }">社区</router-link>
        <a href="javascript:;" :class="{ active: $route.path === '/creator' }" @click="goCreator">创作中心</a>
        <template v-if="userStore.isLoggedIn">
          <el-dropdown trigger="hover" @command="handleCommand" placement="bottom">
            <div class="nav-avatar">
              <img v-if="userStore.userInfo?.avatar" :src="userStore.userInfo.avatar" alt="avatar" class="avatar-img" />
              <span v-else>{{ (userStore.userInfo?.nickname || 'U').charAt(0) }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><UserFilled /></el-icon>个人空间
                </el-dropdown-item>
                <el-dropdown-item command="user-center">
                  <el-icon><Setting /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <button v-else class="btn-login" @click="userStore.openLoginModal()">登录</button>
      </nav>
      <div class="header-icons">
        <button class="icon-btn"><el-icon><Star /></el-icon></button>
        <button class="icon-btn"><el-icon><ChatLineSquare /></el-icon></button>
        <button class="icon-btn"><el-icon><Bell /></el-icon></button>
      </div>
      <button class="btn-create" @click="goPublish">
        <el-icon><Plus /></el-icon>
        发布
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { Search, Plus, UserFilled, SwitchButton, EditPen, Setting, Bell, ChatLineSquare, Star } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { logout } from '@/api/auth'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()

function goCreator() {
  if (!userStore.isLoggedIn) {
    userStore.openLoginModal()
    return
  }
  router.push('/creator')
}

function goPublish() {
  if (!userStore.isLoggedIn) {
    userStore.openLoginModal()
    return
  }
  userStore.creatorActiveNav = 'publish'
  router.push('/creator')
}

async function handleCommand(cmd: string) {
  if (cmd === 'profile') {
    const uid = userStore.userInfo?.id
    if (uid) router.push(`/user/${uid}`)
  } else if (cmd === 'user-center') {
    router.push('/user-center')
  } else if (cmd === 'creator') {
    router.push('/creator')
  } else if (cmd === 'logout') {
    try { await logout() } catch { /* ignore */ }
    userStore.clearAuth()
    router.push('/')
    ElMessage.success('已退出登录')
  }
}
</script>

<style scoped>
.header-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: rgba(254, 246, 255, 0.85);
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

.search-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 240, 247, 0.7);
  border: 1.5px solid rgba(255, 138, 200, 0.2);
  border-radius: 12px;
  padding: 9px 16px;
  transition: all 0.22s ease-out;
}

.search-wrap:focus-within {
  border-color: #B484FF;
  background: white;
  box-shadow: 0 0 0 3px rgba(180, 132, 255, 0.1);
}

.search-wrap .el-icon {
  color: #B484FF;
  transition: color 0.22s ease-out;
}

.search-wrap input {
  border: none;
  background: none;
  outline: none;
  font-size: 13px;
  width: 220px;
  color: var(--text);
}

.search-wrap input::placeholder {
  color: #c4a0b8;
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
  color: #B484FF;
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

.btn-login {
  padding: 9px 20px;
  border-radius: 14px;
  border: 1.5px solid var(--border);
  background: white;
  color: var(--pink);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-login:hover {
  border-color: var(--pink);
}

.avatar {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  cursor: pointer;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 15px;
  font-weight: 800;
  border: 2.5px solid white;
  box-shadow: var(--shadow);
  overflow: hidden;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.nav-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  font-weight: 800;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-avatar:hover {
  transform: scale(1.15);
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.3);
}

.nav-avatar .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.header-icons {
  display: flex;
  gap: 4px;
}

.icon-btn {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease-out;
}

.icon-btn .el-icon {
  color: var(--text-dim);
  font-size: 18px;
}

.icon-btn:hover {
  background: rgba(0, 0, 0, 0.06);
}

.icon-btn:hover .el-icon {
  color: var(--pink);
}

:deep(.el-dropdown-menu) {
  border-radius: 14px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(0, 0, 0, 0.04);
  border: none;
  padding: 8px 0;
  min-width: 170px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  margin-top: 10px !important;
}

:deep(.el-dropdown-menu__item) {
  padding: 12px 20px;
  font-size: 14px;
  color: #333;
  border-radius: 10px;
  margin: 2px 10px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 10px;
}

:deep(.el-dropdown-menu__item:hover) {
  background: linear-gradient(135deg, rgba(255, 182, 193, 0.18), rgba(180, 132, 255, 0.18));
  color: var(--pink);
  transform: translateX(4px);
}

:deep(.el-dropdown-menu__item--divided) {
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  margin-top: 6px;
  padding-top: 14px;
}

@media (max-width: 768px) {
  .header-bar { padding: 0 16px; }
  .nav { display: none; }
}
</style>
