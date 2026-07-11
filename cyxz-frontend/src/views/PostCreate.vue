<template>
  <div class="page-inner">
    <div class="post-card">
      <form class="post-form" @submit.prevent="handleSubmit">
          <div class="form-section">
            <label class="form-label">
              <img src="@/assets/icons/edit.svg" alt="edit" class="label-icon" />
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
              <img src="@/assets/icons/image.svg" alt="image" class="label-icon" />
              <span>封面</span>
            </label>
            <div class="upload-area cover-area" @click="triggerCoverUpload" @dragover.prevent @drop.prevent="handleCoverDrop">
              <div v-if="form.cover" class="cover-preview-wrapper">
                <img :src="form.cover" class="cover-preview" />
                <button type="button" class="remove-btn" @click.stop="removeCover">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"/>
                    <line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
              </div>
              <div v-else class="upload-placeholder">
                <div class="upload-icon-wrapper">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                    <circle cx="8.5" cy="8.5" r="1.5"/>
                    <polyline points="21 15 16 10 5 21"/>
                  </svg>
                </div>
                <span class="upload-title">上传封面</span>
                <span class="upload-desc">点击或拖拽上传，建议尺寸 800×600</span>
              </div>
            </div>
            <input ref="coverInput" type="file" accept="image/*" class="hidden-input" @change="handleCoverChange" />
          </div>

          <div class="form-section">
            <label class="form-label">
              <img src="@/assets/icons/image.svg" alt="image" class="label-icon" />
              <span>图片</span>
            </label>
            <div class="images-grid">
              <div
                v-for="(img, index) in form.images"
                :key="index"
                class="image-item"
              >
                <img :src="img" class="image-preview" />
                <button type="button" class="image-remove" @click="removeImage(index)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"/>
                    <line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
              </div>
              <div
                v-if="form.images.length < 9"
                class="add-image-btn"
                @click="triggerImageUpload"
                @dragover.prevent
                @drop.prevent="handleImageDrop"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M12 5v14M5 12h14"/>
                </svg>
                <span>添加图片</span>
              </div>
            </div>
            <input ref="imageInput" type="file" accept="image/*" multiple class="hidden-input" @change="handleImageChange" />
            <span class="field-hint">最多上传 9 张图片</span>
          </div>

          <div class="form-section">
            <label class="form-label">
              <img src="@/assets/icons/edit.svg" alt="edit" class="label-icon" />
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
              <img src="@/assets/icons/tag.svg" alt="tag" class="label-icon" />
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
              <img src="@/assets/icons/tag.svg" alt="tag" class="label-icon" />
              <span>标签</span>
            </label>
            <div class="tags-container">
              <div v-for="(tag, index) in form.tags" :key="index" class="tag-chip">
                <span class="tag-text">{{ tag }}</span>
                <button type="button" class="tag-remove" @click="removeTag(index)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"/>
                    <line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
              </div>
              <div v-if="form.tags.length < 5" class="add-tag-wrapper">
                <input
                  v-model="tagInput"
                  type="text"
                  class="tag-input"
                  placeholder="输入标签，回车添加"
                  @keyup.enter.prevent="addTag"
                />
                <span class="tag-separator">/</span>
              </div>
            </div>
            <span class="field-hint">最多添加 5 个标签</span>
          </div>

          <div class="form-actions">
            <button type="button" class="action-btn draft-btn" @click="saveDraft">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
                <polyline points="17 21 17 13 7 13 7 21"/>
                <polyline points="7 3 7 8 15 8"/>
              </svg>
              <span>{{ isEditMode ? '保存草稿' : '保存为草稿' }}</span>
            </button>
            <button type="submit" class="action-btn publish-btn" :disabled="loading">
              <svg v-if="loading" class="loading-spinner" viewBox="0 0 24 24">
                <circle class="path" cx="12" cy="12" r="10" fill="none" stroke-width="4"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="19" x2="12" y2="5"/>
                <polyline points="5 12 12 5 19 12"/>
              </svg>
              <span>{{ loading ? '发布中...' : (isEditMode ? '更新帖子' : '发布帖子') }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showAgreement" class="agreement-modal" @click.self="showAgreement = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3><img src="@/assets/icons/info.svg" alt="info" class="title-icon" />社区公约</h3>
          <button class="modal-close" @click="showAgreement = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPost, updatePost, getPostDetail, getCategoryList } from '@/api/post'
import { uploadCover, uploadPostImage } from '@/api/upload'
import type { PostVO, CategoryVO } from '@/api/post'

const router = useRouter()
const route = useRoute()

const emit = defineEmits<{ goBack: [] }>()

const isEditMode = computed(() => !!route.params.id)
const postId = computed(() => Number(route.params.id))
const isInCreatorCenter = computed(() => route.path.startsWith('/creator'))

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
const coverInput = ref<HTMLInputElement | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)

