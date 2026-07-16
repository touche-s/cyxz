<template>
  <div class="creator-container">
    <aside class="sidebar">
      <nav class="sidebar-nav">
        <div class="nav-section">
        <button class="nav-item nav-item-primary" :class="{ active: activeNav === 'publish' }" @click="goPublish">
          <img src="@/assets/icons/edit.svg" class="nav-icon" />
          <span class="nav-text">发布</span>
        </button>
        <button class="nav-item" :class="{ active: activeNav === 'home' }" @click="activeNav = 'home'">
          <img src="@/assets/icons/home-nav.svg" class="nav-icon" />
          <span class="nav-text">创作首页</span>
        </button>
      </div>

        <div class="nav-divider"></div>

        <div class="nav-section">
          <button class="nav-item" :class="{ active: activeNav === 'content' }" @click="activeNav = 'content'">
            <img src="@/assets/icons/content-nav.svg" class="nav-icon" />
            <span class="nav-text">内容管理</span>
          </button>
        </div>

        <div class="nav-divider"></div>

        <div class="nav-section">
          <button class="nav-item" :class="{ active: activeNav === 'fans' }" @click="activeNav = 'fans'">
            <img src="@/assets/icons/fans-nav.svg" class="nav-icon" />
            <span class="nav-text">粉丝管理</span>
          </button>
          <button class="nav-item" :class="{ active: activeNav === 'interaction' }" @click="activeNav = 'interaction'">
            <img src="@/assets/icons/interaction-nav.svg" class="nav-icon" />
            <span class="nav-text">评论管理</span>
          </button>
        </div>

        <div class="nav-divider"></div>

        <div class="nav-section">
          <button class="nav-item" :class="{ active: activeNav === 'magic' }" @click="activeNav = 'magic'">
            <img src="@/assets/icons/magic-nav.svg" class="nav-icon" />
            <span class="nav-text">妙笔</span>
          </button>
        </div>

        <div class="nav-divider"></div>

        <div class="nav-section">
          <button class="nav-item" :class="{ active: activeNav === 'agreement' }" @click="activeNav = 'agreement'">
            <img src="@/assets/icons/community-agreement-nav.svg" class="nav-icon" />
            <span class="nav-text">社区公约</span>
          </button>
        </div>
      </nav>
    </aside>

    <main class="main-content">
      <PostCreate v-if="activeNav === 'publish'" @go-back="goHome" />
      <template v-else-if="activeNav === 'home'">
        <div class="home-hero">
          <div class="hero-info">
            <h1>创作中心</h1>
            <p>记录你的灵感瞬间，发布图文作品，和同好一起交流</p>
          </div>
          <button class="hero-publish-btn" @click="goCreate">
            <img src="@/assets/icons/edit.svg" alt="edit" class="btn-icon" />
            <span>发布新作品</span>
          </button>
        </div>

        <div class="stats-section">
          <h3 class="section-title">数据概览</h3>
          <div class="stats-grid">
            <StatCard icon-class="works-icon" :icon="iconEdit" :value="dataStats?.totalPosts ?? 0" label="总作品" />
            <StatCard icon-class="views-icon" :icon="iconEye" :value="formatNumber(dataStats?.totalViews ?? 0)" label="总浏览" />
            <StatCard icon-class="likes-icon" :icon="iconLike" :value="formatNumber(dataStats?.totalLikes ?? 0)" label="总点赞" />
            <StatCard icon-class="collections-icon" :icon="iconFavorite" :value="formatNumber(dataStats?.totalCollections ?? 0)" label="总收藏" />
            <StatCard icon-class="fans-icon" :icon="iconFans" :value="formatNumber(followerCount)" label="粉丝数" />
            <StatCard icon-class="comments-icon" :icon="iconComment" :value="formatNumber(commentsTotal)" label="评论数" />
          </div>
        </div>

        <div class="recent-section" v-if="recentPosts.length > 0">
          <h3 class="section-title">最近作品</h3>
          <div class="recent-posts-list">
            <div class="recent-post-item" v-for="post in recentPosts" :key="post.id" @click="viewPost(post.id)">
              <div class="recent-post-cover">
                <img v-if="post.cover" :src="post.cover" alt="" />
                <div v-else class="cover-placeholder-small">📷</div>
              </div>
              <div class="recent-post-info">
                <h4 class="recent-post-title">{{ post.title }}</h4>
                <span class="recent-post-time">{{ formatTime(post.createTime) }}</span>
              </div>
              <div class="recent-post-stats">
                <span class="stat-item"><img src="@/assets/icons/eye.svg" alt="eye" class="stat-mini-icon" />{{ post.views }}</span>
                <span class="stat-item"><img src="@/assets/icons/like.svg" alt="like" class="stat-mini-icon" />{{ post.likes }}</span>
                <span class="stat-item"><img src="@/assets/icons/favorite.svg" alt="favorite" class="stat-mini-icon" />{{ post.collections }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="recent-section">
          <h3 class="section-title">热门作品</h3>
          <div class="ranking-list">
            <div class="ranking-item" v-for="(item, index) in rankingList" :key="item.id" @click="viewPost(item.id)">
              <div class="rank-num" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
              <div class="rank-cover">
                <img v-if="item.cover" :src="item.cover" alt="" />
                <div v-else class="cover-placeholder-small">📷</div>
              </div>
              <div class="rank-info">
                <h4>{{ item.title }}</h4>
                <span class="rank-time">{{ formatDateTime(item.createTime) }}</span>
                <div class="rank-stats">
                  <span class="stat-item"><img src="@/assets/icons/eye.svg" alt="eye" class="stat-mini-icon" />{{ item.views }}</span>
                  <span class="stat-item"><img src="@/assets/icons/like.svg" alt="like" class="stat-mini-icon" />{{ item.likes }}</span>
                  <span class="stat-item"><img src="@/assets/icons/favorite.svg" alt="favorite" class="stat-mini-icon" />{{ item.collections }}</span>
                </div>
              </div>
            </div>
            <EmptyState v-if="rankingList.length === 0" title="还没有发布作品" />
          </div>
        </div>

      </template>

      <template v-else-if="activeNav === 'content'">
        <div class="page-container">
          <header class="page-header">
            <div class="header-left">
              <h1>内容管理</h1>
              <p>管理你的所有作品</p>
            </div>
            <button class="publish-btn" @click="goCreate">
              <img src="@/assets/icons/edit.svg" alt="edit" class="btn-icon" />
              <span>发布新作品</span>
            </button>
          </header>

          <div class="filter-bar">
            <div class="filter-group">
              <button 
                v-for="tab in contentTabs" 
                :key="tab.value"
                class="filter-btn"
                :class="{ active: activeContentTab === tab.value }"
                @click="activeContentTab = tab.value"
              >
                {{ tab.label }}
                <span class="filter-count">{{ tab.count }}</span>
              </button>
            </div>
            <button class="refresh-btn" :class="{ spinning: loading }" @click="refreshPosts" title="刷新列表">
              <img src="@/assets/icons/refresh.svg" alt="refresh" class="refresh-icon" />
            </button>
          </div>

          <div class="search-bar">
            <div class="search-box">
              <img src="@/assets/icons/search.svg" alt="search" class="search-icon" />
              <input type="text" placeholder="搜索当前分类下的作品标题..." class="search-input" v-model="searchKeyword" />
            </div>
          </div>

          <div class="content-list" v-if="!loading && filteredContentPosts.length > 0">
            <div class="content-item" v-for="post in filteredContentPosts" :key="post.id">
              <div class="content-cover" @click="viewPost(post.id)">
                <img v-if="post.cover" :src="post.cover" alt="cover" />
                <div v-else class="cover-placeholder">
                  <span>暂无封面</span>
                </div>
              </div>
              <div class="content-info">
                <h3 class="content-title" @click="viewPost(post.id)">{{ post.title }}</h3>
                <div class="content-meta">
                  <span class="category-tag" v-if="post.categoryName">{{ post.categoryName }}</span>
                  <span class="content-time">{{ formatDateTime(post.createTime) }}</span>
                </div>
                <div class="content-stats">
                  <span class="stat-item"><img src="@/assets/icons/eye.svg" alt="eye" class="stat-mini-icon" />{{ post.views }}</span>
                  <span class="stat-item"><img src="@/assets/icons/like.svg" alt="like" class="stat-mini-icon" />{{ post.likes }}</span>
                  <span class="stat-item"><img src="@/assets/icons/favorite.svg" alt="favorite" class="stat-mini-icon" />{{ post.collections }}</span>
                </div>
              </div>
              <div class="content-status">
                <span class="status-tag" :class="'status-' + post.status">
                  {{ statusText(post.status) }}
                </span>
              </div>
              <div class="content-actions">
                <button class="action-btn edit" @click="editPost(post.id)" title="编辑">
                  <img src="@/assets/icons/edit.svg" alt="edit" class="action-icon" />
                </button>
                <button 
                  v-if="post.status === 0" 
                  class="action-btn publish" 
                  @click="publishPost(post.id)" 
                  title="发布"
                >
                  <img src="@/assets/icons/rocket.svg" alt="rocket" class="action-icon" />
                </button>
                <button 
                  v-if="post.status === 2" 
                  class="action-btn restore" 
                  @click="restorePost(post.id)" 
                  title="恢复"
                >
                  <img src="@/assets/icons/refresh.svg" alt="refresh" class="action-icon" />
                </button>
                <button class="action-btn delete" @click="confirmDelete(post)" title="删除">
                  <img src="@/assets/icons/trash.svg" alt="trash" class="action-icon" />
                </button>
              </div>
            </div>
          </div>

          <EmptyState v-else-if="!loading" :icon="iconEmpty" :title="`还没有${activeContentTab === 'published' ? '已发布的' : activeContentTab === 'draft' ? '草稿' : activeContentTab === 'deleted' ? '已删除的' : ''}作品`">
            <template #actions>
              <button class="create-btn" @click="goCreate">去创作</button>
            </template>
          </EmptyState>

          <LoadingSpinner v-else text="加载中..." />
        </div>
      </template>

      <template v-else-if="activeNav === 'fans'">
        <div class="page-container">
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

          <div class="fans-list" v-if="!fansLoading">
            <div class="fan-item" v-for="fan in fansList" :key="fan.userId">
              <div class="fan-avatar">
                <img v-if="fan.avatar" :src="fan.avatar" alt="" />
                <div v-else class="avatar-placeholder">👤</div>
              </div>
              <div class="fan-info">
                <h4 class="fan-name">{{ fan.nickname || '未知用户' }}</h4>
                <span class="fan-time">{{ formatTime(fan.createTime) }}</span>
              </div>
              <FollowButton :following="fan.following"
                      text="回关"
                      variant="list"
                      @click="handleFollow(fan.userId, fan.following)" />
            </div>
          </div>

          <LoadingSpinner v-else text="加载中..." />

          <EmptyState v-if="!fansLoading && fansList.length === 0" :icon="iconEmpty" :title="activeFansTab === 'followers' ? '还没有粉丝' : '还没有关注的人'" :hint="activeFansTab === 'followers' ? '发布更多优质内容，吸引粉丝关注' : '去发现有趣的内容和人吧'" />

          <Pagination :current="fansPage" :total="fansTotal" :page-size="fansPageSize" @change="handleFansPageChange" />
        </div>
      </template>

      <template v-else-if="activeNav === 'interaction'">
        <div class="page-container">
          <header class="page-header">
            <div class="header-left">
              <h1>评论管理</h1>
              <p>管理你作品下的评论内容</p>
            </div>
          </header>

          <div class="filter-bar">
            <div class="filter-group comment-filter-group">
              <el-select
                v-model="selectedCommentPostId"
                placeholder="全部帖子"
                class="comment-post-select"
                clearable
                @change="handleCommentPostFilterChange"
              >
                <el-option value="" label="全部帖子" />
                <el-option
                  v-for="post in publishedPostOptions"
                  :key="post.id"
                  :value="post.id"
                  :label="post.title"
                />
              </el-select>
              <span class="comment-total">共 {{ commentsTotal }} 条评论</span>
            </div>
            <div class="sort-group">
              <button
                class="sort-btn"
                :class="{ active: !commentSortAsc }"
                @click="handleCommentSortChange(false)"
              >最新</button>
              <button
                class="sort-btn"
                :class="{ active: commentSortAsc }"
                @click="handleCommentSortChange(true)"
              >最早</button>
            </div>
          </div>

          <div class="comment-list" v-if="!commentsLoading && managedCommentsList.length > 0">
            <div class="comment-manage-item" v-for="comment in managedCommentsList" :key="comment.id">
              <div class="comment-avatar">
                <img v-if="comment.userAvatar" :src="comment.userAvatar" alt="" />
                <div v-else class="avatar-placeholder"></div>
              </div>
              <div class="comment-body">
                <div class="comment-top-row">
                  <span class="comment-name">{{ comment.userName }}</span>
                  <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                </div>
                <div class="comment-main-row">
                  <span class="comment-context">
                    在「<span class="context-post-title" @click="viewPost(comment.postId)" :title="comment.postTitle">{{ truncatePostTitle(comment.postTitle, comment.postId) }}</span>」中
                    <template v-if="comment.replyToUserName">
                      回复了 <span class="context-reply-to">@{{ comment.replyToUserName }}</span>
                    </template>
                    <template v-else>
                      发表了评论
                    </template>
                  </span>
                  <span class="comment-content">{{ comment.content }}</span>
                </div>
              </div>
              <button class="comment-delete-btn" @click="confirmDeleteManagedComment(comment)" title="删除评论">
                <img src="@/assets/icons/trash.svg" alt="delete" class="delete-icon" />
              </button>
            </div>
          </div>

          <LoadingSpinner v-else-if="commentsLoading" text="加载中..." />

          <EmptyState v-else :icon="iconEmpty" :title="selectedCommentPostId ? '当前帖子还没有评论' : '当前还没有人给你的作品留言'" />

          <Pagination :current="commentPage" :total="commentsTotal" :page-size="commentPageSize" @change="handleCommentPageChange" />
        </div>
      </template>

      <template v-else-if="activeNav === 'magic'">
        <div class="page-container magic-page">
          <header class="page-header">
            <div class="header-left">
              <h1><img src="@/assets/icons/sparkle.svg" alt="sparkle" class="title-icon" />妙笔</h1>
              <p>AI辅助创作工具</p>
            </div>
          </header>

          <div class="magic-banner">
            <div class="magic-banner-content">
              <h2>让AI帮你创作</h2>
              <p>智能选题、文案生成、排版优化</p>
            </div>
          </div>

          <div class="magic-features">
            <div class="magic-card" v-for="feature in magicFeatures" :key="feature.title">
              <img :src="feature.icon" :alt="feature.title" class="magic-icon" />
              <h3>{{ feature.title }}</h3>
              <p>{{ feature.desc }}</p>
              <button class="magic-btn">{{ feature.btnText }}</button>
            </div>
          </div>

          <div class="magic-tips">
            <div class="tips-header">
              <h3><img src="@/assets/icons/lightbulb.svg" alt="lightbulb" class="title-icon" />使用提示</h3>
            </div>
            <ul class="tips-list">
              <li>输入关键词或主题，AI会帮你生成相关选题</li>
              <li>可以选择不同风格的文案生成（轻松、正式、幽默等）</li>
              <li>生成的内容需要人工审核后再发布</li>
              <li>建议结合自己的风格进行修改和润色</li>
            </ul>
          </div>
        </div>
      </template>

      <template v-else-if="activeNav === 'agreement'">
        <div class="agreement-page">
          <div class="agreement-container">
            <header class="agreement-page-header">
              <h1><img src="@/assets/icons/info.svg" alt="info" class="title-icon" />社区公约</h1>
              <p>共同维护健康的社区环境</p>
            </header>

            <div class="agreement-intro">
              <p><strong>次元小站</strong>是一个由大家共同创建的内容社区。社区鼓励大家围绕各自的爱好认真创作和交流，结识同好，收获成长。为了能让所有用户享有开放、友善和有收获感的社区，我们希望以下能成为社区的共识：</p>
            </div>

            <div class="agreement-principles">
              <div class="principle-card">
                <img src="@/assets/icons/edit.svg" alt="edit" class="principle-icon" />
                <h3>认真创作</h3>
                <p>我们鼓励大家创作真实的内容，分享自己的见解和作品。尊重原创，不抄袭、不搬运，对自己发布的内容负责。</p>
              </div>
              <div class="principle-card">
                <img src="@/assets/icons/handshake.svg" alt="handshake" class="principle-icon" />
                <h3>友善交流</h3>
                <p>尊重不同观点，理性讨论。避免人身攻击和恶意言论，营造和谐友好的交流氛围。</p>
              </div>
              <div class="principle-card">
                <img src="@/assets/icons/shield.svg" alt="shield" class="principle-icon" />
                <h3>拥抱创新</h3>
                <p>鼓励探索新的表达方式和创作形式，支持创意和灵感的碰撞，让社区充满活力。</p>
              </div>
            </div>

            <div class="agreement-content">
              <div class="content-section">
                <h2>一、内容规范</h2>
                <div class="content-item">
                  <h4>1.1 禁止发布的内容</h4>
                  <ul>
                    <li>违法、违规、色情、暴力等不良内容</li>
                    <li>广告、营销推广等商业性质内容</li>
                    <li>虚假信息、谣言等不实内容</li>
                    <li>人身攻击、恶意诋毁等言论</li>
                    <li>侵犯他人隐私的内容</li>
                  </ul>
                </div>
                <div class="content-item">
                  <h4>1.2 内容审核机制</h4>
                  <p>平台有权对用户发布的内容进行审核，对于违反社区公约的内容，平台有权采取删除、屏蔽等措施。</p>
                </div>
              </div>

              <div class="content-section">
                <h2>二、版权声明</h2>
                <div class="content-item">
                  <h4>2.1 原创内容保护</h4>
                  <p>用户发布的内容需保证原创或拥有合法授权，禁止抄袭、盗用他人作品。</p>
                </div>
                <div class="content-item">
                  <h4>2.2 平台权利</h4>
                  <p>平台有权对侵权内容进行处理，并保留追究法律责任的权利。</p>
                </div>
              </div>

              <div class="content-section">
                <h2>三、社区行为</h2>
                <div class="content-item">
                  <h4>3.1 互动规范</h4>
                  <ul>
                    <li>尊重他人，友好交流</li>
                    <li>遵守平台规则，维护社区秩序</li>
                    <li>合理使用平台功能，不滥用资源</li>
                    <li>不进行恶意刷屏、灌水等行为</li>
                  </ul>
                </div>
              </div>

              <div class="content-section">
                <h2>四、违规处理</h2>
                <div class="content-item">
                  <h4>4.1 处理方式</h4>
                  <p>对于违反社区公约的用户，平台将根据情节轻重采取警告、限制功能、封禁账号等措施。</p>
                </div>
                <div class="content-item">
                  <h4>4.2 申诉渠道</h4>
                  <p>用户对处理结果有异议的，可以通过平台申诉渠道进行申诉。</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </main>

    <div class="modal-overlay" v-if="showDeleteModal" @click="cancelDelete">
      <div class="modal-content" @click.stop>
        <div class="modal-icon-wrapper">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="modal-warn-icon">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        </div>
        <h3>确认删除</h3>
        <p class="modal-post-title">「{{ postToDelete?.title }}」</p>
        <p class="modal-hint">删除后可在"已删除"标签中恢复</p>
        <div class="modal-actions">
          <button class="modal-btn cancel" @click="cancelDelete">取消</button>
          <button class="modal-btn confirm" @click="doDelete">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="modal-btn-icon">
              <polyline points="3 6 5 6 21 6"/>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
            </svg>
            确认删除
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import { getUserPosts, deletePost, updatePost, getTopPosts } from '@/api/post'
import { formatNumber, formatTime, formatDateTime } from '@/utils/format'
import type { PostVO } from '@/api/post'
import { usePostStats } from '@/composables/usePostStats'
import { getFollowerList, getFollowingList, followUser, unfollowUser, getFollowStats } from '@/api/user'
import { getManagedComments, deleteComment } from '@/api/comment'
import { useUserStore } from '@/stores/user'
import type { FollowUserVO } from '@/api/user'
import type { CommentVO } from '@/api/comment'
import PostCreate from '@/views/PostCreate.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import Pagination from '@/components/Pagination.vue'
import EmptyState from '@/components/EmptyState.vue'
import FollowButton from '@/components/FollowButton.vue'

const router = useRouter()
const userStore = useUserStore()

const posts = ref<PostVO[]>([])
const loading = ref(false)
const dataLoading = ref(false)
const fansLoading = ref(false)
const commentsLoading = ref(false)
const showDeleteModal = ref(false)
const activeNav = ref<'home' | 'content' | 'fans' | 'interaction' | 'magic' | 'agreement' | 'publish'>('home')

const postToDelete = ref<PostVO | null>(null)

const activeContentTab = ref<'all' | 'published' | 'draft' | 'deleted'>('all')
const searchKeyword = ref('')

const activeFansTab = ref<'followers' | 'following'>('followers')

const { stats: dataStats, loadMyStats: loadDataStats } = usePostStats()

const followerCount = ref(0)
const followingCount = ref(0)

const contentTabs = computed(() => {
  const activePosts = posts.value.filter(p => p.status !== 2)
  return [
    { label: '全部', value: 'all' as const, count: activePosts.length },
    { label: '已发布', value: 'published' as const, count: posts.value.filter(p => p.status === 1).length },
    { label: '草稿', value: 'draft' as const, count: posts.value.filter(p => p.status === 0).length },
    { label: '已删除', value: 'deleted' as const, count: posts.value.filter(p => p.status === 2).length },
  ]
})

const filteredContentPosts = computed(() => {
  let filtered = posts.value
  
  // 按状态筛选
  switch (activeContentTab.value) {
    case 'published':
      filtered = filtered.filter(p => p.status === 1)
      break
    case 'draft':
      filtered = filtered.filter(p => p.status === 0)
      break
    case 'deleted':
      filtered = filtered.filter(p => p.status === 2)
      break
    default:
      filtered = filtered.filter(p => p.status !== 2)
  }
  
  // 按关键词搜索
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.trim().toLowerCase()
    filtered = filtered.filter(p => p.title.toLowerCase().includes(keyword))
  }

  return filtered
})

