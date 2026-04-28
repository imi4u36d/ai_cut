<template>
  <main ref="scrollRoot" class="official-site" id="top">
    <MarketingTopbar active-page="home" scroll-sections @section-request="scrollToSection" />

    <section class="hero-section reveal-on-scroll is-visible">
      <div class="hero-section__copy">
        <p class="hero-section__eyebrow">Open-source AI video production</p>
        <h1>
          <span>开源免费的</span>
          <span>AI 短剧</span>
          <span>生产系统</span>
        </h1>
        <p class="hero-section__lead">
          从文本到分镜、关键帧和成片。自由部署，免费使用，专注高效和一致性。
        </p>
        <div class="hero-section__actions">
          <RouterLink class="button button-primary" to="/generate">开始创作</RouterLink>
          <a class="button button-secondary" href="#principles" @click.prevent="scrollToSection('principles')">查看产品原则</a>
        </div>
      </div>

      <div class="hero-console" aria-label="煎豆工作流预览">
        <div class="hero-console__top">
          <span></span>
          <span></span>
          <span></span>
          <strong>JianDou Workflow</strong>
        </div>
        <div class="hero-console__body">
          <aside class="hero-console__sidebar">
            <span v-for="item in consoleNav" :key="item" :class="{ 'is-active': item === 'Storyboard' }">{{ item }}</span>
          </aside>
          <div class="hero-console__main">
            <div class="pipeline">
              <article v-for="(stage, index) in pipelineStages" :key="stage.title" class="pipeline__stage" :style="{ '--stage-index': index }">
                <span>{{ stage.code }}</span>
                <strong>{{ stage.title }}</strong>
                <small>{{ stage.description }}</small>
              </article>
            </div>
            <div class="consistency-board">
              <article>
                <span>角色锚点</span>
                <strong>98%</strong>
              </article>
              <article>
                <span>镜头连续</span>
                <strong>Start / End</strong>
              </article>
              <article>
                <span>复用参数</span>
                <strong>{{ primarySeedLabel }}</strong>
              </article>
            </div>
            <div class="timeline-preview">
              <span v-for="shot in timelineShots" :key="shot" :style="{ '--shot': shot }"></span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="proof-strip reveal-on-scroll">
      <article v-for="item in proofItems" :key="item.title">
        <span>{{ item.value }}</span>
        <strong>{{ item.title }}</strong>
        <p>{{ item.description }}</p>
      </article>
    </section>

    <section class="section-block reveal-on-scroll" id="principles">
      <div class="section-heading">
        <p>Product principles</p>
        <h2>少一点手工搬运，多一点稳定产出。</h2>
      </div>
      <div class="principle-grid">
        <article v-for="(principle, index) in principles" :key="principle.title" class="principle-card reveal-on-scroll" :style="{ '--reveal-delay': `${index * 80}ms` }">
          <span>{{ principle.index }}</span>
          <h3>{{ principle.title }}</h3>
          <p>{{ principle.description }}</p>
        </article>
      </div>
    </section>

    <section class="product-section reveal-on-scroll" id="features">
      <div class="product-section__copy">
        <p class="section-kicker">Fast by workflow</p>
        <h2>从小说到可生成镜头，不再靠人工搬运上下文。</h2>
        <p>
          分镜、角色、关键帧、视频片段和最终合成都在同一条链路里。
        </p>
        <div class="feature-list">
          <article v-for="feature in features" :key="feature.title">
            <strong>{{ feature.title }}</strong>
            <span>{{ feature.description }}</span>
          </article>
        </div>
      </div>
      <div class="workflow-panel">
        <article v-for="stage in workflowCards" :key="stage.title">
          <div>
            <span>{{ stage.label }}</span>
            <strong>{{ stage.title }}</strong>
          </div>
          <p>{{ stage.description }}</p>
          <small>{{ stage.meta }}</small>
        </article>
      </div>
    </section>

    <section class="showcase-section reveal-on-scroll" id="solutions">
      <div class="section-heading">
        <p>Consistency engine</p>
        <h2>一致性来自清楚的生产链路。</h2>
      </div>
      <div class="showcase-layout">
        <div class="shot-grid">
          <article v-for="(item, index) in showcaseCards" :key="item.id" class="shot-card reveal-on-scroll" :style="{ '--reveal-delay': `${index * 80}ms` }">
            <div class="shot-card__visual" :style="{ '--scene': item.scene }">
              <video v-if="item.previewUrl" :src="item.previewUrl" autoplay loop muted playsinline preload="metadata"></video>
              <div v-else class="shot-placeholder">
                <span></span>
                <strong>{{ item.media?.title || item.title }}</strong>
              </div>
            </div>
            <div class="shot-card__body">
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
              <div>
                <span>{{ item.aspectRatio || "Auto" }}</span>
                <span>{{ item.rating }}</span>
              </div>
            </div>
          </article>
        </div>
        <aside class="runtime-card">
          <span class="section-kicker">Live contract</span>
          <pre>{{ showcaseDetailsJson }}</pre>
          <p>{{ showcaseStatusText }}</p>
        </aside>
      </div>
    </section>

    <section class="model-section reveal-on-scroll">
      <div class="section-heading">
        <p>Model orchestration</p>
        <h2>模型可替换，流程不漂移。</h2>
      </div>
      <div class="model-rail">
        <article v-for="model in showcaseModels" :key="model.key" :class="model.className">
          <span>{{ model.badge }}</span>
          <strong>{{ model.name }}</strong>
          <small>{{ model.vendor }}</small>
        </article>
      </div>
    </section>

    <section class="cta-section reveal-on-scroll">
      <div>
        <p class="section-kicker">Available today</p>
        <h2>免费开始。自由部署。</h2>
      </div>
      <div class="cta-section__actions">
        <RouterLink class="button button-primary" to="/generate">进入工作台</RouterLink>
        <RouterLink class="button button-secondary" to="/docs">阅读文档</RouterLink>
      </div>
    </section>

    <footer class="site-footer reveal-on-scroll" id="footer">
      <strong>JianDou 煎豆</strong>
      <span>Open-source AI video workflow.</span>
      <span>Copyright © 2026 JianDou</span>
    </footer>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import type { TaskShowcaseItem } from "@/types";
