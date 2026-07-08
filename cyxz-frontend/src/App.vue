<template>
  <div class="app">
    <div class="bg-layer"></div>
    <HeaderBar @open-login="showLogin = true" />
    <router-view />
    <LoginModal v-model:visible="showLogin" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import HeaderBar from '@/components/HeaderBar.vue'
import LoginModal from '@/components/LoginModal.vue'

const showLogin = ref(false)
</script>

<style scoped>
.app {
  min-height: 100vh;
}

.bg-layer {
  position: fixed;
  inset: 0;
  z-index: -2;
  background:
    radial-gradient(ellipse 800px 600px at 15% 15%, rgba(255, 182, 215, 0.4) 0%, transparent 65%),
    radial-gradient(ellipse 600px 500px at 80% 55%, rgba(192, 132, 252, 0.25) 0%, transparent 65%),
    radial-gradient(ellipse 500px 400px at 55% 85%, rgba(129, 140, 248, 0.15) 0%, transparent 65%),
    radial-gradient(ellipse 400px 300px at 40% 40%, rgba(251, 191, 36, 0.08) 0%, transparent 70%);
  animation: bgFlow 12s ease-in-out infinite alternate;
}

@keyframes bgFlow {
  0% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 0.9; }
  100% { opacity: 1; transform: scale(1.02); }
}
</style>
