<template>
  <main class="main-content">
    <div class="page-inner">
      <div class="card">
        <h1>社区规范</h1>
        <p class="update-time">最后更新于 2026年7月</p>

        <section>
          <h2>一、总则</h2>
          <p>次元小站是一个面向 ACG 爱好者与内容创作者的垂直内容社区。我们鼓励友善交流、原创分享和围绕作品/IP 的有意义讨论。所有用户在发布内容、参与讨论时，请遵守本规范。</p>
        </section>

        <section>
          <h2>二、鼓励的内容</h2>
          <ul>
            <li>原创同人图、同人文、同人音乐等创作作品</li>
            <li>关于动漫、游戏、轻小说等作品的有深度讨论与安利</li>
            <li>角色分析与世界观考据</li>
            <li>攻略整理与新手引导</li>
            <li>COS 摄影与道具制作分享</li>
            <li>周边开箱与收藏展示</li>
            <li>友善、有建设性的评论与互动</li>
          </ul>
        </section>

        <section>
          <h2>三、禁止的行为与内容</h2>
          <ul>
            <li>发布违法信息或侵犯他人合法权益的内容</li>
            <li>发布色情、低俗或引人不适的内容</li>
            <li>人身攻击、辱骂、引战或恶意骚扰他人</li>
            <li>发布广告、垃圾信息或恶意刷屏</li>
            <li>冒充他人身份或官方人员</li>
            <li>未经授权大量转载他人原创作品</li>
            <li>发布与 ACG 完全无关的纯水帖</li>
            <li>诱导点击、恶意跳转等不安全链接</li>
          </ul>
        </section>

        <section>
          <h2>四、版权与原创声明</h2>
          <ul>
            <li>发布他人作品时必须标明出处并获得授权</li>
            <li>转载内容请在标题或正文显著位置说明来源</li>
            <li>AI 生成内容建议标注，便于读者区分</li>
            <li>如发现自己的原创作品被未经授权转载，可通过举报功能联系我们</li>
          </ul>
        </section>

        <section>
          <h2>五、举报与处理</h2>
          <p>如果你发现违规内容，可以通过帖子或评论的举报按钮提交举报。我们会在收到举报后尽快处理。</p>
          <p>对于违规行为，我们将视情节采取以下措施：</p>
          <ul>
            <li>警告提醒</li>
            <li>隐藏或删除违规内容</li>
            <li>临时或永久限制发布权限</li>
          </ul>
        </section>

        <section>
          <h2>六、反馈建议</h2>
          <p>如果你对社区规范或平台功能有任何建议，欢迎通过反馈渠道联系我们。次元小站会持续完善社区环境，感谢每一位用户的贡献。</p>
        </section>
      </div>
    </div>
  </main>
</template>

<style scoped>
.main-content {
  padding: 90px 0 60px;
}

.page-inner {
  width: min(800px, calc(100vw - 48px));
  margin: 0 auto;
}

.card {
  background: var(--card);
  border-radius: 16px;
  padding: 36px 40px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
}

h1 {
  font-size: 24px;
  font-weight: 800;
  color: var(--text);
  margin: 0 0 4px 0;
}

.update-time {
  font-size: 13px;
  color: var(--text-dim);
  margin: 0 0 32px 0;
}

section {
  margin-bottom: 28px;
}

section h2 {
  font-size: 17px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}

section p {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.8;
  margin: 0 0 8px 0;
}

ul {
  margin: 0;
  padding-left: 20px;
}

li {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.8;
}

@media (max-width: 768px) {
  .card { padding: 24px 20px; }
}
</style>
