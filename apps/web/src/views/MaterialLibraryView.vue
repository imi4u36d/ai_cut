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
          <AppSelect v-model="filters.assetType" :options="typeFilterOptions" />
        </label>

        <label class="material-field">
          <span>最低评分</span>
          <AppSelect v-model="filters.minRating" :options="ratingFilterOptions" />
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
            <p class="material-eyebrow">{{ assetTypeLabel(asset.assetType) }} · {{ asset.stageType }}</p>
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
          <button
            v-else-if="asset.mediaType === 'image'"
            class="material-preview-trigger material-preview-trigger-image"
            type="button"
            @click="openImagePreview(asset)"
          >
            <img :src="assetPreviewUrl(asset)" :alt="asset.title" />
            <span>查看图片</span>
          </button>
          <button
            v-else
            class="material-preview-trigger material-preview-trigger-text"
            type="button"
            @click="openStoryboardPreview(asset)"
          >
            <div class="material-card__text" v-html="storyboardPreviewHtml(asset)"></div>
            <span>查看完整分镜</span>
          </button>
        </div>

        <div class="material-card__content">
          <div class="material-meta">
            <span class="surface-chip">工作流 {{ asset.workflowId }}</span>
            <span class="surface-chip">镜头 {{ asset.clipIndex }}</span>
            <span class="surface-chip">版本 {{ asset.versionNo }}</span>
            <span class="surface-chip">模型 {{ asset.originModel || "-" }}</span>
            <span class="surface-chip">评分 {{ ratingLabel(asset.userRating) }}</span>
            <button
              v-if="asset.remoteUrl"
              class="material-remote-chip"
              type="button"
              :title="asset.remoteUrl"
              @click="copyRemoteUrl(asset.remoteUrl)"
            >
              远程 {{ compactUrl(asset.remoteUrl) }}
            </button>
            <span v-else class="surface-chip material-remote-empty">暂未上传</span>
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

          <div class="material-actions">
            <button class="btn-secondary btn-sm" type="button" :disabled="busyActionKey === `upload-${asset.id}` || Boolean(asset.remoteUrl)" @click="handleUploadAsset(asset.id)">
              {{ busyActionKey === `upload-${asset.id}` ? "上传中..." : (asset.remoteUrl ? "已上传" : "上传") }}
            </button>
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
            <button class="btn-danger btn-sm" type="button" :disabled="busyActionKey === `delete-${asset.id}`" @click="handleDeleteAsset(asset)">
              {{ busyActionKey === `delete-${asset.id}` ? "删除中..." : "删除" }}
            </button>
          </div>
        </div>
      </article>
    </section>

    <div v-if="previewDialog.open" class="material-preview-overlay" role="dialog" aria-modal="true" @click.self="closePreviewDialog">
      <div class="material-preview-dialog" :class="{ 'material-preview-dialog-image': previewDialog.kind === 'image' }">
        <div class="material-preview-dialog__head">
          <div>
            <p class="material-eyebrow">{{ previewDialog.kind === "image" ? "Image Preview" : "Storyboard" }}</p>
            <h3>{{ previewDialog.title }}</h3>
          </div>
          <button type="button" class="btn-ghost btn-sm" @click="closePreviewDialog">关闭</button>
        </div>
        <img
          v-if="previewDialog.kind === 'image'"
          class="material-preview-dialog__image"
          :src="previewDialog.url"
          :alt="previewDialog.title"
        />
        <div v-else class="material-preview-dialog__markdown" v-html="previewDialog.html"></div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { deleteMaterialAsset, fetchMaterialAssets, rateMaterialAsset, reuseMaterialAsset, uploadMaterialAsset } from "@/api/material-assets";
import AppSelect from "@/components/common/AppSelect.vue";
import type { AppSelectOption } from "@/components/common/app-select";
import type { MaterialAssetLibraryItem, MaterialAssetQuery, MaterialAssetType } from "@/types";
import { renderMarkdownToHtml } from "@/utils/markdown";

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const errorMessage = ref("");
const busyActionKey = ref("");

const assets = ref<MaterialAssetLibraryItem[]>([]);
const ratingOptions = [5, 4, 3, 2, 1];
const typeFilterOptions: AppSelectOption[] = [
  { label: "全部", value: "" },
  { label: "角色三视图", value: "character_sheet" },
  { label: "场景", value: "scene" },
  { label: "道具", value: "prop" },
  { label: "自由模式", value: "free" },
  { label: "工作流产物", value: "workflow" },
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
  assetType: "",
  minRating: "",
  model: "",
  aspectRatio: "",
  clipIndex: "",
});

