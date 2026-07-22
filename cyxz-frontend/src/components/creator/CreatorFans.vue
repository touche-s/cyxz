<template>
  <div class="creator-fans">
    <header class="page-header">
      <div class="header-left">
        <h1>粉丝管理</h1>
        <p>管理你的粉丝关系</p>
      </div>
    </header>

    <div class="fans-tabs">
      <button class="fans-tab-btn" :class="{ active: activeFansTab === 'followers' }" @click="switchFansTab('followers')">
        我的粉丝 <span class="tab-badge">{{ followerCount }}</span>
      </button>
      <button class="fans-tab-btn" :class="{ active: activeFansTab === 'following' }" @click="switchFansTab('following')">
        我的关注 <span class="tab-badge">{{ followingCount }}</span>
      </button>
    </div>

    <div class="search-bar">
      <SearchInput v-model="fansSearchKeyword" variant="inline" placeholder="搜索粉丝昵称..." />
    </div>

    <div class="fans-list" v-if="!fansLoading">
      <div class="fan-item" v-for="fan in filteredFansList" :key="fan.userId">
        <div class="fan-avatar clickable" @click="goToUser(fan.userId)">
          <img v-if="fan.avatar" :src="fan.avatar" alt="" />
          <div v-else class="avatar-placeholder">👤</div>
        </div>
        <div class="fan-info">
          <h4 class="fan-name clickable" @click="goToUser(fan.userId)">{{ fan.nickname || '未知用户' }}</h4>
          <span class="fan-time">{{ formatTime(fan.createTime) }}</span>
        </div>
        <FollowButton :following="fan.following"
                text="回关"
                variant="list"
                @click="handleFollow(fan.userId, fan.following)" />
      </div>
    </div>

    <LoadingSpinner v-else text="加载中..." />

    <EmptyState
      v-if="!fansLoading && filteredFansList.length === 0"
      icon="ph:tray"
      :title="fansSearchKeyword ? '没有匹配的粉丝' : (activeFansTab === 'followers' ? '还没有粉丝' : '还没有关注的人')"
      :hint="fansSearchKeyword ? '' : (activeFansTab === 'followers' ? '发布更多优质内容，吸引粉丝关注' : '去发现有趣的内容和人吧')"
    />

    <Pagination
      v-if="fansTotal > fansPageSize"
      :current="fansPage"
      :total="fansTotal"
      :page-size="fansPageSize"
      @change="handleFansPageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Icon } from '@iconify/vue'

import { useNavigate } from '@/composables/useNavigate'
import { getFollowerList, getFollowingList, followUser, unfollowUser, getFollowStats } from '@/api/user'
import type { FollowUserVO } from '@/api/user'
import { formatTime } from '@/utils/format'
import SearchInput from '@/components/SearchInput.vue'
import FollowButton from '@/components/FollowButton.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import EmptyState from '@/components/EmptyState.vue'
import Pagination from '@/components/Pagination.vue'

const { open } = useNavigate()

const activeFansTab = ref<'followers' | 'following'>('followers')
const followerCount = ref(0)
const followingCount = ref(0)
const fansLoading = ref(false)

const fansList = ref<FollowUserVO[]>([])
const fansTotal = ref(0)
const fansPage = ref(1)
const fansPageSize = 10
const fansSearchKeyword = ref('')

const filteredFansList = computed(() => {
  const kw = fansSearchKeyword.value.trim().toLowerCase()
  if (!kw) return fansList.value
  return fansList.value.filter(f => (f.nickname || '').toLowerCase().includes(kw))
})

const loadFans = async () => {
  fansLoading.value = true
  try {
    const data = activeFansTab.value === 'followers'
      ? await getFollowerList({ page: fansPage.value, size: fansPageSize })
      : await getFollowingList({ page: fansPage.value, size: fansPageSize })
    fansList.value = data.records || []
    fansTotal.value = data.total || 0
  } catch (error) {
    console.error('加载粉丝列表失败:', error)
  } finally {
    fansLoading.value = false
  }
}

const loadFollowStats = async () => {
  try {
    const stats = await getFollowStats()
    followerCount.value = stats.followerCount || 0
    followingCount.value = stats.followingCount || 0
  } catch (error) {
    console.error('加载关注统计失败:', error)
  }
}

const switchFansTab = (tab: 'followers' | 'following') => {
  if (activeFansTab.value === tab) return
  activeFansTab.value = tab
  fansPage.value = 1
  fansSearchKeyword.value = ''
  loadFans()
}

const handleFansPageChange = (page: number) => {
  fansPage.value = page
  loadFans()
}

const handleFollow = async (userId: string, isFollowing: boolean) => {
  try {
    if (isFollowing) {
      await unfollowUser(userId)
      ElMessage.success('已取消关注')
    } else {
      await followUser(userId)
      ElMessage.success('关注成功')
    }
    await loadFans()
    await loadFollowStats()
  } catch (error) {
    console.error('关注操作失败:', error)
    ElMessage.error('操作失败')
  }
}

const goToUser = (userId: string | number) => {
  open(`/user/${userId}`)
}

onMounted(() => {
  loadFollowStats()
  loadFans()
})
</script>

<style scoped>
.creator-fans {
  background: var(--card);
  border-radius: 20px;
  padding: 28px;
  border: 1.5px solid var(--border);
  box-shadow: 0 4px 12px rgba(180, 132, 255, 0.06);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

.page-header h1 {
  font-size: 24px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.header-left p {
  font-size: 13px;
  color: var(--text-dim);
  margin-top: 4px;
}

.fans-tabs {
  display: flex;
  gap: 6px;
  background: var(--pink-bg);
  border-radius: 12px;
  padding: 4px;
  margin-top: 0;
  width: fit-content;
}

.fans-tab-btn {
  padding: 7px 16px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  background: transparent;
  color: var(--text-dim);
  transition: all 0.22s ease-out;
  display: flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}

.fans-tab-btn:hover:not(.active) {
  background: var(--pink-bg-hover);
  color: var(--pink);
}

.fans-tab-btn.active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.15), rgba(180, 132, 255, 0.15));
  color: var(--pink);
}

.tab-badge {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.06);
  font-weight: 600;
  line-height: 1.6;
  display: inline-flex;
  align-items: center;
}

.fans-tab-btn.active .tab-badge {
  background: rgba(255, 255, 255, 0.3);
}

.search-bar {
  margin-top: 20px;
}

.fans-list {
  margin-top: 24px;
}

.fan-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  background: var(--pink-bg);
  border: 1.5px solid transparent;
  transition: all 0.22s ease-out;
  margin-bottom: 12px;
}

.fan-item:hover {
  border-color: var(--border);
}

.fan-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.fan-avatar.clickable,
.fan-name.clickable {
  cursor: pointer;
  transition: opacity 0.15s;
}

.fan-avatar.clickable:hover,
.fan-name.clickable:hover {
  opacity: 0.75;
}

.fan-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #ffd1dc, #e6e6fa);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.fan-info {
  flex: 1;
  min-width: 0;
}

.fan-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 4px;
}

.fan-time {
  font-size: 12px;
  color: var(--text-dim);
}
</style>
