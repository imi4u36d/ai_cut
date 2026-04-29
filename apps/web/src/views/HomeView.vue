<template>
  <main class="home-page">
    <section class="home-hero">
      <h1>开启你的 <span>Agent 模式</span> 即刻造梦！</h1>
      <RouterLink class="home-composer" to="/generate">
        <div class="home-composer__upload" aria-hidden="true">+</div>
        <div class="home-composer__copy">
          <p>上传小说、输入文字，自动拆解脚本、关键帧与视频生成链路</p>
        </div>
        <div class="home-composer__toolbar">
          <span>Agent 模式</span>
          <span>自动</span>
          <span>灵感搜索</span>
          <span>创意设计</span>
        </div>
        <div class="home-composer__submit" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 19V5" />
            <path d="m5 12 7-7 7 7" />
          </svg>
        </div>
      </RouterLink>
    </section>

    <section class="feature-strip">
      <RouterLink v-for="item in featureCards" :key="item.to" class="feature-card" :to="item.to">
        <span class="feature-card__icon">{{ item.icon }}</span>
        <span>
          <strong>{{ item.title }}</strong>
          <small>{{ item.subtitle }}</small>
        </span>
      </RouterLink>
    </section>

    <section class="cases-section">
      <div class="cases-section__nav">
        <button class="cases-tab cases-tab-active" type="button">发现</button>
        <button class="cases-tab" type="button">短片</button>
        <button class="cases-tab" type="button">活动</button>
        <label class="cases-search">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input type="search" placeholder="产品" />
        </label>
      </div>

      <div v-if="caseStudies.length" class="case-masonry">
        <article
          v-for="(item, index) in caseStudies"
          :key="item.id"
          class="case-card"
          :class="`case-card-${(index % 5) + 1}`"
          :style="{ '--case-accent': item.accent, '--case-scene': item.scene }"
        >
          <div class="case-card__media">
            <video
              v-if="item.previewUrl"
              class="case-card__video"
              :src="item.previewUrl"
              autoplay
              loop
              muted
              playsinline
              preload="metadata"
            ></video>
            <div v-else class="case-card__placeholder">
              <span>{{ item.posterLabel }}</span>
            </div>
          </div>
          <div class="case-card__body">
            <h2>{{ item.title }}</h2>
            <p>{{ item.subtitle }}</p>
            <span>{{ item.description }}</span>
          </div>
        </article>
      </div>
      <div v-else class="cases-empty">
        {{ casesHint }}
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useTaskShowcase } from "@/composables/useTaskShowcase";
import { formatShowcaseTimeMeta, resolveShowcaseVisual, selectShowcasePrimaryModel } from "@/utils/showcase";

const { items, loading, errorMessage } = useTaskShowcase();

const featureCards = [
  {
    title: "一键生成",
    subtitle: "小说到成片",
    icon: "2.0",
    to: "/generate",
  },
  {
    title: "阶段工作流",
    subtitle: "逐步校准",
    icon: "AI",
    to: "/workflows",
  },
  {
    title: "图片生成",
    subtitle: "关键帧资产",
    icon: "4.1",
    to: "/materials",
  },
  {
    title: "任务管理",
    subtitle: "追踪结果",
    icon: "JD",
    to: "/tasks",
  },
];

const caseStudies = computed(() => {
  return items.value.slice(0, 8).map((item, index) => {
    const visual = resolveShowcaseVisual(item, index);
    return {
      ...item,
      ...visual,
      posterLabel: selectShowcasePrimaryModel(item) || item.aspectRatio || "真实案例",
      subtitle: formatShowcaseTimeMeta(item),
      description: item.description || "真实任务案例",
    };
  });
});

const casesHint = computed(() => {
  if (loading.value) {
    return "正在同步真实案例...";
  }
  if (errorMessage.value) {
    return errorMessage.value;
  }
  return "暂无可展示的真实案例";
});
</script>

<style scoped>
.home-page {
  min-height: 100%;
  padding: 72px 48px 56px;
  color: var(--text-strong);
}

.home-hero {
  display: grid;
  justify-items: center;
  gap: 42px;
}

.home-hero h1 {
  margin: 0;
  font-size: clamp(1.35rem, 2vw, 1.85rem);
  font-weight: 800;
  letter-spacing: -0.02em;
}

.home-hero h1 span {
  color: var(--accent-cyan);
}

.home-composer {
  position: relative;
  display: grid;
  width: min(100%, 1204px);
  min-height: 176px;
  padding: 20px 56px 16px 92px;
  border: 1px solid rgba(15, 20, 25, 0.06);
  border-radius: 24px;
  background: #fff;
  box-shadow: var(--shadow-panel);
}

