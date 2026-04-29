<template>
  <main class="home-page">
    <section class="home-hero">
      <h1>
        开启你的
        <button type="button" class="hero-mode-button" @click="toggleMenu('mode')">
          {{ selectedMode.label }}
          <span aria-hidden="true">{{ activeMenu === "mode" ? "⌃" : "⌄" }}</span>
        </button>
        即刻造梦！
      </h1>

      <form class="home-composer" @submit.prevent="submitComposer">
        <button
          type="button"
          class="home-composer__upload"
          :disabled="uploadingReference"
          @click="handleReferenceEntryClick"
        >
          <template v-if="selectedMode.kind === 'image' && referenceImages.length">
            <img :src="referenceImages[0].fileUrl" :alt="referenceImages[0].label" />
            <span class="home-reference-add">+</span>
          </template>
          <span v-else>+</span>
          <small>参考图</small>
        </button>
        <input
          ref="textFileInput"
          type="file"
          :accept="selectedMode.kind === 'image' ? 'image/*' : '.txt,text/plain'"
          class="home-hidden-input"
          :multiple="selectedMode.kind === 'image'"
          @change="handleReferenceFileChange"
        />

        <div v-if="selectedMode.kind === 'image' && referenceImages.length" class="home-reference-stack" aria-label="已添加参考图">
          <span
            v-for="item in referenceImages.slice(0, 3)"
            :key="item.id"
            class="home-reference-thumb"
            :class="{ 'home-reference-thumb-mentioned': promptText.includes(`@${item.label}`) }"
          >
            <img :src="item.fileUrl" :alt="item.label" />
            <button type="button" :aria-label="`移除${item.label}`" @click="removeReferenceImage(item.id)">×</button>
          </span>
          <span v-if="referenceImages.length > 3" class="home-reference-more">+{{ referenceImages.length - 3 }}</span>
        </div>

        <label class="home-composer__prompt">
          <span>{{ promptLabel }}</span>
          <textarea
            v-model="promptText"
            rows="5"
            :placeholder="promptPlaceholder"
          ></textarea>
        </label>

        <div class="home-composer__toolbar">
          <div class="home-menu">
            <button type="button" class="home-tool home-tool-accent" :class="{ 'home-tool-active': activeMenu === 'mode' }" @click="toggleMenu('mode')">
              <span class="home-tool__icon">{{ selectedMode.icon }}</span>
              {{ selectedMode.label }}
              <span aria-hidden="true">⌄</span>
            </button>
            <div v-if="activeMenu === 'mode'" class="home-popover home-popover-mode">
              <p class="home-popover__label">创作类型</p>
              <button
                v-for="option in modeOptions"
                :key="option.value"
                type="button"
                class="home-popover__item"
                :class="{ 'home-popover__item-active': selectedModeValue === option.value }"
                @click="selectMode(option.value)"
              >
                <span class="home-popover__icon">{{ option.icon }}</span>
                <span>
                  <strong>{{ option.label }}</strong>
                  <small>{{ option.description }}</small>
                </span>
                <span v-if="selectedModeValue === option.value" class="home-popover__check" aria-hidden="true">✓</span>
              </button>
            </div>
          </div>

          <div class="home-menu">
            <button type="button" class="home-tool" :class="{ 'home-tool-active': activeMenu === 'model' }" @click="toggleMenu('model')">
              <span class="home-tool__icon">□</span>
              {{ selectedPrimaryModelLabel }}
              <span aria-hidden="true">⌄</span>
            </button>
            <div v-if="activeMenu === 'model'" class="home-popover home-popover-model">
              <p class="home-popover__label">{{ selectedMode.kind === "video" ? "模型链路" : "图片模型" }}</p>
              <template v-if="selectedMode.kind === 'video'">
                <label class="home-field">
                  <span>文本模型</span>
                  <select v-model="form.textAnalysisModel">
                    <option v-for="model in textModelOptions" :key="model.value" :value="model.value">{{ model.label }}</option>
                  </select>
                </label>
                <label class="home-field">
                  <span>关键帧模型</span>
                  <select v-model="form.imageModel">
                    <option v-for="model in imageModelOptions" :key="model.value" :value="model.value">{{ model.label }}</option>
                  </select>
                </label>
                <label class="home-field">
                  <span>视频模型</span>
                  <select v-model="form.videoModel">
                    <option v-for="model in videoModelOptions" :key="model.value" :value="model.value">{{ model.label }}</option>
                  </select>
                </label>
              </template>
              <template v-else>
                <label class="home-field">
                  <span>文本模型</span>
                  <select v-model="form.textAnalysisModel">
                    <option v-for="model in textModelOptions" :key="model.value" :value="model.value">{{ model.label }}</option>
                  </select>
                </label>
                <label class="home-field">
                  <span>图片模型</span>
                  <select v-model="form.imageModel">
                    <option v-for="model in imageModelOptions" :key="model.value" :value="model.value">{{ model.label }}</option>
                  </select>
                </label>
              </template>
            </div>
          </div>

          <div class="home-menu">
            <button type="button" class="home-tool" :class="{ 'home-tool-active': activeMenu === 'ratio' }" @click="toggleMenu('ratio')">
              <span class="home-tool__shape"></span>
              {{ ratioToolLabel }}
            </button>
            <div v-if="activeMenu === 'ratio'" class="home-popover home-popover-ratio">
              <p class="home-popover__label">选择比例</p>
              <div class="home-ratio-list home-ratio-list-immersive">
                <button
                  v-for="ratio in ratioOptions"
                  :key="ratio.value"
                  type="button"
                  :class="{ 'home-ratio-active': form.aspectRatio === ratio.value }"
                  @click="selectRatio(ratio.value)"
                >
                  <span class="home-ratio__shape" :style="{ aspectRatio: ratio.shape }"></span>
                  <span>{{ ratio.shortLabel }}</span>
                </button>
              </div>
              <template v-if="selectedMode.kind === 'image'">
                <p class="home-popover__label">选择分辨率</p>
                <div class="home-resolution-list">
                  <button
                    v-for="size in imageSizeOptions"
                    :key="size.value"
                    type="button"
                    :class="{ 'home-resolution-active': form.imageSize === size.value }"
                    @click="form.imageSize = size.value"
                  >
                    {{ formatImageSizeOptionLabel(size) }}
                  </button>
                </div>
                <template v-if="selectedImageSizeDimensions">
                  <p class="home-popover__label">尺寸</p>
                  <div class="home-dimension-row">
                    <span>W</span>
                    <strong>{{ selectedImageSizeDimensions.width }}</strong>
                    <span class="home-dimension-link">⌁</span>
                    <span>H</span>
                    <strong>{{ selectedImageSizeDimensions.height }}</strong>
                    <span>PX</span>
                  </div>
                </template>
              </template>
              <template v-else>
                <p class="home-popover__label">视频尺寸</p>
                <div class="home-resolution-list">
                  <button
                    v-for="size in videoSizeOptions"
                    :key="size.value"
                    type="button"
                    :class="{ 'home-resolution-active': form.videoSize === size.value }"
                    @click="form.videoSize = size.value"
                  >
                    {{ formatVideoSizeLabel(size.label || size.value) }}
                  </button>
                </div>
              </template>
            </div>
          </div>

          <div v-if="selectedMode.kind === 'video'" class="home-menu">
            <button type="button" class="home-tool" :class="{ 'home-tool-active': activeMenu === 'duration' }" @click="toggleMenu('duration')">
              <span class="home-tool__icon">◷</span>
              {{ durationLabel }}
            </button>
            <div v-if="activeMenu === 'duration'" class="home-popover home-popover-compact">
              <p class="home-popover__label">视频时长</p>
              <div class="home-segment-grid">
                <button type="button" :class="{ 'home-segment-active': durationMode === 'auto' }" @click="durationMode = 'auto'">自动</button>
                <button
                  v-for="duration in durationOptions"
                  :key="duration.value"
                  type="button"
                  :class="{ 'home-segment-active': durationMode === 'manual' && selectedDurationSeconds === duration.value }"
                  @click="selectDuration(duration.value)"
                >
                  {{ duration.value }}s
                </button>
              </div>
            </div>
          </div>

          <div v-if="selectedModeValue !== 'character_sheet'" class="home-menu">
            <button type="button" class="home-tool" :class="{ 'home-tool-active': activeMenu === 'count' }" @click="toggleMenu('count')">
              ✦ {{ selectedMode.kind === "image" ? `${imageOutputCount} / 张` : outputCountLabel }}
            </button>
            <div v-if="activeMenu === 'count'" class="home-popover home-popover-compact">
              <p class="home-popover__label">{{ selectedMode.kind === "image" ? "图片张数" : "分镜数量" }}</p>
              <div class="home-segment-grid">
                <template v-if="selectedMode.kind === 'image'">
                  <button
                    v-for="count in imageOutputCountOptions"
                    :key="count"
                    type="button"
                    :class="{ 'home-segment-active': imageOutputCount === count }"
                    @click="imageOutputCount = count"
                  >
                    {{ count }} 张
                  </button>
                </template>
                <template v-else>
                  <button type="button" :class="{ 'home-segment-active': form.outputCount === 'auto' }" @click="form.outputCount = 'auto'">自动</button>
                  <button
                    v-for="count in videoOutputCountOptions"
                    :key="count"
                    type="button"
                    :class="{ 'home-segment-active': form.outputCount === count }"
                    @click="form.outputCount = count"
                  >
                    {{ count }}
                  </button>
                </template>
              </div>
            </div>
          </div>

          <div class="home-menu">
            <button type="button" class="home-tool" :class="{ 'home-tool-active': activeMenu === 'mention' }" @click="toggleMenu('mention')">
              <span class="home-tool__icon">@</span>
              引用
            </button>
            <div v-if="activeMenu === 'mention'" class="home-popover home-popover-mention">
              <p class="home-popover__label">可能@的内容</p>
              <button type="button" class="home-popover__item" @click="insertMention('创建主体')">
                <span class="home-popover__icon">+</span>
                <span>
                  <strong>创建主体</strong>
                  <small>{{ selectedMode.kind === "image" ? "基于参考图或描述生成主体" : "视频主体功能正在开发中" }}</small>
                </span>
              </button>
              <template v-if="selectedMode.kind === 'image'">
                <button
                  v-for="item in referenceImages"
                  :key="item.id"
                  type="button"
                  class="home-popover__item"
                  @click="insertMention(item.label)"
                >
                  <span class="home-popover__image">
                    <img :src="item.fileUrl" :alt="item.label" />
                  </span>
                  <span>
                    <strong>{{ item.label }}</strong>
                    <small>{{ item.fileName }}</small>
                  </span>
                </button>
              </template>
              <p v-if="selectedMode.kind === 'video'" class="home-popover__empty">视频模式参考图正在开发中</p>
              <p v-else-if="!referenceImages.length" class="home-popover__empty">暂无参考图</p>
            </div>
          </div>

          <div class="home-menu">
            <button type="button" class="home-tool" :class="{ 'home-tool-active': activeMenu === 'seed' }" @click="toggleMenu('seed')">
              <span class="home-tool__icon">#</span>
              {{ seedMode === "auto" ? "自动种子" : "手动种子" }}
            </button>
            <div v-if="activeMenu === 'seed'" class="home-popover home-popover-seed">
              <p class="home-popover__label">种子</p>
              <div class="home-segment-grid">
                <button type="button" :class="{ 'home-segment-active': seedMode === 'auto' }" @click="seedMode = 'auto'">自动</button>
                <button type="button" :class="{ 'home-segment-active': seedMode === 'manual' }" @click="seedMode = 'manual'">手动</button>
              </div>
              <label v-if="seedMode === 'manual'" class="home-field">
                <span>种子值</span>
                <input v-model="seedInput" inputmode="numeric" placeholder="输入非负整数" />
              </label>
              <div v-else class="home-seed-row">
                <span>{{ autoSeed }}</span>
                <button type="button" @click="refreshAutoSeed">换一个</button>
              </div>
              <small>{{ seedCapabilityHint }}</small>
            </div>
          </div>
        </div>

        <div class="home-composer__meta">
          <span>{{ statusText }}</span>
          <RouterLink v-if="createdTaskId" :to="{ name: 'tasks', query: { selected: createdTaskId } }">查看任务</RouterLink>
          <RouterLink v-if="createdImageAssetId" :to="createdImageAssetLink">查看素材</RouterLink>
        </div>

        <button class="home-composer__submit" type="submit" :disabled="submitting || loadingOptions || !isFormReady" :title="submitLabel">
          <svg v-if="!submitting" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 19V5" />
            <path d="m5 12 7-7 7 7" />
          </svg>
          <span v-else>...</span>
        </button>
      </form>

      <div v-if="referenceDevelopingDialogOpen" class="home-dialog" role="dialog" aria-modal="true" aria-labelledby="reference-developing-title" @click.self="referenceDevelopingDialogOpen = false">
        <div class="home-dialog__panel">
          <h2 id="reference-developing-title">正在开发中</h2>
          <p>视频模式添加参考图正在开发中。</p>
          <button type="button" @click="referenceDevelopingDialogOpen = false">知道了</button>
        </div>
      </div>
    </section>

    <section class="feature-strip">
      <button v-for="item in quickModeCards" :key="item.value" class="feature-card" type="button" @click="selectMode(item.value)">
        <span class="feature-card__icon">{{ item.icon }}</span>
        <span>
          <strong>{{ item.title }}</strong>
          <small>{{ item.subtitle }}</small>
        </span>
      </button>
      <RouterLink class="feature-card" to="/workflows">
        <span class="feature-card__icon">AI</span>
        <span>
          <strong>阶段工作流</strong>
          <small>逐步校准</small>
        </span>
      </RouterLink>
      <RouterLink class="feature-card" to="/tasks">
        <span class="feature-card__icon">JD</span>
        <span>
          <strong>任务管理</strong>
          <small>追踪结果</small>
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
import { computed, onMounted, ref, watch } from "vue";
import { createMaterialGeneration } from "@/api/material-assets";
import { fetchGenerationOptions } from "@/api/generation";
import { createGenerationTask, uploadText } from "@/api/tasks";
import { requireAuth } from "@/auth/modal";
import { useTaskShowcase } from "@/composables/useTaskShowcase";
import { formatShowcaseTimeMeta, resolveShowcaseVisual, selectShowcasePrimaryModel } from "@/utils/showcase";
import { formatVideoSizeLabel } from "@/utils/presentation";
import { shouldStopBeforeVideoGeneration } from "@/workbench/developer-settings";
import type {
  CreateGenerationTaskRequest,
  GenerationImageSizeOption,
  GenerationOptionsResponse,
  GenerationTextAnalysisModelInfo,
  GenerationVideoDurationOption,
  GenerationVideoModelInfo,
  GenerationVideoSizeOption,
} from "@/types";

