<template>
  <section class="material-center-view">
    <section class="surface-panel center-panel">
      <div class="center-panel__head">
        <div>
          <p class="center-eyebrow">Material Center</p>
          <h2>素材中心</h2>
        </div>
        <RouterLink class="btn-secondary btn-sm" to="/materials">素材库</RouterLink>
      </div>

      <div class="asset-type-tabs" role="tablist" aria-label="素材类型">
        <button
          v-for="option in assetTypeOptions"
          :key="option.value"
          class="asset-type-tab"
          :class="{ 'asset-type-tab-active': form.assetType === option.value }"
          type="button"
          @click="form.assetType = option.value"
        >
          <strong>{{ option.label }}</strong>
          <span>{{ option.description }}</span>
        </button>
      </div>

      <form class="center-form" @submit.prevent="handleSubmit">
        <label class="center-field center-field-wide">
          <span>标题</span>
          <input v-model="form.title" class="field-input" placeholder="例如：女主角三视图 / 雨夜街角场景 / 黄铜钥匙道具" />
        </label>

        <label class="center-field">
          <span>画幅</span>
          <AppSelect v-model="form.aspectRatio" :options="aspectRatioOptions" />
        </label>

        <label class="center-field">
          <span>图像模型</span>
          <AppSelect v-model="form.imageModel" :options="imageModelOptions" placeholder="自动选择" />
        </label>

        <label class="center-field">
          <span>图像尺寸</span>
          <AppSelect v-model="form.imageSize" :options="imageSizeSelectOptions" placeholder="选择尺寸" />
        </label>

        <label class="center-field">
          <span>Seed</span>
          <input v-model="form.seed" class="field-input" inputmode="numeric" placeholder="可为空" />
          <small>{{ materialSeedHint }}</small>
        </label>

        <label class="center-field center-field-full">
          <span>{{ descriptionLabel }}</span>
          <textarea
            v-model="form.description"
            class="field-textarea"
            rows="5"
            :placeholder="descriptionPlaceholder"
          ></textarea>
        </label>

        <label class="center-field center-field-full">
          <span>风格关键词</span>
          <input v-model="form.styleKeywords" class="field-input" placeholder="用逗号分隔，例如 cinematic, soft light, clean design" />
        </label>

        <label class="center-field center-field-full">
          <span>参考图 URL</span>
          <textarea
            v-model="referenceImageUrlsText"
            class="field-textarea"
            rows="4"
            placeholder="每行一个公网 URL，可与上传参考图一起作为参考"
          ></textarea>
        </label>

        <section class="reference-section center-field-full">
          <div class="reference-section__head">
            <div>
              <p class="center-eyebrow">References</p>
              <h3>参考图</h3>
            </div>
            <button class="btn-secondary btn-sm" type="button" :disabled="loadingReferenceAssets" @click="loadReferenceAssets">
              {{ loadingReferenceAssets ? "刷新中..." : "刷新素材库" }}
            </button>
          </div>

          <div class="reference-upload">
            <label class="upload-tile" :class="{ 'upload-tile-disabled': uploadingImage }">
              <input type="file" accept="image/*" multiple :disabled="uploadingImage" @change="handleImageUpload" />
              <span>{{ uploadingImage ? "读取中..." : "选择参考图" }}</span>
            </label>

            <div class="reference-list" :class="{ 'reference-list-empty': !uploadedReferenceItems.length }">
              <span v-for="item in uploadedReferenceItems" :key="item.fileUrl" class="reference-chip">
                <span>{{ compactUrl(referenceItemDisplayUrl(item)) }}</span>
                <button type="button" aria-label="移除参考图" @click="removeUploadedReference(item.fileUrl)">×</button>
              </span>
            </div>
          </div>

          <div v-if="referenceError" class="center-error">{{ referenceError }}</div>

          <div class="reference-library">
            <div class="reference-library__head">
              <strong>从素材库选择</strong>
              <span>{{ selectedReferenceAssetIds.length }} / {{ referenceLibraryAssets.length }}</span>
            </div>
            <div
              class="reference-library-grid"
              :class="{ 'reference-library-grid-empty': !referenceLibraryAssets.length && !loadingReferenceAssets }"
            >
              <button
                v-for="asset in referenceLibraryAssets"
                :key="asset.id"
                class="reference-asset"
                :class="{ 'reference-asset-selected': selectedReferenceAssetIds.includes(asset.id) }"
                type="button"
                @click="toggleReferenceAsset(asset.id)"
              >
                <img :src="materialAssetPreviewUrl(asset)" :alt="asset.title" />
                <span>{{ asset.title }}</span>
              </button>
            </div>
            <div v-if="referenceLibraryError" class="center-error">{{ referenceLibraryError }}</div>
          </div>
        </section>

        <div class="center-actions center-field-full">
          <button class="btn-primary" type="submit" :disabled="submitting">
            {{ submitting ? "生成中..." : "生成素材" }}
          </button>
          <button class="btn-ghost" type="button" :disabled="submitting" @click="resetForm">重置</button>
        </div>
      </form>

      <p v-if="errorMessage" class="center-error">{{ errorMessage }}</p>
    </section>

    <section v-if="resultAssets.length || resultOutputUrl" class="surface-panel result-panel">
      <div class="result-panel__head">
        <div>
          <p class="center-eyebrow">Result</p>
          <h2>生成结果</h2>
        </div>
        <RouterLink class="btn-primary btn-sm" :to="libraryResultLink">跳到素材库</RouterLink>
      </div>

      <div class="result-grid">
        <article v-for="asset in resultAssets" :key="asset.id" class="result-card">
          <button
            v-if="asset.mediaType === 'image'"
            class="result-card__preview"
            type="button"
            :aria-label="`查看${asset.title}原图`"
            @click="openImagePreview(resultAssetUrl(asset), asset.title)"
          >
            <img :src="resultAssetUrl(asset)" :alt="asset.title" />
          </button>
          <div v-else class="result-card__placeholder">{{ asset.mediaType }}</div>
          <div class="result-card__body">
            <h3>{{ asset.title }}</h3>
            <p>{{ asset.originModel || form.imageModel || "-" }}</p>
            <div class="result-card__status">
              <button
                v-if="asset.remoteUrl"
                class="result-remote-chip"
                type="button"
                :title="asset.remoteUrl"
                @click="copyRemoteUrl(asset.remoteUrl)"
              >
                远程 {{ compactUrl(asset.remoteUrl) }}
              </button>
              <span v-else class="result-remote-empty">暂未上传</span>
            </div>
            <button class="btn-secondary btn-sm" type="button" :disabled="busyActionKey === `upload-${asset.id}` || Boolean(asset.remoteUrl)" @click="handleUploadResultAsset(asset.id)">
              {{ busyActionKey === `upload-${asset.id}` ? "上传中..." : (asset.remoteUrl ? "已上传" : "上传") }}
            </button>
          </div>
        </article>

        <article v-if="!resultAssets.length && resultOutputUrl" class="result-card">
          <button
            class="result-card__preview"
            type="button"
            :aria-label="`查看${resultTitle}原图`"
            @click="openImagePreview(resultOutputUrl, resultTitle)"
          >
            <img :src="resultOutputUrl" alt="生成素材" />
          </button>
          <div class="result-card__body">
            <h3>{{ resultTitle }}</h3>
            <p>{{ form.imageModel || "-" }}</p>
            <span class="result-remote-empty">暂未上传</span>
          </div>
        </article>
      </div>
    </section>
  </section>

  <div v-if="imagePreviewState.open" class="image-preview-overlay" role="dialog" aria-modal="true" @click.self="closeImagePreview">
    <div class="image-preview-caption">
      <strong>{{ imagePreviewCaption }}</strong>
    </div>
    <button type="button" class="image-preview-close" aria-label="关闭原图预览" @click="closeImagePreview">
      关闭
    </button>
    <img class="image-preview-full" :src="imagePreviewState.url" :alt="imagePreviewState.alt" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { createMaterialGeneration, fetchMaterialAssets, uploadMaterialAsset } from "@/api/material-assets";
