<template>
  <main class="main-content">
    <div class="page-inner">
      <div class="page-header">
        <button class="back-btn" @click="goBack">← 返回</button>
        <h1>{{ isEditMode ? '编辑帖子' : '发布帖子' }}</h1>
      </div>

      <form class="post-form" @submit.prevent="handleSubmit">
        <div class="form-section">
          <label class="form-label">标题</label>
          <input
            v-model="form.title"
            type="text"
            class="form-input"
            placeholder="请输入帖子标题"
            maxlength="100"
            required
          />
          <span class="input-hint">{{ form.title.length }}/100</span>
        </div>

        <div class="form-section">
          <label class="form-label">分类</label>
          <select v-model="form.categoryId" class="form-select" required>
            <option value="" disabled>请选择分类</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </option>
          </select>
        </div>

        <div class="form-section">
          <label class="form-label">封面</label>
          <div class="upload-area" @click="triggerCoverUpload" @dragover.prevent @drop.prevent="handleCoverDrop">
            <img v-if="form.cover" :src="form.cover" class="cover-preview" />
            <div v-else class="upload-placeholder">
              <span class="upload-icon">📷</span>
              <span class="upload-text">点击或拖拽上传封面</span>
              <span class="upload-hint">支持 JPG、PNG 格式，建议尺寸 800×600</span>
            </div>
            <button v-if="form.cover" type="button" class="remove-cover" @click.stop="removeCover">✕</button>
          </div>
          <input ref="coverInput" type="file" accept="image/*" class="hidden-input" @change="handleCoverChange" />
        </div>

        <div class="form-section">
          <label class="form-label">正文内容</label>
          <textarea
            v-model="form.content"
            class="form-textarea"
            placeholder="请输入帖子内容..."
            rows="12"
            required
          ></textarea>
        </div>

        <div class="form-section">
          <label class="form-label">图片</label>
          <div class="images-upload">
            <div class="upload-area small" @click="triggerImageUpload" @dragover.prevent @drop.prevent="handleImageDrop">
              <span class="upload-icon">➕</span>
              <span class="upload-text">添加图片</span>
            </div>
            <div v-for="(img, index) in form.images" :key="index" class="image-item">
              <img :src="img" class="image-preview" />
              <button type="button" class="remove-image" @click="removeImage(index)">✕</button>
            </div>
          </div>
          <input ref="imageInput" type="file" accept="image/*" multiple class="hidden-input" @change="handleImageChange" />
          <span class="input-hint">最多上传 9 张图片</span>
        </div>

        <div class="form-section">
          <label class="form-label">标签</label>
          <div class="tags-input">
            <span v-for="(tag, index) in form.tags" :key="index" class="tag-item">
              {{ tag }}
              <button type="button" @click="removeTag(index)">✕</button>
            </span>
            <input
              v-model="tagInput"
              type="text"
              class="tag-input"
              placeholder="输入标签后按回车添加"
              @keyup.enter.prevent="addTag"
            />
          </div>
          <span class="input-hint">最多添加 5 个标签</span>
        </div>

        <div class="form-actions">
          <button type="button" class="btn-draft" @click="saveDraft">
            {{ isEditMode ? '保存草稿' : '保存为草稿' }}
          </button>
          <button type="submit" class="btn-publish">
            {{ isEditMode ? '更新帖子' : '发布帖子' }}
          </button>
        </div>
      </form>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPost, updatePost, getPostDetail, getCategoryList } from '@/api/post'
import { uploadPostImage } from '@/api/upload'
import type { PostVO, CategoryVO } from '@/api/post'

const router = useRouter()
const route = useRoute()

const isEditMode = computed(() => !!route.params.id)
const postId = computed(() => Number(route.params.id))

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
    await uploadCover(file)
  }
  target.value = ''
}

const handleCoverDrop = (event: DragEvent) => {
  const file = event.dataTransfer?.files[0]
  if (file && file.type.startsWith('image/')) {
    uploadCover(file)
  }
}

