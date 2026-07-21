<template>
  <div class="page-inner">
    <div class="post-card">
      <form class="post-form" @submit.prevent="handleSubmit">
          <div class="form-section">
            <label class="form-label">
              <Icon icon="ph:text-t" class="label-icon pink-icon" />
              <span>标题</span>
              <span class="label-required">*</span>
            </label>
            <div class="input-wrapper">
              <input
                v-model="form.title"
                type="text"
                class="form-input"
                placeholder="分享你的故事，给帖子起个吸引人的标题吧~"
                maxlength="100"
                required
              />
              <span class="char-count">{{ form.title.length }}/100</span>
            </div>
          </div>

          <div class="form-section">
            <label class="form-label">
              <Icon icon="ph:image" class="label-icon pink-icon" />
              <span>图片</span>
              <span class="label-required">*</span>
            </label>
            <div class="images-grid">
              <div
                v-for="(img, index) in form.images"
                :key="index"
                class="image-item"
              >
                <img :src="img" class="image-preview" />
                <button type="button" class="image-crop-btn" @click.stop="openImageCropper(img, index)" title="裁剪图片">
                  <Icon icon="ph:crop" class="image-crop-icon" />
                </button>
                <button type="button" class="image-remove" @click="removeImage(index)">
                  <Icon icon="ph:x" />
                </button>
              </div>
              <div
                v-if="form.images.length < 9"
                class="add-image-btn"
                :class="{ uploading: imageUploading }"
                @click="triggerImageUpload"
                @dragover.prevent
                @drop.prevent="handleImageDrop"
              >
                <template v-if="imageUploading">
                  <span class="upload-spinner"></span>
                  <span>上传中...</span>
                </template>
                <template v-else>
                  <Icon icon="ph:plus" />
                  <span>添加图片</span>
                </template>
              </div>
            </div>
            <input ref="imageInput" type="file" accept="image/*" multiple class="hidden-input" @change="handleImageChange" />
            <span class="field-hint">最多上传 9 张图片</span>
          </div>

          <div class="form-section">
            <label class="form-label">
              <Icon icon="ph:pencil-simple" class="label-icon pink-icon" />
              <span>正文内容</span>
              <span class="label-required">*</span>
            </label>
            <div class="textarea-wrapper">
              <textarea
                v-model="form.content"
                class="form-textarea"
                placeholder="写下你想分享的内容吧，支持换行哦~"
                rows="10"
                required
              ></textarea>
            </div>
          </div>

          <div class="form-section">
            <label class="form-label">
              <Icon icon="ph:folder" class="label-icon pink-icon" />
              <span>分类</span>
              <span class="label-required">*</span>
            </label>
            <div class="category-selector">
              <button
                v-for="cat in categories"
                :key="cat.id"
                type="button"
                class="category-btn"
                :class="{ active: form.categoryId === String(cat.id) }"
                @click="form.categoryId = String(cat.id)"
              >
                {{ cat.name }}
              </button>
            </div>
            <select v-model="form.categoryId" class="hidden-select" required>
              <option value="" disabled>请选择分类</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                {{ cat.name }}
              </option>
            </select>
          </div>

          <div class="form-section">
            <label class="form-label">
              <Icon icon="ph:tag" class="label-icon pink-icon" />
              <span>标签</span>
            </label>
            <div class="tags-container">
              <div v-for="(tag, index) in form.tags" :key="index" class="tag-chip">
                <span class="tag-text">{{ tag }}</span>
                <button type="button" class="tag-remove" @click="removeTag(index)">
                  <Icon icon="ph:x" />
                </button>
              </div>
              <div v-if="form.tags.length < 5" class="add-tag-wrapper">
                <input
                  v-model="tagInput"
                  type="text"
                  class="tag-input"
                  placeholder="输入标签，回车添加"
                  @keydown.enter.prevent="addTag"
                />
                <span class="tag-separator">/</span>
              </div>
            </div>
            <span class="field-hint">最多添加 5 个标签</span>
          </div>

          <div class="form-actions">
            <button
              v-if="!isEditingPublished"
              type="button"
              class="action-btn draft-btn"
              @click="saveDraft"
            >
              <Icon icon="ph:floppy-disk" />
              <span>{{ isEditMode ? '保存草稿' : '保存为草稿' }}</span>
            </button>
            <button
              type="submit"
              class="action-btn publish-btn"
              :disabled="loading"
            >
              <LoadingSpinner v-if="loading" inline text="" />
              <Icon v-else icon="ph:paper-plane-right" />
              <span>{{ loading ? '发布中...' : (isEditingPublished ? '更新发布' : '发布帖子') }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>

    <ImageCropper
      ref="imageCropperRef"
      :visible="showImageCropper"
      title="裁剪图片"
      :aspect-ratio="currentCropAspectRatio"
      :circular="false"
      :ratio-options="cropperRatioOptions"
      @crop="onImageCrop"
      @cancel="onImageCropCancel"
      @update:aspect-ratio="currentCropAspectRatio = $event"
    />
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, onBeforeRouteLeave } from 'vue-router'
import { Icon } from '@iconify/vue'
import { useNavigate } from '@/composables/useNavigate'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createPost, saveDraftPost, updatePost, getPostDetail, getCategoryList } from '@/api/post'
import { uploadPostImage, deleteUploadedFile } from '@/api/upload'
import type { SaveDraftRequest, PostVO, CategoryVO } from '@/api/post'
import { isDraft, isPublished, isDeleted } from '@/utils/postStatus'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import ImageCropper from '@/components/ImageCropper.vue'