// 最近作品（取前3条已发布的作品）
const recentPosts = computed(() => {
  return posts.value
    .filter(p => p.status === 1)
    .slice(0, 3)
})

// 评论管理帖子筛选下拉框选项（已发布帖子）
const publishedPostOptions = computed(() => {
  return posts.value.filter(p => p.status === 1)
})

// 最近互动（取前3条评论）
const recentInteractions = computed(() => {
  return managedCommentsList.value
    .map(comment => ({
      id: 'cmt-' + comment.id,
      userName: comment.userName || '用户',
      avatar: comment.userAvatar || '',
      postTitle: comment.postTitle || '',
      createTime: comment.createTime || '',
      type: 'comment' as const
    }))
    .slice(0, 3)
})

const rankingList = ref<PostVO[]>([])

const fansList = ref<FollowUserVO[]>([])
const fansTotal = ref(0)
const fansPage = ref(1)
const fansPageSize = 10

const managedCommentsList = ref<CommentVO[]>([])
const commentsTotal = ref(0)
const selectedCommentPostId = ref('')
const commentSortAsc = ref(false)
const commentPage = ref(1)
const commentPageSize = 20

import iconLightbulb from '@/assets/icons/lightbulb.svg'
import iconEdit from '@/assets/icons/edit.svg'
import iconEye from '@/assets/icons/eye.svg'
import iconLike from '@/assets/icons/like.svg'
import iconFavorite from '@/assets/icons/favorite.svg'
import iconFans from '@/assets/icons/fans-nav.svg'
import iconComment from '@/assets/icons/comment.svg'
import iconImage from '@/assets/icons/image.svg'
import iconChart from '@/assets/icons/chart.svg'
import iconEmpty from '@/assets/icons/empty.svg'

