<template>
  <div class="creator-container">
    <aside class="sidebar">
      <nav class="sidebar-nav">
        <div class="nav-section">
        <button class="nav-item nav-item-primary" :class="{ active: activeNav === 'publish' }" @click="switchNav('publish')">
          <img src="@/assets/icons/edit.svg" class="nav-icon" />
          <span class="nav-text">发布</span>
        </button>
        <button class="nav-item" :class="{ active: activeNav === 'home' }" @click="switchNav('home')">
          <img src="@/assets/icons/home-nav.svg" class="nav-icon" />
          <span class="nav-text">创作首页</span>
        </button>
      </div>

        <div class="nav-divider"></div>

        <div class="nav-section">
          <button class="nav-item" :class="{ active: activeNav === 'content' }" @click="switchNav('content')">
            <img src="@/assets/icons/content-nav.svg" class="nav-icon" />
            <span class="nav-text">内容管理</span>
          </button>
        </div>

        <div class="nav-divider"></div>

        <div class="nav-section">
          <button class="nav-item" :class="{ active: activeNav === 'fans' }" @click="switchNav('fans')">
            <img src="@/assets/icons/fans-nav.svg" class="nav-icon" />
            <span class="nav-text">粉丝管理</span>
          </button>
          <button class="nav-item" :class="{ active: activeNav === 'interaction' }" @click="switchNav('interaction')">
            <img src="@/assets/icons/interaction-nav.svg" class="nav-icon" />
            <span class="nav-text">评论管理</span>
          </button>
        </div>

        <div class="nav-divider"></div>

        <div class="nav-section">
          <button class="nav-item" :class="{ active: activeNav === 'magic' }" @click="switchNav('magic')">
            <img src="@/assets/icons/magic-nav.svg" class="nav-icon" />
            <span class="nav-text">妙笔</span>
          </button>
        </div>

        <div class="nav-divider"></div>

        <div class="nav-section">
          <button class="nav-item" :class="{ active: activeNav === 'agreement' }" @click="switchNav('agreement')">
            <img src="@/assets/icons/community-agreement-nav.svg" class="nav-icon" />
            <span class="nav-text">社区公约</span>
          </button>
        </div>
      </nav>
    </aside>

    <main class="main-content">
      <PostCreate ref="postCreateRef" v-if="activeNav === 'publish'" @go-back="goHome" @publish-success="handlePublishSuccess" />
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
            <StatCard icon-class="views-icon" :icon="iconEye" :value="dataStats?.totalViews ?? 0" label="总浏览" />
            <StatCard icon-class="likes-icon" :icon="iconLike" :value="dataStats?.totalLikes ?? 0" label="总点赞" />
            <StatCard icon-class="collections-icon" :icon="iconFavorite" :value="dataStats?.totalCollections ?? 0" label="总收藏" />
            <StatCard icon-class="fans-icon" :icon="iconFans" :value="followerCount" label="粉丝数" />
            <StatCard icon-class="comments-icon" :icon="iconComment" :value="commentsTotal" label="评论数" />
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
            <div class="filter-left">
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
            <div class="sort-group">
              <button
                v-for="opt in sortOptions"
                :key="opt.value"
                class="sort-btn"
                :class="{ active: contentSortField === opt.value }"
                @click="handleContentSort(opt.value)"
              >
                {{ opt.label }}
                <span v-if="contentSortField === opt.value" class="sort-arrow">{{ contentSortOrder === 'asc' ? '↑' : '↓' }}</span>
              </button>
            </div>
          </div>

          <div class="search-bar">
            <div class="search-box">
              <img src="@/assets/icons/search.svg" alt="search" class="search-icon" />
              <input type="text" placeholder="搜索当前分类下的作品标题..." class="search-input" v-model="searchKeyword" />
            </div>
          </div>

          <div class="content-list" v-if="!loading && filteredContentPosts.length > 0">
            <div class="content-item" v-for="post in filteredContentPosts" :key="post.id">
              <div class="content-cover" :class="{ clickable: isPublished(post.status) }" @click="isPublished(post.status) && viewPost(post.id)">
                <img v-if="post.cover" :src="post.cover" alt="cover" />
                <div v-else class="cover-placeholder">
                  <span>暂无封面</span>
                </div>
              </div>
              <div class="content-info">
                <h3 class="content-title" :class="{ clickable: isPublished(post.status) }" @click="isPublished(post.status) && viewPost(post.id)">{{ post.title }}</h3>
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
                <button v-if="!isDeleted(post.status)" class="action-btn edit" @click="editPost(post.id)" title="编辑">
                  <img src="@/assets/icons/edit.svg" alt="edit" class="action-icon" />
                </button>
                <button v-if="isDraft(post.status)" class="action-btn preview" @click="openPreview(post)" title="预览">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="action-icon-svg">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                    <circle cx="12" cy="12" r="3"/>
                  </svg>
                </button>
                <button 
                  v-if="isDraft(post.status)" 
                  class="action-btn publish" 
                  @click="publishPost(post.id)" 
                  title="发布"
                >
                  <img src="@/assets/icons/rocket.svg" alt="rocket" class="action-icon" />
                </button>
                <button 
                  v-if="isDeleted(post.status)" 
                  class="action-btn restore" 
                  @click="restorePost(post.id)" 
                  title="恢复"
                >
                  <img src="@/assets/icons/refresh.svg" alt="refresh" class="action-icon" />
                </button>
                <button class="action-btn delete" @click="confirmDelete(post)" :title="isDeleted(post.status) ? '彻底删除' : '删除'">
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

          <div class="search-bar">
            <div class="search-box">
              <img src="@/assets/icons/search.svg" alt="search" class="search-icon" />
              <input type="text" placeholder="搜索粉丝昵称..." class="search-input" v-model="fansSearchKeyword" />
            </div>
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

          <EmptyState v-if="!fansLoading && filteredFansList.length === 0" :icon="iconEmpty" :title="fansSearchKeyword ? '没有匹配的粉丝' : (activeFansTab === 'followers' ? '还没有粉丝' : '还没有关注的人')" :hint="fansSearchKeyword ? '' : (activeFansTab === 'followers' ? '发布更多优质内容，吸引粉丝关注' : '去发现有趣的内容和人吧')" />

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
                popper-class="comment-post-select-popper"
                clearable
                @change="handleCommentPostFilterChange"
              >
                <el-option value="" label="全部帖子" />
                <el-option
                  v-for="post in publishedPostOptions"
                  :key="post.id"
                  :value="post.id"
                  :label="post.title"
                >
                  <div class="comment-post-option">
                    <span class="comment-post-option-title">{{ post.title }}</span>
                    <span class="comment-post-option-status" :class="`status-${post.status}`">{{ statusText(post.status) }}</span>
                  </div>
                </el-option>
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

          <div class="search-bar">
            <div class="search-box">
              <img src="@/assets/icons/search.svg" alt="search" class="search-icon" />
              <input type="text" placeholder="搜索评论内容或用户名..." class="search-input" v-model="commentSearchKeyword" />
            </div>
          </div>

          <div class="comment-list" v-if="!commentsLoading && filteredManagedComments.length > 0">
            <div class="comment-manage-item" v-for="comment in filteredManagedComments" :key="comment.id">
              <div class="comment-avatar clickable" @click="goToUser(comment.userId)">
                <img v-if="comment.userAvatar" :src="comment.userAvatar" alt="" />
                <div v-else class="avatar-placeholder"></div>
              </div>
              <div class="comment-body">
                <div class="comment-top-row">
                  <span class="comment-name clickable" @click="goToUser(comment.userId)">{{ comment.userName }}</span>
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

          <EmptyState v-else :icon="iconEmpty" :title="commentSearchKeyword ? '没有匹配的评论' : (selectedCommentPostId ? '当前帖子还没有评论' : '当前还没有人给你的作品留言')" />

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

    <ConfirmModal
      v-model:visible="showDeleteModal"
      :title="isDeleted(postToDelete?.status) ? '确认彻底删除' : '确认删除'"
      :post-title="postToDelete?.title"
      :hint="isDeleted(postToDelete?.status) ? '彻底删除后将无法恢复，同时会清理该帖子的评论、评论点赞、帖子点赞和收藏数据' : '删除后可在已删除标签中恢复'"
      :confirm-text="isDeleted(postToDelete?.status) ? '继续彻底删除' : '确认删除'"
      :danger="isDeleted(postToDelete?.status)"
      danger-badge="高风险操作"
      :warning-text="isDeleted(postToDelete?.status) ? '确认后还需再次确认一次，请谨慎操作' : undefined"
      @confirm="doDelete"
      @cancel="cancelDelete"
    >
      <template #icon>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="modal-warn-icon">
          <polyline points="3 6 5 6 21 6"/>
          <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/>
          <line x1="10" y1="11" x2="10" y2="17"/>
          <line x1="14" y1="11" x2="14" y2="17"/>
        </svg>
      </template>
    </ConfirmModal>

    <ConfirmModal
      v-model:visible="showPublishModal"
      title="确认发布"
      :post-title="postToPublish?.title"
      hint="发布后帖子将公开显示，所有用户均可查看。"
      confirm-text="确认发布"
      @confirm="doPublish"
      @cancel="cancelPublish"
    >
      <template #icon>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="modal-warn-icon">
          <path d="M15.59 14.37a6 6 0 0 1-5.84 7.38v-4.8m5.84-2.58a14.98 14.98 0 0 0 6.16-12.12A14.98 14.98 0 0 0 9.631 8.41m5.96 5.96a14.926 14.926 0 0 1-5.841 2.58m-.119-8.54a6 6 0 0 0-7.381 5.84h4.8m2.581-5.84a14.927 14.927 0 0 0-2.58 5.84m2.699 2.7c-.103.021-.207.041-.311.06a15.09 15.09 0 0 1-2.448-2.448 14.9 14.9 0 0 1 .06-.312m-2.24 2.39a4.493 4.493 0 0 0-1.757 4.306 4.493 4.493 0 0 1 4.306-1.758M16.5 9a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0Z"/>
        </svg>
      </template>
    </ConfirmModal>


    <ConfirmModal
      v-model:visible="showRestoreModal"
      title="确认恢复"
      :post-title="postToRestore?.title"
      hint="恢复后帖子将回到草稿箱，你可以继续编辑后再发布。"
      confirm-text="确认恢复"
      @confirm="doRestore"
      @cancel="cancelRestore"
    >
      <template #icon>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="modal-warn-icon">
          <polyline points="1 4 1 10 7 10"/>
          <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
        </svg>
      </template>
    </ConfirmModal>

    <ConfirmModal
      v-model:visible="showDeleteCommentModal"
      title="删除评论"
      :hint="commentToDelete ? `确定删除${commentToDelete.userName}的评论吗？删除后不可恢复` : ''"
      confirm-text="删除"
      danger
      @confirm="doDeleteComment"
      @cancel="cancelDeleteComment"
    >
      <template #icon>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="modal-warn-icon">
          <polyline points="3 6 5 6 21 6"/>
          <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/>
          <line x1="10" y1="11" x2="10" y2="17"/>
          <line x1="14" y1="11" x2="14" y2="17"/>
        </svg>
      </template>
    </ConfirmModal>

    <ConfirmModal
      v-model:visible="showPermanentDeleteModal"
      title="二次确认"
      :post-title="postToDelete?.title"
      :hint="postToDelete ? `帖子「${postToDelete.title}」将被彻底删除，且无法恢复。是否继续？` : ''"
      confirm-text="仍要彻底删除"
      danger
      danger-badge="高风险操作"
      warning-text="确认后将永久删除，请谨慎操作"
      @confirm="doPermanentDelete"
      @cancel="cancelPermanentDelete"
    >
      <template #icon>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="modal-warn-icon">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
      </template>
    </ConfirmModal>

    <!-- 预览弹窗 -->
    <Teleport to="body">
      <Transition name="preview-fade">
        <div v-if="showPreviewModal" class="preview-overlay" @click.self="closePreview" @keydown.escape="closePreview">
          <div class="preview-dialog">
            <div class="preview-dialog-header">
              <span class="preview-status-tag" :class="'status-' + (previewPost?.status ?? 0)">
                {{ previewPost ? statusText(previewPost.status) : '' }}
              </span>
              <button class="preview-close-btn" @click="closePreview">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>

            <div class="preview-dialog-body" v-if="previewPost">
              <div class="preview-category" v-if="previewPost.categoryName">
                <span class="preview-category-tag">{{ previewPost.categoryName }}</span>
              </div>

              <h2 class="preview-title">{{ previewPost.title }}</h2>

              <div class="preview-images" v-if="previewPost.images && previewPost.images.length > 0">
                <div class="preview-carousel" :style="{ aspectRatio: previewCarouselRatio }">
                  <div class="preview-carousel-track" :style="{ transform: `translateX(-${previewImageIndex * 100}%)` }">
                    <img
                      v-for="(img, index) in previewPost.images"
                      :key="index"
                      :src="img"
                      :alt="'图片' + (index + 1)"
                      class="preview-carousel-slide"
                      @load="(e) => onPreviewImageLoad(index, e)"
                    />
                  </div>
                  <button v-if="previewPost.images.length > 1" class="preview-carousel-arrow preview-carousel-prev" @click="previewImageIndex = Math.max(0, previewImageIndex - 1)">
                    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
                  </button>
                  <button v-if="previewPost.images.length > 1" class="preview-carousel-arrow preview-carousel-next" @click="previewImageIndex = Math.min((previewPost.images?.length ?? 1) - 1, previewImageIndex + 1)">
                    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 6 15 12 9 18"/></svg>
                  </button>
                  <div class="preview-carousel-dots" v-if="previewPost.images.length > 1">
                    <span v-for="(_, index) in previewPost.images" :key="index" class="preview-carousel-dot" :class="{ active: index === previewImageIndex }" @click="previewImageIndex = index"></span>
                  </div>
                </div>
              </div>

              <div class="preview-content" v-if="previewPost.content">
                <p v-for="(paragraph, index) in previewParagraphs" :key="index" class="preview-paragraph">
                  {{ paragraph }}
                </p>
              </div>

              <div class="preview-tags" v-if="previewPost.tags && previewPost.tags.length > 0">
                <span v-for="tag in previewPost.tags" :key="tag" class="preview-tag-item">#{{ tag }}</span>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useNavigate } from '@/composables/useNavigate'