const uploadCover = async (file: File) => {
  try {
    const res = await uploadPostImage(file)
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

    router.push('/creator')
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

    router.push('/creator')
  } catch (error) {
    ElMessage.error('保存失败')
    console.error('保存草稿失败:', error)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadCategories()
  loadPostDetail()
})
</script>

<style scoped>
.main-content {
  padding: 90px 0 60px;
  min-height: 100vh;
  background: linear-gradient(180deg, #fff5f9 0%, #f8f9ff 100%);
}

.page-inner {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 24px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
}

.back-btn {
  padding: 8px 16px;
  border-radius: 10px;
  background: rgba(255, 107, 157, 0.1);
  color: var(--pink);
  font-size: 14px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
}

.back-btn:hover {
  background: rgba(255, 107, 157, 0.2);
}

.page-header h1 {
  font-size: 24px;
  font-weight: 800;
  color: var(--text);
}

.post-form {
  background: white;
  border-radius: 20px;
  padding: 32px;
  border: 1.5px solid var(--border);
  box-shadow: 0 4px 16px rgba(180, 132, 255, 0.06);
}

.form-section {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 10px;
}

.form-input,
.form-select,
.form-textarea,
.tag-input {
  width: 100%;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1.5px solid var(--border);
  font-size: 14px;
  color: var(--text);
  transition: all 0.22s ease-out;
  background: white;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus,
.tag-input:focus {
  outline: none;
  border-color: var(--pink);
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.1);
}

.form-input::placeholder,
.form-textarea::placeholder,
.tag-input::placeholder {
  color: var(--text-dim);
}

.form-textarea {
  resize: vertical;
  font-family: inherit;
  line-height: 1.6;
}

.input-hint {
  display: block;
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 6px;
}

.upload-area {
  border: 2px dashed var(--border);
  border-radius: 16px;
  padding: 32px;
  text-align: center;
  cursor: pointer;
  transition: all 0.22s ease-out;
  position: relative;
  overflow: hidden;
}

.upload-area:hover {
  border-color: var(--pink);
  background: rgba(255, 107, 157, 0.03);
}

.upload-area.small {
  padding: 24px;
}

.cover-preview {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 12px;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-icon {
  font-size: 32px;
}

.upload-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
}

.upload-hint {
  font-size: 12px;
  color: var(--text-dim);
}

.remove-cover {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.22s ease-out;
}

.remove-cover:hover {
  background: rgba(255, 71, 87, 0.8);
}

.images-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-item {
  position: relative;
  width: calc((100% - 48px) / 5);
}

.image-preview {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 12px;
}

.remove-image {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #ff4757;
  color: white;
  border: none;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.22s ease-out;
}

.remove-image:hover {
  transform: scale(1.1);
}

.tags-input {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px;
  border: 1.5px solid var(--border);
  border-radius: 12px;
  min-height: 48px;
}

.tag-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(180, 132, 255, 0.1));
  color: var(--pink);
  font-size: 13px;
  font-weight: 500;
}

.tag-item button {
  background: none;
  border: none;
  color: var(--pink);
  cursor: pointer;
  font-size: 12px;
}

.tag-input {
  flex: 1;
  min-width: 100px;
  border: none;
  padding: 0;
  box-shadow: none;
}

.tag-input:focus {
  border: none;
  box-shadow: none;
}

.form-actions {
  display: flex;
  gap: 16px;
  justify-content: flex-end;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--border);
}

.btn-draft,
.btn-publish {
  padding: 12px 28px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease-out;
}

.btn-draft {
  background: #f3f4f6;
  color: var(--text-dim);
}

.btn-draft:hover {
  background: #e5e7eb;
}

.btn-publish {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.3);
}

.btn-publish:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 157, 0.4);
}

.hidden-input {
  display: none;
}

@media (max-width: 768px) {
  .page-inner {
    padding: 0 16px;
  }

  .post-form {
    padding: 20px;
  }

  .image-item {
    width: calc((100% - 24px) / 3);
  }

  .form-actions {
    flex-direction: column;
  }

  .btn-draft,
  .btn-publish {
    width: 100%;
  }
}
</style>