<template>
  <div class="creator-container">
    <aside class="sidebar">
      <nav class="sidebar-nav">
        <div class="nav-section">
        <button class="nav-item nav-item-primary" :class="{ active: activeNav === 'publish' }" @click="switchNav('publish')">
          <Icon icon="ph:pencil-simple" class="nav-icon pink-icon" />
          <span class="nav-text">发布</span>
        </button>
        <button class="nav-item" :class="{ active: activeNav === 'home' }" @click="switchNav('home')">
          <Icon icon="ph:house" class="nav-icon pink-icon" />
          <span class="nav-text">创作首页</span>
        </button>
      </div>

        <div class="nav-divider"></div>

        <div class="nav-section">
          <button class="nav-item" :class="{ active: activeNav === 'content' }" @click="switchNav('content')">
            <Icon icon="ph:files" class="nav-icon pink-icon" />
            <span class="nav-text">内容管理</span>
          </button>
          <button class="nav-item" :class="{ active: activeNav === 'data' }" @click="switchNav('data')">
            <Icon icon="ph:chart-bar" class="nav-icon pink-icon" />
            <span class="nav-text">数据中心</span>
          </button>
        </div>

        <div class="nav-divider"></div>

        <div class="nav-section">
          <button class="nav-item" :class="{ active: activeNav === 'fans' }" @click="switchNav('fans')">
            <Icon icon="ph:users" class="nav-icon pink-icon" />
            <span class="nav-text">粉丝管理</span>
          </button>
          <button class="nav-item" :class="{ active: activeNav === 'interaction' }" @click="switchNav('interaction')">
            <Icon icon="ph:chat-circle-dots" class="nav-icon pink-icon" />
            <span class="nav-text">评论管理</span>
          </button>
        </div>

        <div class="nav-divider"></div>

        <div class="nav-section">
          <button class="nav-item" :class="{ active: activeNav === 'magic' }" @click="switchNav('magic')">
            <Icon icon="ph:magic-wand" class="nav-icon pink-icon" />
            <span class="nav-text">妙笔</span>
          </button>
        </div>

        <div class="nav-divider"></div>

        <div class="nav-section">
          <button class="nav-item" :class="{ active: activeNav === 'agreement' }" @click="switchNav('agreement')">
            <Icon icon="ph:shield-check" class="nav-icon pink-icon" />
            <span class="nav-text">社区公约</span>
          </button>
        </div>
      </nav>
    </aside>

    <main class="main-content">
      <PostCreate ref="postCreateRef" v-if="activeNav === 'publish'" @go-back="goHome" @publish-success="handlePublishSuccess" />
      <CreatorHome v-else-if="activeNav === 'home'" @go-create="goCreate" @go-data="switchNav('data')" @go-content="goContent" @go-interaction="switchNav('interaction')" @go-fans="switchNav('fans')" @edit-post="editPost" />
      <CreatorContent ref="contentRef" v-else-if="activeNav === 'content'" @edit="editPost" @view="viewPost" @create-post="goCreate" @publish="publishPost" @restore="restorePost" @delete="handleDeletePost" @preview="openPreview" />
      <CreatorData v-else-if="activeNav === 'data'" @go-post="viewPost" />
      <CreatorFans v-else-if="activeNav === 'fans'" />
      <CreatorInteraction v-else-if="activeNav === 'interaction'" @delete-comment="handleDeleteComment" />
      <CreatorMagic v-if="activeNav === 'magic'" />
      <CreatorAgreement v-else-if="activeNav === 'agreement'" />
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
    />

    <ConfirmModal
      v-model:visible="showPublishModal"
      title="确认发布"
      :post-title="postToPublish?.title"
      hint="发布后帖子将公开显示，所有用户均可查看。"
      confirm-text="确认发布"
      @confirm="doPublish"
      @cancel="cancelPublish"
    />

    <ConfirmModal
      v-model:visible="showRestoreModal"
      title="确认恢复"
      :post-title="postToRestore?.title"
      hint="恢复后帖子将回到草稿箱，你可以继续编辑后再发布。"
      confirm-text="确认恢复"
      @confirm="doRestore"
      @cancel="cancelRestore"
    />

    <ConfirmModal
      v-model:visible="showDeleteCommentModal"
      title="删除评论"
      :hint="commentToDelete ? `确定删除${commentToDelete.userName}的评论吗？删除后不可恢复` : ''"
      confirm-text="删除"
      danger
      @confirm="doDeleteComment"
      @cancel="cancelDeleteComment"
    />

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
    />

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
                <Icon icon="ph:x" />
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
                    <Icon icon="ph:caret-left" class="carousel-arrow-icon" />
                  </button>
                  <button v-if="previewPost.images.length > 1" class="preview-carousel-arrow preview-carousel-next" @click="previewImageIndex = Math.min((previewPost.images?.length ?? 1) - 1, previewImageIndex + 1)">
                    <Icon icon="ph:caret-right" class="carousel-arrow-icon" />
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
import { ref, computed, onMounted, watch, defineAsyncComponent } from 'vue'
import { useRoute } from 'vue-router'
import { useNavigate } from '@/composables/useNavigate'
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'
import { deletePost, permanentDeletePost, updatePost } from '@/api/post'
import type { PostVO } from '@/api/post'
import { useUserStore } from '@/stores/user'
import type { CommentVO } from '@/api/comment'
import { deleteComment } from '@/api/comment'
import PostCreate from '@/views/PostCreate.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import { isDeleted, statusText, canPublish } from '@/utils/postStatus'