const { to, router } = useNavigate()
const route = useRoute()

const emit = defineEmits<{ goBack: [wasEditingDraft?: boolean]; publishSuccess: [] }>()

const editPostId = computed(() => (route.query.edit as string) || undefined)
const isEditMode = computed(() => !!(editPostId.value || route.params.id))
const postId = computed(() => editPostId.value || (route.params.id as string))
const isInCreatorCenter = computed(() => route.path.startsWith('/creator'))

const currentPostStatus = ref<number | null>(null)
const isEditingDraft = computed(() => isEditMode.value && isDraft(currentPostStatus.value))
const isEditingPublished = computed(() => isEditMode.value && isPublished(currentPostStatus.value))

const categories = ref<CategoryVO[]>([])
const loading = ref(false)

const form = ref({
  title: '',
  categoryId: '',
  content: '',
  cover: '',
  images: [] as string[],
  tags: [] as string[],
})

const tagInput = ref('')
const imageInput = ref<HTMLInputElement | null>(null)
const imageUploading = ref(false)
const dirty = ref(false)
const formInitialized = ref(false)
const imageCropperRef = ref<InstanceType<typeof ImageCropper> | null>(null)
const showImageCropper = ref(false)
const currentCropImageUrl = ref('')
const currentCropImageIndex = ref(-1)
const currentCropAspectRatio = ref(4 / 3)

const cropperRatioOptions = [
  { label: '16:9', value: 16 / 9 },
  { label: '4:3', value: 4 / 3 },
  { label: '3:2', value: 3 / 2 },
  { label: '1:1', value: 1 },
  { label: '自由', value: 0 },
]