const magicFeatures = ref([
  { icon: iconLightbulb, title: '智能选题', desc: '输入关键词，AI帮你生成热门选题', btnText: '开始选题' },
  { icon: iconEdit, title: '文案生成', desc: '根据主题生成不同风格的文案', btnText: '生成文案' },
  { icon: iconImage, title: '图片建议', desc: '根据内容推荐配图方案', btnText: '获取建议' },
  { icon: iconChart, title: '排版优化', desc: '智能优化文章排版和格式', btnText: '优化排版' },
])

const statusText = (status: number) => {
  switch (status) {
    case 0: return '草稿'
    case 1: return '已发布'
    case 2: return '已删除'
    default: return '未知'
  }
}

const truncatePostTitle = (title: string | undefined, postId: string) => {
  const name = title || '帖子' + postId
  return name.length > 12 ? name.slice(0, 12) + '...' : name
}

const loadPosts = async () => {
  if (!userStore.userInfo?.id) return
  loading.value = true
  try {
    const res = await getUserPosts({ page: 1, size: 100 })
    if (res.data.code === 200) {
      posts.value = res.data.data.records || []
    }
  } catch (error) {
    console.error('加载帖子失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const refreshPosts = () => {
  if (!loading.value) {
    loadPosts()
  }
}

const goPublish = () => {
  activeNav.value = 'publish'
}

const goHome = () => {
  router.replace('/creator')
  activeNav.value = 'home'
}

const goCreate = async () => {
  await router.replace('/creator')
  activeNav.value = 'publish'
}

const viewPost = (postId: string) => {
  const url = router.resolve(`/post/${postId}`).href
  window.open(url, '_blank')
}

const editPost = async (postId: string) => {
  await router.replace({ path: '/creator', query: { edit: postId } })
  activeNav.value = 'publish'
}

const publishPost = async (postId: string) => {
  try {
    await updatePost({ id: postId, status: 1 })
    const post = posts.value.find(p => p.id === postId)
    if (post) post.status = 1
    ElMessage.success('发布成功')
  } catch (error) {
    console.error('发布失败:', error)
    ElMessage.error('发布失败')
  }
}

const restorePost = async (postId: string) => {
  try {
    await updatePost({ id: postId, status: 0 })
    const post = posts.value.find(p => p.id === postId)
    if (post) post.status = 0
    ElMessage.success('恢复成功')
  } catch (error) {
    console.error('恢复失败:', error)
    ElMessage.error('恢复失败')
  }
}

const confirmDelete = (post: PostVO) => {
  postToDelete.value = post
  showDeleteModal.value = true
}

const cancelDelete = () => {
  showDeleteModal.value = false
  postToDelete.value = null
}

const doDelete = async () => {
  if (!postToDelete.value) return
  try {
    await deletePost(postToDelete.value.id)
    const post = posts.value.find(p => p.id === postToDelete.value?.id)
    if (post) post.status = 2
    showDeleteModal.value = false
    postToDelete.value = null
    ElMessage.success('删除成功')
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

const loadFans = async () => {
  fansLoading.value = true
  try {
    const res = activeFansTab.value === 'followers'
      ? await getFollowerList({ page: fansPage.value, size: fansPageSize })
      : await getFollowingList({ page: fansPage.value, size: fansPageSize })
    if (res.data.code === 200) {
      fansList.value = res.data.data.records || []
      fansTotal.value = res.data.data.total || 0
    }
  } catch (error) {
    console.error('加载粉丝列表失败:', error)
  } finally {
    fansLoading.value = false
  }
}

const switchFansTab = (tab: 'followers' | 'following') => {
  activeFansTab.value = tab
  fansPage.value = 1
  loadFans()
}

const handleFansPageChange = (page: number) => {
  fansPage.value = page
  loadFans()
}

const loadRanking = async () => {
  try {
    const res = await getTopPosts(5)
    if (res.data.code === 200) {
      rankingList.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载排行榜失败:', error)
  }
}

const loadFollowStats = async () => {
  try {
    const res = await getFollowStats()
    if (res.data.code === 200) {
      followerCount.value = res.data.data.followerCount || 0
      followingCount.value = res.data.data.followingCount || 0
    }
  } catch (error) {
    console.error('加载关注统计失败:', error)
  }
}

const loadManagedComments = async () => {
  commentsLoading.value = true
  try {
    const params: { page: number; size: number; postId?: string; sortAsc?: boolean } = {
      page: commentPage.value,
      size: commentPageSize,
      sortAsc: commentSortAsc.value,
    }
    if (selectedCommentPostId.value) {
      params.postId = selectedCommentPostId.value
    }
    const res = await getManagedComments(params)
    if (res.data.code === 200) {
      managedCommentsList.value = res.data.data.records || []
      commentsTotal.value = res.data.data.total || 0
    }
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    commentsLoading.value = false
  }
}

const handleCommentPostFilterChange = () => {
  commentPage.value = 1
  loadManagedComments()
}

const handleCommentSortChange = (sortAsc: boolean) => {
  commentSortAsc.value = sortAsc
  commentPage.value = 1
  loadManagedComments()
}

const handleCommentPageChange = (page: number) => {
  commentPage.value = page
  loadManagedComments()
}

const confirmDeleteManagedComment = (comment: CommentVO) => {
  ElMessageBox.confirm('确定删除这条评论吗？删除后不可恢复', '删除评论', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    handleDeleteManagedComment(comment.id)
  }).catch(() => {})
}

const handleDeleteManagedComment = async (commentId: string) => {
  try {
    const res = await deleteComment(commentId)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      managedCommentsList.value = managedCommentsList.value.filter(c => c.id !== commentId)
      commentsTotal.value = Math.max(0, commentsTotal.value - 1)
    }
  } catch (error) {
    console.error('删除评论失败:', error)
  }
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

onMounted(() => {
  loadPosts()
  loadDataStats()
  loadRanking()
  loadFans()
  loadFollowStats()
  loadManagedComments()

  if (userStore.creatorActiveNav && userStore.creatorActiveNav !== 'publish') {
    activeNav.value = userStore.creatorActiveNav as typeof activeNav.value
  }
  userStore.creatorActiveNav = 'home'
})

watch(activeNav, (val) => {
  userStore.creatorActiveNav = val
})
</script>

<style scoped>
.creator-container {
  display: flex;
  min-height: 100vh;
  background: linear-gradient(180deg, #fff5f9 0%, #f8f9ff 100%);
}

.sidebar {
  width: 200px;
  background: white;
  border-right: 1.5px solid var(--border);
  padding: 20px 0;
  flex-shrink: 0;
  position: fixed;
  left: 0;
  top: 66px;
  bottom: 0;
  overflow-y: auto;
  z-index: 99;
}

.sidebar-header {
  padding: 0 20px 20px;
  border-bottom: 1px solid var(--border);
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
}

.logo-text {
  font-size: 18px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.sidebar-nav {
  padding: 16px 20px;
}

.nav-section {
  margin-bottom: 8px;
}

.nav-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--border), transparent);
  margin: 16px 0;
}

.nav-item {
  width: 100%;
  padding: 10px 14px;
  border-radius: 10px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: all 0.22s ease-out;
}

.nav-item:hover {
  background: rgba(255, 107, 157, 0.05);
  color: var(--pink);
}

.nav-item.active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(180, 132, 255, 0.1));
  color: var(--pink);
  font-weight: 600;
}

.nav-item-primary {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  margin-bottom: 8px;
}

.nav-item-primary:hover {
  background: linear-gradient(135deg, #ff7ba1, #c084fc);
  color: white;
}

.nav-item-primary.active {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
}

.nav-item-primary .nav-icon {
  filter: brightness(0) invert(1);
}

.nav-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  object-fit: contain;
}

.main-content {
  flex: 1;
  margin-left: 200px;
  padding: 90px 32px 24px;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left h1 {
  font-size: 26px;
  font-weight: 800;
  color: var(--text);
  margin-bottom: 4px;
}

.header-left p {
  font-size: 14px;
  color: var(--text-dim);
}

.publish-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 22px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.3);
}