import { fetchGenerationOptions } from "@/api/generation";
import AppSelect from "@/components/common/AppSelect.vue";
import type { AppSelectOption } from "@/components/common/app-select";
import type {
  CreateMaterialGenerationRequest,
  GenerationImageSizeOption,
  GenerationTextAnalysisModelInfo,
  MaterialAssetLibraryItem,
  MaterialAssetType,
  MaterialGenerationResponse,
} from "@/types";

const route = useRoute();

type MaterialGenerationAssetType = Exclude<MaterialAssetType, "workflow">;

interface AssetTypeOption {
  label: string;
  value: MaterialGenerationAssetType;
  description: string;
}

interface UploadedReferenceItem {
  fileUrl: string;
  fileName: string;
}

const assetTypeOptions: AssetTypeOption[] = [
  {
    label: "角色三视图",
    value: "character_sheet",
    description: "用于角色正侧背或关键外观锚点",
  },
  {
    label: "场景",
    value: "scene",
    description: "用于固定环境、空间关系和光线基准",
  },
  {
    label: "道具",
    value: "prop",
    description: "用于关键物件、手持物和视觉线索",
  },
  {
    label: "自由模式",
    value: "free",
    description: "只按页面提示词生成，不附加类型规则",
  },
];

const form = reactive({
  assetType: "character_sheet" as MaterialGenerationAssetType,
  title: "",
  description: "",
  styleKeywords: "",
  aspectRatio: "16:9",
  imageSize: "",
  textAnalysisModel: "",
  imageModel: "",
  seed: "",
});

