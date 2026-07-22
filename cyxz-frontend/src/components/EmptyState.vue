<template>
  <div class="empty-state">
    <Icon v-if="icon" :icon="icon" class="empty-icon pink-icon" />
    <p class="empty-text">{{ title }}</p>
    <p v-if="hint" class="empty-hint">{{ hint }}</p>
    <div v-if="$slots.actions" class="empty-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'

defineProps<{
  icon?: string
  title: string
  hint?: string
}>()
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
}
.empty-icon {
  width: 64px;
  height: 64px;
  opacity: 0.5;
  margin-bottom: 12px;
}
.empty-text {
  font-size: 14px;
  color: var(--text-dim);
  margin: 0;
}
.empty-hint {
  font-size: 13px;
  color: var(--text-dim);
  margin-top: 6px;
}
.empty-actions {
  margin-top: 16px;
}
</style>
