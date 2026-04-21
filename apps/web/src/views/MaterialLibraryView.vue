<template>
  <section class="material-view">
    <section class="surface-panel material-panel">
      <div class="material-panel__head">
        <div>
          <p class="material-eyebrow">Material Library</p>
          <h2>个人素材库</h2>
        </div>
        <button class="btn-secondary btn-sm" type="button" :disabled="loading" @click="loadAssets">
          {{ loading ? "刷新中..." : "刷新" }}
        </button>
      </div>

      <div class="material-filters">
        <label class="material-field material-field-wide">
          <span>搜索</span>
          <input v-model="filters.q" class="field-input" type="search" placeholder="按标题、模型、工作流搜索" />
        </label>

        <label class="material-field">
          <span>类型</span>
          <AppSelect v-model="filters.type" :options="typeFilterOptions" />
        </label>

        <label class="material-field">
          <span>最低评分</span>
          <AppSelect v-model="filters.minRating" :options="ratingFilterOptions" />
        </label>

        <label class="material-field">
          <span>标签</span>
          <input v-model="filters.tag" class="field-input" placeholder="如 cinematic / selected" />
        </label>

        <label class="material-field">
          <span>模型</span>
          <input v-model="filters.model" class="field-input" placeholder="输入模型名" />
        </label>

        <label class="material-field">
          <span>画幅</span>
          <AppSelect v-model="filters.aspectRatio" :options="aspectRatioFilterOptions" />
        </label>

        <label class="material-field">
          <span>镜头号</span>
          <input v-model="filters.clipIndex" class="field-input" type="number" min="0" step="1" placeholder="全部" />
        </label>
      </div>

      <div class="material-filters__actions">
        <button class="btn-primary btn-sm" type="button" :disabled="loading" @click="loadAssets">应用筛选</button>
        <button class="btn-ghost btn-sm" type="button" :disabled="loading" @click="resetFilters">清空筛选</button>
      </div>

      <p v-if="errorMessage" class="material-error">{{ errorMessage }}</p>
    </section>

    <section v-if="loading" class="surface-panel material-empty">
      正在加载素材库...
    </section>

    <section v-else-if="!assets.length" class="surface-panel material-empty material-empty-large">
      <p class="material-eyebrow">No Asset</p>
      <h2>当前没有匹配的素材</h2>
    </section>

    <section v-else class="material-grid">
      <article v-for="asset in assets" :key="asset.id" class="surface-panel material-card">
        <div class="material-card__head">
          <div class="material-card__title">
            <p class="material-eyebrow">{{ asset.stageType }}</p>
            <h3>{{ asset.title }}</h3>
          </div>
          <span class="material-card__selected" v-if="asset.selectedForNext">已选中</span>
        </div>

        <div class="material-card__preview">
          <video
            v-if="asset.mediaType === 'video'"
            :src="asset.previewUrl"
            controls
            playsinline
            preload="metadata"
          ></video>
          <img v-else-if="asset.mediaType === 'image'" :src="asset.previewUrl" :alt="asset.title" />
          <div v-else class="material-card__text">
            {{ storyboardText(asset) }}
          </div>
        </div>

        <div class="material-card__content">
          <div class="material-meta">
            <span class="surface-chip">工作流 {{ asset.workflowId }}</span>
            <span class="surface-chip">镜头 {{ asset.clipIndex }}</span>
            <span class="surface-chip">版本 {{ asset.versionNo }}</span>
            <span class="surface-chip">模型 {{ asset.originModel || "-" }}</span>
            <span class="surface-chip">评分 {{ ratingLabel(asset.userRating) }}</span>
          </div>

          <div class="material-tags" :class="{ 'material-tags-empty': !asset.tags.length }">
            <span v-for="tag in asset.tags" :key="tag.id" class="tag-chip">
              {{ tag.tagKey }}: {{ tag.tagValue }}
            </span>
          </div>

          <div class="material-rating">
            <div class="rating-row">
              <button
                v-for="score in ratingOptions"
                :key="`${asset.id}-${score}`"
                type="button"
                class="rating-pill"
                :class="{ 'rating-pill-active': Number(ratingDrafts[asset.id] || asset.userRating || 0) === score }"
                @click="ratingDrafts[asset.id] = String(score)"
              >
                {{ score }}
              </button>
            </div>
            <textarea
              v-model="ratingNotes[asset.id]"
              class="field-textarea"
              rows="3"
              placeholder="素材评分备注"
            ></textarea>
            <button class="btn-secondary btn-sm" type="button" :disabled="busyActionKey === `rate-${asset.id}`" @click="handleRateAsset(asset.id)">
              {{ busyActionKey === `rate-${asset.id}` ? "保存中..." : "保存评分" }}
            </button>
          </div>

          <div class="material-tags-editor">
            <label class="material-field">
              <span>自定义标签</span>
              <input v-model="tagDrafts[asset.id]" class="field-input" placeholder="用逗号分隔，例如：悬疑,高反差,雨夜" />
            </label>
            <button class="btn-secondary btn-sm" type="button" :disabled="busyActionKey === `tags-${asset.id}`" @click="handleSaveTags(asset.id)">
              {{ busyActionKey === `tags-${asset.id}` ? "保存中..." : "保存标签" }}
            </button>
          </div>

          <div class="material-actions">
            <button class="btn-primary btn-sm" type="button" :disabled="busyActionKey === `reuse-${asset.id}`" @click="handleReuseAsset(asset.id)">
              {{ busyActionKey === `reuse-${asset.id}` ? "复制中..." : "复制为新工作流" }}
            </button>
            <RouterLink v-if="asset.workflowId" class="btn-secondary btn-sm" :to="`/workflows/${asset.workflowId}`">
              打开工作流
            </RouterLink>
            <a
              class="btn-ghost btn-sm"
              :href="asset.fileUrl"
              download
              target="_blank"
              rel="noopener noreferrer"
            >
              下载
            </a>
          </div>
        </div>
      </article>
    </section>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { fetchMaterialAssets, rateMaterialAsset, reuseMaterialAsset, updateMaterialAssetTags } from "@/api/material-assets";
