<template>
  <header class="header-bar">
    <div class="header-left">
      <div class="logo-wrap">
        <div class="logo-icon"></div>
        <span class="logo-text">次元小站</span>
      </div>
    </div>
    <div class="header-center">
      <div class="search-wrap">
        <el-icon><Search /></el-icon>
        <input type="text" placeholder="搜索感兴趣的内容..." />
      </div>
      <nav class="nav">
        <router-link to="/" :class="{ active: $route.path === '/' }">首页</router-link>
        <router-link to="/discover" :class="{ active: $route.path === '/discover' }">发现</router-link>
        <router-link to="/following" :class="{ active: $route.path === '/following' }">关注</router-link>
        <router-link to="/community" :class="{ active: $route.path === '/community' }">社区</router-link>
      </nav>
    </div>
    <div class="header-right">
      <button class="btn-create" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        发布
      </button>
      <template v-if="userStore.isLoggedIn">
        <el-dropdown trigger="click" @command="handleCommand">
          <div class="avatar">{{ (userStore.userInfo?.nickname || 'U').charAt(0) }}</div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><UserFilled /></el-icon>个人中心
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
      <button v-else class="btn-login" @click="$emit('openLogin')">登录</button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { Search, Plus, UserFilled, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { logout } from '@/api/auth'
import { ElMessage } from 'element-plus'

defineEmits<{ openLogin: [] }>()

const userStore = useUserStore()
const router = useRouter()

function handleCreate() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
}

async function handleCommand(cmd: string) {
  if (cmd === 'profile') {
    const uid = userStore.userInfo?.id
    if (uid) router.push(`/user/${uid}`)
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
  justify-content: center;
}

.header-left {
  position: absolute;
  left: 44px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.header-center {
  display: flex;
  align-items: center;
  gap: 32px;
}

.header-right {
  position: absolute;
  right: 44px;
  display: flex;
  gap: 10px;
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
  background: linear-gradient(135deg, var(--pink), var(--purple));
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.3);
}

.logo-text {
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 1px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.search-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 240, 247, 0.7);
  border: 1.5px solid rgba(255, 182, 215, 0.3);
  border-radius: 14px;
  padding: 9px 16px;
  transition: all 0.3s;
}

.search-wrap:focus-within {
  border-color: var(--pink);
  background: white;
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.08);
}

.search-wrap .el-icon {
  color: #c4a0b8;
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
  gap: 6px;
}

.nav a {
  text-decoration: none;
  color: var(--text-dim);
  font-size: 13px;
  font-weight: 600;
  padding: 8px 16px;
  border-radius: 12px;
  transition: all 0.25s;
}

.nav a:hover {
  color: var(--pink);
  background: rgba(255, 107, 157, 0.06);
}

.nav a.active {
  color: var(--pink);
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(192, 132, 252, 0.1));
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
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-create:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(255, 107, 157, 0.35);
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
}

@media (max-width: 768px) {
  .header-bar { padding: 0 16px; }
  .nav { display: none; }
}
</style>
