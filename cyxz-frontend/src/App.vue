<template>
  <Layout>
    <router-view />
  </Layout>
  <LoginModal v-model:visible="userStore.showLoginModal" />
</template>

<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import LoginModal from '@/components/LoginModal.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>
