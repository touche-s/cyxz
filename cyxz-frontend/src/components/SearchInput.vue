<template>
  <div class="search-input" :class="`search-input--${variant}`" :style="{ width: containerWidth }">
    <Icon icon="ph:magnifying-glass" class="search-input__icon" />
    <input
      :value="modelValue"
      type="text"
      :placeholder="placeholder"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      @keyup.enter="$emit('search')"
    />
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'

defineProps<{
  modelValue: string
  placeholder?: string
  variant?: 'header' | 'page' | 'inline' | 'pill'
  containerWidth?: string
}>()

defineEmits<{
  'update:modelValue': [value: string]
  'search': []
}>()
</script>

<style scoped>
.search-input {
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.22s ease-out;
}

.search-input__icon {
  flex-shrink: 0;
  opacity: 0.4;
  color: var(--text-dim);
}

.search-input input {
  border: none;
  background: transparent;
  outline: none;
  color: var(--text);
  width: 100%;
}

.search-input input::placeholder {
  color: var(--text-dim);
}

/* ===== variant: header ===== */
.search-input--header {
  background: var(--search-bg);
  border: 1.5px solid var(--border);
  border-radius: 12px;
  padding: 9px 16px;
  gap: 10px;
}
.search-input--header:focus-within {
  border-color: var(--purple);
  background: var(--input-focus-bg);
  box-shadow: 0 0 0 3px rgba(180, 132, 255, 0.1);
}
.search-input--header .search-input__icon {
  width: 18px;
  height: 18px;
  color: var(--purple);
  opacity: 1;
}
.search-input--header input {
  font-size: 13px;
  width: 220px;
}

/* ===== variant: page ===== */
.search-input--page {
  flex: 1;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 16px;
  gap: 10px;
}
.search-input--page:focus-within {
  border-color: var(--pink);
}
.search-input--page .search-input__icon {
  width: 18px;
  height: 18px;
}
.search-input--page input {
  font-size: 15px;
  flex: 1;
}

/* ===== variant: inline ===== */
.search-input--inline {
  background: var(--bg);
  border: 1.5px solid var(--border);
  border-radius: 12px;
  padding: 0 14px;
}
.search-input--inline:focus-within {
  border-color: var(--border);
  background: var(--card);
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.08);
}
.search-input--inline .search-input__icon {
  width: 16px;
  height: 16px;
}
.search-input--inline input {
  font-size: 13px;
  padding: 8px 10px;
  width: 180px;
}

/* ===== variant: pill ===== */
.search-input--pill {
  background: rgba(255,248,252,0.95);
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 8px 16px;
  box-shadow: inset 0 1px 3px rgba(255,107,157,0.06);
}
.search-input--pill:focus-within {
  background: var(--card);
  border-color: var(--border);
  box-shadow:
    inset 0 1px 3px rgba(255,107,157,0.06),
    0 0 0 2px rgba(255,107,157,0.12);
}
.search-input--pill .search-input__icon {
  width: 14px;
  height: 14px;
}
.search-input--pill input {
  font-size: 13px;
  width: 150px;
}
</style>
