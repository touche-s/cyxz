<template>
  <div ref="cardRef" class="stat-card">
    <div class="stat-icon-wrapper" :class="iconClass">
      <img :src="icon" :alt="label" class="stat-icon" />
    </div>
    <div class="stat-info">
      <span class="stat-value">{{ formattedValue }}</span>
      <span class="stat-label">{{ label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useCountUp } from '@/composables/useCountUp'
import { formatNumber } from '@/utils/format'

const props = defineProps<{
  icon: string
  iconClass: string
  value: string | number
  label: string
}>()

const cardRef = ref<HTMLElement | null>(null)
const targetValue = () => Number(props.value) || 0
const { displayValue, animate } = useCountUp(targetValue)

const formattedValue = computed(() => formatNumber(displayValue.value))

let started = false

onMounted(() => {
  if (cardRef.value) {
    startIfReady()
  }
})

watch(() => props.value, () => {
  startIfReady()
})

function startIfReady() {
  if (started) return
  if (targetValue() <= 0) return
  if (!cardRef.value) return
  started = true
  displayValue.value = 0
  animate()
}
</script>

<style scoped>
.stat-card {
  background: var(--card);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1.5px solid var(--border);
  box-shadow: 0 4px 12px var(--shadow);
  transition: all 0.22s ease-out;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px var(--shadow-lg);
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.works-icon {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(99, 102, 241, 0.1));
}

.views-icon {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(52, 211, 153, 0.1));
}

.likes-icon {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.1), rgba(248, 113, 113, 0.1));
}

.collections-icon {
  background: linear-gradient(135deg, rgba(234, 179, 8, 0.1), rgba(250, 204, 21, 0.1));
}

.fans-icon {
  background: linear-gradient(135deg, #e8d5f5, #d4b8f0);
}

.comments-icon {
  background: linear-gradient(135deg, #d5e8f5, #b8d4f0);
}

.stat-icon {
  font-size: 24px;
  width: 24px;
  height: 24px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 22px;
  font-weight: 800;
  color: var(--text);
}

.stat-label {
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 2px;
}
</style>
