<template>
  <Teleport to="body">
    <Transition name="crop-fade">
      <div v-if="visible" class="crop-overlay" @click.self="$emit('cancel')">
        <div class="crop-modal">
          <h3 class="crop-title">{{ title }}</h3>
          <div class="crop-container">
            <Cropper
              ref="cropperRef"
              :src="imageSrc"
              :stencil-props="{ aspectRatio: props.aspectRatio }"
              :stencil-component="props.circular ? CircleStencil : RectangleStencil"
            />
          </div>
          <div class="crop-actions">
            <button class="crop-btn crop-cancel" @click="$emit('cancel')">取消</button>
            <button class="crop-btn crop-confirm" @click="confirmCrop">确定</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Cropper, CircleStencil, RectangleStencil } from 'vue-advanced-cropper'

const props = defineProps<{
  visible: boolean
  title: string
  aspectRatio: number
  circular: boolean
}>()

const emit = defineEmits<{
  crop: [blob: Blob]
  cancel: []
}>()

const imageSrc = ref('')
const cropperRef = ref<InstanceType<typeof Cropper> | null>(null)

watch(() => props.visible, (val) => {
  if (!val) {
    imageSrc.value = ''
  }
})

function loadImage(file: File) {
  const reader = new FileReader()
  reader.onload = () => {
    imageSrc.value = reader.result as string
  }
  reader.readAsDataURL(file)
}

function confirmCrop() {
  const cropper = cropperRef.value
  if (!cropper) return
  const { canvas } = cropper.getResult()
  if (!canvas) return

  canvas.toBlob((blob) => {
    if (blob) {
      emit('crop', blob)
    }
  }, 'image/jpeg', 0.92)
}

defineExpose({ loadImage })
</script>

<style scoped>
.crop-overlay {
  position: fixed;
  inset: 0;
  z-index: 400;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.crop-modal {
  background: #fff;
  border-radius: 20px;
  padding: 24px 28px 20px;
  width: 600px;
  max-width: 94vw;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
}

.crop-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 16px;
  color: #333;
  text-align: center;
}

.crop-container {
  background: #f0f0f0;
  border-radius: 12px;
  overflow: hidden;
  max-height: 60vh;
}

.crop-actions {
  display: flex;
  justify-content: flex-end;
  gap: 14px;
  margin-top: 18px;
}

.crop-btn {
  padding: 10px 28px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.22s ease;
}

.crop-cancel {
  background: #f5f5f5;
  color: #666;
}

.crop-cancel:hover {
  background: #e8e8e8;
}

.crop-confirm {
  background: linear-gradient(135deg, #FF8AC8, #B484FF);
  color: white;
  box-shadow: 0 4px 16px rgba(255, 138, 200, 0.3);
}

.crop-confirm:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(255, 138, 200, 0.4);
}

.crop-fade-enter-active,
.crop-fade-leave-active {
  transition: opacity 0.25s ease;
}

.crop-fade-enter-from,
.crop-fade-leave-to {
  opacity: 0;
}
</style>