const loadingOptions = ref(false);
const submitting = ref(false);
const uploadingImage = ref(false);
const loadingReferenceAssets = ref(false);
const busyActionKey = ref("");
const errorMessage = ref("");
const referenceError = ref("");
const referenceLibraryError = ref("");
const referenceImageUrlsText = ref("");
const uploadedReferenceItems = ref<UploadedReferenceItem[]>([]);
const referenceLibraryAssets = ref<MaterialAssetLibraryItem[]>([]);
const selectedReferenceAssetIds = ref<string[]>([]);
const result = ref<MaterialGenerationResponse | null>(null);
const aspectRatios = ref<AppSelectOption[]>([]);
const imageModels = ref<GenerationTextAnalysisModelInfo[]>([]);
const imageSizes = ref<GenerationImageSizeOption[]>([]);
const imagePreviewState = reactive({
  open: false,
  url: "",
  alt: "",
});

const aspectRatioOptions = computed<AppSelectOption[]>(() =>
  aspectRatios.value.length
    ? aspectRatios.value
    : [
        { label: "9:16", value: "9:16" },
        { label: "16:9", value: "16:9" },
        { label: "1:1", value: "1:1" },
      ],
);
const imageModelOptions = computed<AppSelectOption[]>(() =>
  imageModels.value.length
    ? imageModels.value.map((item) => ({
        label: item.label || item.value,
        value: item.value,
        description: item.description || item.provider || undefined,
      }))
    : [{ label: "默认模型", value: "" }],
);
const selectedImageModel = computed(() => normalizeModelName(form.imageModel));
const selectedImageModelOption = computed<GenerationTextAnalysisModelInfo | null>(() => {
  if (!selectedImageModel.value) {
    return null;
  }
  return imageModels.value.find((item) => normalizeModelName(item.value) === selectedImageModel.value) ?? null;
});
const selectedImageModelSupportsSeed = computed(() => Boolean(selectedImageModelOption.value?.supportsSeed));
const materialSeedHint = computed(() =>
  selectedImageModelSupportsSeed.value ? "当前图像模型会使用该种子。" : "当前图像模型未声明支持种子，保存后仅做素材记录。"
);
const imageSizeOptions = computed<GenerationImageSizeOption[]>(() => {
  const source = imageSizes.value;
  if (!source.length) {
    return [];
  }
  const selectedModelSizes = selectedImageModelOption.value?.supportedSizes ?? [];
  const normalizedModelSizes = selectedModelSizes.map(normalizeImageSize).filter(Boolean);
  const filtered = source.filter((item) => {
    const normalizedValue = normalizeImageSize(item.value);
    if (normalizedModelSizes.length && !normalizedModelSizes.includes(normalizedValue)) {
      return false;
    }
    const supportedModels = Array.isArray(item.supportedModels) ? item.supportedModels : [];
    if (!selectedImageModel.value || !supportedModels.length) {
      return true;
    }
    return supportedModels.some((model) => normalizeModelName(model) === selectedImageModel.value);
  });
  const modelFiltered = filtered.length ? filtered : source;
  const ratioFiltered = modelFiltered.filter((item) => imageSizeMatchesAspectRatio(item, form.aspectRatio));
  return ratioFiltered.length ? ratioFiltered : modelFiltered;
});
const imageSizeSelectOptions = computed<AppSelectOption[]>(() =>
  imageSizeOptions.value.map((item) => ({
    label: item.label || (item.width && item.height ? `${item.width} × ${item.height}` : item.value),
    value: item.value,
  })),
);
const descriptionLabel = computed(() => (form.assetType === "free" ? "提示词" : "描述"));
const descriptionPlaceholder = computed(() =>
  form.assetType === "free"
    ? "直接输入图片生成提示词，自由模式不会附加三视图、场景或道具规则"
    : "描述角色外观、场景布局或道具细节",
);
const typedReferenceUrls = computed(() => parseReferenceUrls(referenceImageUrlsText.value));
const uploadedReferenceUrls = computed(() =>
  uploadedReferenceItems.value
    .map((item) => item.fileUrl)
    .filter(Boolean),
);
const selectedReferenceUrls = computed(() => uniqueUrls([...typedReferenceUrls.value, ...uploadedReferenceUrls.value]));
const resultAssets = computed(() => {
  if (!result.value) {
    return [];
  }
  const assets = Array.isArray(result.value.assets) ? result.value.assets : [];
  if (result.value.asset) {
    return [result.value.asset, ...assets.filter((asset) => asset.id !== result.value?.asset?.id)];
  }
  return assets;
});
const resultOutputUrl = computed(() => result.value?.previewUrl || result.value?.fileUrl || result.value?.outputUrl || "");
const resultTitle = computed(() => result.value?.title || form.title || "生成素材");
const libraryResultLink = computed(() => `/materials?assetType=${encodeURIComponent(form.assetType)}`);
const imagePreviewCaption = computed(() => imagePreviewState.alt || "图片预览");