const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    if (res.data.code === 200) {
      categories.value = res.data.data
    }
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const loadPostDetail = async () => {
  if (!isEditMode.value) return
  try {
    const res = await getPostDetail(postId.value)
    if (res.data.code === 200) {
      const post = res.data.data as PostVO
      form.value = {
        title: post.title,
        categoryId: String(post.categoryId),
        content: post.content,
        cover: post.cover,
        images: post.images || [],
        tags: post.tags || [],
      }
    }
  } catch (error) {
    console.error('加载帖子详情失败:', error)
    ElMessage.error('帖子不存在或已删除')
    router.push('/creator')
  }
}

const triggerCoverUpload = () => {
  coverInput.value?.click()
}

const triggerImageUpload = () => {
  imageInput.value?.click()
}

const handleCoverChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    await doUploadCover(file)
  }
  target.value = ''
}

const handleCoverDrop = (event: DragEvent) => {
  const file = event.dataTransfer?.files[0]
  if (file && file.type.startsWith('image/')) {
    doUploadCover(file)
  }
}

const doUploadCover = async (file: File) => {
  try {
    const res = await uploadCover(file)
    if (res.data.code === 200) {
      form.value.cover = res.data.data
      ElMessage.success('封面上传成功')
    }
  } catch (error) {
    ElMessage.error('封面上传失败')
    console.error('上传封面失败:', error)
  }
}

const removeCover = () => {
  form.value.cover = ''
}

const handleImageChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (files) {
    const remainingSlots = 9 - form.value.images.length
    const filesToUpload = Array.from(files).slice(0, remainingSlots)
    for (const file of filesToUpload) {
      await uploadImage(file)
    }
  }
  target.value = ''
}

const handleImageDrop = (event: DragEvent) => {
  const files = event.dataTransfer?.files
  if (files) {
    const remainingSlots = 9 - form.value.images.length
    Array.from(files)
      .filter(f => f.type.startsWith('image/'))
      .slice(0, remainingSlots)
      .forEach(file => uploadImage(file))
  }
}

const uploadImage = async (file: File) => {
  try {
    const res = await uploadPostImage(file)
    if (res.data.code === 200) {
      form.value.images.push(res.data.data)
    }
  } catch (error) {
    ElMessage.error('图片上传失败')
    console.error('上传图片失败:', error)
  }
}

