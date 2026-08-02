<template>
  <div class="masonry" :style="{ gap: `${gap}px` }">
    <div v-for="(col, ci) in columns" :key="ci" class="masonry-col" :style="{ gap: `${gap}px` }">
      <div v-for="item in col" :key="getKey(item)">
        <slot name="item" :item="item" :index="getIndex(item)" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" generic="T">
import { computed, ref, onMounted, onUnmounted } from 'vue'

const props = withDefaults(defineProps<{
  items: T[]
  columnCount?: number
  gap?: number
  keyField?: string
  estimateHeight?: (item: T) => number
}>(), {
  columnCount: 4,
  gap: 16,
  keyField: 'id',
})

const windowWidth = ref(window.innerWidth)

function onResize() {
  windowWidth.value = window.innerWidth
}

onMounted(() => window.addEventListener('resize', onResize, { passive: true }))
onUnmounted(() => window.removeEventListener('resize', onResize))

const actualColumnCount = computed(() => {
  const w = windowWidth.value
  if (w >= 1280) return 4
  if (w >= 960) return 3
  return 2
})

function getKey(item: T): string | number {
  return (item as any)[props.keyField] ?? 0
}

function getIndex(item: T): number {
  return props.items.indexOf(item)
}

const columns = computed(() => {
  const cols = actualColumnCount.value
  const result: T[][] = Array.from({ length: cols }, () => [])
  const heights = Array(cols).fill(0)

  props.items.forEach(item => {
    const shortest = heights.indexOf(Math.min(...heights))
    result[shortest].push(item)
    heights[shortest] += props.estimateHeight?.(item) ?? 1
  })

  return result
})
</script>

<style scoped>
.masonry {
  display: flex;
  align-items: flex-start;
}

.masonry-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
</style>
