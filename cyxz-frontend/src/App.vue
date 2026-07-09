<template>
  <Layout @open-login="showLogin = true">
    <router-view />
  </Layout>
  <LoginModal v-model:visible="showLogin" />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Layout from '@/components/Layout.vue'
import LoginModal from '@/components/LoginModal.vue'

const showLogin = ref(false)
</script>
