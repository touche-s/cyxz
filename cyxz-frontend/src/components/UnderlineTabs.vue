<template>
  <div class="underline-tabs">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      class="tab-btn"
      :class="{ active: modelValue === tab.key }"
      @click="$emit('update:modelValue', tab.key)"
    >
      <Icon v-if="tab.icon" :icon="tab.icon" class="tab-icon" />
      <span>{{ tab.label }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'

export interface TabItem {
  key: string
  label: string
  icon?: string
}

defineProps<{
  tabs: TabItem[]
  modelValue: string
}>()

defineEmits<{
  'update:modelValue': [key: string]
}>()
</script>

<style scoped>
.underline-tabs {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
  padding: 0 0 8px 6px;
  border-bottom: 1px solid var(--border-light);
}

.tab-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 0 6px 0;
  border: none;
  background: transparent;
  color: var(--text-dim);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.22s ease-out;
}

.tab-btn::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  border-radius: 1px;
  background: var(--pink);
  transition: width 0.22s ease-out;
}

.tab-btn:hover {
  color: var(--pink);
}

.tab-btn.active {
  color: var(--pink);
  font-weight: 700;
}

.tab-btn.active::after {
  width: 20px;
}

.tab-icon {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
}
</style>