type ModeValue = "video" | "image" | "character_sheet";
type MenuKey = "" | "mode" | "model" | "ratio" | "duration" | "count" | "mention" | "seed";
type AspectRatioValue = "16:9" | "9:16";
type RatioOptionValue = "智能" | "1:1" | "21:9" | "16:9" | "3:2" | "4:3" | "3:4" | "2:3" | "9:16";

interface ReferenceImageItem {
  id: string;
  label: string;
  fileUrl: string;
  fileName: string;
}

type WorkbenchForm = Omit<CreateGenerationTaskRequest, "aspectRatio"> & {
  aspectRatio: RatioOptionValue;
  imageSize?: string | null;
};

const { items, loading, errorMessage } = useTaskShowcase();

const activeMenu = ref<MenuKey>("");
const selectedModeValue = ref<ModeValue>("video");
const loadingOptions = ref(false);
const submitting = ref(false);
const uploadingReference = ref(false);
const referenceDevelopingDialogOpen = ref(false);
const statusText = ref("参数加载中...");
const promptText = ref("");
const textFileInput = ref<HTMLInputElement | null>(null);
const referenceImages = ref<ReferenceImageItem[]>([]);
const createdTaskId = ref("");
const createdImageAssetId = ref("");
const seedMode = ref<"auto" | "manual">("auto");
const seedInput = ref("");
const autoSeed = ref(createRandomSeed());
const durationMode = ref<"auto" | "manual">("auto");
const selectedDurationSeconds = ref<number | null>(null);
const imageOutputCount = ref(1);