.publish-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 157, 0.4);
}

.publish-btn .btn-icon {
  width: 16px;
  height: 16px;
  filter: brightness(0) invert(1);
}

/* 创作首页 Hero 区域 */
.home-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 32px;
  background: linear-gradient(135deg, #ff85a2 0%, #b484ff 100%);
  border-radius: 16px;
  color: white;
  position: relative;
  overflow: hidden;
}

.home-hero::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 300px;
  height: 300px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

.home-hero::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -10%;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

.hero-info {
  position: relative;
  z-index: 1;
}

.hero-info h1 {
  font-size: 28px;
  font-weight: 800;
  margin-bottom: 8px;
}

.hero-info p {
  font-size: 14px;
  opacity: 0.9;
}

.hero-publish-btn {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  border-radius: 12px;
  background: white;
  color: var(--pink);
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.hero-publish-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}

.hero-publish-btn .btn-icon {
  width: 20px;
  height: 20px;
  filter: brightness(0) saturate(100%) invert(32%) sepia(95%) saturate(1500%) hue-rotate(320deg) brightness(104%) contrast(96%);
}

.stats-section {
  margin-bottom: 32px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.action-icon {
  width: 16px;
  height: 16px;
}

.title-icon {
  width: 20px;
  height: 20px;
  margin-right: 8px;
}

.principle-icon {
  width: 24px;
  height: 24px;
  margin-right: 12px;
}

.magic-icon {
  width: 40px;
  height: 40px;
}

.overview-icon {
  width: 24px;
  height: 24px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 22px;
  font-weight: 800;
  color: var(--text);
}

.stat-label {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 2px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 16px;
}

/* 首页：最近作品 & 最近互动 */
.recent-section {
  margin-bottom: 28px;
}

.recent-posts-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.recent-post-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: white;
  border: 1px solid var(--border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.recent-post-item:hover {
  border-color: rgba(255, 107, 157, 0.3);
  box-shadow: 0 2px 8px rgba(180, 132, 255, 0.1);
}

.recent-post-cover {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--bg-secondary);
}

.recent-post-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recent-post-info {
  flex: 1;
  min-width: 0;
}

.recent-post-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recent-post-time {
  font-size: 12px;
  color: var(--text-dim);
}

/* 排行榜时间 */
.rank-time {
  display: block;
  font-size: 12px;
  color: var(--text-dim);
  margin-bottom: 4px;
}

/* 内容管理页面样式 */
.page-container {
  background: white;
  border-radius: 20px;
  padding: 28px;
  border: 1.5px solid var(--border);
  box-shadow: 0 4px 12px rgba(180, 132, 255, 0.06);
}

.page-container .page-header {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

.page-container .page-header h1 {
  font-size: 24px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
}

.filter-group {
  display: flex;
  gap: 6px;
  background: rgba(255, 107, 157, 0.04);
  border-radius: 12px;
  padding: 4px;
}

.filter-btn {
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

.filter-btn:hover:not(.active) {
  background: rgba(255, 107, 157, 0.08);
  color: var(--pink);
}

.filter-btn.active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.15), rgba(180, 132, 255, 0.15));
  color: var(--pink);
  box-shadow: none;
}