// 创作中心子组件按需懒加载，首次进入时不加载未使用的 Tab 组件
const CreatorHome = defineAsyncComponent(() => import('@/components/creator/CreatorHome.vue'))
const CreatorContent = defineAsyncComponent(() => import('@/components/creator/CreatorContent.vue'))
const CreatorData = defineAsyncComponent(() => import('@/components/creator/CreatorData.vue'))
const CreatorFans = defineAsyncComponent(() => import('@/components/creator/CreatorFans.vue'))
const CreatorInteraction = defineAsyncComponent(() => import('@/components/creator/CreatorInteraction.vue'))
const CreatorMagic = defineAsyncComponent(() => import('@/components/creator/CreatorMagic.vue'))
const CreatorAgreement = defineAsyncComponent(() => import('@/components/creator/CreatorAgreement.vue'))

const { open, router } = useNavigate()
const route = useRoute()
const userStore = useUserStore()

const activeNav = ref<'home' | 'content' | 'data' | 'fans' | 'interaction' | 'magic' | 'agreement' | 'publish'>('home')
const postCreateRef = ref<InstanceType<typeof PostCreate>>()
const contentRef = ref<InstanceType<typeof CreatorContent>>()

const showDeleteModal = ref(false)
const showPublishModal = ref(false)
const showRestoreModal = ref(false)
const showDeleteCommentModal = ref(false)
const showPermanentDeleteModal = ref(false)
const showPreviewModal = ref(false)

const postToDelete = ref<PostVO | null>(null)
const postToPublish = ref<PostVO | null>(null)
const postToRestore = ref<PostVO | null>(null)
const commentToDelete = ref<CommentVO | null>(null)

const iconEdit = 'ph:pencil-simple'
const iconEye = 'ph:eye'
const iconLike = 'ph:heart'
const iconFavorite = 'ph:star'
const iconFans = 'ph:users'
const iconComment = 'ph:chat-circle-text'
const iconEmpty = 'ph:tray'

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

const previewParagraphs = computed(() => {
  return previewPost.value?.content?.split('\n').filter(p => p.trim()) || []
})

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

const switchNav = async (nav: typeof activeNav.value) => {
  if (activeNav.value === 'publish' && nav !== 'publish' && postCreateRef.value) {
    const canLeave = await postCreateRef.value.confirmLeave()
    if (!canLeave) return
  }
  if (activeNav.value === 'publish' && nav !== 'publish') {
    router.replace('/creator')
  }
  activeNav.value = nav
}