const ratingDrafts = reactive<Record<string, string>>({});
const ratingNotes = reactive<Record<string, string>>({});
const previewDialog = reactive({
  open: false,
  kind: "storyboard" as "storyboard" | "image",
  title: "",
  html: "",
  url: "",
});

function buildQuery(): MaterialAssetQuery {
  return {
    q: filters.q.trim() || undefined,
    assetType: filters.assetType as MaterialAssetQuery["assetType"],
    minRating: filters.minRating ? Number(filters.minRating) : null,
    model: filters.model.trim() || undefined,
    aspectRatio: filters.aspectRatio || undefined,
    clipIndex: filters.clipIndex ? Number(filters.clipIndex) : null,
  };
}

function assetTypeLabel(value?: MaterialAssetType | string | null) {
  if (value === "character_sheet") {
    return "角色三视图";
  }
  if (value === "scene") {
    return "场景";
  }
  if (value === "prop") {
    return "道具";
  }
  if (value === "free") {
    return "自由模式";
  }
  if (value === "workflow") {
    return "工作流产物";
  }
  return "工作流产物";
}

function ratingLabel(value?: number | null) {
  return typeof value === "number" && value > 0 ? `${value}/5` : "未评分";
}

function compactUrl(url: string) {
  if (url.length <= 42) {
    return url;
  }
  return `${url.slice(0, 24)}...${url.slice(-14)}`;
}

function storyboardText(asset: MaterialAssetLibraryItem) {
  const scriptMarkdown = typeof asset.metadata?.scriptMarkdown === "string" ? asset.metadata.scriptMarkdown : "";
  return scriptMarkdown || asset.title;
}

function storyboardPreviewHtml(asset: MaterialAssetLibraryItem) {
  return renderMarkdownToHtml(storyboardText(asset));
}

function assetPreviewUrl(asset: MaterialAssetLibraryItem) {
  return asset.previewUrl || asset.fileUrl || asset.remoteUrl || "";
}

function closePreviewDialog() {
  previewDialog.open = false;
  previewDialog.html = "";
  previewDialog.url = "";
}

function openStoryboardPreview(asset: MaterialAssetLibraryItem) {
  previewDialog.kind = "storyboard";
  previewDialog.title = asset.title;
  previewDialog.html = storyboardPreviewHtml(asset);
  previewDialog.url = "";
  previewDialog.open = true;
}

function openImagePreview(asset: MaterialAssetLibraryItem) {
  previewDialog.kind = "image";
  previewDialog.title = asset.title;
  previewDialog.html = "";
  previewDialog.url = assetPreviewUrl(asset);
  previewDialog.open = true;
}