import { ElMessage } from 'element-plus'

import { getUserPosts, deletePost, permanentDeletePost, updatePost, getTopPosts } from '@/api/post'
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
import StatCard from '@/components/StatCard.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import { isDraft, isPublished, isDeleted, statusText, canPublish } from '@/utils/postStatus'

const { open, router } = useNavigate()
const route = useRoute()
const userStore = useUserStore()

const posts = ref<PostVO[]>([])
const loading = ref(false)
const dataLoading = ref(false)
const fansLoading = ref(false)
const commentsLoading = ref(false)
const showDeleteModal = ref(false)
const showPublishModal = ref(false)
const showRestoreModal = ref(false)
const showDeleteCommentModal = ref(false)
const showPermanentDeleteModal = ref(false)
const showPreviewModal = ref(false)
const previewPost = ref<PostVO | null>(null)
const previewImageIndex = ref(0)
const previewImageRatios = ref<number[]>([])

function onPreviewImageLoad(index: number, e: Event) {
  const img = e.target as HTMLImageElement
  previewImageRatios.value[index] = img.naturalWidth / img.naturalHeight
}

const previewCarouselRatio = computed(() => {
  const ratio = previewImageRatios.value[previewImageIndex.value]
  return ratio ? `${ratio}` : '4/3'
})
const activeNav = ref<'home' | 'content' | 'fans' | 'interaction' | 'magic' | 'agreement' | 'publish'>('home')
const postCreateRef = ref<InstanceType<typeof PostCreate>>()

