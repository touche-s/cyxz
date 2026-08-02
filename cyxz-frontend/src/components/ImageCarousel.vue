<template>
  <div class="ic-container" :style="{ aspectRatio: carouselRatio }">
    <div class="ic-track" :style="{ transform: `translateX(-${currentIndex * 100}%)` }">
      <img v-for="(img, i) in images" :key="i" :src="img"
           class="ic-slide" @load="(e) => onLoad(i, e)"
           @click="lightbox && $emit('lightbox-open', i)" />
    </div>
    <button v-if="images.length > 1" class="ic-arrow ic-prev" @click.stop="prev">
      <Icon icon="ph:caret-left" />
    </button>
    <button v-if="images.length > 1" class="ic-arrow ic-next" @click.stop="next">
      <Icon icon="ph:caret-right" />
    </button>
    <div class="ic-dots" v-if="images.length > 1">
      <span v-for="(_, i) in images" :key="i" class="ic-dot"
            :class="{ active: i === currentIndex }" @click="currentIndex = i" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Icon } from '@iconify/vue'

const props = withDefaults(defineProps<{
  images: string[]
  lightbox?: boolean
}>(), {
  lightbox: false,
})

defineEmits<{
  'lightbox-open': [index: number]
}>()

const currentIndex = ref(0)
const imageRatios = ref<number[]>([])

function onLoad(index: number, e: Event) {
  const img = e.target as HTMLImageElement
  imageRatios.value[index] = img.naturalWidth / img.naturalHeight
}

const carouselRatio = computed(() => {
  const ratio = imageRatios.value[currentIndex.value]
  return ratio ? `${ratio}` : '4/3'
})

function prev() {
  if (currentIndex.value > 0) currentIndex.value--
}

function next() {
  if (currentIndex.value < props.images.length - 1) {
    currentIndex.value++
  }
}
</script>

<style scoped>
.ic-container {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: var(--card);
  border-radius: 14px;
}

.ic-track {
  display: flex;
  height: 100%;
  transition: transform 0.3s ease;
}

.ic-slide {
  min-width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center;
  user-select: none;
}

.ic-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  color: var(--white);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  z-index: 2;
}

.ic-arrow:hover {
  background: rgba(0, 0, 0, 0.6);
}

.ic-prev {
  left: 12px;
}

.ic-next {
  right: 12px;
}

.ic-dots {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
  z-index: 2;
}

.ic-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: background 0.2s;
}

.ic-dot.active {
  background: var(--card);
}
</style>