function syncDrafts() {
  for (const asset of assets.value) {
    ratingDrafts[asset.id] = String(asset.userRating ?? 5);
    ratingNotes[asset.id] = asset.ratingNote ?? "";
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
  filters.assetType = "";
  filters.minRating = "";
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

async function copyRemoteUrl(remoteUrl?: string | null) {
  const value = remoteUrl?.trim();
  if (!value) {
    return;
  }
  try {
    await navigator.clipboard.writeText(value);
  } catch {
    errorMessage.value = "远程路径复制失败，请手动复制";
  }
}

async function handleUploadAsset(assetId: string) {
  await refreshAfterMutation(() => uploadMaterialAsset(assetId), `upload-${assetId}`);
}

async function handleDeleteAsset(asset: MaterialAssetLibraryItem) {
  if (!window.confirm(`确认删除素材「${asset.title}」吗？`)) {
    return;
  }
  await refreshAfterMutation(() => deleteMaterialAsset(asset.id), `delete-${asset.id}`);
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
  const queryAssetType = typeof route.query.assetType === "string" ? route.query.assetType : "";
  if (typeFilterOptions.some((option) => option.value === queryAssetType)) {
    filters.assetType = queryAssetType;
  }
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
  height: 276px;
  min-height: 276px;
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
  height: 100%;
  min-height: 0;
}

.material-card__preview video,
.material-card__preview img {
  width: 100%;
  height: 100%;
  border-radius: 18px;
  background: rgba(0, 0, 0, 0.35);
  object-fit: cover;
}

.material-preview-trigger {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
  padding: 0;
  border: 0;
  border-radius: 18px;
  background: transparent;
  color: inherit;
  overflow: hidden;
  cursor: zoom-in;
  text-align: left;
}

.material-preview-trigger:focus-visible {
  outline: 2px solid rgba(255, 180, 92, 0.9);
  outline-offset: 4px;
}

.material-preview-trigger > span {
  position: absolute;
  right: 12px;
  bottom: 12px;
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 999px;
  background: rgba(10, 13, 22, 0.78);
  color: rgba(255, 255, 255, 0.84);
  font-size: 0.78rem;
  font-weight: 700;
  backdrop-filter: blur(8px);
}

.material-preview-trigger-image img {
  transition: transform 0.18s ease;
}

.material-preview-trigger-image:hover img {
  transform: scale(1.025);
}

.material-card__text {
  position: relative;
  padding: 16px;
  border-radius: 18px;
  background: rgba(4, 6, 10, 0.72);
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  overflow: hidden;
}

.material-card__text::after {
  content: "";
  position: absolute;
  inset: auto 0 0;
  height: 72px;
  border-radius: 0 0 18px 18px;
  background: linear-gradient(180deg, rgba(4, 6, 10, 0), rgba(4, 6, 10, 0.96));
  pointer-events: none;
}

.material-card__text :deep(h1),
.material-card__text :deep(h2),
.material-card__text :deep(h3),
.material-card__text :deep(p) {
  margin: 0 0 10px;
}

.material-card__text :deep(table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.82rem;
}

.material-card__text :deep(th),
.material-card__text :deep(td) {
  padding: 6px 8px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  vertical-align: top;
}

.material-rating {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.material-meta {
  min-height: 64px;
  align-content: flex-start;
}

.material-remote-chip {
  display: inline-flex;
  max-width: 100%;
  min-height: 32px;
  align-items: center;
  padding: 0 12px;
  border: 1px solid rgba(145, 180, 255, 0.26);
  border-radius: 999px;
  background: rgba(145, 180, 255, 0.1);
  color: rgba(210, 224, 255, 0.88);
  font-size: 0.82rem;
  line-height: 1;
  cursor: copy;
}

.material-remote-chip:hover {
  border-color: rgba(145, 180, 255, 0.46);
  background: rgba(145, 180, 255, 0.16);
}

.material-remote-empty {
  color: rgba(255, 255, 255, 0.5);
}

.material-rating .field-textarea {
  min-height: 88px;
  resize: vertical;
}

.material-actions {
  margin-top: auto;
  padding-top: 4px;
}

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

.material-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: rgba(8, 10, 18, 0.82);
  backdrop-filter: blur(10px);
}

.material-preview-dialog {
  display: flex;
  flex-direction: column;
  width: min(980px, calc(100vw - 48px));
  max-height: min(86vh, 960px);
  border: 1px solid rgba(145, 180, 255, 0.2);
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(28, 31, 44, 0.98), rgba(16, 19, 29, 0.98));
  box-shadow: 0 28px 72px rgba(0, 0, 0, 0.48);
  overflow: hidden;
}

.material-preview-dialog-image {
  width: min(1280px, calc(100vw - 48px));
}

.material-preview-dialog__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.material-preview-dialog__head h3 {
  margin: 6px 0 0;
  line-height: 1.35;
}

.material-preview-dialog__image {
  display: block;
  max-width: 100%;
  max-height: calc(86vh - 96px);
  object-fit: contain;
  background: rgba(0, 0, 0, 0.36);
}

.material-preview-dialog__markdown {
  padding: 24px;
  overflow: auto;
  color: rgba(255, 255, 255, 0.84);
  line-height: 1.75;
}

.material-preview-dialog__markdown :deep(h1),
.material-preview-dialog__markdown :deep(h2),
.material-preview-dialog__markdown :deep(h3),
.material-preview-dialog__markdown :deep(h4) {
  margin: 0 0 14px;
  color: rgba(255, 255, 255, 0.94);
  line-height: 1.35;
}

.material-preview-dialog__markdown :deep(p) {
  margin: 0 0 14px;
}

.material-preview-dialog__markdown :deep(table) {
  width: 100%;
  margin: 12px 0 22px;
  border-collapse: collapse;
  font-size: 0.92rem;
}

.material-preview-dialog__markdown :deep(th),
.material-preview-dialog__markdown :deep(td) {
  padding: 9px 10px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  vertical-align: top;
}

.material-preview-dialog__markdown :deep(th) {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.94);
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

  .material-card__preview {
    height: 220px;
    min-height: 220px;
  }

  .material-preview-overlay {
    padding: 16px;
  }

  .material-preview-dialog {
    width: calc(100vw - 32px);
    max-height: calc(100vh - 32px);
    border-radius: 18px;
  }

  .material-preview-dialog__head {
    padding: 16px;
  }

  .material-preview-dialog__markdown {
    padding: 18px;
  }
}
</style>