const switchNav = async (nav: typeof activeNav.value) => {
  if (activeNav.value === 'publish' && nav !== 'publish' && postCreateRef.value) {
    const canLeave = await postCreateRef.value.confirmLeave()
    if (!canLeave) return
  }
  // 离开发布页时清除编辑参数，避免下次回来残留旧帖子数据
  if (activeNav.value === 'publish' && nav !== 'publish') {
    router.replace('/creator')
  }
  activeNav.value = nav
}

const postToDelete = ref<PostVO | null>(null)
const postToPublish = ref<PostVO | null>(null)
const postToRestore = ref<PostVO | null>(null)
const commentToDelete = ref<CommentVO | null>(null)

const activeContentTab = ref<'all' | 'published' | 'draft' | 'deleted'>('all')
const searchKeyword = ref('')

const activeFansTab = ref<'followers' | 'following'>('followers')

const postStatsState = usePostStats()
const dataStats = postStatsState.stats

const followerCount = ref(0)
const followingCount = ref(0)

const contentTabs = computed(() => {
  const activePosts = posts.value.filter(p => !isDeleted(p.status))
  return [
    { label: '全部', value: 'all' as const, count: activePosts.length },
    { label: '已发布', value: 'published' as const, count: posts.value.filter(p => isPublished(p.status)).length },
    { label: '草稿', value: 'draft' as const, count: posts.value.filter(p => isDraft(p.status)).length },
    { label: '已删除', value: 'deleted' as const, count: posts.value.filter(p => isDeleted(p.status)).length },
  ]
})

