<template>
  <div
    class="user-avatar"
    :class="[shapeClass, borderClass, shadowClass, { clickable: isClickable }]"
    :style="containerStyle"
    @click="handleClick"
  >
    <img v-if="displaySrc" :src="displaySrc" :alt="alt || name || 'avatar'" class="avatar-img" />
    <span v-else class="avatar-fallback">{{ initial }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useNavigate } from '@/composables/useNavigate'
import { avatarUrl } from '@/utils/avatar'

const props = withDefaults(defineProps<{
  /** Avatar image URL */
  src?: string | null
  /** User name, used for initial-letter fallback */
  name?: string
  /** Avatar size in px */
  size?: number
  /** User id — when provided, clicking opens /user/:id in a new tab */
  userId?: string | number
  /** Force pointer cursor + hover feedback (without auto-navigation) */
  clickable?: boolean
  /** Stop click event propagation (e.g. inside a clickable card) */
  stopPropagation?: boolean
  /** Avatar shape */
  shape?: 'circle' | 'rounded'
  /** Border style variant */
  border?: 'white' | 'card' | 'pink' | false
  /** Shadow variant */
  shadow?: 'sm' | 'lg' | false
  /** Empty-avatar fallback strategy: 'initial' (letter + gradient) or 'image' (default SVG) */
  fallback?: 'initial' | 'image'
  /** img alt text */
  alt?: string
}>(), {
  size: 40,
  shape: 'circle',
  border: false,
  shadow: false,
  stopPropagation: false,
  fallback: 'initial',
})

const emit = defineEmits<{ click: [e: MouseEvent] }>()

const { open } = useNavigate()

const initial = computed(() => (props.name || 'U').charAt(0).toUpperCase())

const displaySrc = computed(() => {
  if (props.fallback === 'image') return avatarUrl(props.src)
  return props.src || ''
})

const isClickable = computed(() => props.clickable || !!props.userId)

const containerStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  fontSize: `${Math.round(props.size * 0.42)}px`,
}))

const shapeClass = computed(() => (props.shape === 'rounded' ? 'is-rounded' : 'is-circle'))
const borderClass = computed(() => (props.border ? `border-${props.border}` : ''))
const shadowClass = computed(() => (props.shadow ? `shadow-${props.shadow}` : ''))

function handleClick(e: MouseEvent) {
  if (props.stopPropagation) e.stopPropagation()
  emit('click', e)
  if (props.userId) {
    open(`/user/${props.userId}`)
  }
}
</script>

<style scoped>
.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--gradient-brand);
  transition: opacity 0.15s ease, transform 0.2s ease;
}

.is-circle { border-radius: 50%; }
.is-rounded { border-radius: 32%; }

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-fallback {
  color: #fff;
  font-weight: 700;
  line-height: 1;
  user-select: none;
}

.clickable { cursor: pointer; }
.clickable:hover { opacity: 0.85; }

.border-white { border: 2px solid var(--white); }
.border-card { border: 2px solid var(--card); }
.border-pink { border: 1px solid rgba(255, 107, 157, 0.12); }

.shadow-sm { box-shadow: 0 1px 4px var(--shadow); }
.shadow-lg { box-shadow: var(--shadow-lg); }
</style>
