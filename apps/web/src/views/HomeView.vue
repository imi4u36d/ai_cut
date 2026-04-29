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
          :disabled="uploadingText"
          @click="textFileInput?.click()"
        >
          <span>+</span>
          <small>{{ selectedMode.kind === "video" ? "参考内容" : "参考图" }}</small>
        </button>
        <input
          ref="textFileInput"
          type="file"
          accept=".txt,text/plain"
          class="home-hidden-input"
          @change="handleTextFileChange"
        />

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
              {{ form.aspectRatio }}
            </button>
            <div v-if="activeMenu === 'ratio'" class="home-popover home-popover-ratio">
              <p class="home-popover__label">选择比例</p>
              <div class="home-ratio-list">
                <button
                  v-for="ratio in ratioOptions"
                  :key="ratio.value"
                  type="button"
                  :class="{ 'home-ratio-active': form.aspectRatio === ratio.value }"
                  @click="selectRatio(ratio.value)"
                >
                  <span class="home-ratio__shape" :style="{ aspectRatio: ratio.shape }"></span>
                  <span>{{ ratio.label }}</span>
                </button>
              </div>
              <template v-if="selectedMode.kind === 'image'">
                <p class="home-popover__label">选择分辨率</p>
                <div class="home-segment-grid">
                  <button
                    v-for="size in imageSizeOptions"
                    :key="size.value"
                    type="button"
                    :class="{ 'home-segment-active': form.imageSize === size.value }"
                    @click="form.imageSize = size.value"
                  >
                    {{ compactImageSizeLabel(size.label || size.value) }}
                  </button>
                </div>
              </template>
              <template v-else>
                <p class="home-popover__label">视频尺寸</p>
                <div class="home-segment-grid">
                  <button
                    v-for="size in videoSizeOptions"
                    :key="size.value"
                    type="button"
                    :class="{ 'home-segment-active': form.videoSize === size.value }"
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

          <div class="home-menu">
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
            <button type="button" class="home-tool" :class="{ 'home-tool-active': activeMenu === 'seed' }" @click="toggleMenu('seed')">
              <span class="home-tool__icon">@</span>
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
          <RouterLink v-if="createdImageAssetId" to="/materials?assetType=free">查看素材</RouterLink>
        </div>

        <button class="home-composer__submit" type="submit" :disabled="submitting || loadingOptions || !isFormReady" :title="submitLabel">
          <svg v-if="!submitting" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 19V5" />
            <path d="m5 12 7-7 7 7" />
          </svg>
          <span v-else>...</span>
        </button>
      </form>
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

type ModeValue = "video" | "image";
type MenuKey = "" | "mode" | "model" | "ratio" | "duration" | "count" | "seed";

const { items, loading, errorMessage } = useTaskShowcase();

const activeMenu = ref<MenuKey>("");
const selectedModeValue = ref<ModeValue>("video");
const loadingOptions = ref(false);
const submitting = ref(false);
const uploadingText = ref(false);
const statusText = ref("参数加载中...");
const promptText = ref("");
const textFileInput = ref<HTMLInputElement | null>(null);
const createdTaskId = ref("");
const createdImageAssetId = ref("");
const seedMode = ref<"auto" | "manual">("auto");
const seedInput = ref("");
const autoSeed = ref(createRandomSeed());
const durationMode = ref<"auto" | "manual">("auto");
const selectedDurationSeconds = ref<number | null>(null);
const imageOutputCount = ref(1);