const filteredContentPosts = computed(() => {
  let filtered = posts.value
  
  // 按状态筛选
  switch (activeContentTab.value) {
    case 'published':
      filtered = filtered.filter(p => isPublished(p.status))
      break
    case 'draft':
      filtered = filtered.filter(p => isDraft(p.status))
      break
    case 'deleted':
      filtered = filtered.filter(p => isDeleted(p.status))
      break
    default:
      filtered = filtered.filter(p => !isDeleted(p.status))
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
    .filter(p => isPublished(p.status))
    .slice(0, 3)
})

// 评论管理帖子筛选下拉框选项（排除已删除帖子）
const publishedPostOptions = computed(() => {
  return posts.value.filter(p => !isDeleted(p.status))
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
const fansSearchKeyword = ref('')

const managedCommentsList = ref<CommentVO[]>([])
const commentsTotal = ref(0)
const selectedCommentPostId = ref('')
const commentSortAsc = ref(false)
const commentSearchKeyword = ref('')
const contentSortField = ref('create_time')
const contentSortOrder = ref('desc')
const commentPage = ref(1)
const commentPageSize = 20

import {
  lightbulb as iconLightbulb,
  edit as iconEdit,
  eye as iconEye,
  like as iconLike,
  favorite as iconFavorite,
  fansNav as iconFans,
  comment as iconComment,
  image as iconImage,
  chart as iconChart,
  empty as iconEmpty,
} from '@/assets/icons'

const magicFeatures = ref([
  { icon: iconLightbulb, title: '智能选题', desc: '输入关键词，AI帮你生成热门选题', btnText: '开始选题' },
  { icon: iconEdit, title: '文案生成', desc: '根据主题生成不同风格的文案', btnText: '生成文案' },
  { icon: iconImage, title: '图片建议', desc: '根据内容推荐配图方案', btnText: '获取建议' },
  { icon: iconChart, title: '排版优化', desc: '智能优化文章排版和格式', btnText: '优化排版' },
])

const truncatePostTitle = (title: string | undefined, postId: string) => {
  const name = title || '帖子' + postId
  return name.length > 12 ? name.slice(0, 12) + '...' : name
}

const loadPosts = async () => {
  if (!userStore.userInfo?.id) return
  loading.value = true
  try {
    const data = await getUserPosts({ page: 1, size: 100, sortField: contentSortField.value, sortOrder: contentSortOrder.value })
    posts.value = data.records || []
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

const sortOptions = [
  { label: '创建时间', value: 'create_time' },
  { label: '浏览量', value: 'views' },
  { label: '点赞数', value: 'likes' },
  { label: '收藏数', value: 'collections' },
]

const handleContentSort = (field: string) => {
  if (contentSortField.value === field) {
    contentSortOrder.value = contentSortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    contentSortField.value = field
    contentSortOrder.value = 'desc'
  }
  loadPosts()
}

const filteredFansList = computed(() => {
  const kw = fansSearchKeyword.value.trim().toLowerCase()
  if (!kw) return fansList.value
  return fansList.value.filter(f => (f.nickname || '').toLowerCase().includes(kw))
})

const filteredManagedComments = computed(() => {
  const kw = commentSearchKeyword.value.trim().toLowerCase()
  if (!kw) return managedCommentsList.value
  return managedCommentsList.value.filter(c =>
    c.content?.toLowerCase().includes(kw) ||
    (c.userName || '').toLowerCase().includes(kw)
  )
})

const navigateToContent = async (tab: 'all' | 'draft' = 'all') => {
  await router.replace('/creator')
  activeNav.value = 'content'
  activeContentTab.value = tab
  searchKeyword.value = ''
  await loadPosts()
}

const navigateToPublish = async (postId?: string) => {
  if (postId) {
    await router.replace({ path: '/creator', query: { edit: postId } })
  } else {
    await router.replace('/creator')
  }
  activeNav.value = 'publish'
}

const goHome = (wasEditingDraft?: boolean) => {
  const toDraftTab = wasEditingDraft ?? (typeof route.query.edit === 'string')
  navigateToContent(toDraftTab ? 'draft' : 'all')
}

const handlePublishSuccess = () => {
  navigateToContent('all')
}

const goToUser = (userId: string | number) => {
  open(`/user/${userId}`)
}

const goCreate = () => {
  navigateToPublish()
}

const viewPost = (postId: string) => {
  open(`/post/${postId}`)
}

const editPost = (postId: string) => {
  navigateToPublish(postId)
}

const openPreview = (post: PostVO) => {
  previewPost.value = post
  showPreviewModal.value = true
}

const closePreview = () => {
  showPreviewModal.value = false
  previewPost.value = null
  previewImageIndex.value = 0
  previewImageRatios.value = []
}

const previewParagraphs = computed(() => {
  return previewPost.value?.content?.split('\n').filter(p => p.trim()) || []
})

const publishPost = (postId: string) => {
  postToPublish.value = posts.value.find(p => p.id === postId) || null
  showPublishModal.value = true
}

const doPublish = async () => {
  if (!postToPublish.value) return

  if (!canPublish(postToPublish.value)) {
    showPublishModal.value = false
    ElMessage.warning('请先完善标题、分类、正文和图片后再发布')
    await editPost(postToPublish.value.id)
    postToPublish.value = null
    return
  }

  try {
    await updatePost({
      id: postToPublish.value.id,
      categoryId: postToPublish.value.categoryId,
      title: postToPublish.value.title,
      content: postToPublish.value.content,
      images: postToPublish.value.images,
      tags: postToPublish.value.tags,
      cover: postToPublish.value.cover,
      status: 1,
    })
    const post = posts.value.find(p => p.id === postToPublish.value?.id)
    if (post) post.status = 1
    ElMessage.success('发布成功')
    showPublishModal.value = false
    postToPublish.value = null
  } catch (error: any) {
    const msg = error?.response?.data?.msg
    if (msg) {
      ElMessage.warning(msg)
    } else {
      ElMessage.error('发布失败')
    }
    console.error('发布失败:', error)
  }
}

const cancelPublish = () => {
  showPublishModal.value = false
  postToPublish.value = null
}

const restorePost = (postId: string) => {
  postToRestore.value = posts.value.find(p => p.id === postId) || null
  showRestoreModal.value = true
}

const doRestore = async () => {
  if (!postToRestore.value) return
  try {
    await updatePost({ id: postToRestore.value.id, status: 0 })
    const post = posts.value.find(p => p.id === postToRestore.value?.id)
    if (post) post.status = 0
    ElMessage.success('已恢复到草稿')
    showRestoreModal.value = false
    postToRestore.value = null
  } catch (error) {
    console.error('恢复失败:', error)
    ElMessage.error('恢复失败')
  }
}

const cancelRestore = () => {
  showRestoreModal.value = false
  postToRestore.value = null
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
    const isPermanent = isDeleted(postToDelete.value.status)
    if (isPermanent) {
      showDeleteModal.value = false
      showPermanentDeleteModal.value = true
    } else {
      await deletePost(postToDelete.value.id)
      const post = posts.value.find(p => p.id === postToDelete.value?.id)
      if (post) post.status = 2
      ElMessage.success('已移入回收站')
      showDeleteModal.value = false
      postToDelete.value = null
    }
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

const doPermanentDelete = async () => {
  if (!postToDelete.value) return
  try {
    await permanentDeletePost(postToDelete.value.id)
    posts.value = posts.value.filter(p => p.id !== postToDelete.value?.id)
    ElMessage.success('彻底删除成功')
  } catch (error) {
    console.error('彻底删除失败:', error)
    ElMessage.error('彻底删除失败')
  } finally {
    showPermanentDeleteModal.value = false
    postToDelete.value = null
  }
}

const cancelPermanentDelete = () => {
  showPermanentDeleteModal.value = false
  postToDelete.value = null
}

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

const switchFansTab = (tab: 'followers' | 'following') => {
  activeFansTab.value = tab
  fansPage.value = 1
  fansSearchKeyword.value = ''
  loadFans()
}

const handleFansPageChange = (page: number) => {
  fansPage.value = page
  loadFans()
}

const loadRanking = async () => {
  try {
    rankingList.value = await getTopPosts(5) || []
  } catch (error) {
    console.error('加载排行榜失败:', error)
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
    const data = await getManagedComments(params)
    managedCommentsList.value = data.records || []
    commentsTotal.value = data.total || 0
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    commentsLoading.value = false
  }
}

const handleCommentPostFilterChange = () => {
  commentPage.value = 1
  commentSearchKeyword.value = ''
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
  commentToDelete.value = comment
  showDeleteCommentModal.value = true
}

const doDeleteComment = async () => {
  if (!commentToDelete.value) return
  try {
    await deleteComment(commentToDelete.value.id)
    ElMessage.success('删除成功')
    managedCommentsList.value = managedCommentsList.value.filter(c => c.id !== commentToDelete.value?.id)
    commentsTotal.value = Math.max(0, commentsTotal.value - 1)
  } catch (error) {
    console.error('删除评论失败:', error)
  } finally {
    showDeleteCommentModal.value = false
    commentToDelete.value = null
  }
}

const cancelDeleteComment = () => {
  showDeleteCommentModal.value = false
  commentToDelete.value = null
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
  postStatsState.loadMyStats()
  loadRanking()
  loadFollowStats()
  loadManagedComments()

  if (userStore.creatorActiveNav) {
    activeNav.value = userStore.creatorActiveNav as typeof activeNav.value
    if (activeNav.value === 'fans') {
      activeFansTab.value = userStore.creatorFansTab
    }
  }
  loadFans()
})

watch(activeNav, (val) => {
  userStore.creatorActiveNav = val
})
</script>

<style scoped>
.creator-container {
  display: flex;
  min-height: 100vh;
  background: var(--bg);
}

.sidebar {
  width: 200px;
  background: var(--card);
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
  background: var(--pink-bg);
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
  background: linear-gradient(135deg, var(--pink), var(--purple));
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
  background: linear-gradient(135deg, var(--pink) 0%, var(--purple) 100%);
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
  background: var(--card);
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
  grid-template-columns: repeat(auto-fit, minmax(180px, 220px));
  gap: 14px;
  justify-content: start;
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
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.recent-post-item:hover {
  border-color: var(--border);
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
  background: var(--card);
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

.filter-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-group {
  display: flex;
  gap: 6px;
  background: var(--pink-bg);
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
  background: var(--pink-bg-hover);
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
  background: var(--bg);
  border-radius: 12px;
  padding: 0 14px;
  border: 1.5px solid var(--border);
  transition: all 0.22s ease-out;
  flex-shrink: 0;
}

.search-box:focus-within {
  border-color: var(--border);
  background: var(--card);
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
  background: var(--card);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease-out;
  flex-shrink: 0;
}

.refresh-btn:hover {
  border-color: var(--pink);
  background: var(--pink-bg);
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
  margin-top: 16px;
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
  background: var(--card);
  border: 1.5px solid var(--border);
  box-shadow: 0 2px 8px rgba(180, 132, 255, 0.04);
  transition: all 0.22s ease-out;
}

.content-item:hover {
  border-color: var(--border);
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

.content-cover.clickable {
  cursor: pointer;
}

.content-cover.clickable:hover img {
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
  transition: color 0.22s ease-out;
}

.content-title.clickable {
  cursor: pointer;
}

.content-title.clickable:hover {
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
  background: var(--card);
}

.action-btn:hover {
  transform: scale(1.06);
  border-color: transparent;
}

.action-btn .action-icon,
.action-btn .action-icon-svg {
  width: 14px;
  height: 14px;
}

.action-btn.edit .action-icon {
  filter: brightness(0) saturate(100%) invert(66%) sepia(41%) saturate(2417%) hue-rotate(302deg) brightness(101%) contrast(101%);
}

.action-btn.edit:hover {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.12), rgba(180, 132, 255, 0.12));
  border-color: var(--border);
}

.action-btn.preview .action-icon-svg {
  color: #6366f1;
}

.action-btn.preview:hover {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1), rgba(139, 92, 246, 0.1));
  border-color: rgba(99, 102, 241, 0.3);
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
  background: var(--pink-bg);
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
  background: var(--pink-bg-hover);
  color: var(--pink);
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
  background: var(--pink-bg);
  border: 1.5px solid transparent;
  transition: all 0.22s ease-out;
  cursor: pointer;
}

.ranking-item:hover {
  border-color: var(--border);
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
  background: var(--pink-bg);
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
  background: var(--pink-bg-hover);
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
.comment-avatar.clickable,
.fan-name.clickable,
.comment-name.clickable {
  cursor: pointer;
  transition: opacity 0.15s;
}

.fan-avatar.clickable:hover,
.comment-avatar.clickable:hover,
.fan-name.clickable:hover,
.comment-name.clickable:hover {
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

.fan-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.fan-tag {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 12px;
  background: var(--pink-bg-hover);
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
  background: var(--bg);
  box-shadow: none;
  transition: all 0.22s ease-out;
}

.comment-post-select :deep(.el-input__wrapper:hover) {
  border-color: #FF6B9D;
  background: var(--bg);
}

.comment-post-select :deep(.el-input__wrapper.is-focus),
.comment-post-select :deep(.el-select__wrapper.is-focused),
.comment-post-select :deep(.el-input.is-focus .el-input__wrapper) {
  border-color: #FF6B9D !important;
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.15) !important;
  background: var(--card);
}

.comment-post-select :deep(.el-select__caret) {
  color: #FF6B9D;
}

.comment-post-select :deep(.el-select__placeholder) {
  color: var(--text-dim);
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
  color: var(--text-dim);
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
  color: var(--text-dim);
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
  color: var(--text);
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
  background: var(--pink-bg);
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
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.sort-btn:hover:not(.active) {
  color: var(--pink);
}

.sort-btn.active {
  background: var(--card);
  color: var(--pink);
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.sort-arrow {
  font-size: 11px;
  line-height: 1;
}

.magic-page {
  padding: 0;
}

.magic-banner {
  background: linear-gradient(135deg, var(--pink) 0%, var(--purple) 100%);
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
  background: var(--pink-bg);
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  border: 1.5px solid transparent;
  transition: all 0.22s ease-out;
}

.magic-card:hover {
  border-color: var(--border);
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
  background: var(--card);
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
  border: 1px solid var(--border);
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
  background: var(--card);
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
  border-color: var(--border);
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

<style>
/* el-select 下拉弹出层——非 scoped（popper 挂载到 body） */
.comment-post-select-popper .el-select-dropdown__item.is-selected {
  color: var(--pink);
  background: transparent;
  font-weight: 600;
}

.comment-post-select-popper .el-select-dropdown__item.hover,
.comment-post-select-popper .el-select-dropdown__item:hover {
  background: var(--pink-bg);
  color: var(--pink);
}

.comment-post-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.comment-post-option-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comment-post-option-status {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.comment-post-option-status.status-0 {
  color: #8b5cf6;
  background: rgba(139, 92, 246, 0.12);
}

.comment-post-option-status.status-1 {
  color: #16a34a;
  background: rgba(22, 163, 74, 0.12);
}

.comment-post-option-status.status-2 {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.12);
}

/* ===== 预览弹窗 ===== */
.preview-overlay {
  position: fixed;
  inset: 0;
  background: var(--overlay);
  backdrop-filter: blur(4px);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.preview-dialog {
  background: var(--card);
  border-radius: 20px;
  width: 100%;
  max-width: 720px;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 16px 48px rgba(120, 60, 160, 0.2);
}

.preview-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  background: var(--card);
  border-radius: 20px 20px 0 0;
  z-index: 1;
}

.preview-status-tag {
  font-size: 13px;
  padding: 4px 14px;
  border-radius: 8px;
  font-weight: 600;
}

.preview-status-tag.status-0 {
  background: rgba(255, 152, 0, 0.12);
  color: #f57c00;
}

.preview-status-tag.status-1 {
  background: rgba(76, 175, 80, 0.12);
  color: #2e7d32;
}

.preview-close-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.05);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;
  color: var(--text-dim);
}

.preview-close-btn:hover {
  background: var(--pink-bg-hover);
  color: var(--pink);
  transform: rotate(90deg);
}

.preview-close-btn svg {
  width: 16px;
  height: 16px;
}

.preview-dialog-body {
  padding: 24px;
}

.preview-category {
  margin-bottom: 12px;
}

.preview-category-tag {
  display: inline-block;
  font-size: 13px;
  color: var(--pink);
  background: var(--pink-bg-hover);
  padding: 3px 10px;
  border-radius: 8px;
  font-weight: 500;
}

.preview-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  line-height: 1.5;
  margin-bottom: 20px;
}

.preview-images {
  margin-bottom: 20px;
}

.preview-carousel {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  background: var(--bg);
}

.preview-carousel-track {
  display: flex;
  transition: transform 0.35s ease;
  height: 100%;
}

.preview-carousel-slide {
  min-width: 100%;
  height: 100%;
  object-fit: contain;
  background: var(--bg);
}

.preview-carousel-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.35);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease;
  z-index: 2;
}

.preview-carousel-arrow:hover {
  background: rgba(0, 0, 0, 0.55);
  transform: translateY(-50%) scale(1.08);
}

.preview-carousel-prev {
  left: 12px;
}

.preview-carousel-next {
  right: 12px;
}

.preview-carousel-dots {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  z-index: 2;
}

.preview-carousel-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.22s ease;
}

.preview-carousel-dot.active {
  background: white;
  transform: scale(1.3);
}

.preview-content {
  margin-bottom: 20px;
}

.preview-paragraph {
  font-size: 15px;
  line-height: 1.8;
  color: var(--text);
  margin-bottom: 12px;
}

.preview-paragraph:last-child {
  margin-bottom: 0;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
}

.preview-tag-item {
  font-size: 13px;
  color: var(--purple);
  background: var(--purple-bg);
  padding: 4px 12px;
  border-radius: 14px;
  font-weight: 500;
}

/* 预览弹窗过渡动画 */
.preview-fade-enter-active {
  transition: opacity 0.25s ease;
}
.preview-fade-enter-active .preview-dialog {
  transition: transform 0.25s ease, opacity 0.25s ease;
}
.preview-fade-leave-active {
  transition: opacity 0.2s ease;
}
.preview-fade-leave-active .preview-dialog {
  transition: transform 0.2s ease, opacity 0.2s ease;
}
.preview-fade-enter-from {
  opacity: 0;
}
.preview-fade-enter-from .preview-dialog {
  transform: scale(0.95) translateY(20px);
  opacity: 0;
}
.preview-fade-leave-to {
  opacity: 0;
}
.preview-fade-leave-to .preview-dialog {
  transform: scale(0.95) translateY(20px);
  opacity: 0;
}
</style>