const options = ref<GenerationOptionsResponse | null>(null);
const form = ref<WorkbenchForm>({
  title: "工作台生成任务",
  creativePrompt: "",
  aspectRatio: "16:9",
  textAnalysisModel: null,
  imageModel: null,
  videoModel: null,
  videoSize: null,
  imageSize: null,
  outputCount: "auto",
  seed: null,
  videoDurationSeconds: "auto",
  transcriptText: "",
});

const modeOptions = [
  {
    value: "video" as const,
    kind: "video" as const,
    label: "视频生成",
    description: "输入文本，自动拆分脚本、关键帧和视频",
    icon: "◔",
  },
  {
    value: "image" as const,
    kind: "image" as const,
    label: "图片生成",
    description: "素材中心自由模式，支持参考图再创作",
    icon: "▧",
  },
  {
    value: "character_sheet" as const,
    kind: "image" as const,
    label: "角色三视图",
    description: "生成同一角色正面、侧面、背面设定图",
    icon: "▥",
  },
];

const quickModeCards = [
  {
    value: "video" as const,
    title: "视频生成",
    subtitle: "文生视频链路",
    icon: "V",
  },
  {
    value: "image" as const,
    title: "图片生成",
    subtitle: "自由模式出图",
    icon: "I",
  },
  {
    value: "character_sheet" as const,
    title: "角色三视图",
    subtitle: "人物设定图",
    icon: "3V",
  },
];

const selectedMode = computed(() => modeOptions.find((item) => item.value === selectedModeValue.value) ?? modeOptions[0]);
const promptLabel = computed(() => selectedMode.value.kind === "video" ? "文本 / 小说正文" : "图片提示词");
const promptPlaceholder = computed(() =>
  selectedMode.value.kind === "video"
    ? "输入文字或小说正文，描述你想生成的视频内容。"
    : selectedMode.value.value === "character_sheet"
      ? "描述角色身份、年龄感、脸部五官、发型、体型身高、服装和稳定配饰，用于生成正面、侧面、背面三视图。"
      : "上传参考图、输入文字或 @ 主体，描述你想生成的图片。",
);

const textModelOptions = computed<GenerationTextAnalysisModelInfo[]>(() => options.value?.textAnalysisModels ?? []);
const imageModelOptions = computed<GenerationTextAnalysisModelInfo[]>(() => options.value?.imageModels ?? []);
const videoModelOptions = computed<GenerationVideoModelInfo[]>(() => options.value?.videoModels ?? []);

