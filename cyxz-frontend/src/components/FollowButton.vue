<template>
  <button
    class="follow-btn"
    :class="[`variant-${variant}`, { followed: following }]"
    :disabled="loading"
    @click="$emit('toggle')"
  >
    {{ following ? followedText : text }}
  </button>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  following: boolean
  loading?: boolean
  text?: string
  followedText?: string
  variant?: 'profile' | 'author' | 'list'
}>(), {
  loading: false,
  text: '关注',
  followedText: '已关注',
  variant: 'profile'
})

defineEmits<{ toggle: [] }>()
</script>

<style scoped>
.follow-btn {
  cursor: pointer;
  border: none;
  transition: all 0.25s ease;
  flex-shrink: 0;
}
.follow-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ProfilePage 变体：粉紫渐变 */
.variant-profile {
  padding: 4px 14px;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: white;
  box-shadow: 0 2px 8px rgba(255, 107, 157, 0.25);
}
.variant-profile:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 4px 16px rgba(180, 132, 255, 0.35);
}
.variant-profile.followed {
  background: white;
  color: var(--text-dim);
  border: 1px solid var(--border);
  box-shadow: none;
}
.variant-profile.followed:hover:not(:disabled) {
  border-color: var(--pink);
  color: var(--pink);
  background: rgba(255, 182, 193, 0.15);
}

/* PostDetail 作者区变体：淡粉背景 + 紫字 */
.variant-author {
  padding: 3px 12px;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
  background: #F8DDF8;
  color: #B14FCF;
}
.variant-author:hover:not(:disabled) {
  background: #F3C8F3;
  color: #9A3FB0;
  transform: translateY(-1px);
}
.variant-author.followed {
  background: white;
  color: var(--text-dim);
  border: 1px solid var(--border);
}
.variant-author.followed:hover:not(:disabled) {
  border-color: var(--pink);
  color: var(--pink);
  background: rgba(255, 182, 193, 0.15);
}

/* CreatorCenter 列表态变体：浅粉描边 */
.variant-list {
  padding: 8px 24px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  border: 1.5px solid #FFB6CC;
  background: #FFF7FA;
  color: #FF6B9D;
}
.variant-list:hover:not(:disabled) {
  background: #FFE8F0;
}
.variant-list.followed {
  border-color: transparent;
  background: #F8DDF8;
  color: #B14FCF;
  cursor: default;
}
.variant-list.followed:hover:not(:disabled) {
  background: #F8DDF8;
}
</style>
