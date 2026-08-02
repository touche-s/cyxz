<template>
  <div class="page-inner">
    <div class="post-card">
      <form class="post-form" @submit.prevent="handleSubmit">
        <!-- 标题 -->
        <div class="form-section">
          <label class="form-label">
            <Icon icon="ph:text-t" class="label-icon pink-icon" />
            <span>标题</span>
            <span class="label-required">*</span>
          </label>
          <div class="input-wrapper">
            <input
              v-model="editor.form.value.title"
              @input="resetSensitiveResult"
              type="text"
              class="form-input"
              placeholder="分享你的故事，给帖子起个吸引人的标题吧~"
              maxlength="100"
              required
            />
            <span class="char-count">{{ editor.form.value.title.length }}/100</span>
          </div>
        </div>

        <!-- 图片 -->
        <div class="form-section">
          <label class="form-label">
            <Icon icon="ph:image" class="label-icon pink-icon" />
            <span>图片</span>
            <span class="label-required">*</span>
          </label>
          <div class="images-grid">
            <div
              v-for="(img, index) in editor.form.value.images"
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
              v-if="editor.form.value.images.length < 9"
              class="add-image-btn"
              :class="{ uploading: imgLoading }"
              @click="triggerImageUpload"
              @dragover.prevent
              @drop.prevent="handleImageDrop"
            >
              <template v-if="imgLoading">
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

        <!-- 正文 -->
        <div class="form-section">
          <label class="form-label">
            <Icon icon="ph:pencil-simple" class="label-icon pink-icon" />
            <span>正文内容</span>
            <span class="label-required">*</span>
          </label>
          <div class="textarea-wrapper">
            <textarea
              v-model="editor.form.value.content"
              class="form-textarea"
              placeholder="写下你想分享的内容吧，支持换行哦~"
              rows="10"
              required
            ></textarea>
          </div>
        </div>

        <!-- 圈子 -->
        <div class="form-section">
          <label class="form-label">
            <Icon icon="ph:circles-three-plus" class="label-icon pink-icon" />
            <span>圈子</span>
            <span class="label-required">*</span>
          </label>
          <div v-if="editor.sortedCircles.value.length" class="circle-selector">
            <button
              v-for="c in editor.sortedCircles.value"
              :key="c.id"
              type="button"
              class="circle-btn"
              :class="{ active: editor.form.value.circleId === c.id }"
              @click="selectCircle(c.id)"
            >
              {{ c.name }}
            </button>
          </div>
          <span v-if="editor.sortedCircles.value.length" class="field-hint">仅显示你已加入的圈子</span>
          <span v-else class="field-hint">你还没有加入任何圈子，请先前往圈子页加入后再发布</span>
        </div>

        <!-- 板块 -->
        <div class="form-section">
          <label class="form-label">
            <Icon icon="ph:stack" class="label-icon pink-icon" />
            <span>板块</span>
          </label>
          <div class="section-selector">
            <button
              v-for="s in editor.sections.value"
              :key="s.id"
              type="button"
              class="section-btn"
              :class="{ active: editor.form.value.sectionId === s.id }"
              @click="editor.form.value.sectionId = s.id"
            >
              {{ s.name }}
            </button>
          </div>
        </div>

        <!-- 标签 -->
        <div class="form-section">
          <label class="form-label">
            <Icon icon="ph:tag" class="label-icon pink-icon" />
            <span>标签</span>
          </label>
          <div class="tags-container">
            <div v-for="(tag, index) in editor.form.value.tags" :key="index" class="tag-chip">
              <span class="tag-text">{{ tag }}</span>
              <button type="button" class="tag-remove" @click="editor.removeTag(index)">
                <Icon icon="ph:x" />
              </button>
            </div>
            <div v-if="editor.form.value.tags.length < 5" class="add-tag-wrapper">
              <input
                v-model="editor.tagInput.value"
                type="text"
                class="tag-input"
                placeholder="输入标签，回车添加"
                @keydown.enter.prevent="editor.addTag()"
              />
              <span class="tag-separator">/</span>
            </div>
          </div>
          <span class="field-hint">最多添加 5 个标签</span>
        </div>

        <!-- 操作按钮 -->
        <div class="form-actions">
          <button
            type="button"
            class="action-btn back-btn"
            @click="$emit('goBack')"
          >
            <Icon icon="ph:arrow-left" />
            <span>返回</span>
          </button>
          <button
            v-if="!editor.isEditingPublished.value"
            type="button"
            class="action-btn draft-btn"
            @click="saveDraft"
          >
            <Icon icon="ph:floppy-disk" />
            <span>{{ editor.isEditMode.value ? '保存草稿' : '保存为草稿' }}</span>
          </button>
          <button
            type="button"
            class="action-btn check-btn"
            :class="{ 'check-pass': sensitiveResult === 'pass', 'check-fail': sensitiveResult === 'fail' }"
            :disabled="checkingSensitive"
            @click="handleCheckSensitive"
          >
            <LoadingSpinner v-if="checkingSensitive" inline text="" />
            <Icon v-else-if="sensitiveResult === 'pass'" icon="ph:check-circle" />
            <Icon v-else-if="sensitiveResult === 'fail'" icon="ph:warning-circle" />
            <Icon v-else icon="ph:shield-check" />
            <span v-if="checkingSensitive">检测中...</span>
            <span v-else-if="sensitiveResult === 'pass'">检测通过</span>
            <span v-else-if="sensitiveResult === 'fail'">{{ sensitiveHitCount }} 个敏感词</span>
            <span v-else>敏感词检测</span>
          </button>
          <button
            type="submit"
            class="action-btn publish-btn"
            :disabled="editor.submitLoading.value"
          >
            <LoadingSpinner v-if="editor.submitLoading.value" inline text="" />
            <Icon v-else icon="ph:paper-plane-right" />
            <span>{{ editor.submitLoading.value ? '发布中...' : (editor.isEditingPublished.value ? '更新发布' : '发布帖子') }}</span>
          </button>
        </div>
      </form>
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
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'
import { usePostEditor } from '@/composables/usePostEditor'
import { watch } from 'vue'
import { useApi } from '@/composables/useApi'
import { uploadPostImage, deleteUploadedFile } from '@/api/upload'
import { checkSensitive } from '@/api/post'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import ImageCropper from '@/components/ImageCropper.vue'