const loadCategories = async () => {
  try {
    categories.value = await getCategoryList()
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const loadPostDetail = async () => {
  if (!isEditMode.value) return
  try {
    const post = await getPostDetail(postId.value) as PostVO
    form.value = {
      title: post.title,
      categoryId: String(post.categoryId),
      content: post.content,
      cover: post.cover,
      images: post.images || [],
      tags: post.tags || [],
    }
    currentPostStatus.value = post.status
    // 已删除帖子不可编辑
    if (isDeleted(currentPostStatus.value)) {
      ElMessage.warning('已删除内容不可编辑，请先恢复')
      to('/creator')
      return
    }
  } catch (error) {
    console.error('加载帖子详情失败:', error)
    ElMessage.error('帖子不存在或已删除')
    to('/creator')
  }
}

const triggerImageUpload = () => {
  imageInput.value?.click()
}

const handleImageChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (files) {
    const remainingSlots = 9 - form.value.images.length
    const filesToAdd = Array.from(files).slice(0, remainingSlots)
    for (const file of filesToAdd) {
      await uploadImage(file)
    }
  }
  target.value = ''
}

const handleImageDrop = async (event: DragEvent) => {
  const files = event.dataTransfer?.files
  if (files) {
    const remainingSlots = 9 - form.value.images.length
    const filesToAdd = Array.from(files)
      .filter(f => f.type.startsWith('image/'))
      .slice(0, remainingSlots)
    for (const file of filesToAdd) {
      await uploadImage(file)
    }
  }
}

function openImageCropper(url: string, index: number) {
  currentCropImageUrl.value = url
  currentCropImageIndex.value = index
  currentCropAspectRatio.value = 4 / 3
  imageCropperRef.value?.setImageUrl(url)
  showImageCropper.value = true
}

async function onImageCrop(blob: Blob) {
  showImageCropper.value = false
  const file = new File([blob], 'post-image.jpg', { type: 'image/jpeg' })
  if (currentCropImageIndex.value >= 0) {
    await uploadAndReplace(currentCropImageIndex.value, file)
  }
  currentCropImageIndex.value = -1
  currentCropImageUrl.value = ''
}

function onImageCropCancel() {
  showImageCropper.value = false
  currentCropImageIndex.value = -1
  currentCropImageUrl.value = ''
}

async function uploadAndReplace(index: number, file: File) {
  const oldUrl = form.value.images[index]
  imageUploading.value = true
  try {
    const newUrl = await uploadPostImage(file)
    form.value.images[index] = newUrl
    if (form.value.cover === oldUrl) {
      form.value.cover = newUrl
    }
    dirty.value = true
    deleteUploadedFile(oldUrl).catch(() => {})
    ElMessage.success('裁剪完成')
  } catch (error) {
    ElMessage.error('图片上传失败')
    console.error('裁剪上传失败:', error)
  } finally {
    imageUploading.value = false
  }
}

const uploadImage = async (file: File) => {
  imageUploading.value = true
  try {
    const url = await uploadPostImage(file)
    form.value.images.push(url)
    dirty.value = true
    if (form.value.images.length === 1 && !form.value.cover) {
      form.value.cover = url
    }
  } catch (error) {
    ElMessage.error('图片上传失败')
    console.error('上传图片失败:', error)
  } finally {
    imageUploading.value = false
  }
}

const removeImage = (index: number) => {
  const removed = form.value.images[index]
  form.value.images.splice(index, 1)
  deleteUploadedFile(removed).catch(() => {})
  if (form.value.cover === removed) {
    form.value.cover = form.value.images[0] || ''
  }
}

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

const hasDraftContent = () => {
  return form.value.title.trim() !== ''
    || (form.value.categoryId !== null && form.value.categoryId !== '')
    || form.value.content.trim() !== ''
    || form.value.images.length > 0
}

const handleSubmit = async () => {
  if (!form.value.title || !form.value.categoryId || !form.value.content) {
    ElMessage.warning('请填写必填项')
    return
  }

  if (form.value.images.length === 0) {
    ElMessage.warning('请至少上传一张图片')
    return
  }

  loading.value = true
  try {
    const images = form.value.images.length > 0 ? form.value.images : []

    if (isEditMode.value) {
      await updatePost({
        id: postId.value,
        categoryId: Number(form.value.categoryId),
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
        categoryId: Number(form.value.categoryId),
        title: form.value.title,
        content: form.value.content,
        cover: form.value.cover || undefined,
        images,
        tags: form.value.tags.length > 0 ? form.value.tags : undefined,
      })
      ElMessage.success('发布成功')
    }

    dirty.value = false

    if (isInCreatorCenter.value) {
      emit('publishSuccess')
    } else {
      to('/creator')
    }
  } catch (error) {
    ElMessage.error(isEditingPublished.value ? '更新失败' : '发布失败')
    console.error('提交失败:', error)
  } finally {
    loading.value = false
  }
}

const saveDraftOnly = async (): Promise<boolean> => {
  if (!hasDraftContent()) {
    ElMessage.warning('请至少填写一项内容后再保存草稿')
    return false
  }

  loading.value = true
  try {
    const data: SaveDraftRequest = {
      categoryId: form.value.categoryId ? Number(form.value.categoryId) : undefined,
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
  } catch (error: any) {
    const msg = error?.response?.data?.msg || '保存失败'
    ElMessage.error(msg)
    console.error('保存草稿失败:', error)
    return false
  } finally {
    loading.value = false
  }
}

const saveDraft = async () => {
  if (!hasDraftContent()) {
    ElMessage.warning('请至少填写一项内容')
    return
  }
  const ok = await saveDraftOnly()
  if (!ok) return

  if (isInCreatorCenter.value) {
    emit('goBack', true)
  } else {
    to('/creator')
  }
}

const confirmLeave = async (): Promise<boolean> => {
  if (!dirty.value) return true

  try {
    await ElMessageBox.confirm('有未保存的内容，是否保存为草稿？', '提示', {
      confirmButtonText: '保存',
      cancelButtonText: '不保存',
      distinguishCancelAndClose: true,
      customClass: 'leave-confirm-dialog',
    })
    // 点击保存
    const ok = await saveDraftOnly()
    return ok
  } catch (action: any) {
    if (action === 'cancel') {
      // 点击不保存
      dirty.value = false
      return true
    }
    // 关闭弹窗
    return false
  }
}

const goBack = async () => {
  const canLeave = await confirmLeave()
  if (!canLeave) return

  if (isInCreatorCenter.value) {
    emit('goBack', isEditingDraft.value)
  } else {
    router.back()
  }
}

onMounted(async () => {
  loadCategories()
  await loadPostDetail()
  formInitialized.value = true
  window.addEventListener('beforeunload', handleBeforeUnload)
})

// 监听表单字段变化，标记 dirty
watch(
  () => [form.value.title, form.value.categoryId, form.value.content, form.value.cover],
  () => {
    if (formInitialized.value) {
      dirty.value = true
    }
  }
)

const handleBeforeUnload = (e: BeforeUnloadEvent) => {
  if (dirty.value) {
    e.preventDefault()
  }
}

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

onBeforeRouteLeave(async () => {
  return confirmLeave()
})

defineExpose({ dirty, confirmLeave })
</script>

<style scoped>
.page-inner {
  max-width: 760px;
  margin: 0 auto;
  padding: 0;
}

.post-card {
  background: var(--card);
  border-radius: 16px;
  padding: 28px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
}

.form-section {
  margin-bottom: 24px;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 8px;
}

.label-icon {
  font-size: 14px;
  width: 14px;
  height: 14px;
}

.label-required {
  color: #ff4d4f;
  font-size: 12px;
}

.input-wrapper {
  position: relative;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  padding-right: 70px;
  border-radius: 10px;
  border: 1px solid var(--border);
  font-size: 14px;
  color: var(--text);
  transition: all 0.22s ease-out;
  background: var(--card);
}

.form-input:focus {
  outline: none;
  border-color: var(--pink);
  box-shadow: 0 0 0 3px var(--pink-bg);
}

.form-input::placeholder {
  color: var(--text-dim);
}

.char-count {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  color: var(--text-dim);
}

.category-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-btn {
  padding: 8px 18px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid var(--border);
  background: var(--card);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.22s ease-out;
}

.category-btn:hover {
  border-color: var(--pink);
  color: var(--pink);
}

.category-btn.active {
  background: var(--pink-bg-hover);
  border-color: var(--pink);
  color: var(--pink);
}

.hidden-select {
  display: none;
}

.textarea-wrapper {
  position: relative;
}

.form-textarea {
  width: 100%;
  padding: 14px;
  border-radius: 10px;
  border: 1px solid var(--border);
  font-size: 14px;
  color: var(--text);
  transition: all 0.22s ease-out;
  background: var(--card);
  resize: vertical;
  font-family: inherit;
  line-height: 1.7;
  min-height: 200px;
}

.form-textarea:focus {
  outline: none;
  border-color: var(--pink);
  box-shadow: 0 0 0 3px var(--pink-bg);
}

.form-textarea::placeholder {
  color: var(--text-dim);
}

.images-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 10px;
  overflow: hidden;
}

.image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-remove {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease-out;
}

.image-remove svg {
  width: 12px;
  height: 12px;
}

.image-remove:hover {
  background: rgba(255, 71, 87, 0.9);
}

.image-crop-btn {
  position: absolute;
  top: 6px;
  left: 6px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease-out;
  opacity: 1;
}

.image-crop-btn svg {
  width: 12px;
  height: 12px;
}

.image-crop-btn:hover {
  background: rgba(255, 107, 157, 0.9);
}

.add-image-btn {
  aspect-ratio: 1;
  border-radius: 10px;
  border: 1px dashed var(--border);
  background: var(--pink-bg);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  color: var(--text-dim);
  transition: all 0.22s ease-out;
}

.add-image-btn svg {
  width: 20px;
  height: 20px;
}

.add-image-btn span {
  font-size: 11px;
}

.add-image-btn:hover {
  border-color: var(--pink);
  background: var(--pink-bg);
  color: var(--pink);
}

.add-image-btn.uploading {
  border-color: var(--pink);
  color: var(--pink);
  cursor: not-allowed;
  pointer-events: none;
}

.upload-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--border);
  border-top-color: var(--pink);
  border-radius: 50%;
  animation: uploadSpin 0.6s linear infinite;
}

