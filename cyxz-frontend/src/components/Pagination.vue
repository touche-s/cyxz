<template>
  <div class="pagination-container" v-if="totalPages > 1">
    <button class="page-btn" :disabled="current <= 1" @click="change(current - 1)">上一页</button>
    <span class="page-info">{{ current }} / {{ totalPages }}</span>
    <button class="page-btn" :disabled="current >= totalPages" @click="change(current + 1)">下一页</button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  current: number
  total: number
  pageSize: number
}>()

const emit = defineEmits<{
  change: [page: number]
}>()

const totalPages = computed(() => Math.ceil(props.total / props.pageSize))

function change(page: number) {
  if (page < 1 || page > totalPages.value) return
  emit('change', page)
}
</script>

<style scoped>
.pagination-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
  padding-bottom: 8px;
}

.page-btn {
  padding: 6px 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: default;
}

.page-info {
  font-size: 13px;
  color: var(--text-dim);
}
</style>