const selectedImageModelOption = computed(() => {
  const selected = normalizeModelName(form.value.imageModel);
  return imageModelOptions.value.find((item) => normalizeModelName(item.value) === selected) ?? null;
});
const selectedVideoModelOption = computed(() => {
  const selected = normalizeModelName(form.value.videoModel);
  return videoModelOptions.value.find((item) => normalizeModelName(item.value) === selected) ?? null;
});
const selectedPrimaryModelLabel = computed(() => {
  if (selectedMode.value.kind === "video") {
    return selectedVideoModelOption.value?.label || selectedVideoModelOption.value?.value || "视频模型";
  }
  return selectedImageModelOption.value?.label || selectedImageModelOption.value?.value || "图片模型";
});

const ratioDisplayOrder: RatioOptionValue[] = ["智能", "21:9", "16:9", "3:2", "4:3", "1:1", "3:4", "2:3", "9:16"];

const ratioOptions = computed(() => {
  const values = selectedMode.value.kind === "image" ? availableImageRatios.value : availableVideoRatios.value;
  return [...values]
    .sort((a, b) => ratioDisplayOrder.indexOf(a) - ratioDisplayOrder.indexOf(b))
    .map((value) => ({
      value,
      label: value,
      shortLabel: value === "智能" ? "智能" : value,
      shape: ratioShape(value),
    }));
});

const availableVideoRatios = computed<RatioOptionValue[]>(() => {
  const catalog = options.value?.aspectRatios ?? [];
  const values = catalog
    .map((item) => item.value)
    .filter((value): value is AspectRatioValue => value === "16:9" || value === "9:16");
  return values.length ? [...new Set(values)] : ["16:9", "9:16"];
});

const availableImageRatios = computed<RatioOptionValue[]>(() => {
  const ratios = imageCandidateSizes.value
    .map((item) => sizeRatioLabel(item))
    .filter((value): value is RatioOptionValue => Boolean(value));
  const unique = [...new Set(ratios)];
  return unique.length ? unique : availableVideoRatios.value;
});

const imageCandidateSizes = computed<GenerationImageSizeOption[]>(() => {
  const source = options.value?.imageSizes ?? [];
  const selectedSizes = selectedImageModelOption.value?.supportedSizes ?? [];
  const normalizedSelectedSizes = selectedSizes.map(normalizeSizeValue);
  return source.filter((item) => {
    return !normalizedSelectedSizes.length || normalizedSelectedSizes.includes(normalizeSizeValue(item.value));
  });
});

const imageSizeOptions = computed<GenerationImageSizeOption[]>(() => {
  const filtered = imageCandidateSizes.value
    .filter((item) => imageSizeMatchesRatio(item, form.value.aspectRatio))
    .sort(compareSizeByArea);
  return filtered.length ? filtered : imageCandidateSizes.value.filter((item) => imageSizeMatchesRatio(item, form.value.aspectRatio)).sort(compareSizeByArea);
});

const videoSizeOptions = computed<GenerationVideoSizeOption[]>(() => {
  const selectedModel = normalizeModelName(form.value.videoModel);
  const ratio = videoAspectRatio(form.value.aspectRatio);
  return (options.value?.videoSizes ?? [])
    .filter((item) => resolveVideoSizeRatio(item) === ratio)
    .filter((item) => {
      const supportedModels = Array.isArray(item.supportedModels) ? item.supportedModels : [];
      return !selectedModel || !supportedModels.length || supportedModels.some((model) => normalizeModelName(model) === selectedModel);
    })
    .sort(compareSizeByArea);
});

const durationOptions = computed<GenerationVideoDurationOption[]>(() => {
  const modelDurations = selectedVideoModelOption.value?.supportedDurations ?? [];
  if (modelDurations.length) {
    return [...new Set(modelDurations)]
      .filter((item) => Number.isFinite(item) && item > 0)
      .sort((a, b) => a - b)
      .map((item) => ({ value: Math.trunc(item), label: `${Math.trunc(item)} 秒` }));
  }
  return [...(options.value?.videoDurations ?? [])].sort((a, b) => a.value - b.value);
});

const durationLabel = computed(() => {
  if (durationMode.value === "auto") {
    return "自动时长";
  }
  return selectedDurationSeconds.value ? `${selectedDurationSeconds.value}s` : "选择时长";
});

const videoOutputCountOptions = [1, 2, 3, 4, 6, 8, 10, 12];
const imageOutputCountOptions = [1, 2, 3, 4];
const outputCountLabel = computed(() => form.value.outputCount === "auto" ? "自动分镜" : `${form.value.outputCount} 段`);
const selectedImageSizeOption = computed(() => {
  return imageSizeOptions.value.find((item) => item.value === form.value.imageSize) ?? null;
});
const selectedImageSizeDimensions = computed(() => {
  return selectedImageSizeOption.value ? parseSize(selectedImageSizeOption.value) : null;
});
const selectedMaterialAssetType = computed(() => selectedMode.value.value === "character_sheet" ? "character_sheet" : "free");
const createdImageAssetLink = computed(() => `/materials?assetType=${encodeURIComponent(selectedMaterialAssetType.value)}`);
const ratioToolLabel = computed(() => {
  if (selectedMode.value.kind === "image") {
    const quality = selectedImageSizeOption.value ? imageQualityLabel(selectedImageSizeOption.value) : "";
    return [form.value.aspectRatio, quality].filter(Boolean).join(" | ") || form.value.aspectRatio;
  }
  return form.value.aspectRatio;
});

const parsedManualSeed = computed(() => parseSeed(seedInput.value));
const seedCapabilityHint = computed(() => {
  if (selectedMode.value.kind === "image" && !selectedImageModelOption.value?.supportsSeed) {
    return "当前图片模型未声明支持种子，提交时不会传 seed。";
  }
  return "当前设置会记录到本次生成任务。";
});
const isSeedReady = computed(() => seedMode.value === "auto" || parsedManualSeed.value !== null);
const isFormReady = computed(() => {
  if (!promptText.value.trim()) {
    return false;
  }
  if (!form.value.textAnalysisModel || !form.value.imageModel) {
    return false;
  }
  if (selectedMode.value.kind === "video" && (!form.value.videoModel || !form.value.videoSize)) {
    return false;
  }
  if (selectedMode.value.kind === "image" && !form.value.imageSize) {
    return false;
  }
  return isSeedReady.value;
});
const submitLabel = computed(() => {
  if (submitting.value) {
    return "创建中...";
  }
  return selectedMode.value.kind === "video" ? "生成视频" : selectedMode.value.value === "character_sheet" ? "生成三视图" : "生成图片";
});

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