@keyframes uploadSpin {
  to { transform: rotate(360deg); }
}

.field-hint {
  display: block;
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 10px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  min-height: 48px;
  background: var(--pink-bg);
}

.tag-chip {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 14px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.15), rgba(180, 132, 255, 0.15));
  color: var(--pink);
  font-size: 13px;
  font-weight: 500;
}

.tag-text {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-remove {
  background: var(--border);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.tag-remove:hover {
  background: rgba(255, 71, 87, 0.8);
  color: white;
}

.tag-remove svg {
  width: 12px;
  height: 12px;
}

.add-tag-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 140px;
}

.tag-input {
  flex: 1;
  border: none;
  background: rgba(0, 0, 0, 0.03);
  font-size: 13px;
  color: var(--text);
  outline: none;
  min-width: 100px;
  padding: 6px 12px;
  border-radius: 8px;
}

.tag-input::placeholder {
  color: var(--text-dim);
}

.tag-separator {
  color: var(--text-dim);
  font-size: 14px;
  opacity: 0.6;
}

.field-hint {
  display: block;
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 8px;
  padding-left: 4px;
}

.form-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--border), transparent);
  margin: 16px 0;
}

.form-actions {
  display: flex;
  gap: 14px;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 22px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
}

.action-btn svg {
  width: 14px;
  height: 14px;
}

.draft-btn {
  background: rgba(255, 255, 255, 0.9);
  color: var(--purple);
  border: 1.5px solid var(--purple);
}

.draft-btn:hover {
  background: var(--purple-bg);
}

.publish-btn {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  box-shadow: 0 4px 20px var(--shadow-lg);
}

.publish-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px var(--shadow-lg);
}

.publish-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.hidden-input {
  display: none;
}

@media (max-width: 768px) {
  .page-inner {
    padding: 0 16px;
  }

  .post-card {
    padding: 20px;
  }

  .page-title {
    font-size: 20px;
  }

  .images-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .form-actions {
    flex-direction: column;
  }

  .action-btn {
    width: 100%;
    justify-content: center;
  }

  .category-btn {
    padding: 8px 16px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .images-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

<style>
.leave-confirm-dialog {
  border-radius: 16px;
  padding-bottom: 8px;
  box-shadow: 0 12px 48px var(--shadow-lg);
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
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
  background: linear-gradient(135deg, var(--pink), var(--purple));
  border: none;
  box-shadow: 0 4px 14px var(--shadow-lg);
}

.leave-confirm-dialog .el-button--primary:hover {
  background: linear-gradient(135deg, #ff7db0, #d190f5);
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