.filter-count {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.06);
  font-weight: 600;
  line-height: 1.6;
  display: inline-flex;
  align-items: center;
}

.filter-btn.active .filter-count {
  background: rgba(255, 255, 255, 0.3);
}

.search-box {
  display: flex;
  align-items: center;
  background: #f8f9fc;
  border-radius: 12px;
  padding: 0 14px;
  border: 1.5px solid var(--border);
  transition: all 0.22s ease-out;
  flex-shrink: 0;
}

.search-box:focus-within {
  border-color: rgba(255, 107, 157, 0.4);
  background: white;
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.08);
}

.search-icon {
  width: 16px;
  height: 16px;
  opacity: 0.4;
  flex-shrink: 0;
}

.search-input {
  border: none;
  background: transparent;
  outline: none;
  font-size: 13px;
  padding: 8px 10px;
  width: 180px;
  color: var(--text);
}

.search-input::placeholder {
  color: var(--text-dim);
}

.refresh-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1.5px solid var(--border);
  background: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease-out;
  flex-shrink: 0;
}

.refresh-btn:hover {
  border-color: var(--pink);
  background: rgba(255, 107, 157, 0.05);
}

.refresh-btn.spinning .refresh-icon {
  animation: spin 0.8s linear infinite;
}

.refresh-icon {
  width: 16px;
  height: 16px;
  opacity: 0.5;
  transition: opacity 0.22s ease-out;
}