function toggleMenu(menu: Exclude<MenuKey, "">) {
  activeMenu.value = activeMenu.value === menu ? "" : menu;
}

function selectMode(value: ModeValue) {
  selectedModeValue.value = value;
  activeMenu.value = "";
  if (value === "character_sheet" && availableImageRatios.value.includes("1:1")) {
    form.value.aspectRatio = "1:1";
  } else if (value === "video" && form.value.aspectRatio !== "16:9" && form.value.aspectRatio !== "9:16") {
    form.value.aspectRatio = "16:9";
  }
  statusText.value = value === "video"
    ? "视频生成会创建工作台视频任务。"
    : value === "character_sheet"
      ? "角色三视图会生成到素材库，可在阶段工作流中选择。"
      : "图片生成会使用素材中心自由模式。";
}

function selectRatio(value: RatioOptionValue) {
  form.value.aspectRatio = value;
}

function selectDuration(value: number) {
  durationMode.value = "manual";
  selectedDurationSeconds.value = value;
}

function normalizeModelName(value: unknown) {
  return String(value ?? "").trim().toLowerCase().replace(/[\s._-]/g, "");
}

function normalizeSizeValue(value: unknown) {
  return String(value ?? "").trim().toLowerCase().replace(/\*/g, "x");
}

function parseSeed(value: unknown): number | null {
  const raw = String(value ?? "").trim();
  if (!raw) {
    return null;
  }
  const numeric = Number(raw);
  if (!Number.isFinite(numeric) || !Number.isInteger(numeric) || numeric < 0) {
    return null;
  }
  return Math.trunc(numeric);
}

function createRandomSeed() {
  if (typeof window !== "undefined" && window.crypto?.getRandomValues) {
    const values = new Uint32Array(1);
    window.crypto.getRandomValues(values);
    return Math.max(1, values[0] % 2147483647);
  }
  return Math.max(1, Math.floor(Math.random() * 2147483647));
}

function refreshAutoSeed() {
  autoSeed.value = createRandomSeed();
}

function readTextFile(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(typeof reader.result === "string" ? reader.result : "");
    reader.onerror = () => reject(reader.error ?? new Error("读取文本文件失败"));
    reader.readAsText(file, "utf-8");
  });
}

function readImageAsDataUri(file: File): Promise<ReferenceImageItem> {
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
      const nextIndex = referenceImages.value.length + 1;
      resolve({
        id: `${Date.now()}-${nextIndex}-${file.name}`,
        label: `图片${nextIndex}`,
        fileUrl: result,
        fileName: file.name || `图片${nextIndex}`,
      });
    };
    reader.onerror = () => reject(reader.error ?? new Error("参考图读取失败"));
    reader.readAsDataURL(file);
  });
}

function handleReferenceEntryClick() {
  if (selectedMode.value.kind === "video") {
    referenceDevelopingDialogOpen.value = true;
    statusText.value = "视频模式添加参考图正在开发中。";
    return;
  }
  textFileInput.value?.click();
}

async function handleReferenceFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []);
  if (!files.length) {
    return;
  }
  if (selectedMode.value.kind === "image") {
    await handleImageReferenceFiles(files, input);
    return;
  }
  await handleTextReferenceFile(files[0], input);
}

async function handleTextReferenceFile(file: File, input: HTMLInputElement) {
  const authenticated = await requireAuth({
    title: "登录后上传参考内容",
    message: "文本上传会保存到你的账号下，请先登录或使用邀请码注册。",
  });
  if (!authenticated) {
    input.value = "";
    return;
  }
  uploadingReference.value = true;
  statusText.value = "正在读取参考内容...";
  try {
    const [, content] = await Promise.all([uploadText(file), readTextFile(file)]);
    if (content.trim()) {
      promptText.value = content;
      form.value.title = file.name.replace(/\.txt$/i, "") || form.value.title;
    }
    statusText.value = "参考内容已填入输入框。";
  } catch (error) {
    statusText.value = error instanceof Error ? error.message : "参考内容读取失败";
  } finally {
    uploadingReference.value = false;
    input.value = "";
  }
}

async function handleImageReferenceFiles(files: File[], input: HTMLInputElement) {
  uploadingReference.value = true;
  statusText.value = "正在读取参考图...";
  try {
    const items = await Promise.all(files.map(readImageAsDataUri));
    const previousCount = referenceImages.value.length;
    const merged = [...referenceImages.value, ...items].slice(0, 12);
    referenceImages.value = merged.map((item, index) => ({
      ...item,
      label: `图片${index + 1}`,
    }));
    const addedCount = Math.max(referenceImages.value.length - previousCount, 0);
    statusText.value = addedCount > 0
      ? `已添加 ${addedCount} 张参考图，可通过 @ 引用。`
      : "最多支持 12 张参考图。";
  } catch (error) {
    statusText.value = error instanceof Error ? error.message : "参考图读取失败";
  } finally {
    uploadingReference.value = false;
    input.value = "";
  }
}

function removeReferenceImage(id: string) {
  referenceImages.value = referenceImages.value
    .filter((item) => item.id !== id)
    .map((item, index) => ({
      ...item,
      label: `图片${index + 1}`,
    }));
}

function insertMention(label: string) {
  const mention = `@${label}`;
  if (!promptText.value.includes(mention)) {
    promptText.value = promptText.value.trim() ? `${promptText.value.trim()} ${mention} ` : `${mention} `;
  }
  activeMenu.value = "";
}

function parseSize(item: { value: string; width?: number; height?: number }) {
  if (typeof item.width === "number" && typeof item.height === "number" && item.width > 0 && item.height > 0) {
    return { width: item.width, height: item.height };
  }
  const matched = String(item.value ?? "").match(/^(\d+)\s*[xX*]\s*(\d+)$/);
  if (!matched) {
    return null;
  }
  const width = Number(matched[1]);
  const height = Number(matched[2]);
  if (!Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) {
    return null;
  }
  return { width, height };
}

function resolveVideoSizeRatio(item: { value: string; width?: number; height?: number }): AspectRatioValue | null {
  const parsed = parseSize(item);
  if (!parsed || parsed.width === parsed.height) {
    return null;
  }
  return parsed.width > parsed.height ? "16:9" : "9:16";
}

