import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useApi } from '@/composables/useApi'
import { createPost, saveDraftPost, updatePost, getPostDetail } from '@/api/post'
import { getCircleList, getJoinedCircles, getCircleSections } from '@/api/circle'
import type { CircleVO, CircleSectionVO } from '@/api/circle'
import type { SaveDraftRequest, PostVO } from '@/api/post'
import { isDraft, isPublished, isDeleted } from '@/utils/postStatus'
import { useUserStore } from '@/stores/user'
import { useNavigate } from '@/composables/useNavigate'

export type PostType = 'NORMAL' | 'ARTICLE'

export interface PostFormData {
  postType: string
  title: string
  sectionId: number | null   // 圈内板块，选圈后从该圈子已启用的板块中加载
  circleId: number | null
  content: string
  cover: string
  images: string[]
  tags: string[]
}

export function usePostEditor(postType: PostType) {
  const route = useRoute()
  const userStore = useUserStore()
  const { to } = useNavigate()

  const editPostId = computed(() => (route.query.edit as string) || undefined)
  const isEditMode = computed(() => !!(editPostId.value || route.params.id))
  const postId = computed(() => editPostId.value || (route.params.id as string))
  const isInCreatorCenter = computed(() => route.path.startsWith('/creator'))

  const currentPostStatus = ref<number | null>(null)
  const isEditingDraft = computed(() => isEditMode.value && isDraft(currentPostStatus.value))
  const isEditingPublished = computed(() => isEditMode.value && isPublished(currentPostStatus.value))

  // 当前选中圈子对应的板块列表（圈子级联加载）
  const sections = ref<CircleSectionVO[]>([])
  const circles = ref<CircleVO[]>([])
  const joinedCircleIds = ref<Set<number>>(new Set())

  // 只展示已加入的圈子
  const sortedCircles = computed(() => {
    return circles.value.filter(c => joinedCircleIds.value.has(c.id))
  })

  const { loading: submitLoading, run: submit } = useApi()

  const form = ref<PostFormData>({
    postType,
    title: '',
    sectionId: null,
    circleId: null,
    content: '',
    cover: '',
    images: [],
    tags: [],
  })

  const tagInput = ref('')
  const dirty = ref(false)
  const formInitialized = ref(false)

  const addTag = () => {
    const tag = tagInput.value.trim()
    if (tag && !form.value.tags.includes(tag) && form.value.tags.length < 5) {
      form.value.tags.push(tag)
      tagInput.value = ''
    }
  }

  const removeTag = (index: number) => {
    form.value.tags.splice(index, 1)
  }

  /**
   * 根据圈子 ID 加载该圈子已启用的板块列表
   * 圈子切换时调用，确保只展示该圈子配置的板块
   */
  const loadSectionsForCircle = async (circleId: number) => {
    try {
      sections.value = await getCircleSections(circleId)
    } catch {
      sections.value = []
    }
  }

  /**
   * 加载全量圈子列表 + 当前用户已加入的圈子 ID
   * 如果从圈子详情页跳转过来，自动预填目标圈子
   */
  const loadCircles = async () => {
    try {
      circles.value = await getCircleList()
      try {
        const joined = await getJoinedCircles()
        joinedCircleIds.value = new Set(joined.map((c: CircleVO) => c.id))
      } catch { /* 未登录忽略 */ }
      // 从圈子详情页跳转发布时，预填圈子
      if (userStore.pendingCircleId && !isEditMode.value) {
        form.value.circleId = userStore.pendingCircleId
        userStore.pendingCircleId = null
      }
    } catch (error) {
      console.error('加载圈子失败:', error)
    }
  }

  /**
   * 编辑模式下回填帖子原数据
   * 已删除的帖子不可编辑，自动跳回创作中心
   */
  const loadPostDetail = async () => {
    if (!isEditMode.value) return
    try {
      const post = await getPostDetail(postId.value) as PostVO
      form.value = {
        postType: (post as any).postType || postType,
        title: post.title,
        sectionId: (post as any).sectionId || null,
        circleId: post.circleId || null,
        content: post.content,
        cover: post.cover,
        images: post.images || [],
        tags: post.tags || [],
      }
      currentPostStatus.value = post.status
      if (isDeleted(currentPostStatus.value)) {
        ElMessage.warning('已删除内容不可编辑，请先恢复')
        to('/creator')
        return false
      }
      return true
    } catch (error) {
      console.error('加载帖子详情失败:', error)
      ElMessage.error('帖子不存在或已删除')
      to('/creator')
      return false
    }
  }

  /**
   * 判断草稿是否至少有一项内容（标题/圈子/正文/图片）
   */
  const hasDraftContent = () => {
    return form.value.title.trim() !== ''
      || form.value.circleId !== null
      || form.value.content.trim() !== ''
      || form.value.images.length > 0
  }

  /**
   * 发布前校验：标题、圈子、正文必填，图文帖至少一张图片
   */
  const validateForm = (): string | null => {
    if (!form.value.title) return '请填写标题'
    if (!form.value.circleId) return '请选择圈子'
    if (!form.value.content) return '请填写正文内容'
    if (postType === 'NORMAL' && form.value.images.length === 0) return '图文帖请至少上传一张图片'
    return null
  }

  /**
   * 提交发布：创建新帖或更新已有帖子，status 设为 1（待审核）
   */
  const doSubmit = async (): Promise<boolean> => {
    const error = validateForm()
    if (error) {
      ElMessage.warning(error)
      return false
    }

    const result = await submit(async () => {
      const images = form.value.images.length > 0 ? form.value.images : []

      if (isEditMode.value) {
        await updatePost({
          id: postId.value,
          postType: form.value.postType,
          sectionId: form.value.sectionId ?? undefined,
          circleId: form.value.circleId ?? undefined,
          title: form.value.title,
          content: form.value.content,
          cover: form.value.cover || undefined,
          images,
          tags: form.value.tags.length > 0 ? form.value.tags : undefined,
          status: 1,
        })
        ElMessage.success(isEditingPublished.value ? '更新成功' : '发布成功')
      } else {
        await createPost({
          postType: form.value.postType,
          sectionId: form.value.sectionId ?? undefined,
          circleId: form.value.circleId!,
          title: form.value.title,
          content: form.value.content,
          cover: form.value.cover || undefined,
          images,
          tags: form.value.tags.length > 0 ? form.value.tags : undefined,
        })
        ElMessage.success('发布成功')
      }

      dirty.value = false
      return true
    }, { onError: () => ElMessage.error(isEditingPublished.value ? '更新失败' : '发布失败') })

    return result ?? false
  }

  /**
   * 保存草稿：至少一项有内容即可保存，不校验完整性
   */
  const saveDraftOnly = async (): Promise<boolean> => {
    if (!hasDraftContent()) {
      ElMessage.warning('请至少填写一项内容后再保存草稿')
      return false
    }

    const result = await submit(async () => {
      const data: SaveDraftRequest = {
        postType: form.value.postType,
        sectionId: form.value.sectionId ?? undefined,
        circleId: form.value.circleId ?? undefined,
        title: form.value.title || undefined,
        content: form.value.content || undefined,
        cover: form.value.cover || undefined,
        images: form.value.images.length > 0 ? form.value.images : undefined,
        tags: form.value.tags.length > 0 ? form.value.tags : undefined,
      }

      if (isEditMode.value) {
        await updatePost({ ...data, id: postId.value } as any)
      } else {
        await saveDraftPost(data)
      }

      dirty.value = false
      ElMessage.success('草稿保存成功')
      return true
    }, { onError: (e: any) => {
      const msg = e?.response?.data?.msg || '保存失败'
      ElMessage.error(msg)
    }})

    return result ?? false
  }

  /**
   * 页面离开确认：有未保存内容时询问是否保存草稿
   */
  const confirmLeave = async (): Promise<boolean> => {
    if (!dirty.value) return true

    try {
      await ElMessageBox.confirm('有未保存的内容，是否保存为草稿？', '提示', {
        confirmButtonText: '保存',
        cancelButtonText: '不保存',
        distinguishCancelAndClose: true,
        customClass: 'leave-confirm-dialog',
      })
      const ok = await saveDraftOnly()
      return ok
    } catch (action: any) {
      if (action === 'cancel') {
        dirty.value = false
        return true
      }
      return false
    }
  }

  const handleBeforeUnload = (e: BeforeUnloadEvent) => {
    if (dirty.value) e.preventDefault()
  }

  onMounted(async () => {
    loadCircles()
    await loadPostDetail()
    // 编辑模式下，回填了 circleId 后需加载该圈子的板块列表
    if (form.value.circleId) {
      await loadSectionsForCircle(form.value.circleId)
    }
    formInitialized.value = true
    window.addEventListener('beforeunload', handleBeforeUnload)
  })

  // 表单内容变化时标记脏状态，用于离开确认和 beforeunload
  watch(
    () => [form.value.title, form.value.circleId, form.value.sectionId, form.value.content, form.value.cover],
    () => {
      if (formInitialized.value) dirty.value = true
    }
  )

  onBeforeUnmount(() => {
    window.removeEventListener('beforeunload', handleBeforeUnload)
  })

  onBeforeRouteLeave(async () => {
    return confirmLeave()
  })

  return {
    form,
    sections,
    circles,
    sortedCircles,
    tagInput,
    addTag,
    removeTag,
    submitLoading,
    dirty,
    isEditMode,
    isEditingDraft,
    isEditingPublished,
    isInCreatorCenter,
    postId,
    hasDraftContent,
    doSubmit,
    saveDraftOnly,
    confirmLeave,
    loadSectionsForCircle,
  }
}