import MarketingTopbar from "@/components/marketing/MarketingTopbar.vue";
import { useTaskShowcase } from "@/composables/useTaskShowcase";
import {
  collectShowcaseModelNodes,
  formatShowcaseDuration,
  formatShowcaseRatingLabel,
  resolveShowcaseVisual,
  selectShowcasePrimaryModel,
} from "@/utils/showcase";

const scrollRoot = ref<HTMLElement | null>(null);
let revealObserver: IntersectionObserver | null = null;
let reducedMotionQuery: MediaQueryList | null = null;

const {
  items: showcaseItems,
  loading: showcaseLoading,
  errorMessage: showcaseErrorMessage,
  generatedAt: showcaseGeneratedAt,
  totalCompletedTasks,
} = useTaskShowcase();

const mockShowcaseItems: TaskShowcaseItem[] = [
  {
    id: "mock-campus-romance-night-run",
    title: "校园重逢夜跑告白",
    status: "COMPLETED",
    createdAt: "2026-04-08T20:15:00+08:00",
    updatedAt: "2026-04-08T20:36:00+08:00",
    sourceFileName: "chapter_12_night_run.txt",
    aspectRatio: "9:16",
    minDurationSeconds: 42,
    maxDurationSeconds: 46,
    completedOutputCount: 3,
    taskSeed: 381204,
    effectRating: 8.9,
    description: "雨后操场、逆光路灯和贴近人物表情的近景切换，适合校园情绪向短剧片段。",
    previewUrl: null,
    downloadUrl: null,
    joinName: "night-run-final.mp4",
    models: {
      textAnalysisModel: "qwen-max",
      imageModel: "seedream-3.0",
      videoModel: "seedance-pro",
    },
    media: {
      title: "夜跑告白",
      clipIndex: 1,
      durationSeconds: 44,
      width: 1080,
      height: 1920,
      hasAudio: true,
    },
  },
  {
    id: "mock-city-suspense-elevator",
    title: "都市悬疑电梯停电",
    status: "COMPLETED",
    createdAt: "2026-04-10T22:04:00+08:00",
    updatedAt: "2026-04-10T22:29:00+08:00",
    sourceFileName: "ep03_elevator_blackout.txt",
    aspectRatio: "16:9",
    minDurationSeconds: 28,
    maxDurationSeconds: 32,
    completedOutputCount: 2,
    taskSeed: 902771,
    effectRating: 9.2,
    description: "封闭空间压迫感强，镜头语言偏克制，适合悬疑反转和停顿拉 tension 的场景。",
    previewUrl: null,
    downloadUrl: null,
    joinName: "elevator-cut-v2.mp4",
    models: {
      textAnalysisModel: "gpt-4.1-mini",
      imageModel: "seedream-3.0",
      videoModel: "wanx2.1-video",
    },
    media: {
      title: "电梯停电",
      clipIndex: 2,
      durationSeconds: 30,
      width: 1920,
      height: 1080,
      hasAudio: true,
    },
  },
  {
    id: "mock-republic-teahouse-confrontation",
    title: "民国茶楼正面对峙",
    status: "COMPLETED",
    createdAt: "2026-04-12T14:20:00+08:00",
    updatedAt: "2026-04-12T14:47:00+08:00",
    sourceFileName: "republic_ep07_teahouse.txt",
    aspectRatio: "21:9",
    minDurationSeconds: 36,
    maxDurationSeconds: 40,
    completedOutputCount: 2,
    taskSeed: 614338,
    effectRating: 8.6,
    description: "暖色室内光配合缓推镜头，更适合人物关系对峙和身份揭晓的情节。",
    previewUrl: null,
    downloadUrl: null,
    joinName: "teahouse-confrontation.mp4",
    models: {
      textAnalysisModel: "qwen-max",
      imageModel: "seedream-3.0",
      videoModel: "seedance-pro",
    },
    media: {
      title: "茶楼对峙",
      clipIndex: 4,
      durationSeconds: 38,
      width: 1920,
      height: 816,
      hasAudio: true,
    },
  },
];