function imageSizeMatchesRatio(item: GenerationImageSizeOption, ratio: string) {
  const itemRatio = sizeRatioLabel(item);
  return !itemRatio || itemRatio === ratio;
}

function compareSizeByArea(a: { value: string; width?: number; height?: number }, b: { value: string; width?: number; height?: number }) {
  const aSize = parseSize(a);
  const bSize = parseSize(b);
  const aArea = aSize ? aSize.width * aSize.height : 0;
  const bArea = bSize ? bSize.width * bSize.height : 0;
  return aArea - bArea;
}

function sizeRatioLabel(item: { value: string; width?: number; height?: number }): RatioOptionValue | null {
  const parsed = parseSize(item);
  if (!parsed) {
    return null;
  }
  const divisor = gcd(parsed.width, parsed.height);
  const width = parsed.width / divisor;
  const height = parsed.height / divisor;
  const ratio = `${width}:${height}`;
  return ratioDisplayOrder.includes(ratio as RatioOptionValue) ? ratio as RatioOptionValue : null;
}

function ratioShape(value: RatioOptionValue) {
  if (value === "智能") {
    return "1 / 1";
  }
  return value.replace(":", " / ");
}

function videoAspectRatio(value: RatioOptionValue): AspectRatioValue {
  return value === "9:16" ? "9:16" : "16:9";
}

function gcd(a: number, b: number): number {
  let x = Math.abs(Math.trunc(a));
  let y = Math.abs(Math.trunc(b));
  while (y) {
    const next = x % y;
    x = y;
    y = next;
  }
  return x || 1;
}

function imageQualityLabel(item: { value: string; label?: string | null; width?: number; height?: number }) {
  const label = String(item.label ?? "");
  if (/\b4K\b/i.test(label)) {
    return "超清 4K";
  }
  if (/\b2K\b/i.test(label)) {
    return "高清 2K";
  }
  if (/\b1K\b/i.test(label)) {
    return "标准 1K";
  }
  const size = parseSize(item);
  if (!size) {
    return String(item.value ?? "");
  }
  const longest = Math.max(size.width, size.height);
  if (longest >= 2800) {
    return "超清 4K";
  }
  if (longest >= 1800) {
    return "高清 2K";
  }
  return "标准 1K";
}

function formatImageSizeOptionLabel(item: GenerationImageSizeOption) {
  const label = imageQualityLabel(item);
  return label.includes("4K") ? `${label} ✦` : label;
}

async function loadOptions() {
  loadingOptions.value = true;
  try {
    const result = await fetchGenerationOptions();
    options.value = result;
    form.value.aspectRatio = (result.defaultAspectRatio as AspectRatioValue | null) || "16:9";
    form.value.textAnalysisModel = result.defaultTextAnalysisModel || result.textAnalysisModels?.[0]?.value || null;
    form.value.imageModel = resolveDefaultImageModel(result.imageModels ?? [], form.value.imageModel);
    form.value.videoModel = result.defaultVideoModel || result.videoModels?.[0]?.value || null;
    selectedDurationSeconds.value = result.defaultVideoDurationSeconds ?? result.videoDurations?.[0]?.value ?? null;
    statusText.value = "已按当前模型能力加载默认参数。";
  } catch (error) {
    statusText.value = error instanceof Error ? error.message : "加载模型配置失败";
  } finally {
    loadingOptions.value = false;
  }
}

function resolveDefaultImageModel(models: GenerationTextAnalysisModelInfo[], current?: string | null) {
  const currentValue = String(current ?? "").trim();
  if (currentValue && models.some((item) => item.value === currentValue)) {
    return currentValue;
  }
  const gptModel = models.find((item) => [item.family, item.provider, item.value, item.label].map(normalizeModelName).some((value) => value.includes("gpt")));
  return gptModel?.value || models[0]?.value || null;
}

watch(
  imageSizeOptions,
  (items) => {
    if (selectedMode.value.kind !== "image") {
      return;
    }
    if (!items.length) {
      form.value.imageSize = null;
      return;
    }
    const configured = options.value?.defaultImageSize;
    const currentValid = form.value.imageSize && items.some((item) => item.value === form.value.imageSize);
    if (!currentValid) {
      form.value.imageSize = items.find((item) => item.value === configured)?.value ?? items[0].value;
    }
  },
  { immediate: true },
);

watch(
  videoSizeOptions,
  (items) => {
    if (!items.length) {
      form.value.videoSize = null;
      return;
    }
    const configured = options.value?.defaultVideoSize;
    const currentValid = form.value.videoSize && items.some((item) => item.value === form.value.videoSize);
    if (!currentValid) {
      form.value.videoSize = items.find((item) => item.value === configured)?.value ?? items[0].value;
    }
  },
  { immediate: true },
);

watch(seedMode, (mode, previousMode) => {
  if (mode === "auto" && previousMode !== "auto") {
    refreshAutoSeed();
  }
});

async function submitComposer() {
  if (!isFormReady.value) {
    statusText.value = "请先输入内容并补全参数。";
    return;
  }
  const authenticated = await requireAuth({
    title: "登录后开始生成",
    message: "生成结果会保存到你的任务和素材库中，请先登录或使用邀请码注册。",
  });
  if (!authenticated) {
    statusText.value = "登录后即可继续生成。";
    return;
  }
  submitting.value = true;
  createdTaskId.value = "";
  createdImageAssetId.value = "";
  try {
    if (selectedMode.value.kind === "image") {
      await submitImageGeneration();
    } else {
      await submitVideoGeneration();
    }
  } catch (error) {
    statusText.value = error instanceof Error ? error.message : "创建失败";
  } finally {
    submitting.value = false;
  }
}

async function submitImageGeneration() {
  const isCharacterSheet = selectedMaterialAssetType.value === "character_sheet";
  const result = await createMaterialGeneration({
    assetType: selectedMaterialAssetType.value,
    title: promptText.value.trim().slice(0, 32) || (isCharacterSheet ? "角色三视图" : "图片生成"),
    description: promptText.value.trim(),
    styleKeywords: [],
    aspectRatio: form.value.aspectRatio,
    imageSize: form.value.imageSize || null,
    textAnalysisModel: form.value.textAnalysisModel || null,
    imageModel: form.value.imageModel || null,
    seed: selectedImageModelOption.value?.supportsSeed
      ? (seedMode.value === "manual" ? parsedManualSeed.value : autoSeed.value)
      : null,
    referenceImageUrls: referenceImages.value.map((item) => item.fileUrl),
    referenceAssetIds: [],
  });
  createdImageAssetId.value = result.asset?.id || result.id || "";
  statusText.value = isCharacterSheet
    ? "角色三视图已提交，生成后会进入素材库。"
    : imageOutputCount.value > 1
    ? "当前图片接口按单张素材生成，已创建 1 张图片。"
    : (referenceImages.value.length ? `图片生成已提交，已附带 ${referenceImages.value.length} 张参考图。` : "图片生成已提交。");
}