const options = ref<GenerationOptionsResponse | null>(null);
const form = ref<CreateGenerationTaskRequest & { imageSize?: string | null }>({
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
];

const selectedMode = computed(() => modeOptions.find((item) => item.value === selectedModeValue.value) ?? modeOptions[0]);
const promptLabel = computed(() => selectedMode.value.kind === "video" ? "文本 / 小说正文" : "图片提示词");
const promptPlaceholder = computed(() =>
  selectedMode.value.kind === "video"
    ? "上传最多 12 个参考素材、输入文字或 @ 参考内容，自由组合图、文、音、视频多元素，定义精彩互动。"
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

const ratioOptions = computed(() => {
  const catalog = options.value?.aspectRatios ?? [];
  const usable = catalog.filter((item) => item.value === "16:9" || item.value === "9:16");
  const source = usable.length ? usable : [
    { value: "16:9", label: "16:9" },
    { value: "9:16", label: "9:16" },
  ];
  return source.map((item) => ({
    value: item.value as "16:9" | "9:16",
    label: item.value,
    shape: item.value === "16:9" ? "16 / 9" : "9 / 16",
  }));
});

const imageSizeOptions = computed<GenerationImageSizeOption[]>(() => {
  const source = options.value?.imageSizes ?? [];
  const selectedSizes = selectedImageModelOption.value?.supportedSizes ?? [];
  const normalizedSelectedSizes = selectedSizes.map(normalizeSizeValue);
  const filtered = source
    .filter((item) => {
      if (normalizedSelectedSizes.length && !normalizedSelectedSizes.includes(normalizeSizeValue(item.value))) {
        return false;
      }
      return imageSizeMatchesRatio(item, form.value.aspectRatio);
    });
  return filtered.length ? filtered : source.filter((item) => imageSizeMatchesRatio(item, form.value.aspectRatio));
});

const videoSizeOptions = computed<GenerationVideoSizeOption[]>(() => {
  const selectedModel = normalizeModelName(form.value.videoModel);
  return (options.value?.videoSizes ?? [])
    .filter((item) => resolveSizeRatio(item) === form.value.aspectRatio)
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
  return selectedMode.value.kind === "video" ? "生成视频" : "生成图片";
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
  statusText.value = value === "video" ? "视频生成会创建一键生成任务。" : "图片生成会使用素材中心自由模式。";
}

function selectRatio(value: "16:9" | "9:16") {
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

async function handleTextFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  uploadingText.value = true;
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
    uploadingText.value = false;
    input.value = "";
  }
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

function resolveSizeRatio(item: { value: string; width?: number; height?: number }): "16:9" | "9:16" | null {
  const parsed = parseSize(item);
  if (!parsed || parsed.width === parsed.height) {
    return null;
  }
  return parsed.width > parsed.height ? "16:9" : "9:16";
}

function imageSizeMatchesRatio(item: GenerationImageSizeOption, ratio: string) {
  const itemRatio = resolveSizeRatio(item);
  return !itemRatio || itemRatio === ratio;
}

function compareSizeByArea(a: { value: string; width?: number; height?: number }, b: { value: string; width?: number; height?: number }) {
  const aSize = parseSize(a);
  const bSize = parseSize(b);
  const aArea = aSize ? aSize.width * aSize.height : 0;
  const bArea = bSize ? bSize.width * bSize.height : 0;
  return aArea - bArea;
}

function compactImageSizeLabel(value: string) {
  return value.replace(/（.*?）/g, "").replace(/\s*·\s*/g, " ");
}

async function loadOptions() {
  loadingOptions.value = true;
  try {
    const result = await fetchGenerationOptions();
    options.value = result;
    form.value.aspectRatio = (result.defaultAspectRatio as "16:9" | "9:16" | null) || "16:9";
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
  const result = await createMaterialGeneration({
    assetType: "free",
    title: promptText.value.trim().slice(0, 32) || "图片生成",
    description: promptText.value.trim(),
    styleKeywords: [],
    aspectRatio: form.value.aspectRatio,
    imageSize: form.value.imageSize || null,
    textAnalysisModel: form.value.textAnalysisModel || null,
    imageModel: form.value.imageModel || null,
    seed: selectedImageModelOption.value?.supportsSeed
      ? (seedMode.value === "manual" ? parsedManualSeed.value : autoSeed.value)
      : null,
    referenceImageUrls: [],
    referenceAssetIds: [],
  });
  createdImageAssetId.value = result.asset?.id || result.id || "";
  statusText.value = imageOutputCount.value > 1
    ? "当前图片接口按单张素材生成，已创建 1 张图片。"
    : "图片生成已提交。";
}

async function submitVideoGeneration() {
  const duration = durationMode.value === "manual" && selectedDurationSeconds.value ? selectedDurationSeconds.value : null;
  const payload: CreateGenerationTaskRequest = {
    title: promptText.value.trim().slice(0, 32) || "工作台视频生成",
    creativePrompt: "",
    aspectRatio: form.value.aspectRatio,
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

.home-composer__upload small {
  font-size: 0.68rem;
  font-weight: 700;
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
  width: 510px;
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

.home-ratio-list,
.home-segment-grid {
  display: grid;
  gap: 8px;
}

.home-ratio-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  padding: 10px;
  border-radius: 12px;
  background: #f7f8f9;
}

.home-ratio-list button,
.home-segment-grid button,
.home-seed-row button {
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: var(--text-body);
  font-weight: 800;
  cursor: pointer;
}

.home-ratio-list button {
  display: grid;
  justify-items: center;
  gap: 6px;
  min-height: 70px;
  padding: 8px 6px;
}

.home-ratio-active,
.home-segment-active {
  background: #fff !important;
  color: var(--accent-cyan) !important;
  box-shadow: 0 1px 4px rgba(15, 20, 25, 0.08);
}

.home-ratio__shape {
  width: 30px;
  max-height: 34px;
  min-height: 16px;
  border: 2px solid currentColor;
  border-radius: 4px;
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