.home-composer__upload {
  position: absolute;
  left: 24px;
  top: 24px;
  display: grid;
  place-items: center;
  width: 48px;
  height: 64px;
  border-radius: 2px;
  background: #f0f1f2;
  color: var(--text-muted);
  font-size: 1.5rem;
  transform: rotate(-7deg);
}

.home-composer__copy p {
  margin: 0;
  color: #8a97a1;
  font-size: 0.92rem;
}

.home-composer__toolbar {
  align-self: end;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 64px;
}

.home-composer__toolbar span {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid rgba(15, 20, 25, 0.06);
  border-radius: 9px;
  color: var(--text-strong);
  font-size: 0.78rem;
  font-weight: 650;
}

.home-composer__toolbar span:first-child {
  color: var(--accent-cyan);
}

.home-composer__submit {
  position: absolute;
  right: 18px;
  bottom: 16px;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #dce0e4;
  color: #fff;
}

.home-composer__submit svg {
  width: 18px;
  height: 18px;
}

.feature-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  width: min(100%, 1204px);
  margin: 40px auto 72px;
}

.feature-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 76px;
  padding: 12px 18px;
  border: 1px solid rgba(15, 20, 25, 0.05);
  border-radius: 22px;
  background: #fff;
}

.feature-card__icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #00d4f0, #246bfe);
  color: #fff;
  font-weight: 900;
  letter-spacing: -0.04em;
}

.feature-card strong,
.feature-card small {
  display: block;
}

.feature-card strong {
  font-size: 0.94rem;
}

.feature-card small {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 0.78rem;
}

.cases-section {
  display: grid;
  gap: 18px;
}

.cases-section__nav {
  display: flex;
  align-items: center;
  gap: 12px;
  width: min(100%, 1500px);
  margin: 0 auto;
}

.cases-tab {
  min-width: 68px;
  min-height: 36px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: var(--text-body);
  font-weight: 700;
  cursor: pointer;
}

.cases-tab-active {
  background: #eceff2;
  color: var(--text-strong);
}

.cases-search {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 312px;
  min-height: 36px;
  margin-left: 18px;
  padding: 0 12px;
  border: 1px solid rgba(15, 20, 25, 0.08);
  border-radius: 9px;
  background: #fff;
  color: var(--text-muted);
}

.cases-search svg {
  width: 17px;
  height: 17px;
}

.cases-search input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-strong);
  font-size: 0.82rem;
}

.case-masonry {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  grid-auto-flow: dense;
  gap: 0;
  width: min(100%, 1560px);
  margin: 0 auto;
}

.case-card {
  position: relative;
  min-height: 260px;
  overflow: hidden;
  background: var(--case-scene);
}

.case-card-1 {
  grid-column: span 2;
}

.case-card-2 {
  grid-row: span 2;
}

.case-card-4 {
  grid-row: span 2;
}

.case-card__media {
  position: absolute;
  inset: 0;
}

.case-card__video,
.case-card__placeholder {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.case-card__placeholder {
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at 50% 40%, var(--case-accent), transparent 56%),
    linear-gradient(135deg, #eef5f8, #ffffff);
  color: rgba(15, 20, 25, 0.52);
  font-weight: 800;
}

.case-card__body {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: grid;
  gap: 5px;
  padding: 46px 18px 16px;
  color: #fff;
  background: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.58));
}

.case-card__body h2,
.case-card__body p {
  margin: 0;
}

.case-card__body h2 {
  font-size: 1rem;
  font-weight: 800;
}

.case-card__body p,
.case-card__body span {
  font-size: 0.78rem;
  color: rgba(255, 255, 255, 0.82);
}

.cases-empty {
  display: grid;
  place-items: center;
  min-height: 220px;
  width: min(100%, 1204px);
  margin: 0 auto;
  border: 1px dashed rgba(15, 20, 25, 0.12);
  border-radius: 20px;
  background: #fff;
  color: var(--text-muted);
}

@media (max-width: 1180px) {
  .home-page {
    padding: 44px 22px 36px;
  }

  .feature-strip,
  .case-masonry {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .home-page {
    padding: 28px 14px 32px;
  }

  .home-composer {
    padding: 18px 18px 16px;
  }

  .home-composer__upload {
    position: static;
    margin-bottom: 16px;
  }

  .home-composer__toolbar {
    padding-top: 34px;
  }

  .feature-strip,
  .case-masonry {
    grid-template-columns: 1fr;
  }

  .cases-section__nav {
    flex-wrap: wrap;
  }

  .cases-search {
    width: 100%;
    margin-left: 0;
  }
}
</style>