import AppSelect from "@/components/common/AppSelect.vue";
import type { AppSelectOption } from "@/components/common/app-select";
import type { MaterialAssetLibraryItem, MaterialAssetQuery } from "@/types";

const router = useRouter();
const loading = ref(false);
const errorMessage = ref("");
const busyActionKey = ref("");

const assets = ref<MaterialAssetLibraryItem[]>([]);
const ratingOptions = [5, 4, 3, 2, 1];
const typeFilterOptions: AppSelectOption[] = [
  { label: "全部", value: "" },
  { label: "分镜", value: "storyboard" },
  { label: "关键帧", value: "keyframe" },
  { label: "视频", value: "video" },
  { label: "最终 join", value: "joined" },
];
const ratingFilterOptions: AppSelectOption[] = [
  { label: "全部", value: "" },
  ...ratingOptions.map((score) => ({ label: `${score} 星及以上`, value: String(score) })),
];
const aspectRatioFilterOptions: AppSelectOption[] = [
  { label: "全部", value: "" },
  { label: "9:16", value: "9:16" },
  { label: "16:9", value: "16:9" },
];

const filters = reactive({
  q: "",
  type: "",
  minRating: "",
  tag: "",
  model: "",
  aspectRatio: "",
  clipIndex: "",
});

const ratingDrafts = reactive<Record<string, string>>({});
const ratingNotes = reactive<Record<string, string>>({});
const tagDrafts = reactive<Record<string, string>>({});

function buildQuery(): MaterialAssetQuery {
  return {
    q: filters.q.trim() || undefined,
    type: filters.type as MaterialAssetQuery["type"],
    minRating: filters.minRating ? Number(filters.minRating) : null,
    tag: filters.tag.trim() || undefined,
    model: filters.model.trim() || undefined,
    aspectRatio: filters.aspectRatio || undefined,
    clipIndex: filters.clipIndex ? Number(filters.clipIndex) : null,
  };
}

function ratingLabel(value?: number | null) {
  return typeof value === "number" && value > 0 ? `${value}/5` : "未评分";
}

function storyboardText(asset: MaterialAssetLibraryItem) {
  const scriptMarkdown = typeof asset.metadata?.scriptMarkdown === "string" ? asset.metadata.scriptMarkdown : "";
  return scriptMarkdown || asset.title;
}

function syncDrafts() {
  for (const asset of assets.value) {
    ratingDrafts[asset.id] = String(asset.userRating ?? 5);
    ratingNotes[asset.id] = asset.ratingNote ?? "";
    tagDrafts[asset.id] = asset.tags
      .filter((tag) => tag.tagType === "custom")
      .map((tag) => tag.tagValue)
      .join(", ");
  }
}

