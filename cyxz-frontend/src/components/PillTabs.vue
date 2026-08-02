<template>
  <div class="pill-tabs">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      class="pill"
      :class="{ active: modelValue === tab.key }"
      @click="$emit('update:modelValue', tab.key)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>

<script setup lang="ts">
export interface PillTabItem {
  key: string
  label: string
}

defineProps<{
  tabs: PillTabItem[]
  modelValue: string | number | null
}>()

defineEmits<{
  'update:modelValue': [key: string | number | null]
}>()
</script>

<style scoped>
.pill-tabs {
  display: inline-flex;
  max-width: 100%;
  gap: 6px;
  margin-bottom: 16px;
  overflow-x: auto;
  padding: 5px;
  background: rgba(255, 244, 250, 0.96);
  border: 1px solid var(--border-light);
  border-radius: 999px;
  box-shadow: 0 4px 14px rgba(255, 107, 157, 0.06);
}

.pill-tabs::-webkit-scrollbar { display: none; }

.pill {
  display: flex;
  align-items: center;
  padding: 8px 18px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  border: none;
  background: transparent;
  color: var(--text-dim);
  transition: all 0.22s ease-out;
  flex-shrink: 0;
}

.pill.active {
  background: var(--card);
  color: var(--pink);
  font-weight: 700;
  box-shadow:
    0 4px 12px rgba(255, 107, 157, 0.1),
    inset 0 0 0 1px rgba(255, 107, 157, 0.16);
}

.pill:hover:not(.active) {
  color: var(--pink);
  background: var(--pink-bg);
}
</style>