async function submitVideoGeneration() {
  const duration = durationMode.value === "manual" && selectedDurationSeconds.value ? selectedDurationSeconds.value : null;
  const payload: CreateGenerationTaskRequest = {
    title: promptText.value.trim().slice(0, 32) || "工作台视频生成",
    creativePrompt: "",
    aspectRatio: videoAspectRatio(form.value.aspectRatio),
    textAnalysisModel: form.value.textAnalysisModel || null,
    imageModel: form.value.imageModel || null,
    videoModel: form.value.videoModel || null,
    videoSize: form.value.videoSize || null,
    outputCount: form.value.outputCount ?? "auto",
    seed: seedMode.value === "manual" ? parsedManualSeed.value : autoSeed.value,
    videoDurationSeconds: "auto",
    minDurationSeconds: duration,
    maxDurationSeconds: duration,
    transcriptText: promptText.value.trim(),
    stopBeforeVideoGeneration: shouldStopBeforeVideoGeneration(),
  };
  const task = await createGenerationTask(payload);
  createdTaskId.value = task.id;
  statusText.value = "视频任务已创建，进入任务管理可查看进度。";
}

onMounted(loadOptions);
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
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 0;
  font-size: clamp(1.45rem, 2.4vw, 2rem);
  font-weight: 800;
  letter-spacing: 0;
}

.hero-mode-button {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--accent-cyan);
  font: inherit;
  cursor: pointer;
}

.home-composer {
  position: relative;
  display: grid;
  width: min(100%, 1420px);
  min-height: 204px;
  padding: 24px 74px 18px 110px;
  border: 1px solid rgba(15, 20, 25, 0.06);
  border-radius: 24px;
  background: #fff;
  box-shadow: var(--shadow-panel);
}

.home-hidden-input {
  display: none;
}

.home-composer__upload {
  position: absolute;
  left: 24px;
  top: 28px;
  display: grid;
  place-items: center;
  gap: 4px;
  width: 58px;
  height: 78px;
  border: 1px dashed rgba(15, 20, 25, 0.08);
  border-radius: 3px;
  background: #f0f1f2;
  color: var(--text-muted);
  transform: rotate(-7deg);
  cursor: pointer;
}

.home-composer__upload span {
  font-size: 1.45rem;
  line-height: 1;
}

.home-composer__upload img {
  width: 42px;
  height: 42px;
  border-radius: 4px;
  object-fit: cover;
  transform: rotate(7deg);
}

.home-reference-add {
  position: absolute;
  right: -8px;
  top: -8px;
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #18c1d9;
  color: #fff;
  font-size: 0.92rem !important;
  font-weight: 900;
  transform: rotate(7deg);
  box-shadow: 0 6px 14px rgba(24, 193, 217, 0.28);
}

.home-composer__upload small {
  font-size: 0.68rem;
  font-weight: 700;
}

.home-reference-stack {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  margin: -8px 0 10px;
}

.home-reference-thumb {
  position: relative;
  display: inline-grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border: 2px solid transparent;
  border-radius: 7px;
  background: #f0f1f2;
}

.home-reference-thumb-mentioned {
  border-color: var(--accent-cyan);
}

.home-reference-thumb img {
  width: 100%;
  height: 100%;
  border-radius: 5px;
  object-fit: cover;
}

.home-reference-thumb button {
  position: absolute;
  right: -7px;
  top: -7px;
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  border: 0;
  border-radius: 50%;
  background: rgba(15, 20, 25, 0.78);
  color: #fff;
  font-size: 0.72rem;
  line-height: 1;
  cursor: pointer;
}

.home-reference-more {
  display: grid;
  place-items: center;
  min-width: 32px;
  height: 32px;
  border-radius: 999px;
  background: #eef0f2;
  color: var(--text-muted);
  font-size: 0.78rem;
  font-weight: 800;
}

.home-composer__prompt {
  display: grid;
  gap: 8px;
}

.home-composer__prompt span {
  color: #9aa4ad;
  font-size: 0.82rem;
  font-weight: 700;
}

.home-composer__prompt textarea {
  min-height: 82px;
  width: 100%;
  resize: vertical;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-strong);
  font-size: 1rem;
  line-height: 1.7;
}

.home-composer__prompt textarea::placeholder {
  color: #9aa4ad;
}

.home-composer__toolbar {
  align-self: end;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 18px;
}

.home-tool {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(15, 20, 25, 0.07);
  border-radius: 9px;
  background: #fff;
  color: var(--text-strong);
  font-size: 0.86rem;
  font-weight: 750;
  cursor: pointer;
}

.home-tool-accent {
  color: var(--accent-cyan);
}

.home-tool-active {
  background: #eef0f2;
  box-shadow: inset 0 1px 2px rgba(15, 20, 25, 0.08);
}

.home-tool__icon {
  display: inline-grid;
  place-items: center;
  width: 18px;
  height: 18px;
  color: currentColor;
}

.home-tool__shape {
  width: 20px;
  height: 12px;
  border: 2px solid currentColor;
  border-radius: 4px;
}

.home-menu {
  position: relative;
}

.home-popover {
  position: absolute;
  left: 0;
  bottom: calc(100% + 12px);
  z-index: 5;
  display: grid;
  gap: 12px;
  width: 360px;
  padding: 16px;
  border: 1px solid rgba(15, 20, 25, 0.06);
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 24px 56px rgba(20, 28, 36, 0.16);
}

.home-popover-ratio {
  width: min(820px, calc(100vw - 48px));
  gap: 22px;
  padding: 24px 26px 28px;
  border-radius: 18px;
  box-shadow: 0 28px 84px rgba(20, 28, 36, 0.18);
}

.home-popover-compact,
.home-popover-seed {
  width: 300px;
}

.home-popover__label {
  margin: 0;
  color: #9aa4ad;
  font-size: 0.76rem;
  font-weight: 800;
}