function parseReferenceUrls(value: string) {
  return value
    .split(/\r?\n|,/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function uniqueUrls(urls: string[]) {
  return Array.from(new Set(urls.filter(Boolean)));
}

function compactUrl(url: string) {
  if (url.length <= 42) {
    return url;
  }
  return `${url.slice(0, 24)}...${url.slice(-14)}`;
}

function replaceResultAsset(updated?: MaterialAssetLibraryItem | null) {
  if (!result.value || !updated) {
    return;
  }
  if (result.value.asset?.id === updated.id) {
    result.value.asset = updated;
  }
  result.value.assets = (result.value.assets ?? []).map((asset) => (asset.id === updated.id ? updated : asset));
}

function resultAssetUrl(asset: MaterialAssetLibraryItem) {
  return asset.previewUrl || asset.fileUrl || "";
}

function materialAssetPreviewUrl(asset: MaterialAssetLibraryItem) {
  return asset.previewUrl || asset.fileUrl || asset.remoteUrl || "";
}

function isImageMaterialAsset(asset: MaterialAssetLibraryItem) {
  return asset.mediaType === "image" && Boolean(materialAssetPreviewUrl(asset));
}

function referenceItemDisplayUrl(item: UploadedReferenceItem) {
  return item.fileName || item.fileUrl;
}

function normalizeModelName(value: unknown) {
  return String(value ?? "").trim().toLowerCase();
}

function normalizeImageSize(value: unknown) {
  return String(value ?? "").trim().toLowerCase().replace(/\*/g, "x");
}

function queryString(value: unknown) {
  return typeof value === "string" ? value.trim() : "";
}

function applyEntryQuery() {
  const ratio = queryString(route.query.ratio);
  if (ratio === "16:9" || ratio === "9:16" || ratio === "1:1") {
    form.aspectRatio = ratio;
  }
  const mode = queryString(route.query.mode);
  if (mode === "text-to-image" || mode === "image-to-image") {
    form.assetType = "free";
  }
}

function resolveDefaultImageModel(models: GenerationTextAnalysisModelInfo[], current?: string | null) {
  const currentValue = String(current ?? "").trim();
  if (currentValue && models.some((item) => item.value === currentValue)) {
    return currentValue;
  }
  const gptModel = models.find((item) => {
    const searchable = [item.family, item.provider, item.value, item.label].map(normalizeModelName);
    return searchable.some((value) => value.includes("gpt"));
  });
  return gptModel?.value || models[0]?.value || currentValue;
}

function syncImageSizeSelection(preferred?: string | null) {
  const available = imageSizeOptions.value;
  if (!available.length) {
    form.imageSize = "";
    return;
  }
  const preferredValue = normalizeImageSize(preferred);
  const currentValue = normalizeImageSize(form.imageSize);
  const next =
    available.find((item) => preferredValue && normalizeImageSize(item.value) === preferredValue)?.value
    ?? available.find((item) => currentValue && normalizeImageSize(item.value) === currentValue)?.value
    ?? available[0].value;
  form.imageSize = String(next);
}

function imageSizeMatchesAspectRatio(item: GenerationImageSizeOption, aspectRatio: string) {
  const width = Number(item.width);
  const height = Number(item.height);
  if (!Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) {
    return true;
  }
  const expected = aspectRatio.trim();
  if (expected === "16:9") {
    return width > height;
  }
  if (expected === "9:16") {
    return height > width;
  }
  if (expected === "1:1") {
    return width === height;
  }
  return true;
}

function removeUploadedReference(fileUrl: string) {
  uploadedReferenceItems.value = uploadedReferenceItems.value.filter((item) => item.fileUrl !== fileUrl);
}

function readImageAsDataUri(file: File): Promise<UploadedReferenceItem> {
  if (!file.type.startsWith("image/")) {
    return Promise.reject(new Error(`${file.name || "参考图"} 不是图片文件`));
  }
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const result = typeof reader.result === "string" ? reader.result : "";
      if (!result.startsWith("data:image/") || !result.includes(";base64,")) {
        reject(new Error(`${file.name || "参考图"} 无法转换为 base64 图片`));
        return;
      }
      resolve({
        fileUrl: result,
        fileName: file.name || "本地参考图",
      });
    };
    reader.onerror = () => reject(reader.error ?? new Error("参考图读取失败"));
    reader.readAsDataURL(file);
  });
}