const hasMockShowcase = computed(() => !showcaseItems.value.length);
const showcaseFeed = computed(() => hasMockShowcase.value ? mockShowcaseItems : showcaseItems.value);
const showcaseModels = computed(() => collectShowcaseModelNodes(showcaseFeed.value));
const primarySeedLabel = computed(() => {
  const seed = showcaseFeed.value.find((item) => typeof item.taskSeed === "number")?.taskSeed;
  return typeof seed === "number" ? `#${seed}` : "Ready";
});

const consoleNav = ["Intake", "Storyboard", "Characters", "Keyframes", "Video", "Review"];
const timelineShots = ["18%", "28%", "16%", "22%", "34%", "20%"];

const pipelineStages = [
  { code: "01", title: "解析文本", description: "提取剧情、角色与节奏" },
  { code: "02", title: "生成分镜", description: "结构化镜头脚本" },
  { code: "03", title: "锁定角色", description: "外观锚点保持一致" },
  { code: "04", title: "渲染成片", description: "片段生成与合成" },
];

const proofItems = [
  { value: "Open", title: "开源可控", description: "代码与配置可审计。" },
  { value: "Free", title: "免费使用", description: "本地部署即可开始。" },
  { value: "Fast", title: "高效生产", description: "阶段化生成与重试。" },
  { value: "Stable", title: "高一致性", description: "锚点、首尾帧、Seed 贯穿。" },
];

const principles = [
  {
    index: "01",
    title: "开源",
    description: "代码、配置、提示词都可审计。",
  },
  {
    index: "02",
    title: "免费",
    description: "本地部署，直接开始。",
  },
  {
    index: "03",
    title: "高效",
    description: "少复制，多复用。",
  },
  {
    index: "04",
    title: "高一致性",
    description: "角色、场景、首尾帧持续传递。",
  },
];

const features = [
  { title: "文本到分镜", description: "小说直接拆镜头。" },
  { title: "角色资产", description: "外观锚点可复用。" },
  { title: "关键帧边界", description: "首尾帧约束衔接。" },
  { title: "运行观测", description: "阶段、错误、调用可追踪。" },
];

const workflowCards = [
  { label: "Script", title: "脚本结构化", description: "长文本拆成镜头。", meta: "Markdown" },
  { label: "Visual", title: "视觉一致性", description: "角色和首尾帧传递。", meta: "Anchor" },
  { label: "Runtime", title: "可恢复执行", description: "失败后从阶段继续。", meta: "Queue" },
];

const showcaseCards = computed(() => {
  return showcaseFeed.value.slice(0, 3).map((item, index) => {
    const visual = resolveShowcaseVisual(item, index);
    return {
      ...item,
      ...visual,
      rating: formatShowcaseRatingLabel(item.effectRating),
      description: item.description || formatShowcaseDuration(item),
    };
  });
});

const showcaseStatusText = computed(() => {
  if (showcaseLoading.value) {
    return "正在同步真实案例...";
  }
  if (showcaseErrorMessage.value) {
    return hasMockShowcase.value ? "真实案例暂时不可用，当前展示精选示例案例。" : showcaseErrorMessage.value;
  }
  return hasMockShowcase.value
    ? "当前展示精选示例案例，字段结构与真实任务一致。"
    : `已同步 ${showcaseFeed.value.length} 条真实案例，累计完成 ${totalCompletedTasks.value} 个任务。`;
});

