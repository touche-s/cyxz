<template>
  <div class="loading-container" :class="{ inline }">
    <div class="loading-spinner"></div>
    <p v-if="text">{{ text }}</p>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  text?: string
  inline?: boolean
}>(), {
  text: '加载中...',
  inline: false
})
</script>

<style scoped>
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  gap: 12px;
}
.loading-container.inline {
  flex-direction: row;
  padding: 0;
  gap: 8px;
}
.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border);
  border-top-color: var(--brand);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
.loading-container.inline .loading-spinner {
  width: 16px;
  height: 16px;
  border-width: 2px;
}
.loading-container p {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
