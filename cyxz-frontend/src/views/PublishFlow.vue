<template>
  <PostSelect
    v-if="phase === 'select'"
    @go-image-edit="goImageEdit"
    @go-article-edit="phase = 'article-edit'"
  />
  <PostCreate
    v-else-if="phase === 'image-edit'"
    ref="currentEditorRef"
    :initial-images="initialImages"
    @go-back="goBackFromEdit"
    @publish-success="$emit('publishSuccess')"
  />
  <ArticleCreate
    v-else-if="phase === 'article-edit'"
    ref="currentEditorRef"
    @go-back="goBackFromEdit"
    @publish-success="$emit('publishSuccess')"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPostDetail } from '@/api/post'
import type { PostVO } from '@/api/post'
import PostSelect from '@/views/PostSelect.vue'
import PostCreate from '@/views/PostCreate.vue'
import ArticleCreate from '@/views/ArticleCreate.vue'

type Phase = 'select' | 'image-edit' | 'article-edit'

const emit = defineEmits<{
  goBack: [wasEditingDraft?: boolean]
  publishSuccess: []
}>()

const route = useRoute()

const phase = ref<Phase>('select')
const initialImages = ref<string[]>([])
const currentEditorRef = ref<InstanceType<typeof PostCreate> | InstanceType<typeof ArticleCreate> | null>(null)

const isEditMode = computed(() => !!(route.query.edit || route.params.id))

const goImageEdit = (images: string[]) => {
  initialImages.value = images
  phase.value = 'image-edit'
}

const goBackFromEdit = (wasEditingDraft?: boolean) => {
  if (isEditMode.value) {
    emit('goBack', wasEditingDraft)
  } else {
    phase.value = 'select'
  }
}

// 编辑模式：加载帖子确定类型
onMounted(async () => {
  if (!isEditMode.value) return

  const postId = (route.query.edit as string) || (route.params.id as string)
  try {
    const post = await getPostDetail(postId) as PostVO
    const postType = (post as any).postType || 'NORMAL'
    if (postType === 'ARTICLE') {
      phase.value = 'article-edit'
    } else {
      initialImages.value = post.images || []
      phase.value = 'image-edit'
    }
  } catch {
    phase.value = 'image-edit' // 降级
  }
})

// 代理确认离开
const confirmLeave = async (): Promise<boolean> => {
  if (currentEditorRef.value && 'confirmLeave' in currentEditorRef.value) {
    return (currentEditorRef.value as any).confirmLeave()
  }
  return true
}

const dirty = computed(() => {
  if (currentEditorRef.value && 'dirty' in currentEditorRef.value) {
    return (currentEditorRef.value as any).dirty
  }
  return false
})

defineExpose({ confirmLeave, dirty })
</script>

<style>
.leave-confirm-dialog {
  border-radius: 16px;
  padding-bottom: 8px;
  box-shadow: 0 12px 48px var(--shadow-lg);
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
}

html.dark .leave-confirm-dialog {
  background: rgba(30, 26, 50, 0.94);
}

.leave-confirm-dialog .el-message-box__header {
  padding: 20px 24px 0;
}

.leave-confirm-dialog .el-message-box__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text);
}

.leave-confirm-dialog .el-message-box__close {
  color: var(--text-dim);
  transition: color 0.2s, transform 0.25s;
}

.leave-confirm-dialog .el-message-box__close:hover {
  color: var(--pink);
  transform: rotate(90deg);
}

.leave-confirm-dialog .el-message-box__content {
  padding: 12px 24px 20px;
}

.leave-confirm-dialog .el-message-box__message {
  color: var(--text-dim);
  font-size: 14px;
  line-height: 1.6;
}

.leave-confirm-dialog .el-message-box__btns {
  padding: 0 24px 20px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.leave-confirm-dialog .el-button {
  border-radius: 10px;
  padding: 9px 22px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.22s ease;
}

.leave-confirm-dialog .el-button--primary {
  background: var(--gradient-brand);
  border: none;
  box-shadow: 0 4px 14px var(--shadow-lg);
}

.leave-confirm-dialog .el-button--primary:hover {
  background: var(--gradient-brand-hover);
  box-shadow: 0 6px 20px var(--shadow-lg);
  transform: translateY(-1px);
}

.leave-confirm-dialog .el-button--primary:active {
  transform: translateY(0);
}

.leave-confirm-dialog .el-button:not(.el-button--primary) {
  background: transparent;
  border: 1.5px solid var(--border);
  color: var(--text-dim);
}

.leave-confirm-dialog .el-button:not(.el-button--primary):hover {
  border-color: var(--purple);
  color: var(--text-dim);
  background: var(--purple-bg);
}
</style>
