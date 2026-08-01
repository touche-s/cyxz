<template>
  <Layout>
    <router-view />
  </Layout>
  <LoginModal v-model:visible="userStore.showLoginModal" />
</template>

<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import LoginModal from '@/components/LoginModal.vue'
import { watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { useChatWebSocket } from '@/composables/useChatWebSocket'

const userStore = useUserStore()
const messageStore = useMessageStore()
const { connect, disconnect, onNotification } = useChatWebSocket()

// 登录后建立 WS 连接，登出后断开
watch(() => userStore.userInfo, (user) => {
  if (user) {
    connect()
  } else {
    disconnect()
  }
}, { immediate: true })

// 收到通知推送，未读数实时 +1
onNotification(() => {
  messageStore.unreadCount++
})
</script>