const showcaseDetailsJson = computed(() => {
  const first = showcaseFeed.value[0];
  return JSON.stringify({
    source: hasMockShowcase.value ? "demo" : "live",
    cases: showcaseFeed.value.length,
    completed: totalCompletedTasks.value || 86,
    seed: typeof first?.taskSeed === "number" ? first.taskSeed : "unset",
    primaryModel: first ? (selectShowcasePrimaryModel(first) || "unset") : "unset",
    duration: first ? formatShowcaseDuration(first) : "unset",
    generatedAt: showcaseGeneratedAt.value || "demo",
  }, null, 2);
});

function prefersReducedMotion() {
  return reducedMotionQuery?.matches ?? false;
}

function replaceHash(targetId: string) {
  if (typeof window === "undefined") {
    return;
  }
  const nextUrl = targetId === "top" ? window.location.pathname : `${window.location.pathname}#${targetId}`;
  window.history.replaceState(null, "", nextUrl);
}

function scrollToSection(targetId: string) {
  if (typeof document === "undefined") {
    return;
  }
  if (targetId === "top") {
    scrollRoot.value?.scrollTo({ top: 0, behavior: prefersReducedMotion() ? "auto" : "smooth" });
    replaceHash(targetId);
    return;
  }
  const target = document.getElementById(targetId);
  if (!target) {
    return;
  }
  target.scrollIntoView({ behavior: prefersReducedMotion() ? "auto" : "smooth", block: "start" });
  replaceHash(targetId);
}

async function setupRevealAnimations() {
  await nextTick();
  const root = scrollRoot.value;
  if (!root) {
    return;
  }
  const revealNodes = Array.from(root.querySelectorAll<HTMLElement>(".reveal-on-scroll"));
  if (prefersReducedMotion() || typeof IntersectionObserver === "undefined") {
    revealNodes.forEach((node) => node.classList.add("is-visible"));
    return;
  }
  revealObserver?.disconnect();
  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          revealObserver?.unobserve(entry.target);
        }
      });
    },
    { root, threshold: 0.12, rootMargin: "0px 0px -8% 0px" }
  );
  revealNodes.forEach((node) => {
    if (!node.classList.contains("is-visible")) {
      revealObserver?.observe(node);
    }
  });
}

onMounted(() => {
  reducedMotionQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
  void setupRevealAnimations();
  const hash = window.location.hash.replace(/^#/, "");
  if (hash) {
    setTimeout(() => scrollToSection(hash), 0);
  }
});

onBeforeUnmount(() => {
  revealObserver?.disconnect();
});
</script>

<style scoped>
.official-site {
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px max(18px, calc((100vw - 1180px) / 2)) 28px;
  color: #f6f3ea;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 28%),
    radial-gradient(circle at 18% 12%, rgba(218, 176, 82, 0.16), transparent 22%),
    radial-gradient(circle at 82% 18%, rgba(42, 181, 164, 0.14), transparent 24%),
    #08090b;
}

.hero-section {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(460px, 1.08fr);
  gap: 42px;
  align-items: center;
  min-height: min(760px, calc(100vh - 156px));
  padding: 72px 0 52px;
}

.hero-section__copy {
  max-width: 620px;
}

.hero-section__eyebrow,
.section-heading p,
.section-kicker {
  margin: 0;
  font-size: 0.76rem;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #c8b27a;
}

.hero-section h1,
.section-heading h2,
.product-section h2,
.cta-section h2 {
  margin: 14px 0 0;
  font-family: "Plus Jakarta Sans", "Sora", "PingFang SC", sans-serif;
  font-weight: 820;
  letter-spacing: 0;
  color: #fffaf0;
}

.hero-section h1 {
  font-size: clamp(3.6rem, 7vw, 6.6rem);
  line-height: 0.95;
}

.hero-section h1 span {
  display: block;
}

.hero-section__lead {
  max-width: 620px;
  margin: 24px 0 0;
  color: #b9b5aa;
  font-size: 1.08rem;
  line-height: 1.9;
}

.hero-section__actions,
.cta-section__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 30px;
}

.button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 18px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  font-size: 0.92rem;
  font-weight: 800;
  transition:
    transform 160ms ease,
    border-color 160ms ease,
    background 160ms ease;
}

.button:hover {
  transform: translateY(-1px);
}