function toggleReferenceAsset(assetId: string) {
  if (selectedReferenceAssetIds.value.includes(assetId)) {
    selectedReferenceAssetIds.value = selectedReferenceAssetIds.value.filter((item) => item !== assetId);
    return;
  }
  selectedReferenceAssetIds.value = [...selectedReferenceAssetIds.value, assetId];
}

function openImagePreview(url: string, alt: string) {
  if (!url) {
    return;
  }
  imagePreviewState.open = true;
  imagePreviewState.url = url;
  imagePreviewState.alt = alt || "图片预览";
}

function closeImagePreview() {
  imagePreviewState.open = false;
  imagePreviewState.url = "";
  imagePreviewState.alt = "";
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

async function handleUploadResultAsset(assetId: string) {
  busyActionKey.value = `upload-${assetId}`;
  errorMessage.value = "";
  try {
    const updated = await uploadMaterialAsset(assetId);
    replaceResultAsset(updated);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "素材上传失败";
  } finally {
    busyActionKey.value = "";
  }
}

function buildPayload(): CreateMaterialGenerationRequest {
  const seed = form.seed.trim() ? Number(form.seed.trim()) : null;
  return {
    assetType: form.assetType,
    title: form.title.trim(),
    description: form.description.trim() || null,
    styleKeywords: parseReferenceUrls(form.styleKeywords),
    aspectRatio: form.aspectRatio,
    imageSize: form.imageSize || null,
    textAnalysisModel: form.textAnalysisModel || null,
    imageModel: form.imageModel || null,
    seed: selectedImageModelSupportsSeed.value && Number.isFinite(seed) ? seed : null,
    referenceImageUrls: selectedReferenceUrls.value,
    referenceAssetIds: selectedReferenceAssetIds.value,
  };
}

async function loadOptions() {
  loadingOptions.value = true;
  try {
    const options = await fetchGenerationOptions();
    applyEntryQuery();
    aspectRatios.value = (options.aspectRatios ?? []).map((item) => ({
      label: item.label || item.value,
      value: item.value,
    }));
    imageModels.value = options.imageModels ?? [];
    imageSizes.value = options.imageSizes ?? [];
    const ratio = queryString(route.query.ratio);
    form.aspectRatio =
      ratio === "16:9" || ratio === "9:16" || ratio === "1:1"
        ? ratio
        : options.defaultAspectRatio || String(aspectRatioOptions.value[0]?.value || form.aspectRatio);
    form.textAnalysisModel = options.defaultTextAnalysisModel || options.textAnalysisModels?.[0]?.value || form.textAnalysisModel;
    form.imageModel = resolveDefaultImageModel(options.imageModels ?? [], form.imageModel);
    syncImageSizeSelection(options.defaultImageSize);
  } finally {
    loadingOptions.value = false;
  }
}

async function loadReferenceAssets() {
  loadingReferenceAssets.value = true;
  referenceLibraryError.value = "";
  try {
    const assets = await fetchMaterialAssets();
    referenceLibraryAssets.value = assets.filter(isImageMaterialAsset);
    const availableIds = new Set(referenceLibraryAssets.value.map((asset) => asset.id));
    selectedReferenceAssetIds.value = selectedReferenceAssetIds.value.filter((assetId) => availableIds.has(assetId));
  } catch (error) {
    referenceLibraryError.value = error instanceof Error ? error.message : "素材库参考图加载失败";
  } finally {
    loadingReferenceAssets.value = false;
  }
}

async function handleImageUpload(event: Event) {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []);
  if (!files.length) {
    return;
  }
  uploadingImage.value = true;
  referenceError.value = "";
  try {
    const items = await Promise.all(files.map(readImageAsDataUri));
    const merged = [...uploadedReferenceItems.value, ...items];
    uploadedReferenceItems.value = merged.filter((item, index, array) => array.findIndex((candidate) => candidate.fileUrl === item.fileUrl) === index);
    input.value = "";
  } catch (error) {
    referenceError.value = error instanceof Error ? error.message : "参考图读取失败";
  } finally {
    uploadingImage.value = false;
  }
}

