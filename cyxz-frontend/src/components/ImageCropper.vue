<template>
  <Teleport to="body">
    <Transition name="crop-fade">
      <div v-if="visible" class="crop-overlay" @click.self="$emit('cancel')">
        <div class="crop-modal">
          <div class="crop-modal-glow glow-left"></div>
          <div class="crop-modal-glow glow-right"></div>
          <div class="crop-modal-deco deco-top"></div>
          <div class="crop-modal-deco deco-bottom"></div>
          <div class="crop-header">
            <div class="crop-title-wrap">
              <span class="crop-badge">IMAGE CROP</span>
              <h3 class="crop-title">{{ title }}</h3>
              <p class="crop-subtitle">拖动图片并调整选区，裁出更合适的展示效果</p>
            </div>
            <button class="crop-close" @click="$emit('cancel')">✕</button>
          </div>
          <div v-if="!circular && ratioOptions && ratioOptions.length > 0" class="crop-ratios">
            <button
              v-for="opt in ratioOptions"
              :key="opt.value"
              class="ratio-btn"
              :class="{ active: currentRatio === opt.value }"
              @click="selectRatio(opt.value)"
            >
              {{ opt.label }}
            </button>
          </div>
          <div class="crop-container">
            <Cropper
              ref="cropperRef"
              class="cropper"
              :src="imageSrc"
              :stencil-props="stencilProps"
              :stencil-component="stencilComponent"
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
import { ref, watch, computed } from 'vue'
import { Cropper, CircleStencil, RectangleStencil } from 'vue-advanced-cropper'
import 'vue-advanced-cropper/dist/style.css'

interface RatioOption {
  label: string
  value: number
}

const props = withDefaults(defineProps<{
  visible: boolean
  title: string
  aspectRatio: number
  circular: boolean
  ratioOptions?: RatioOption[]
}>(), {
  ratioOptions: () => [],
})

const emit = defineEmits<{
  crop: [blob: Blob]
  cancel: []
  'update:aspectRatio': [value: number]
}>()

const imageSrc = ref('')
const cropperRef = ref<InstanceType<typeof Cropper> | null>(null)
const currentRatio = ref(props.aspectRatio)

watch(() => props.aspectRatio, (val) => {
  currentRatio.value = val
})

watch(() => props.visible, (val) => {
  if (!val) {
    imageSrc.value = ''
  }
})

const stencilProps = computed(() => {
  if (props.circular) return { aspectRatio: 1 }
  if (props.aspectRatio > 0) return { aspectRatio: props.aspectRatio }
  return {}
})

const stencilComponent = computed(() => {
  return props.circular ? CircleStencil : RectangleStencil
})

function selectRatio(value: number) {
  currentRatio.value = value
  emit('update:aspectRatio', value)
}

function loadImage(file: File) {
  const reader = new FileReader()
  reader.onload = () => {
    imageSrc.value = reader.result as string
  }
  reader.readAsDataURL(file)
}

function setImageUrl(url: string) {
  imageSrc.value = url
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

defineExpose({ loadImage, setImageUrl })
</script>

<style scoped>
.crop-overlay {
  position: fixed;
  inset: 0;
  z-index: 400;
  background: rgba(18, 12, 27, 0.52);
  backdrop-filter: blur(16px) saturate(140%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.crop-modal {
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(255, 248, 252, 0.94));
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 28px;
  padding: 22px 22px 20px;
  width: 660px;
  max-width: 94vw;
  box-shadow:
    0 28px 80px rgba(27, 18, 39, 0.22),
    0 10px 30px rgba(255, 138, 200, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.crop-modal-glow {
  position: absolute;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.55;
  pointer-events: none;
}

.glow-left {
  top: -40px;
  left: -30px;
  background: rgba(255, 153, 214, 0.28);
}

.glow-right {
  right: -36px;
  bottom: 56px;
  background: rgba(180, 132, 255, 0.22);
}

.crop-modal-deco {
  position: absolute;
  pointer-events: none;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.08));
}

.deco-top {
  top: 16px;
  right: 72px;
  width: 112px;
  height: 112px;
  opacity: 0.42;
}

.deco-bottom {
  left: 22px;
  bottom: -26px;
  width: 140px;
  height: 140px;
  opacity: 0.34;
}

.crop-header {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
}

.crop-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.crop-badge {
  align-self: flex-start;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: #ff6fae;
  background: rgba(255, 138, 200, 0.12);
  border: 1px solid rgba(255, 138, 200, 0.18);
}

.crop-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.15;
  color: #2f2538;
}

.crop-subtitle {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #8a7c94;
}

.crop-close {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.72);
  color: #8c8095;
  font-size: 18px;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(73, 52, 92, 0.08);
  transition: all 0.24s ease;
}

.crop-close:hover {
  color: #ff6fae;
  transform: rotate(90deg) scale(1.05);
  box-shadow: 0 10px 28px rgba(255, 138, 200, 0.18);
}

.crop-container {
  position: relative;
  z-index: 1;
  background: linear-gradient(135deg, rgba(255, 245, 250, 0.92), rgba(244, 239, 255, 0.92));
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 22px;
  overflow: hidden;
  height: 430px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 14px 32px rgba(61, 42, 81, 0.08);
}

.cropper {
  height: 100%;
  background: transparent;
}

.crop-actions {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 18px;
}

.crop-btn {
  min-width: 92px;
  padding: 11px 24px;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 700;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.24s ease;
}

.crop-cancel {
  background: rgba(255, 255, 255, 0.78);
  color: #7f7488;
  border-color: rgba(225, 216, 234, 0.82);
  box-shadow: 0 6px 18px rgba(58, 42, 76, 0.05);
}

.crop-cancel:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.96);
  border-color: rgba(255, 138, 200, 0.2);
}

.crop-confirm {
  color: white;
  background: linear-gradient(135deg, #ff8ac8 0%, #d98bff 52%, #b484ff 100%);
  box-shadow: 0 12px 28px rgba(232, 124, 196, 0.28);
}

.crop-confirm:hover {
  transform: translateY(-1px) scale(1.01);
  box-shadow: 0 14px 32px rgba(232, 124, 196, 0.34);
}

.crop-fade-enter-active,
.crop-fade-leave-active {
  transition: opacity 0.28s ease;
}

.crop-fade-enter-from,
.crop-fade-leave-to {
  opacity: 0;
}

.crop-ratios {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.ratio-btn {
  padding: 5px 14px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid rgba(225, 216, 234, 0.82);
  background: rgba(255, 255, 255, 0.6);
  color: #7f7488;
  cursor: pointer;
  transition: all 0.22s ease;
}

.ratio-btn:hover {
  border-color: rgba(255, 138, 200, 0.35);
  color: #ff6fae;
  background: rgba(255, 138, 200, 0.06);
}

.ratio-btn.active {
  background: rgba(255, 107, 157, 0.1);
  border-color: rgba(255, 107, 157, 0.3);
  color: #ff6b9d;
  font-weight: 700;
}

@media (max-width: 768px) {
  .crop-modal {
    padding: 18px 18px 16px;
    width: calc(100vw - 24px);
    border-radius: 24px;
  }

  .crop-title {
    font-size: 20px;
  }

  .crop-subtitle {
    font-size: 12px;
  }

  .crop-container {
    height: 320px;
    border-radius: 18px;
  }

  .crop-actions {
    gap: 10px;
  }

  .crop-btn {
    min-width: 84px;
    padding: 10px 20px;
  }
}
</style>
