<template>
  <div class="magic-page">
    <header class="magic-page-header">
      <div class="header-left">
        <h1><Icon icon="ph:magic-wand" class="title-icon" />妙笔</h1>
        <p>AI辅助创作工具</p>
      </div>
    </header>

    <div class="magic-banner">
      <div class="magic-banner-content">
        <h2>让AI帮你创作</h2>
        <p>智能选题、文案生成、排版优化</p>
      </div>
    </div>

    <div class="magic-features">
      <div class="magic-card" v-for="feature in magicFeatures" :key="feature.title">
        <Icon :icon="feature.icon" class="magic-icon" />
        <h3>{{ feature.title }}</h3>
        <p>{{ feature.desc }}</p>
        <button class="magic-btn">{{ feature.btnText }}</button>
      </div>
    </div>

    <div class="magic-tips">
      <div class="tips-header">
        <h3><Icon icon="ph:lightbulb" class="title-icon" />使用提示</h3>
      </div>
      <ul class="tips-list">
        <li>输入关键词或主题，AI会帮你生成相关选题</li>
        <li>可以选择不同风格的文案生成（轻松、正式、幽默等）</li>
        <li>生成的内容需要人工审核后再发布</li>
        <li>建议结合自己的风格进行修改和润色</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'

const magicFeatures = [
  { icon: 'ph:lightbulb', title: '智能选题', desc: '输入关键词，AI帮你生成热门选题', btnText: '开始选题' },
  { icon: 'ph:pencil-simple', title: '文案生成', desc: '根据主题生成不同风格的文案', btnText: '生成文案' },
  { icon: 'ph:image', title: '图片建议', desc: '根据内容推荐配图方案', btnText: '获取建议' },
  { icon: 'ph:chart-bar', title: '排版优化', desc: '智能优化文章排版和格式', btnText: '优化排版' },
]
</script>

<style scoped>
.magic-page {
  background: var(--card);
  border-radius: 20px;
  padding: 0;
  border: 1.5px solid var(--border);
  box-shadow: 0 4px 12px rgba(180, 132, 255, 0.06);
}

.magic-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 28px 28px 20px;
  border-bottom: 1px solid var(--border);
}

.header-left h1 {
  font-size: 24px;
  font-weight: 800;
  color: var(--text);
  margin-bottom: 4px;
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.header-left p {
  font-size: 14px;
  color: var(--text-dim);
}

.title-icon {
  width: 20px;
  height: 20px;
  margin-right: 8px;
}

.magic-banner {
  background: linear-gradient(135deg, var(--pink) 0%, var(--purple) 100%);
  border-radius: 20px;
  padding: 40px;
  color: var(--white);
  text-align: center;
  margin: 0 28px 24px;
}

.magic-banner-content h2 {
  font-size: 28px;
  font-weight: 800;
  margin-bottom: 8px;
}

.magic-banner-content p {
  font-size: 15px;
  opacity: 0.9;
}

.magic-features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin: 0 28px 24px;
}

.magic-card {
  background: var(--pink-bg);
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  border: 1.5px solid transparent;
  transition: all 0.22s ease-out;
}

.magic-card:hover {
  border-color: var(--border);
  transform: translateY(-2px);
}

.magic-icon {
  width: 36px;
  height: 36px;
  margin-bottom: 12px;
  color: var(--purple);
}

.magic-card h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 8px;
}

.magic-card p {
  font-size: 13px;
  color: var(--text-dim);
  line-height: 1.5;
  margin-bottom: 16px;
}

.magic-btn {
  padding: 10px 24px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  background: var(--gradient-brand);
  color: var(--white);
  transition: all 0.22s ease-out;
}

.magic-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.3);
}

.magic-tips {
  background: rgba(255, 193, 7, 0.1);
  border-radius: 16px;
  padding: 24px;
  margin: 0 28px 28px;
  border: 1.5px solid rgba(255, 193, 7, 0.3);
}

.tips-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 12px;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
}

.tips-list li {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 2;
  margin-bottom: 6px;
}
</style>