.refresh-btn:hover .refresh-icon {
  opacity: 0.8;
}

.search-bar {
  margin-bottom: 20px;
}

.search-bar .search-box {
  max-width: 360px;
}

.content-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.content-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  background: white;
  border: 1.5px solid var(--border);
  box-shadow: 0 2px 8px rgba(180, 132, 255, 0.04);
  transition: all 0.22s ease-out;
}

.content-item:hover {
  border-color: rgba(255, 107, 157, 0.3);
  box-shadow: 0 4px 16px rgba(180, 132, 255, 0.1);
  transform: translateY(-2px);
}

.content-cover {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(180, 132, 255, 0.1);
}

.content-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.22s ease-out;
}

.content-cover:hover img {
  transform: scale(1.05);
}

.content-info {
  flex: 1;
  min-width: 0;
}

.content-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
  transition: color 0.22s ease-out;
}

.content-title:hover {
  color: var(--pink);
}

.content-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.content-time {
  font-size: 12px;
  color: var(--text-dim);
}

.content-status {
  flex-shrink: 0;
}

.content-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f0e6ff, #ffe6f0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: var(--text-dim);
}

.category-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 6px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(180, 132, 255, 0.1));
  color: var(--pink);
  font-weight: 500;
}

.content-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-dim);
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-dim);
  line-height: 1;
}

.recent-post-stats {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-dim);
}

.stat-mini-icon {
  width: 14px;
  height: 14px;
  vertical-align: middle;
}

.status-tag {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 8px;
  font-weight: 500;
}

.status-0 {
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.12), rgba(255, 152, 0, 0.12));
  color: #f57c00;
}

.status-1 {
  background: linear-gradient(135deg, rgba(76, 175, 80, 0.12), rgba(56, 142, 60, 0.12));
  color: #2e7d32;
}

.status-2 {
  background: linear-gradient(135deg, rgba(244, 67, 54, 0.12), rgba(211, 47, 47, 0.12));
  color: #c62828;
}

.action-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1.5px solid var(--border);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease-out;
  background: white;
}

.action-btn:hover {
  transform: scale(1.1);
  border-color: transparent;
}

.action-btn .action-icon {
  width: 16px;
  height: 16px;
}

.action-btn.edit:hover {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.12), rgba(180, 132, 255, 0.12));
  border-color: rgba(255, 107, 157, 0.3);
}