async function loadAssets() {
  loading.value = true;
  errorMessage.value = "";
  try {
    assets.value = await fetchMaterialAssets(buildQuery());
    syncDrafts();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "素材列表加载失败";
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  filters.q = "";
  filters.type = "";
  filters.minRating = "";
  filters.tag = "";
  filters.model = "";
  filters.aspectRatio = "";
  filters.clipIndex = "";
  void loadAssets();
}

async function refreshAfterMutation(mutator: () => Promise<unknown>, actionKey: string) {
  busyActionKey.value = actionKey;
  errorMessage.value = "";
  try {
    await mutator();
    await loadAssets();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "素材操作失败";
  } finally {
    busyActionKey.value = "";
  }
}

async function handleRateAsset(assetId: string) {
  await refreshAfterMutation(
    () =>
      rateMaterialAsset(assetId, {
        effectRating: Number(ratingDrafts[assetId] || 5),
        effectRatingNote: ratingNotes[assetId]?.trim() || null,
      }),
    `rate-${assetId}`
  );
}

async function handleSaveTags(assetId: string) {
  const tags = tagDrafts[assetId]
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
  await refreshAfterMutation(() => updateMaterialAssetTags(assetId, { tags }), `tags-${assetId}`);
}

async function handleReuseAsset(assetId: string) {
  busyActionKey.value = `reuse-${assetId}`;
  errorMessage.value = "";
  try {
    const workflow = await reuseMaterialAsset(assetId, { mode: "clone" });
    await loadAssets();
    await router.push(`/workflows/${workflow.id}`);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "素材操作失败";
  } finally {
    busyActionKey.value = "";
  }
}

onMounted(async () => {
  await loadAssets();
});
</script>

<style scoped>
.material-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px;
  overscroll-behavior: contain;
}

.material-panel {
  padding: 22px;
}

.material-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.material-panel__head h2 {
  margin: 6px 0 0;
}

.material-eyebrow {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.56);
}

.material-filters {
  display: grid;
  grid-template-columns:
    minmax(240px, 1.65fr)
    minmax(140px, 0.8fr)
    minmax(140px, 0.8fr)
    minmax(180px, 0.9fr)
    minmax(180px, 0.9fr)
    minmax(120px, 0.65fr)
    minmax(120px, 0.65fr);
  gap: 14px;
  align-items: end;
}

.material-filters__actions,
.material-meta,
.material-tags,
.material-actions,
.rating-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.material-filters__actions {
  margin-top: 14px;
}

.material-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.material-field span {
  font-size: 0.86rem;
  color: rgba(255, 255, 255, 0.72);
}

.material-field-wide {
  grid-column: auto;
}

.material-error {
  margin: 12px 0 0;
  color: #ffb4b4;
}

.material-empty {
  padding: 28px 18px;
  text-align: center;
  color: rgba(255, 255, 255, 0.64);
}

.material-empty-large {
  padding: 72px 24px;
}

.material-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
  align-content: start;
  align-items: stretch;
  padding-bottom: 8px;
}

.material-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  padding: 22px;
}

.material-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.material-card__title {
  flex: 1 1 auto;
  min-width: 0;
}

.material-card__head h3 {
  margin: 6px 0 0;
  min-height: calc(1.35em * 2);
  line-height: 1.35;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.material-card__selected {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  min-width: 76px;
  min-height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(145, 180, 255, 0.28);
  background:
    linear-gradient(180deg, rgba(18, 22, 36, 0.92), rgba(8, 10, 18, 0.92));
  color: rgba(255, 255, 255, 0.86);
  font-size: 0.84rem;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: inset 0 0 0 1px rgba(72, 112, 214, 0.08);
}

.material-card__preview {
  min-height: 200px;
}

.material-card__content {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 16px;
}

.material-card__preview video,
.material-card__preview img,
.material-card__text {
  min-height: 200px;
}

.material-card__preview video,
.material-card__preview img {
  width: 100%;
  height: 100%;
  border-radius: 18px;
  background: rgba(0, 0, 0, 0.35);
  object-fit: cover;
}

.material-card__text {
  padding: 16px;
  border-radius: 18px;
  background: rgba(4, 6, 10, 0.72);
  color: rgba(255, 255, 255, 0.8);
  white-space: pre-wrap;
  line-height: 1.6;
  overflow: auto;
}

.material-rating,
.material-tags-editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.material-meta {
  min-height: 64px;
  align-content: flex-start;
}

.material-tags {
  min-height: 108px;
  align-content: flex-start;
  overflow: hidden;
}

.material-tags-empty {
  margin: 0;
}

.material-rating .field-textarea {
  min-height: 88px;
  resize: vertical;
}

.material-actions {
  margin-top: auto;
  padding-top: 4px;
}

.tag-chip,
.rating-pill {
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.04);
  font-size: 0.82rem;
}

.rating-pill-active {
  border-color: rgba(255, 180, 92, 0.72);
  background: rgba(255, 180, 92, 0.16);
  color: #ffe1b1;
}

@media (max-width: 1680px) {
  .material-filters {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .material-field-wide {
    grid-column: span 2;
  }
}

@media (max-width: 1280px) {
  .material-filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .material-field-wide {
    grid-column: span 2;
  }
}

@media (max-width: 720px) {
  .material-filters {
    grid-template-columns: 1fr;
  }

  .material-field-wide {
    grid-column: span 1;
  }
}
</style>
