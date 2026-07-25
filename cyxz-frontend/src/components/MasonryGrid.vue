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
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  items: T[]
  columnCount?: number
  gap?: number
  keyField?: string
  /** 估算卡片高度，用于最短列分配。不传则均匀分配（退化为轮询） */
  estimateHeight?: (item: T) => number
}>(), {
  columnCount: 4,
  gap: 16,
  keyField: 'id',
})

function getKey(item: T): string | number {
  return (item as any)[props.keyField] ?? 0
}

function getIndex(item: T): number {
  return props.items.indexOf(item)
}

const columns = computed(() => {
  const cols = props.columnCount
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