.action-btn.publish:hover {
  background: linear-gradient(135deg, rgba(76, 175, 80, 0.1), rgba(56, 142, 60, 0.1));
  border-color: rgba(76, 175, 80, 0.3);
}

.action-btn.publish .action-icon {
  filter: brightness(0) saturate(100%) invert(42%) sepia(60%) saturate(500%) hue-rotate(80deg) brightness(95%) contrast(90%);
}

.action-btn.restore:hover {
  background: linear-gradient(135deg, rgba(33, 150, 243, 0.1), rgba(25, 118, 210, 0.1));
  border-color: rgba(33, 150, 243, 0.3);
}

.action-btn.restore .action-icon {
  filter: brightness(0) saturate(100%) invert(45%) sepia(70%) saturate(400%) hue-rotate(170deg) brightness(95%) contrast(90%);
}

.action-btn.delete:hover {
  background: linear-gradient(135deg, rgba(244, 67, 54, 0.1), rgba(211, 47, 47, 0.1));
  border-color: rgba(244, 67, 54, 0.3);
}

.action-btn.delete .action-icon {
  filter: brightness(0) saturate(100%) invert(35%) sepia(80%) saturate(500%) hue-rotate(340deg) brightness(95%) contrast(90%);
}

.create-btn {
  padding: 12px 32px;
  border-radius: 25px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  font-size: 15px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.3);
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 157, 0.4);
}

.status-tabs {
  display: flex;
  gap: 8px;
  padding: 4px;
  background: rgba(255, 107, 157, 0.05);
  border-radius: 12px;
}

.status-tab-btn {
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  background: transparent;
  color: var(--text-dim);
  transition: all 0.22s ease-out;
}

.status-tab-btn.active {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  box-shadow: 0 2px 8px rgba(255, 107, 157, 0.2);
}

.status-tab-btn:hover:not(.active) {
  background: rgba(255, 107, 157, 0.08);
  color: var(--pink);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: white;
  border-radius: 24px;
  padding: 36px 32px 28px;
  max-width: 420px;
  width: 90%;
  text-align: center;
  animation: slideUp 0.3s ease-out;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modal-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 71, 87, 0.1), rgba(255, 107, 129, 0.1));
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.modal-warn-icon {
  width: 28px;
  height: 28px;
  color: #ff4757;
  stroke-width: 2;
}

.modal-content h3 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 8px;
}

.modal-post-title {
  font-size: 15px;
  color: var(--text-secondary);
  margin-bottom: 6px;
  font-weight: 500;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.modal-hint {
  font-size: 12px;
  color: var(--text-dim);
  margin-bottom: 28px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.modal-btn {
  padding: 10px 32px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.22s ease-out;
}

.modal-btn.cancel {
  background: #f3f4f6;
  color: var(--text-dim);
}

.modal-btn.cancel:hover {
  background: #e5e7eb;
}

.modal-btn.confirm {
  display: flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #ff4757, #ff6b81);
  color: white;
  box-shadow: 0 4px 14px rgba(255, 71, 87, 0.3);
}

.modal-btn-icon {
  width: 16px;
  height: 16px;
}

.modal-btn.confirm:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(255, 71, 87, 0.4);
}

@media (max-width: 1024px) {
  .sidebar {
    width: 60px;
    top: 66px;
  }
  
  .sidebar-header {
    padding: 0;
    display: flex;
    justify-content: center;
  }
  
  .logo-wrapper {
    flex-direction: column;
    gap: 4px;
  }
  
  .logo-text {
    font-size: 10px;
  }
  
  .nav-text {
    display: none;
  }
  
  .section-title {
    display: none;
  }
  
  .nav-item {
    justify-content: center;
    padding: 12px;
  }
  
  .main-content {
    margin-left: 60px;
    padding: 90px 20px 20px;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .sidebar {
    display: none;
  }
  
  .main-content {
    margin-left: 0;
    padding: 80px 16px 20px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .banner {
    padding: 20px;
  }
  
  .banner-content h2 {
    font-size: 20px;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .post-card {
    flex-wrap: wrap;
  }
  
  .card-cover {
    width: 60px;
    height: 60px;
  }
  
  .card-status {
    width: 100%;
    margin-top: 8px;
  }
  
  .card-actions {
    width: 100%;
    justify-content: flex-end;
    margin-top: 8px;
  }
  
  .status-tabs {
    flex-wrap: wrap;
  }
  
  .status-tab-btn {
    flex: 1 1 calc(50% - 4px);
  }
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 12px;
  background: rgba(255, 107, 157, 0.03);
  border: 1.5px solid transparent;
  transition: all 0.22s ease-out;
  cursor: pointer;
}

.ranking-item:hover {
  border-color: rgba(180, 132, 255, 0.3);
}

.rank-num {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  background: var(--border);
  color: var(--text-dim);
}

.rank-num.rank-1 {
  background: linear-gradient(135deg, #ffd700, #ffb700);
  color: white;
}

.rank-num.rank-2 {
  background: linear-gradient(135deg, #c0c0c0, #a0a0a0);
  color: white;
}

.rank-num.rank-3 {
  background: linear-gradient(135deg, #cd7f32, #b87333);
  color: white;
}

.rank-cover {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.rank-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder-small {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f0e6ff, #ffe6f0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-info h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-dim);
}

.rank-stats .stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  line-height: 1;
}

.fans-tabs {
  display: flex;
  gap: 6px;
  background: rgba(255, 107, 157, 0.04);
  border-radius: 12px;
  padding: 4px;
  margin-top: 24px;
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
  background: rgba(255, 107, 157, 0.08);
  color: var(--pink);
}

.fans-tab-btn.active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.15), rgba(180, 132, 255, 0.15));
  color: var(--pink);
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
  background: rgba(255, 107, 157, 0.03);
  border: 1.5px solid transparent;
  transition: all 0.22s ease-out;
  margin-bottom: 12px;
}

.fan-item:hover {
  border-color: rgba(180, 132, 255, 0.3);
}

.fan-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
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

.fan-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.fan-tag {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 12px;
  background: rgba(255, 107, 157, 0.1);
  color: var(--pink);
}

.comment-filter-group {
  display: flex;
  align-items: center;
  background: transparent;
  border-radius: 0;
  padding: 0;
}

.comment-post-select {
  min-width: 180px;
}

/* el-select 粉色圆角定制 */
.comment-post-select {
  --el-color-primary: #FF6B9D;
  --el-color-primary-light-3: #ff8fb5;
  --el-color-primary-light-5: #ffb6cc;
  --el-border-color-hover: #FF6B9D;
  --el-border-color: #FFB6CC;
  --el-input-focus-border-color: #FF6B9D;
}

.comment-post-select :deep(.el-input__wrapper) {
  border-radius: 20px;
  border-color: #FFB6CC;
  background: #FFF7FA;
  box-shadow: none;
  transition: all 0.22s ease-out;
}

.comment-post-select :deep(.el-input__wrapper:hover) {
  border-color: #FF6B9D;
  background: #FFF0F5;
}

.comment-post-select :deep(.el-input__wrapper.is-focus),
.comment-post-select :deep(.el-select__wrapper.is-focused),
.comment-post-select :deep(.el-input.is-focus .el-input__wrapper) {
  border-color: #FF6B9D !important;
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.15) !important;
  background: white;
}

.comment-post-select :deep(.el-select__caret) {
  color: #FF6B9D;
}

.comment-post-select :deep(.el-select__placeholder) {
  color: #C4A0B4;
}


.comment-list {
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--border);
}

.comment-manage-item {
  display: flex;
  gap: 14px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border);
  transition: background 0.15s;
}

.comment-manage-item:hover {
  background: rgba(0, 0, 0, 0.012);
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.comment-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-top-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 6px;
}

.comment-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-main-row {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 0;
  line-height: 1.65;
}

.comment-context {
  font-size: 13px;
  color: #999;
  white-space: nowrap;
  margin-right: 6px;
}

.context-post-title {
  color: var(--purple);
  cursor: pointer;
  transition: opacity 0.15s;
}

.context-post-title:hover {
  opacity: 0.75;
}

.context-reply-to {
  color: var(--purple);
}

.comment-content {
  font-size: 14px;
  color: #222;
  word-break: break-word;
}

.comment-delete-btn {
  padding: 4px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  flex-shrink: 0;
  align-self: flex-start;
  transition: background 0.15s;
  line-height: 0;
}

.comment-delete-btn:hover {
  background: rgba(239, 68, 68, 0.08);
}

.delete-icon {
  width: 15px;
  height: 15px;
  opacity: 0.35;
  transition: opacity 0.15s;
}

.comment-delete-btn:hover .delete-icon {
  opacity: 0.7;
}

/* 整行 hover 时才让删除按钮显现 */
.comment-manage-item .delete-icon {
  opacity: 0.35;
  transition: opacity 0.15s;
}

.comment-manage-item:hover .delete-icon {
  opacity: 0.6;
}

/* 总数 */
.comment-total {
  font-size: 13px;
  color: var(--text-dim);
  margin-left: 4px;
  white-space: nowrap;
}

/* 排序按钮组 */
.sort-group {
  display: flex;
  gap: 2px;
  background: rgba(255, 107, 157, 0.04);
  border-radius: 8px;
  padding: 3px;
}

.sort-btn {
  padding: 5px 14px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-dim);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.sort-btn:hover:not(.active) {
  color: var(--pink);
}

.sort-btn.active {
  background: white;
  color: var(--pink);
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.magic-page {
  padding: 0;
}

.magic-banner {
  background: linear-gradient(135deg, #ff85a2 0%, #b484ff 100%);
  border-radius: 20px;
  padding: 40px;
  color: white;
  text-align: center;
  margin-bottom: 24px;
}

.magic-banner-content h2 {
  font-size: 28px;
  font-weight: 800;
  margin-bottom: 8px;
}

.magic-banner-content p {
  font-size: 15px;
  opacity: 0.9;
}

.magic-features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.magic-card {
  background: rgba(255, 107, 157, 0.05);
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  border: 1.5px solid transparent;
  transition: all 0.22s ease-out;
}

.magic-card:hover {
  border-color: rgba(255, 107, 157, 0.3);
  transform: translateY(-2px);
}

.magic-icon {
  font-size: 36px;
  margin-bottom: 12px;
}

.magic-card h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 8px;
}

.magic-card p {
  font-size: 13px;
  color: var(--text-dim);
  line-height: 1.5;
  margin-bottom: 16px;
}

.magic-btn {
  padding: 10px 24px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  transition: all 0.22s ease-out;
}

.magic-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.3);
}

