<template>
  <div class="private-messages">
    <div class="pm-container">
      <!-- 左侧对话列表 -->
      <aside class="pm-sidebar">
        <div class="sidebar-header">
          <h2>私信</h2>
        </div>
        <div class="sidebar-empty">
          <div class="empty-icon-wrap">
            <Icon icon="ph:chat-circle-text" class="empty-iconify pink-icon" />
          </div>
          <p class="empty-label">暂无私信对话</p>
        </div>
      </aside>

      <!-- 右侧对话区 -->
      <div class="pm-main-wrap">
        <main class="pm-main">
          <div class="pm-empty">
            <div class="pm-empty-icon">
              <Icon icon="ph:envelope-open" class="pm-empty-iconify pink-icon" />
            </div>
            <p class="pm-empty-title">选择一条对话开始聊天</p>
            <p class="pm-empty-desc">这里会显示你与其他用户的私信对话</p>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
</script>

<style scoped>
.private-messages {
  min-height: calc(100vh - 78px);
  padding-top: 78px;
  background: var(--bg-soft);
}

.pm-container {
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 24px 28px 0;
  display: grid;
  grid-template-columns: 280px 1fr;
  justify-content: start;
  gap: 0;
  min-height: calc(100vh - 122px);
}

.pm-main-wrap {
  display: flex;
  justify-content: center;
}

/* ===== 左侧对话列表 ===== */
.pm-sidebar {
  background: var(--card);
  border-radius: 16px;
  border: 1.5px solid var(--border-light);
  box-shadow: var(--shadow);
  height: fit-content;
  overflow: hidden;
}

.sidebar-header {
  padding: 20px 20px 16px;
  border-bottom: 1px solid var(--border-light);
}

.sidebar-header h2 {
  font-size: 18px;
  font-weight: 800;
  color: var(--text);
  margin: 0;
  letter-spacing: 0.5px;
}

.sidebar-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;
}

.empty-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--pink-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.7;
}

.empty-icon-wrap svg {
  width: 26px;
  height: 26px;
}

.empty-label {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0;
}

/* ===== 右侧对话区 ===== */
.pm-main {
  width: 640px;
  max-width: 100%;
  background: var(--card);
  border-radius: 16px;
  border: 1.5px solid var(--border-light);
  box-shadow: var(--shadow);
  overflow: hidden;
  margin: 0 auto;
}

.pm-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
  gap: 16px;
}

.pm-empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--pink-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.75;
}

.pm-empty-icon svg {
  width: 40px;
  height: 40px;
}

.pm-empty-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.pm-empty-desc {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .pm-container {
    grid-template-columns: 1fr;
    padding: 16px;
    gap: 16px;
  }
}
</style>