const navigateToContent = async (tab: 'all' | 'draft' = 'all') => {
  await router.replace('/creator')
  activeNav.value = 'content'
  void tab
  contentRef.value?.refreshPosts()
}

const goContent = (tab: 'all' | 'draft' = 'all') => {
  navigateToContent(tab)
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

const goCreate = () => {
  navigateToPublish()
}

const viewPost = (postId: string) => {
  open(`/post/${postId}`)
}

const editPost = (postId: string) => {
  navigateToPublish(postId)
}

const publishPost = (postId: string) => {
  const posts = contentRef.value?.posts
  postToPublish.value = posts?.find(p => p.id === postId) || null
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
    ElMessage.success('发布成功')
    showPublishModal.value = false
    postToPublish.value = null
    contentRef.value?.refreshPosts()
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
  const posts = contentRef.value?.posts
  postToRestore.value = posts?.find(p => p.id === postId) || null
  showRestoreModal.value = true
}

const doRestore = async () => {
  if (!postToRestore.value) return
  try {
    await updatePost({ id: postToRestore.value.id, status: 0 })
    ElMessage.success('已恢复到草稿')
    showRestoreModal.value = false
    postToRestore.value = null
    contentRef.value?.refreshPosts()
  } catch (error) {
    console.error('恢复失败:', error)
    ElMessage.error('恢复失败')
  }
}

const cancelRestore = () => {
  showRestoreModal.value = false
  postToRestore.value = null
}

const handleDeletePost = (post: PostVO) => {
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
      ElMessage.success('已移入回收站')
      showDeleteModal.value = false
      postToDelete.value = null
      contentRef.value?.refreshPosts()
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
    ElMessage.success('彻底删除成功')
    contentRef.value?.refreshPosts()
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

const handleDeleteComment = (comment: CommentVO) => {
  commentToDelete.value = comment
  showDeleteCommentModal.value = true
}

const doDeleteComment = async () => {
  if (!commentToDelete.value) return
  try {
    await deleteComment(commentToDelete.value.id)
    ElMessage.success('删除成功')
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

onMounted(() => {
  if (userStore.creatorActiveNav) {
    activeNav.value = userStore.creatorActiveNav as typeof activeNav.value
  }
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
  color: var(--white);
  margin-bottom: 8px;
}

.nav-item-primary:hover {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: var(--white);
}

.nav-item-primary.active {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: var(--white);
}

.nav-item-primary .nav-icon {
  color: var(--white);
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

.stat-mini-icon {
  width: 14px;
  height: 14px;
  vertical-align: middle;
}

.create-btn {
  padding: 12px 32px;
  border-radius: 25px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: var(--white);
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

/* ===== 状态标签 ===== */
.status-0 {
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.12), rgba(255, 152, 0, 0.12));
  color: var(--warning);
}

.status-1 {
  background: linear-gradient(135deg, rgba(76, 175, 80, 0.12), rgba(56, 142, 60, 0.12));
  color: var(--success);
}

.status-2 {
  background: linear-gradient(135deg, rgba(244, 67, 54, 0.12), rgba(211, 47, 47, 0.12));
  color: var(--error);
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
  color: var(--warning);
}

.preview-status-tag.status-1 {
  background: rgba(76, 175, 80, 0.12);
  color: var(--success);
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
  color: var(--white);
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
  background: var(--card);
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

/* ===== 响应式 ===== */
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

  .nav-item {
    justify-content: center;
    padding: 12px;
  }

  .main-content {
    margin-left: 60px;
    padding: 90px 20px 20px;
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
  color: var(--success);
  background: rgba(22, 163, 74, 0.12);
}

.comment-post-option-status.status-2 {
  color: var(--error);
  background: rgba(239, 68, 68, 0.12);
}
</style>