.magic-tips {
  background: rgba(255, 193, 7, 0.1);
  border-radius: 16px;
  padding: 24px;
  border: 1.5px solid rgba(255, 193, 7, 0.3);
}

.tips-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 12px;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
}

.tips-list li {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 2;
  margin-bottom: 6px;
}

.agreement-page {
  max-width: 800px;
  margin: 0 auto;
}

.agreement-container {
  background: white;
  border-radius: 20px;
  padding: 32px;
  border: 1.5px solid var(--border);
  box-shadow: 0 8px 32px rgba(180, 132, 255, 0.08);
}

.agreement-page-header {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

.agreement-page-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 8px 0;
}

.agreement-page-header p {
  font-size: 14px;
  color: var(--text-dim);
  margin: 0;
}

.agreement-intro {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.08), rgba(180, 132, 255, 0.08));
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 28px;
  border: 1px solid rgba(255, 107, 157, 0.15);
}

.agreement-intro p {
  font-size: 14px;
  color: var(--text);
  line-height: 1.8;
  margin: 0;
}

.agreement-intro strong {
  color: var(--pink);
}

.agreement-principles {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}

.principle-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  text-align: center;
  border: 1.5px solid var(--border);
  box-shadow: 0 4px 12px rgba(180, 132, 255, 0.06);
  transition: all 0.22s ease-out;
}

.principle-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(180, 132, 255, 0.12);
  border-color: rgba(255, 107, 157, 0.2);
}

.principle-icon {
  font-size: 32px;
  margin-bottom: 12px;
}

.principle-card h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 8px 0;
}

.principle-card p {
  font-size: 13px;
  color: var(--text-dim);
  line-height: 1.6;
  margin: 0;
}

.agreement-content {
  padding-top: 20px;
  border-top: 1px solid var(--border);
}

.content-section {
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

.content-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.content-section h2 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.content-section h2::before {
  content: '';
  width: 4px;
  height: 18px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  border-radius: 2px;
}

.content-item {
  margin-bottom: 16px;
}

.content-item:last-child {
  margin-bottom: 0;
}

.content-item h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 8px 0;
}

.content-item p {
  font-size: 14px;
  color: var(--text-dim);
  line-height: 1.8;
  margin: 0;
  padding-left: 12px;
}

.content-item ul {
  margin: 0;
  padding-left: 28px;
}

.content-item li {
  font-size: 14px;
  color: var(--text-dim);
  line-height: 1.8;
  margin-bottom: 6px;
}

.content-item li:last-child {
  margin-bottom: 0;
}
</style>