<template>
  <div class="agreement-page">
    <div class="agreement-container">
      <header class="agreement-page-header">
        <h1><Icon icon="ph:info" class="title-icon" />社区公约</h1>
        <p>共同维护健康的社区环境</p>
      </header>

      <div class="agreement-intro">
        <p><strong>次元小站</strong>是一个由大家共同创建的内容社区。社区鼓励大家围绕各自的爱好认真创作和交流，结识同好，收获成长。为了能让所有用户享有开放、友善和有收获感的社区，我们希望以下能成为社区的共识：</p>
      </div>

      <div class="agreement-principles">
        <div class="principle-card">
          <Icon icon="ph:pencil-simple" class="principle-icon pink-icon" />
          <h3>认真创作</h3>
          <p>我们鼓励大家创作真实的内容，分享自己的见解和作品。尊重原创，不抄袭、不搬运，对自己发布的内容负责。</p>
        </div>
        <div class="principle-card">
          <Icon icon="ph:handshake" class="principle-icon pink-icon" />
          <h3>友善交流</h3>
          <p>尊重不同观点，理性讨论。避免人身攻击和恶意言论，营造和谐友好的交流氛围。</p>
        </div>
        <div class="principle-card">
          <Icon icon="ph:shield" class="principle-icon pink-icon" />
          <h3>拥抱创新</h3>
          <p>鼓励探索新的表达方式和创作形式，支持创意和灵感的碰撞，让社区充满活力。</p>
        </div>
      </div>

      <div class="agreement-content">
        <div class="content-section">
          <h2>一、内容规范</h2>
          <div class="content-item">
            <h4>1.1 禁止发布的内容</h4>
            <ul>
              <li>违法、违规、色情、暴力等不良内容</li>
              <li>广告、营销推广等商业性质内容</li>
              <li>虚假信息、谣言等不实内容</li>
              <li>人身攻击、恶意诋毁等言论</li>
              <li>侵犯他人隐私的内容</li>
            </ul>
          </div>
          <div class="content-item">
            <h4>1.2 内容审核机制</h4>
            <p>平台有权对用户发布的内容进行审核，对于违反社区公约的内容，平台有权采取删除、屏蔽等措施。</p>
          </div>
        </div>

        <div class="content-section">
          <h2>二、版权声明</h2>
          <div class="content-item">
            <h4>2.1 原创内容保护</h4>
            <p>用户发布的内容需保证原创或拥有合法授权，禁止抄袭、盗用他人作品。</p>
          </div>
          <div class="content-item">
            <h4>2.2 平台权利</h4>
            <p>平台有权对侵权内容进行处理，并保留追究法律责任的权利。</p>
          </div>
        </div>

        <div class="content-section">
          <h2>三、社区行为</h2>
          <div class="content-item">
            <h4>3.1 互动规范</h4>
            <ul>
              <li>尊重他人，友好交流</li>
              <li>遵守平台规则，维护社区秩序</li>
              <li>合理使用平台功能，不滥用资源</li>
              <li>不进行恶意刷屏、灌水等行为</li>
            </ul>
          </div>
        </div>

        <div class="content-section">
          <h2>四、违规处理</h2>
          <div class="content-item">
            <h4>4.1 处理方式</h4>
            <p>对于违反社区公约的用户，平台将根据情节轻重采取警告、限制功能、封禁账号等措施。</p>
          </div>
          <div class="content-item">
            <h4>4.2 申诉渠道</h4>
            <p>用户对处理结果有异议的，可以通过平台申诉渠道进行申诉。</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
</script>

<style scoped>
.agreement-page {
  max-width: 800px;
  margin: 0 auto;
}

.agreement-container {
  background: var(--card);
  border-radius: 20px;
  padding: 32px;
  border: 1.5px solid var(--border);
  box-shadow: 0 8px 32px rgba(180, 132, 255, 0.08);
}

.agreement-page-header {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

.agreement-page-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 8px 0;
}

.agreement-page-header p {
  font-size: 14px;
  color: var(--text-dim);
  margin: 0;
}

.title-icon {
  width: 20px;
  height: 20px;
  margin-right: 8px;
}

.agreement-intro {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.08), rgba(180, 132, 255, 0.08));
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 28px;
  border: 1px solid var(--border);
}

.agreement-intro p {
  font-size: 14px;
  color: var(--text);
  line-height: 1.8;
  margin: 0;
}

.agreement-intro strong {
  color: var(--pink);
}

.agreement-principles {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}

.principle-card {
  background: var(--card);
  border-radius: 16px;
  padding: 20px;
  text-align: center;
  border: 1.5px solid var(--border);
  box-shadow: 0 4px 12px rgba(180, 132, 255, 0.06);
  transition: all 0.22s ease-out;
}

.principle-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(180, 132, 255, 0.12);
  border-color: var(--border);
}

.principle-icon {
  width: 24px;
  height: 24px;
  margin-right: 12px;
  font-size: 32px;
  margin-bottom: 12px;
}

.principle-card h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 8px 0;
}

.principle-card p {
  font-size: 13px;
  color: var(--text-dim);
  line-height: 1.6;
  margin: 0;
}

.agreement-content {
  padding-top: 20px;
  border-top: 1px solid var(--border);
}

.content-section {
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}

.content-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.content-section h2 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.content-section h2::before {
  content: '';
  width: 4px;
  height: 18px;
  background: var(--gradient-brand);
  border-radius: 2px;
}

.content-item {
  margin-bottom: 16px;
}

.content-item:last-child {
  margin-bottom: 0;
}

.content-item h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 8px 0;
}

.content-item p {
  font-size: 14px;
  color: var(--text-dim);
  line-height: 1.8;
  margin: 0;
  padding-left: 12px;
}

.content-item ul {
  margin: 0;
  padding-left: 28px;
}

.content-item li {
  font-size: 14px;
  color: var(--text-dim);
  line-height: 1.8;
  margin-bottom: 6px;
}

.content-item li:last-child {
  margin-bottom: 0;
}
</style>