.home-popover__item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 18px;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 58px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--text-strong);
  text-align: left;
  cursor: pointer;
}

.home-popover__item-active {
  background: #f1f3f4;
}

.home-popover__icon {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: #e9f8fb;
  color: var(--accent-cyan);
  font-size: 0.78rem;
  font-weight: 900;
}

.home-popover__image {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  overflow: hidden;
  background: #eef0f2;
}

.home-popover__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.home-popover__item strong,
.home-popover__item small {
  display: block;
}

.home-popover__item strong {
  font-size: 0.88rem;
}

.home-popover__item small,
.home-popover-seed small {
  margin-top: 3px;
  color: var(--text-muted);
  font-size: 0.74rem;
}

.home-popover__check {
  color: var(--text-strong);
  font-size: 0.94rem;
}

.home-popover__empty {
  margin: 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f7f8f9;
  color: var(--text-muted);
  font-size: 0.78rem;
  font-weight: 700;
}

.home-dialog {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 20, 25, 0.28);
}

.home-dialog__panel {
  display: grid;
  gap: 14px;
  width: min(100%, 360px);
  padding: 22px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 24px 70px rgba(15, 20, 25, 0.2);
}

.home-dialog__panel h2,
.home-dialog__panel p {
  margin: 0;
}

.home-dialog__panel h2 {
  font-size: 1rem;
  font-weight: 850;
}

.home-dialog__panel p {
  color: var(--text-muted);
  font-size: 0.86rem;
  line-height: 1.6;
}

.home-dialog__panel button {
  justify-self: end;
  min-width: 82px;
  min-height: 36px;
  border: 0;
  border-radius: 9px;
  background: var(--text-strong);
  color: #fff;
  font-weight: 800;
  cursor: pointer;
}

.home-field {
  display: grid;
  gap: 6px;
}

.home-field span {
  color: var(--text-muted);
  font-size: 0.76rem;
  font-weight: 800;
}

.home-field select,
.home-field input {
  min-height: 40px;
  width: 100%;
  border: 1px solid rgba(15, 20, 25, 0.07);
  border-radius: 10px;
  background: #f7f8f9;
  color: var(--text-strong);
  font-size: 0.86rem;
  outline: 0;
  padding: 0 12px;
}

.home-segment-grid {
  display: grid;
  gap: 8px;
}

.home-ratio-list {
  display: grid;
  gap: 6px;
}

.home-segment-grid button,
.home-seed-row button {
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: var(--text-body);
  font-weight: 800;
  cursor: pointer;
}

.home-ratio-list-immersive {
  grid-template-columns: repeat(auto-fit, minmax(70px, 1fr));
  align-items: stretch;
  padding: 8px;
  border-radius: 19px;
  background: #f4f5f6;
}

.home-ratio-list button {
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 8px;
  min-height: 90px;
  border: 0;
  border-radius: 17px;
  background: transparent;
  color: #1f2831;
  font-size: 0.88rem;
  font-weight: 850;
  cursor: pointer;
}

.home-ratio-active,
.home-segment-active {
  background: #fff !important;
  color: var(--accent-cyan) !important;
  box-shadow: 0 1px 4px rgba(15, 20, 25, 0.08);
}

.home-ratio-active {
  color: #1f2831 !important;
  box-shadow: 0 10px 28px rgba(20, 28, 36, 0.08) !important;
}

.home-ratio__shape {
  width: 28px;
  max-height: 34px;
  min-height: 18px;
  border: 2.2px solid currentColor;
  border-radius: 4px;
}

.home-resolution-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0;
  overflow: hidden;
  padding: 6px;
  border-radius: 16px;
  background: #f4f5f6;
}

.home-resolution-list button {
  min-height: 68px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  color: #1f2831;
  font-size: 1rem;
  font-weight: 850;
  cursor: pointer;
}

.home-resolution-active {
  background: #fff !important;
  color: #1f2831 !important;
  box-shadow: 0 8px 22px rgba(20, 28, 36, 0.07);
}

.home-dimension-row {
  display: grid;
  grid-template-columns: 42px minmax(110px, 1fr) 52px 42px minmax(110px, 1fr) 42px;
  align-items: center;
  gap: 12px;
}

.home-dimension-row span,
.home-dimension-row strong {
  min-height: 58px;
  display: grid;
  align-items: center;
  border-radius: 13px;
  background: #f4f5f6;
}

.home-dimension-row span {
  justify-items: center;
  color: #63717d;
  font-size: 0.92rem;
  font-weight: 800;
}

.home-dimension-row strong {
  justify-items: end;
  padding: 0 20px;
  color: #1f2831;
  font-size: 1rem;
  font-weight: 850;
}

.home-dimension-row .home-dimension-link {
  background: transparent;
  color: #63717d;
  font-size: 1.1rem;
}

.home-segment-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  padding: 8px;
  border-radius: 12px;
  background: #f7f8f9;
}

.home-segment-grid button {
  min-height: 38px;
  padding: 0 10px;
}

.home-seed-row {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: space-between;
  min-height: 40px;
  padding: 0 10px;
  border-radius: 10px;
  background: #f7f8f9;
  color: var(--text-strong);
  font-weight: 800;
}

.home-seed-row button {
  color: var(--accent-cyan);
}

.home-composer__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  min-height: 24px;
  padding-top: 10px;
  color: var(--text-muted);
  font-size: 0.78rem;
}

.home-composer__meta a {
  color: var(--accent-cyan);
  font-weight: 800;
}

.home-composer__submit {
  position: absolute;
  right: 22px;
  bottom: 24px;
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border: 0;
  border-radius: 50%;
  background: #dce0e4;
  color: #fff;
  cursor: pointer;
}

.home-composer__submit:not(:disabled) {
  background: linear-gradient(135deg, #18c1d9, #3674ff);
}

.home-composer__submit:disabled {
  cursor: not-allowed;
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
  color: var(--text-strong);
  text-align: left;
  cursor: pointer;
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
  letter-spacing: 0;
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

.case-card-2,
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
    padding: 18px 18px 18px;
  }

  .home-composer__upload {
    position: static;
    margin-bottom: 16px;
  }

  .home-composer__submit {
    position: static;
    margin-left: auto;
  }

  .home-popover {
    position: fixed;
    left: 14px;
    right: 14px;
    bottom: 88px;
    width: auto;
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