async function handleSubmit() {
  if (!form.title.trim()) {
    errorMessage.value = "请填写素材标题";
    return;
  }
  submitting.value = true;
  errorMessage.value = "";
  result.value = null;
  busyActionKey.value = "";
  try {
    result.value = await createMaterialGeneration(buildPayload());
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "素材生成失败";
  } finally {
    submitting.value = false;
  }
}

function resetForm() {
  form.title = "";
  form.description = "";
  form.styleKeywords = "";
  form.seed = "";
  referenceImageUrlsText.value = "";
  uploadedReferenceItems.value = [];
  selectedReferenceAssetIds.value = [];
  result.value = null;
  errorMessage.value = "";
  referenceError.value = "";
}

onMounted(async () => {
  await loadOptions();
  await loadReferenceAssets();
});

watch(
  () => [form.imageModel, form.aspectRatio, imageSizes.value] as const,
  () => {
    syncImageSizeSelection(form.imageSize);
  }
);
</script>

<style scoped>
.material-center-view {
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

.center-panel,
.result-panel {
  padding: 22px;
}

.center-panel__head,
.reference-section__head,
.result-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.center-panel__head h2,
.reference-section__head h3,
.result-panel__head h2 {
  margin: 6px 0 0;
}

.center-eyebrow {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.56);
}

.asset-type-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.asset-type-tab {
  display: flex;
  min-height: 92px;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.78);
  text-align: left;
}

.asset-type-tab strong {
  color: rgba(255, 255, 255, 0.92);
}

.asset-type-tab span {
  font-size: 0.84rem;
  line-height: 1.45;
  color: rgba(255, 255, 255, 0.58);
}

.asset-type-tab-active {
  border-color: rgba(145, 180, 255, 0.48);
  background: rgba(90, 128, 210, 0.18);
  box-shadow: inset 0 0 0 1px rgba(145, 180, 255, 0.12);
}

.center-form {
  display: grid;
  grid-template-columns: minmax(260px, 1.4fr) minmax(140px, 0.7fr) minmax(180px, 0.9fr) minmax(120px, 0.55fr);
  gap: 14px;
  align-items: end;
}

.center-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.center-field span {
  font-size: 0.86rem;
  color: rgba(255, 255, 255, 0.72);
}

.center-field-wide {
  grid-column: span 1;
}

.center-field-full {
  grid-column: 1 / -1;
}

.field-textarea {
  resize: vertical;
}

.reference-section {
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.reference-upload {
  display: grid;
  grid-template-columns: minmax(180px, 0.35fr) minmax(0, 1fr);
  gap: 14px;
}

.upload-tile {
  display: flex;
  min-height: 92px;
  align-items: center;
  justify-content: center;
  border: 1px dashed rgba(255, 255, 255, 0.24);
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.18);
  color: rgba(255, 255, 255, 0.78);
  cursor: pointer;
}

