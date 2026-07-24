<template>
  <main class="main-content">
    <div class="page-inner">
      <section class="hero-grid">
        <div class="hero-card hero-copy-card">
          <span class="hero-kicker">首页 · 圈子广场</span>
          <h1>进入你喜欢的同好圈</h1>
          <p>
            从作品与兴趣出发找到自己的固定落点。先进入圈子，再看内容、发作品、认识同好，让首页先有归属感，再有信息流。
          </p>
          <div class="hero-actions">
            <button class="primary-btn">进入我的圈子</button>
            <button class="ghost-btn">查看全部圈子</button>
          </div>
        </div>

        <aside class="hero-card ranking-card">
          <div class="section-top">
            <div>
              <span class="block-kicker">热门排行</span>
              <h2>本周活跃圈子</h2>
            </div>
            <span class="section-note">Top 5</span>
          </div>
          <div class="ranking-list">
            <div class="ranking-item" v-for="(circle, index) in rankedCircles" :key="circle.id">
              <div class="ranking-index">{{ index + 1 }}</div>
              <div class="ranking-main">
                <strong>{{ circle.name }}</strong>
                <span>{{ circle.intro }}</span>
              </div>
              <div class="ranking-meta">
                <span>{{ circle.postText }}</span>
              </div>
            </div>
          </div>
        </aside>
      </section>

      <section class="circle-section">
        <div class="section-top section-top--spaced">
          <div>
            <span class="block-kicker">全部圈子</span>
            <h2>从作品与主题进入社区</h2>
            <p>当前先用统一封面占位承接圈子