.button-primary {
  color: #11120f;
  background: #f2d37a;
  border-color: #f2d37a;
}

.button-secondary {
  color: #f7f0df;
  background: rgba(255, 255, 255, 0.06);
}

.hero-console,
.runtime-card,
.workflow-panel article,
.shot-card,
.principle-card,
.model-rail article {
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.035));
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.28);
}

.hero-console {
  overflow: hidden;
  border-radius: 18px;
}

.hero-console__top {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 48px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: #c7c3b7;
}

.hero-console__top span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #5a5e64;
}

.hero-console__top span:nth-child(1) {
  background: #e56a5f;
}

.hero-console__top span:nth-child(2) {
  background: #e7b94f;
}

.hero-console__top span:nth-child(3) {
  background: #55b985;
}

.hero-console__top strong {
  margin-left: auto;
  font-size: 0.8rem;
}

.hero-console__body {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  min-height: 420px;
}

.hero-console__sidebar {
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 18px;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
}

.hero-console__sidebar span {
  padding: 10px 12px;
  border-radius: 8px;
  color: #9c998f;
  font-size: 0.82rem;
  font-weight: 700;
}

.hero-console__sidebar .is-active {
  color: #fff8e7;
  background: rgba(242, 211, 122, 0.12);
}

.hero-console__main {
  display: grid;
  gap: 18px;
  align-content: center;
  padding: 24px;
}

.pipeline {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.pipeline__stage {
  position: relative;
  min-height: 132px;
  padding: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
}

.pipeline__stage::before {
  content: "";
  position: absolute;
  inset: auto 12px 12px;
  height: calc(18px + var(--stage-index) * 8px);
  border-radius: 8px;
  background: linear-gradient(90deg, rgba(242, 211, 122, 0.34), rgba(39, 187, 169, 0.22));
}

.pipeline__stage span,
.workflow-panel span,
.shot-card__body span,
.proof-strip span {
  color: #d6bd78;
  font-size: 0.74rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.pipeline__stage strong,
.pipeline__stage small {
  position: relative;
  z-index: 1;
  display: block;
}

.pipeline__stage strong {
  margin-top: 14px;
  color: #fff8e7;
}

.pipeline__stage small {
  margin-top: 8px;
  color: #9f9b90;
  line-height: 1.5;
}

.consistency-board {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.consistency-board article {
  padding: 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
}

.consistency-board span {
  display: block;
  color: #9f9b90;
  font-size: 0.76rem;
}

.consistency-board strong {
  display: block;
  margin-top: 8px;
  color: #fff8e7;
  font-size: 1.04rem;
}

.timeline-preview {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  align-items: end;
  min-height: 90px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.22);
}

.timeline-preview span {
  height: calc(44px + var(--shot));
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.18), transparent),
    linear-gradient(135deg, rgba(242, 211, 122, 0.55), rgba(39, 187, 169, 0.3));
}

.proof-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.1);
}

.proof-strip article {
  min-height: 170px;
  padding: 22px;
  background: #0c0d0f;
}

.proof-strip strong,
.principle-card h3,
.product-section h2,
.workflow-panel strong,
.shot-card h3,
.model-rail strong,
.cta-section h2 {
  color: #fff8e7;
}

.proof-strip strong {
  display: block;
  margin-top: 22px;
  font-size: 1.18rem;
}

.proof-strip p,
.principle-card p,
.product-section p,
.feature-list span,
.workflow-panel p,
.shot-card p,
.runtime-card p,
.model-rail small,
.site-footer {
  color: #aca89d;
  line-height: 1.7;
}

.section-block,
.product-section,
.showcase-section,
.model-section,
.cta-section,
.site-footer {
  margin-top: 90px;
}

.section-heading {
  max-width: 780px;
  margin: 0 auto;
  text-align: center;
}

.section-heading h2,
.product-section h2,
.cta-section h2 {
  font-size: clamp(2rem, 4vw, 4rem);
  line-height: 1.05;
}

.principle-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 34px;
}

.principle-card {
  min-height: 260px;
  padding: 22px;
  border-radius: 14px;
}

.principle-card span {
  color: #69665f;
  font-weight: 900;
}

.principle-card h3 {
  margin: 78px 0 0;
  font-size: 1.42rem;
}

.product-section {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(420px, 1.1fr);
  gap: 46px;
  align-items: center;
}

.product-section p {
  margin: 18px 0 0;
  font-size: 1rem;
}

.feature-list {
  display: grid;
  gap: 14px;
  margin-top: 28px;
}

