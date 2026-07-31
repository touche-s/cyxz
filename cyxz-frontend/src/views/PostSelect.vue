<template>
  <div class="page-inner">
    <div class="create-card">
      <h2 class="phase-title">选择创作类型</h2>
      <div class="type-tabs">
        <button class="type-tab" :class="{ active: selectTab === 'image' }" @click="selectTab = 'image'">
          <Icon icon="ph:image-square" class="tab-icon" />
          <span class="tab-name">图文</span>
          <span class="tab-desc">图片+文字 · 分享/安利/返图</span>
        </button>
        <button class="type-tab" :class="{ active: selectTab === 'article' }" @click="selectTab = 'article'">
          <Icon icon="ph:article" class="tab-icon" />
          <span class="tab-name">长文/攻略</span>
          <span class="tab-desc">Markdown · 攻略/考据/同人文</span>
        </button>
      </div>

      <!-- 图文：上传图片区 -->
      <div v-if="selectTab === 'image'" class="select-panel">
        <div class="upload-hint">
          <Icon icon="ph:image" class="hint-icon" />
          <span>上传图片后即可进入详细编辑</span>
        </div>
        <div class="images-grid">
          <div
            v-for="(img, index) in images"
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
            v-if="images.length < 9"
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

        <div class="select-actions">
          <button class="next-btn" :disabled="images.length === 0 || imgLoading" @click="$emit('goImageEdit', images)">
            <Icon icon="ph:arrow-right" />
            下一步
          </button>
        </div>
      </div>

      <!-- 长文：开始创作按钮 -->
      <div v-else class="select-panel article-panel">
        <div class="article-intro">
          <Icon icon="ph:article" class="intro-icon" />
          <h3>长文 / 攻略创作</h3>
          <p>支持 Markdown 语法，适合写攻略、考据、同人文等长内容</p>
        </div>
        <button class="start-btn" @click="$emit('goArticleEdit')">
          <Icon icon="ph:article" class="start-icon" />
          开始创作
        </button>
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
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'
import { useApi } from '@/composables/useApi'
import { uploadPostImage, deleteUploadedFile } from '@/api/upload'
import ImageCropper from '@/components/ImageCropper.vue'

defineEmits<{
  goImageEdit: [images: string[]]
  goArticleEdit: []
}>()

const selectTab = ref<'image' | 'article'>('image')
const images = ref<string[]>([])
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

const triggerImageUpload = () => {
  imageInput.value?.click()
}

const handleImageChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (files) {
    const remainingSlots = 9 - images.value.length
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
    const remainingSlots = 9 - images.value.length
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
  const oldUrl = images.value[index]
  await upload(async () => {
    const newUrl = await uploadPostImage(file)
    images.value[index] = newUrl
    deleteUploadedFile(oldUrl).catch(() => {})
    ElMessage.success('裁剪完成')
  }, { onError: () => ElMessage.error('图片上传失败') })
}

const uploadImage = async (file: File) => {
  await upload(async () => {
    const url = await uploadPostImage(file)
    images.value.push(url)
  }, { onError: () => ElMessage.error('图片上传失败') })
}

const removeImage = (index: number) => {
  const removed = images.value[index]
  images.value.splice(index, 1)
  deleteUploadedFile(removed).catch(() => {})
}
</script>

<style scoped>
.page-inner {
  max-width: 760px;
  margin: 0 auto;
  padding: 0;
}

.create-card {
  background: var(--card);
  border-radius: 16px;
  padding: 32px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
}

.phase-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 24px;
  text-align: center;
}

.type-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 28px;
}

.type-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  border-radius: 14px;
  border: 2px solid var(--border);
  background: var(--card);
  cursor: pointer;
  transition: all 0.22s ease-out;
  text-align: center;
}

.type-tab:hover {
  border-color: var(--pink);
}

.type-tab.active {
  border-color: var(--pink);
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.06), rgba(180, 132, 255, 0.06));
}

.tab-icon {
  font-size: 28px;
  width: 28px;
  height: 28px;
  color: var(--text-dim);
  transition: color 0.22s ease-out;
}

.type-tab.active .tab-icon {
  color: var(--pink);
}

.tab-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
}

.tab-desc {
  font-size: 12px;
  color: var(--text-dim);
}

.select-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.upload-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-dim);
}

.hint-icon {
  width: 18px;
  height: 18px;
  color: var(--pink);
}

.article-panel {
  align-items: center;
  padding: 40px 0;
}

.article-intro {
  text-align: center;
  margin-bottom: 8px;
}

.article-intro .intro-icon {
  font-size: 48px;
  width: 48px;
  height: 48px;
  color: var(--pink);
  margin-bottom: 16px;
}

.article-intro h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 8px;
}

.article-intro p {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0;
  line-height: 1.6;
}

.start-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 36px;
  border-radius: 14px;
  background: var(--gradient-brand);
  color: var(--white);
  font-size: 15px;
  font-weight: 700;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(255, 107, 157, 0.3);
  transition: all 0.22s ease-out;
}

.start-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 28px rgba(255, 107, 157, 0.4);
}

.start-btn .start-icon {
  width: 20px;
  height: 20px;
}

.select-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
}

.next-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 12px 28px;
  border-radius: 14px;
  background: var(--gradient-brand);
  color: var(--white);
  font-size: 14px;
  font-weight: 700;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(255, 107, 157, 0.3);
  transition: all 0.22s ease-out;
}

.next-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 28px rgba(255, 107, 157, 0.4);
}

.next-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
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

.field-hint {
  display: block;
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 10px;
}

.hidden-input {
  display: none;
}

@media (max-width: 768px) {
  .page-inner {
    padding: 0 16px;
  }

  .create-card {
    padding: 20px;
  }

  .type-tabs {
    flex-direction: column;
  }

  .images-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 480px) {
  .images-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