const removeImage = (index: number) => {
  form.value.images.splice(index, 1)
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

const handleSubmit = async () => {
  if (!form.value.title || !form.value.categoryId || !form.value.content) {
    ElMessage.warning('请填写必填项')
    return
  }

  loading.value = true
  try {
    const data = {
      ...(isEditMode.value && { id: postId.value }),
      categoryId: Number(form.value.categoryId),
      title: form.value.title,
      content: form.value.content,
      cover: form.value.cover || undefined,
      images: form.value.images.length > 0 ? form.value.images : undefined,
      tags: form.value.tags.length > 0 ? form.value.tags : undefined,
      status: 1,
    }

    if (isEditMode.value) {
      await updatePost(data as any)
      ElMessage.success('更新成功')
    } else {
      await createPost(data as any)
      ElMessage.success('发布成功')
    }

    if (isInCreatorCenter.value) {
      emit('goBack')
    } else {
      router.push('/creator')
    }
  } catch (error) {
    ElMessage.error(isEditMode.value ? '更新失败' : '发布失败')
    console.error('提交失败:', error)
  } finally {
    loading.value = false
  }
}

const saveDraft = async () => {
  if (!form.value.title) {
    ElMessage.warning('请至少填写标题')
    return
  }

  loading.value = true
  try {
    const data = {
      ...(isEditMode.value && { id: postId.value }),
      categoryId: form.value.categoryId ? Number(form.value.categoryId) : undefined,
      title: form.value.title,
      content: form.value.content || undefined,
      cover: form.value.cover || undefined,
      images: form.value.images.length > 0 ? form.value.images : undefined,
      tags: form.value.tags.length > 0 ? form.value.tags : undefined,
      status: 0,
    }

    if (isEditMode.value) {
      await updatePost(data as any)
      ElMessage.success('草稿保存成功')
    } else {
      await createPost(data as any)
      ElMessage.success('草稿保存成功')
    }

    if (isInCreatorCenter.value) {
      emit('goBack')
    } else {
      router.push('/creator')
    }
  } catch (error) {
    ElMessage.error('保存失败')
    console.error('保存草稿失败:', error)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  if (isInCreatorCenter.value) {
    emit('goBack')
  } else {
    router.back()
  }
}

onMounted(() => {
  loadCategories()
  loadPostDetail()
})
</script>

<style scoped>
.page-inner {
  max-width: 760px;
  margin: 0 auto;
  padding: 0;
}

.post-card {
  background: white;
  border-radius: 16px;
  padding: 28px;
  border: 1px solid var(--border);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
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
  background: white;
}

.form-input:focus {
  outline: none;
  border-color: var(--pink);
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.08);
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
  background: white;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.22s ease-out;
}

.category-btn:hover {
  border-color: var(--pink);
  color: var(--pink);
}

.category-btn.active {
  background: rgba(255, 107, 157, 0.08);
  border-color: var(--pink);
  color: var(--pink);
}

.hidden-select {
  display: none;
}

.upload-area {
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.22s ease-out;
  position: relative;
  overflow: hidden;
}

.cover-area {
  border: 1px dashed var(--border);
  padding: 32px;
  text-align: center;
}

.cover-area:hover {
  border-color: var(--pink);
  background: rgba(255, 107, 157, 0.02);
}

.cover-preview-wrapper {
  position: relative;
  border-radius: 10px;
  overflow: hidden;
}

.cover-preview {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
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

.remove-btn svg {
  width: 16px;
  height: 16px;
}

.remove-btn:hover {
  background: rgba(255, 71, 87, 0.8);
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-icon-wrapper {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: rgba(255, 107, 157, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--pink);
}

.upload-icon-wrapper svg {
  width: 22px;
  height: 22px;
}

.upload-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.upload-desc {
  font-size: 12px;
  color: var(--text-dim);
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
  background: white;
  resize: vertical;
  font-family: inherit;
  line-height: 1.7;
  min-height: 200px;
}

.form-textarea:focus {
  outline: none;
  border-color: var(--pink);
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.08);
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

.add-image-btn {
  aspect-ratio: 1;
  border-radius: 10px;
  border: 1px dashed var(--border);
  background: rgba(255, 107, 157, 0.02);
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
  background: rgba(255, 107, 157, 0.05);
  color: var(--pink);
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
  background: rgba(255, 107, 157, 0.02);
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
  background: rgba(255, 107, 157, 0.15);
  border: none;
  color: var(--pink);
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
  background: rgba(180, 132, 255, 0.08);
}

.publish-btn {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  box-shadow: 0 4px 20px rgba(255, 107, 157, 0.35);
}

.publish-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(255, 107, 157, 0.45);
}

.publish-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.loading-spinner {
  animation: spin 0.8s linear infinite;
}

.loading-spinner .path {
  stroke: white;
  stroke-linecap: round;
  animation: spinner-path 1.5s ease-in-out infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes spinner-path {
  0% { stroke-dasharray: 1, 150; stroke-dashoffset: 0; }
  50% { stroke-dasharray: 90, 150; stroke-dashoffset: -35; }
  100% { stroke-dasharray: 90, 150; stroke-dashoffset: -124; }
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