.upload-tile input {
  display: none;
}

.upload-tile-disabled {
  cursor: wait;
  opacity: 0.7;
}

.reference-list {
  display: flex;
  min-height: 92px;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: 8px;
}

.reference-list-empty::before {
  content: "暂无参考图";
  color: rgba(255, 255, 255, 0.46);
  font-size: 0.9rem;
}

.reference-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  min-height: 34px;
  padding: 0 8px 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.76);
  font-size: 0.82rem;
}

.reference-chip button {
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.78);
}

.reference-library {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 16px;
}

.reference-library__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: rgba(255, 255, 255, 0.76);
  font-size: 0.86rem;
}

.reference-library__head span {
  color: rgba(255, 255, 255, 0.48);
}

.reference-library-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(128px, 1fr));
  gap: 10px;
  min-height: 112px;
}

.reference-library-grid-empty {
  display: flex;
  align-items: center;
}

.reference-library-grid-empty::before {
  content: "素材库暂无可用图片";
  color: rgba(255, 255, 255, 0.46);
  font-size: 0.9rem;
}

.reference-asset {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.16);
  color: rgba(255, 255, 255, 0.74);
  text-align: left;
}

.reference-asset img {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.28);
  object-fit: cover;
}

.reference-asset span {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 0.8rem;
}

.reference-asset-selected {
  border-color: rgba(145, 180, 255, 0.58);
  background: rgba(90, 128, 210, 0.18);
  box-shadow: inset 0 0 0 1px rgba(145, 180, 255, 0.18);
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.result-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.82);
  text-align: left;
}

.result-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result-card__preview img,
.result-card__placeholder {
  width: 100%;
  aspect-ratio: 16 / 10;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.28);
  object-fit: cover;
}

.result-card__preview {
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: zoom-in;
}

.result-card__preview:focus-visible {
  outline: 2px solid rgba(145, 180, 255, 0.9);
  outline-offset: 3px;
  border-radius: 8px;
}

.center-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 2px;
}

.center-error {
  margin: 12px 0 0;
  color: #ffb4b4;
}

.result-card h3 {
  margin: 0 0 6px;
  font-size: 1rem;
}

.result-card p {
  margin: 0;
  color: rgba(255, 255, 255, 0.58);
  font-size: 0.84rem;
}

.result-card__status {
  display: flex;
  min-height: 30px;
  align-items: center;
}

.result-remote-chip,
.result-remote-empty {
  display: inline-flex;
  max-width: 100%;
  min-height: 30px;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 0.8rem;
}

.result-remote-chip {
  border: 1px solid rgba(145, 180, 255, 0.26);
  background: rgba(145, 180, 255, 0.1);
  color: rgba(210, 224, 255, 0.88);
  cursor: copy;
}

.result-remote-chip:hover {
  border-color: rgba(145, 180, 255, 0.46);
  background: rgba(145, 180, 255, 0.16);
}

.result-remote-empty {
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.5);
}

.result-card__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.58);
}

.image-preview-overlay {
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

.image-preview-caption {
  position: absolute;
  top: 24px;
  left: 50%;
  display: flex;
  min-width: min(420px, calc(100vw - 180px));
  justify-content: center;
  padding: 10px 18px;
  border: 1px solid rgba(255, 180, 92, 0.44);
  border-radius: 14px;
  background: rgba(18, 22, 30, 0.78);
  color: #ffd38a;
  text-align: center;
  transform: translateX(-50%);
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.28);
}

.image-preview-caption strong {
  color: #ff6b6b;
  font-size: 0.98rem;
  font-weight: 800;
}

.image-preview-full {
  max-width: min(92vw, 1440px);
  max-height: 82vh;
  border-radius: 20px;
  background: rgba(12, 14, 20, 0.9);
  box-shadow: 0 28px 60px rgba(0, 0, 0, 0.45);
}

.image-preview-close {
  position: absolute;
  top: 24px;
  right: 24px;
  padding: 10px 16px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  background: rgba(18, 22, 30, 0.8);
  color: #f7f7f8;
  cursor: pointer;
}

@media (max-width: 1180px) {
  .center-form,
  .asset-type-tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .center-form,
  .asset-type-tabs,
  .reference-upload {
    grid-template-columns: 1fr;
  }
}
</style>
