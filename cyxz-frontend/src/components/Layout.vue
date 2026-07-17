<template>
  <div class="layout">
    <div class="bg-layer"></div>
    <HeaderBar />
    <slot />
  </div>
</template>

<script setup lang="ts">
import HeaderBar from '@/components/HeaderBar.vue'
</script>

<style scoped>
.layout {
  min-height: 100vh;
  position: relative;
}

.bg-layer {
  position: fixed;
  inset: 0;
  z-index: -2;
  background:
    radial-gradient(circle at 30% 20%, #FFF0F8 0%, transparent 50%),
    radial-gradient(circle at 70% 80%, #F3ECFF 0%, transparent 50%),
    radial-gradient(ellipse 800px 600px at 15% 15%, rgba(255, 182, 215, 0.35) 0%, transparent 65%),
    radial-gradient(ellipse 600px 500px at 80% 55%, rgba(192, 132, 252, 0.2) 0%, transparent 65%),
    radial-gradient(ellipse 500px 400px at 55% 85%, rgba(129, 140, 248, 0.12) 0%, transparent 65%);
  animation: bgFlow 12s ease-in-out infinite alternate;
}

@keyframes bgFlow {
  0% { opacity: 0.7; transform: scale(1); }
  50% { opacity: 0.9; }
  100% { opacity: 1; transform: scale(1.02); }
}
</style>
