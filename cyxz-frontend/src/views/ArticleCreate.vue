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
              type="text"
              class="form-input"
              placeholder="给你的长文起个吸引人的标题吧~"
              maxlength="100"
              required
            />
            <span class="char-count">{{ editor.form.value.title.length }}/100</span>
          </div>
        </div>

        <!-- Markdown 编辑器 -->
        <div class="form-section">
          <label class="form-label">
            <Icon icon="ph:pencil-simple" class="label-icon pink-icon" />
            <span>正文内容</span>
            <span class="label-required">*</span>
          </label>
          <div class="md-editor-wrapper">
            <v-md-editor
              v-model="editor.form.value.content"
              height="460px"
              placeholder="使用 Markdown 语法编写长文…"
              @upload-image="handleMdImageUpload"
            ></v-md-editor>
          </div>
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

        <!-- 圈子 -->
        <div class="form-section">
          <label class="form-label">
            <Icon icon="ph:circles-three-plus" class="label-icon pink-icon" />
            <span>圈子</span>
            <span class="label-required">*</span>
          </label>
          <div class="circle-selector">
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
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { ElMessage } from 'element-plus'
import { usePostEditor } from '@/composables/usePostEditor'
import { watch } from 'vue'
import { uploadPostImage } from '@/api/upload'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import VMdEditor from '@kangc/v-md-editor/lib/base-editor'
import githubTheme from '@kangc/v-md-editor/lib/theme/github.js'

VMdEditor.use(githubTheme)
VMdEditor.lang.use('zh-CN')

const emit = defineEmits<{
  goBack: [wasEditingDraft?: boolean]
  publishSuccess: []
}>()

const editor = usePostEditor('ARTICLE')

const selectCircle = (circleId: number) => {
  editor.form.value.circleId = circleId
  editor.form.value.sectionId = null  // 切圈子时清空已选板块
  editor.loadSectionsForCircle(circleId)
}

// 圈子变化时自动加载该圈子的板块列表（含编辑模式回填）
watch(() => editor.form.value.circleId, (newId) => {
  if (newId) {
    editor.loadSectionsForCircle(newId)
  }
})

const handleMdImageUpload = async (_event: any, insertImage: Function, files: File[]) => {
  const file = files[0]
  if (!file) return
  try {
    const url = await uploadPostImage(file)
    insertImage({ url, desc: '' })
    ElMessage.success('图片已插入')
  } catch {
    ElMessage.error('图片上传失败')
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

defineExpose({ dirty: editor.dirty, confirmLeave: editor.confirmLeave })
</script>

<style scoped>
.page-inner {
  max-width: 820px;
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

.md-editor-wrapper {
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
  transition: border-color 0.22s ease-out;
  background: var(--card);
}

.md-editor-wrapper:focus-within {
  border-color: var(--pink);
  box-shadow: 0 0 0 3px var(--pink-bg);
}

.md-editor-wrapper :deep(.v-md-editor) {
  border: none;
  border-radius: 0;
  box-shadow: none;
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
}
</style>