.feature-list article {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 18px;
  padding-top: 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.feature-list strong {
  color: #f2d37a;
}

.workflow-panel {
  display: grid;
  gap: 14px;
}

.workflow-panel article {
  display: grid;
  grid-template-columns: minmax(160px, 0.38fr) minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  min-height: 124px;
  padding: 20px;
  border-radius: 14px;
}

.workflow-panel p {
  margin: 0;
}

.workflow-panel small {
  color: #50c7b5;
  font-weight: 800;
}

.showcase-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  margin-top: 34px;
}

.shot-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.shot-card {
  overflow: hidden;
  border-radius: 14px;
}

.shot-card__visual {
  position: relative;
  min-height: 260px;
  background: var(--scene);
  overflow: hidden;
}

.shot-card__visual video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.shot-placeholder {
  position: absolute;
  inset: 0;
  display: grid;
  align-content: end;
  gap: 18px;
  padding: 18px;
  background:
    linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.62)),
    radial-gradient(circle at 32% 20%, rgba(255, 255, 255, 0.18), transparent 22%);
}

.shot-placeholder span {
  width: 64px;
  height: 92px;
  border-radius: 32px 32px 12px 12px;
  background: linear-gradient(180deg, rgba(255, 237, 202, 0.9), rgba(90, 79, 65, 0.9));
  box-shadow: 58px 18px 0 -12px rgba(255, 255, 255, 0.16);
}

.shot-placeholder strong {
  color: #fff8e7;
}

.shot-card__body {
  padding: 18px;
}

.shot-card__body h3 {
  margin: 0;
  font-size: 1.08rem;
}

.shot-card__body p {
  min-height: 78px;
  margin: 10px 0 0;
  font-size: 0.9rem;
}

.shot-card__body div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 16px;
}

.runtime-card {
  align-self: stretch;
  padding: 20px;
  border-radius: 14px;
}

.runtime-card pre {
  margin: 16px 0 0;
  padding: 16px;
  overflow: auto;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.28);
  color: #dfd8c9;
  font-size: 0.78rem;
  line-height: 1.7;
}

.model-rail {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 34px;
}

.model-rail article {
  display: grid;
  gap: 8px;
  min-height: 164px;
  align-content: center;
  justify-items: center;
  border-radius: 14px;
  text-align: center;
}

.model-rail span {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  border-radius: 50%;
  color: #10110e;
  background: #f2d37a;
  font-weight: 900;
}

.model-rail .showcase-node-vision span,
.model-rail .showcase-node-sora span {
  background: #50c7b5;
}

.cta-section {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
  padding: 42px;
  border: 1px solid rgba(242, 211, 122, 0.22);
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(242, 211, 122, 0.12), rgba(80, 199, 181, 0.08)),
    rgba(255, 255, 255, 0.04);
}

.cta-section h2 {
  max-width: 760px;
  font-size: clamp(2rem, 4vw, 3.6rem);
}

.site-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 16px;
  padding: 28px 0 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 0.88rem;
}

.site-footer strong {
  color: #fff8e7;
}

.reveal-on-scroll {
  opacity: 0;
  transform: translateY(22px);
  transition:
    opacity 0.65s ease,
    transform 0.65s ease;
  transition-delay: var(--reveal-delay, 0ms);
}

.reveal-on-scroll.is-visible {
  opacity: 1;
  transform: translateY(0);
}

@media (max-width: 1080px) {
  .hero-section,
  .product-section,
  .showcase-layout {
    grid-template-columns: 1fr;
  }

  .hero-section {
    min-height: auto;
  }

  .hero-section h1 {
    max-width: 12ch;
  }

  .principle-grid,
  .proof-strip,
  .model-rail {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .shot-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .official-site {
    padding-inline: 10px;
  }

  .hero-section {
    gap: 28px;
    padding-top: 42px;
  }

  .hero-section h1 {
    font-size: clamp(3rem, 16vw, 4.4rem);
  }

  .hero-console__body,
  .pipeline,
  .consistency-board,
  .principle-grid,
  .proof-strip,
  .model-rail,
  .workflow-panel article,
  .feature-list article {
    grid-template-columns: 1fr;
  }

  .hero-console__sidebar {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    border-right: 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  .hero-console__sidebar span {
    text-align: center;
  }

  .cta-section {
    display: grid;
    padding: 24px;
  }

  .button {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .reveal-on-scroll {
    opacity: 1;
    transform: none;
    transition: none;
  }
}
</style>