const props = defineProps<{
  initialImages?: string[]
}>()

const emit = defineEmits<{
  goBack: [wasEditingDraft?: boolean]
  publishSuccess: []
}>()

const editor = usePostEditor('NORMAL')

/**
 * 选择圈子后：
 * 1. 清空已选板块（不同圈子的板块配置不同，不能沿用之前的板块 ID）
 * 2. 加载新圈子已启用的板块列表
 */
const selectCircle = (circleId: number) => {
  editor.form.value.circleId = circleId
  editor.form.value.sectionId = null  // 切圈子时清空已选板块，避免引用其他圈子的板块 ID
  editor.loadSectionsForCircle(circleId)
}

// 圈子变化时自动加载该圈子的板块列表（含编辑模式回填）
watch(() => editor.form.value.circleId, (newId) => {
  if (newId) {
    editor.loadSectionsForCircle(newId)
  }
})

// 敏感词检测状态
const checkingSensitive = ref(false)
const sensitiveResult = ref<'pass' | 'fail' | null>(null)
const sensitiveHitCount = ref(0)
const resetSensitiveResult = () => {
  sensitiveResult.value = null
  sensitiveHitCount.value = 0
}

// 标题或正文改动时重置敏感词检测结果
watch(() => [editor.form.value.title, editor.form.value.content], resetSensitiveResult)

const imageInput = ref<HTMLInputElement | null>(null)
const { loading: imgLoading, run: upload } = useApi()

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

// 初始化图片（从 PostSelect 传入的）
if (props.initialImages && props.initialImages.length > 0 && !editor.isEditMode.value) {
  editor.form.value.images = [...props.initialImages]
  editor.form.value.cover = props.initialImages[0]
}

const triggerImageUpload = () => {
  imageInput.value?.click()
}

const handleImageChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (files) {
    const remainingSlots = 9 - editor.form.value.images.length
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
    const remainingSlots = 9 - editor.form.value.images.length
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
  const oldUrl = editor.form.value.images[index]
  await upload(async () => {
    const newUrl = await uploadPostImage(file)
    editor.form.value.images[index] = newUrl
    if (editor.form.value.cover === oldUrl) editor.form.value.cover = newUrl
    deleteUploadedFile(oldUrl).catch(() => {})
    ElMessage.success('裁剪完成')
  }, { onError: () => ElMessage.error('图片上传失败') })
}

