<template>
  <Teleport to="body">
    <div class="modal-overlay" v-if="visible" @click="onCancel">
      <div class="modal-content" :class="{ 'is-danger': danger }" @click.stop>
        <div class="modal-topbar" :class="{ 'is-danger': danger }"></div>
        <div class="modal-icon-wrapper" :class="{ 'is-danger': danger }">
          <slot name="icon">
            <slot name="confirmIcon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="modal-warn-icon">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
            </slot>
          </slot>
        </div>
        <div v-if="danger && dangerBadge" class="modal-danger-badge">{{ dangerBadge }}</div>
        <h3>{{ title }}</h3>
        <p v-if="postTitle" class="modal-post-title">「{{ postTitle }}」</p>
        <p class="modal-hint">{{ hint }}</p>
        <div v-if="danger && warningText" class="modal-warning-box">
          <span class="modal-warning-dot"></span>
          {{ warningText }}
        </div>
        <div class="modal-actions">
          <button class="modal-btn cancel" @click="onCancel">取消</button>
          <button class="modal-btn confirm" :class="{ 'is-danger': danger }" @click="$emit('confirm')">
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { watch, onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  visible: boolean
  title: string
  postTitle?: string
  hint: string
  confirmText: string
  danger?: boolean
  dangerBadge?: string
  warningText?: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  confirm: []
  cancel: []
}>()

const onCancel = () => {
  emit('update:visible', false)
  emit('cancel')
}

const onKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && props.visible) {
    onCancel()
  }
}

watch(() => props.visible, (val) => {
  document.body.style.overflow = val ? 'hidden' : ''
})

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: var(--overlay);
  backdrop-filter: blur(8px) saturate(120%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.22s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  position: relative;
  background: linear-gradient(180deg, rgba(255,255,255,0.98), rgba(255,251,254,0.96));
  border: 1px solid rgba(255, 255, 255, 0.86);
  border-radius: 26px;
  padding: 30px 28px 24px;
  max-width: 420px;
  width: min(92vw, 420px);
  text-align: center;
  animation: slideUp 0.28s ease-out;
  box-shadow: 0 18px 52px rgba(58, 39, 91, 0.14);
  overflow: hidden;
}

.modal-content.is-danger {
  background: linear-gradient(180deg, rgba(255,255,255,0.99), rgba(255,248,248,0.97));
  box-shadow: 0 18px 56px rgba(127, 29, 29, 0.14);
}

.modal-topbar {
  position: absolute;
  inset: 0 0 auto 0;
  height: 4px;
  background: linear-gradient(90deg, #ff7eb6, #c084fc);
}

.modal-topbar.is-danger {
  background: linear-gradient(90deg, #ef4444, #dc2626);
}

@keyframes slideUp {
  from { transform: translateY(18px) scale(0.98); opacity: 0; }
  to { transform: translateY(0) scale(1); opacity: 1; }
}

.modal-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(255, 126, 182, 0.10), rgba(192, 132, 252, 0.10));
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.75);
}

.modal-icon-wrapper.is-danger {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.10), rgba(220, 38, 38, 0.12));
  color: #dc2626;
}

.modal-warn-icon {
  width: 22px;
  height: 22px;
  color: var(--purple);
  stroke-width: 2;
}

.modal-icon-wrapper.is-danger :deep(.modal-warn-icon) {
  color: #dc2626;
}

.modal-danger-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.1);
  color: #b91c1c;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
  margin-bottom: 12px;
}

.modal-content h3 {
  font-size: 18px;
  font-weight: 800;
  color: var(--text);
  margin-bottom: 8px;
}

.modal-content.is-danger h3 {
  color: #7f1d1d;
}

.modal-post-title {
  font-size: 15px;
  color: var(--text-dim);
  margin-bottom: 8px;
  font-weight: 600;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.modal-hint {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-dim);
  margin-bottom: 18px;
}

.modal-content.is-danger .modal-hint {
  color: #9a5d5d;
}

.modal-warning-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 20px;
  border-radius: 14px;
  background: rgba(254, 242, 242, 0.92);
  border: 1px solid rgba(248, 113, 113, 0.22);
  color: #b45309;
  font-size: 12px;
  font-weight: 600;
}

.modal-warning-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  flex-shrink: 0;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.12);
}

.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.modal-btn {
  min-width: 124px;
  min-height: 48px;
  padding: 8px 18px;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  border: none;
  transition: all 0.22s ease-out;
}

.modal-btn.cancel {
  background: rgba(243, 244, 246, 0.78);
  color: var(--text-dim);
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.8);
}

.modal-btn.cancel:hover {
  background: rgba(229, 231, 235, 0.96);
  transform: translateY(-1px);
}

.modal-btn.confirm {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  background: linear-gradient(135deg, #ff7eb6, #c084fc);
  color: white;
  box-shadow: 0 8px 24px rgba(192, 132, 252, 0.3);
}

.modal-btn.confirm:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 26px rgba(192, 132, 252, 0.38);
}

.modal-btn.confirm.is-danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  box-shadow: 0 10px 26px rgba(220, 38, 38, 0.28);
}

.modal-btn.confirm.is-danger:hover {
  box-shadow: 0 12px 30px rgba(220, 38, 38, 0.34);
}
</style>