const uploadImage = async (file: File) => {
  await upload(async () => {
    const url = await uploadPostImage(file)
    editor.form.value.images.push(url)
    if (editor.form.value.images.length === 1 && !editor.form.value.cover) {
      editor.form.value.cover = url
    }
  }, { onError: () => ElMessage.error('图片上传失败') })
}

const removeImage = (index: number) => {
  const removed = editor.form.value.images[index]
  editor.form.value.images.splice(index, 1)
  deleteUploadedFile(removed).catch(() => {})
  if (editor.form.value.cover === removed) {
    editor.form.value.cover = editor.form.value.images[0] || ''
  }
}

const handleSubmit = async () => {
  const ok = await editor.doSubmit()
  if (!ok) return

  if (editor.isInCreatorCenter.value) {
    emit('publishSuccess')
  }
}

const saveDraft = async () => {
  const ok = await editor.saveDraftOnly()
  if (!ok) return

  if (editor.isInCreatorCenter.value) {
    emit('goBack', true)
  }
}

const handleCheckSensitive = async () => {
  if (!editor.form.value.title && !editor.form.value.content) {
    ElMessage.warning('请先填写标题或正文')
    return
  }
  checkingSensitive.value = true
  sensitiveResult.value = null
  try {
    const { run } = useApi()
    const data = await run(() => checkSensitive({
      title: editor.form.value.title,
      content: editor.form.value.content,
    }))
    if (data && data.length > 0) {
      sensitiveResult.value = 'fail'
      sensitiveHitCount.value = data.length
      ElMessage.warning('检测到敏感词：' + data.join('、'))
    } else {
      sensitiveResult.value = 'pass'
      ElMessage.success('未检测到敏感词，内容安全')
    }
  } catch {
    ElMessage.error('检测失败，请稍后重试')
  } finally {
    checkingSensitive.value = false
  }
}

defineExpose({ dirty: editor.dirty, confirmLeave: editor.confirmLeave })
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
  color: var(--error);
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
  color: var(--white);
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
  color: var(--white);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.22s ease-out;
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

.section-selector,
.circle-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.section-btn,
.circle-btn {
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

.section-btn:hover,
.circle-btn:hover {
  border-color: var(--pink);
  color: var(--pink);
}

.section-btn.active,
.circle-btn.active {
  background: var(--pink-bg-hover);
  border-color: var(--pink);
  color: var(--pink);
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
  color: var(--white);
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

.back-btn {
  background: rgba(255, 255, 255, 0.9);
  color: var(--text-dim);
  border: 1.5px solid var(--border);
  margin-right: auto;
}

.back-btn:hover {
  border-color: var(--text-dim);
  color: var(--text);
}

.draft-btn {
  background: rgba(255, 255, 255, 0.9);
  color: var(--purple);
  border: 1.5px solid var(--purple);
}

.draft-btn:hover {
  background: var(--purple-bg);
}

.check-btn {
  background: rgba(255, 255, 255, 0.9);
  color: var(--text-secondary);
  border: 1.5px solid var(--border);
}

.check-btn:hover:not(:disabled) {
  border-color: var(--text-dim);
  color: var(--text);
}

.check-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.check-btn.check-pass {
  color: #16a34a;
  border-color: #16a34a;
  background: rgba(22, 163, 74, 0.08);
}

.check-btn.check-fail {
  color: #ef4444;
  border-color: #ef4444;
  background: rgba(239, 68, 68, 0.08);
}

/* 命中时悬停提示 */
.check-btn.check-fail:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.14);
}

.publish-btn {
  background: var(--gradient-brand);
  color: var(--white);
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

html.dark .draft-btn,
html.dark .back-btn {
  background: rgba(30, 26, 50, 0.85);
}

@media (max-width: 768px) {
  .page-inner {
    padding: 0 16px;
  }

  .post-card {
    padding: 20px;
  }

  .images-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .form-actions {
    flex-wrap: wrap;
  }

  .back-btn {
    margin-right: 0;
  }

  .action-btn {
    flex: 1 1 auto;
    justify-content: center;
  }

  .section-btn,
  .circle-btn {
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
