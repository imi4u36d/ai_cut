<template>
  <main class="home-page">
    <section class="home-hero">
      <h1>
        开启你的
        <button type="button" class="hero-mode-button" @click="toggleMenu('mode')">
          {{ selectedMode.label }}
        </button>
        即刻造梦！
      </h1>

      <form class="home-composer" @submit.prevent="submitComposer">
        <button
          type="button"
          class="home-composer__upload"
          :class="{
            'home-composer__upload-has-reference': selectedMode.kind === 'image' && referenceImages.length > 0,
            'home-composer__upload-has-multiple': selectedMode.kind === 'image' && referenceImages.length > 1,
            'home-composer__upload-expanded': selectedMode.kind === 'image' && referenceImages.length > 0 && referenceExpanded,
          }"
          :disabled="uploadingReference"
          @pointerenter="handleReferenceUploadPointerEnter"
          @pointerleave="handleReferenceUploadPointerLeave"
          @click="handleReferenceEntryClick"
        >
          <template v-if="selectedMode.kind === 'image' && referenceImages.length">
            <span class="home-composer__upload-scene" :style="referenceUploadSceneStyle()" aria-hidden="true">
              <span class="home-composer__upload-preview">
                <span
                  v-for="(item, index) in referenceImages"
                  :key="item.id"
                  class="home-composer__upload-preview-image"
                  :style="referencePreviewImageStyle(index)"
                  @click.stop
                >
                  <img :src="item.fileUrl" :alt="item.label" />
                  <span
                    class="home-composer__upload-preview-image-remove"
                    role="button"
                    :aria-label="`移除${item.label}`"
                    @click.stop="removeReferenceImage(item.id)"
                  >
                    ×
                  </span>
                </span>
              </span>
              <span class="home-composer__upload-add-card" :style="referenceAddCardStyle()">
                <span>+</span>
              </span>
              <span class="home-reference-add">+</span>
            </span>
          </template>
          <span v-else>+</span>
        </button>
        <input
          ref="textFileInput"
          type="file"
          :accept="selectedMode.kind === 'image' ? 'image/*' : '.txt,text/plain'"
          class="home-hidden-input"
          :multiple="selectedMode.kind === 'image'"
          @change="handleReferenceFileChange"
        />

        <div class="home-composer__body">
          <label class="home-composer__prompt">
            <div v-if="showPromptPlaceholder" class="home-composer__placeholder" aria-hidden="true">
              <span>输入想法、剧本或上传参考，支持 "/" 使用技能，</span>
              <span class="home-composer__placeholder-tag">@</span>
              <span> 添加主体，和Agent一起创作</span>
            </div>
            <div
              ref="promptEditor"
              class="home-composer__editor"
              contenteditable="true"
              role="textbox"
              :aria-label="promptLabel"
              aria-multiline="true"
              spellcheck="false"
              @focus="handlePromptEditorFocus"
              @blur="handlePromptEditorBlur"
              @compositionstart="handlePromptEditorCompositionStart"
              @compositionend="handlePromptEditorCompositionEnd"
              @beforeinput="handlePromptEditorBeforeInput"
              @input="handlePromptEditorInput"
              @keydown="handlePromptEditorKeydown"
              @paste="handlePromptEditorPaste"
            ></div>
          </label>
        </div>

        <div class="home-composer__footer">
          <div class="home-composer__toolbar">
            <div class="home-menu">
              <button type="button" class="home-tool home-tool-accent" :class="{ 'home-tool-active': activeMenu === 'mode' }" @click="toggleMenu('mode')">
                <span class="home-tool__icon" v-html="selectedMode.iconSvg"></span>
                {{ selectedMode.label }}
              </button>
              <transition name="home-popover-float">
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
                    <span class="home-popover__icon" v-html="option.iconSvg"></span>
                    <span>
                      <strong>{{ option.label }}</strong>
                      <small>{{ option.description }}</small>
                    </span>
                    <span v-if="selectedModeValue === option.value" class="home-popover__check" aria-hidden="true">
                      <svg viewBox="0 0 20 20" fill="none">
                        <path d="M4.5 10.5 8.2 14.2 15.5 5.8" />
                      </svg>
                    </span>
                  </button>
                </div>
              </transition>
            </div>

            <div class="home-menu">
              <button type="button" class="home-tool" :class="{ 'home-tool-active': activeMenu === 'model' }" @click="toggleMenu('model')">
                <span class="home-tool__icon">□</span>
                {{ selectedPrimaryModelLabel }}
              </button>
              <transition name="home-popover-float">
                <div v-if="activeMenu === 'model'" class="home-popover home-popover-model">
                  <p class="home-popover__label">{{ selectedMode.kind === "video" ? "模型链路" : "图片模型" }}</p>
                  <template v-if="selectedMode.kind === 'video'">
                    <section class="home-popover-section">
                      <p class="home-popover__label">文本模型</p>
                      <button
                        v-for="model in textModelOptions"
                        :key="model.value"
                        type="button"
                        class="home-popover__item"
                        :class="{ 'home-popover__item-active': form.textAnalysisModel === model.value }"
                        @click="form.textAnalysisModel = model.value"
                      >
                        <span class="home-popover__icon">文</span>
                        <span>
                          <strong>{{ model.label }}</strong>
                          <small>{{ modelOptionDescription(model) }}</small>
                        </span>
                        <span v-if="form.textAnalysisModel === model.value" class="home-popover__check" aria-hidden="true">
                          <svg viewBox="0 0 20 20" fill="none">
                            <path d="M4.5 10.5 8.2 14.2 15.5 5.8" />
                          </svg>
                        </span>
                      </button>
                    </section>
                    <section class="home-popover-section">
                      <p class="home-popover__label">关键帧模型</p>
                      <button
                        v-for="model in imageModelOptions"
                        :key="model.value"
                        type="button"
                        class="home-popover__item"
                        :class="{ 'home-popover__item-active': form.imageModel === model.value }"
                        @click="form.imageModel = model.value"
                      >
                        <span class="home-popover__icon">帧</span>
                        <span>
                          <strong>{{ model.label }}</strong>
                          <small>{{ modelOptionDescription(model) }}</small>
                        </span>
                        <span v-if="form.imageModel === model.value" class="home-popover__check" aria-hidden="true">
                          <svg viewBox="0 0 20 20" fill="none">
                            <path d="M4.5 10.5 8.2 14.2 15.5 5.8" />
                          </svg>
                        </span>
                      </button>
                    </section>
                    <section class="home-popover-section">
                      <p class="home-popover__label">视频模型</p>
                      <button
                        v-for="model in videoModelOptions"
                        :key="model.value"
                        type="button"
                        class="home-popover__item"
                        :class="{ 'home-popover__item-active': form.videoModel === model.value }"
                        @click="form.videoModel = model.value"
                      >
                        <span class="home-popover__icon">影</span>
                        <span>
                          <strong>{{ model.label }}</strong>
                          <small>{{ modelOptionDescription(model) }}</small>
                        </span>
                        <span v-if="form.videoModel === model.value" class="home-popover__check" aria-hidden="true">
                          <svg viewBox="0 0 20 20" fill="none">
                            <path d="M4.5 10.5 8.2 14.2 15.5 5.8" />
                          </svg>
                        </span>
                      </button>
                    </section>
                  </template>
                  <template v-else>
                    <section class="home-popover-section">
                      <p class="home-popover__label">文本模型</p>
                      <button
                        v-for="model in textModelOptions"
                        :key="model.value"
                        type="button"
                        class="home-popover__item"
                        :class="{ 'home-popover__item-active': form.textAnalysisModel === model.value }"
                        @click="form.textAnalysisModel = model.value"
                      >
                        <span class="home-popover__icon">文</span>
                        <span>
                          <strong>{{ model.label }}</strong>
                          <small>{{ modelOptionDescription(model) }}</small>
                        </span>
                        <span v-if="form.textAnalysisModel === model.value" class="home-popover__check" aria-hidden="true">
                          <svg viewBox="0 0 20 20" fill="none">
                            <path d="M4.5 10.5 8.2 14.2 15.5 5.8" />
                          </svg>
                        </span>
                      </button>
                    </section>
                    <section class="home-popover-section">
                      <p class="home-popover__label">图片模型</p>
                      <button
                        v-for="model in imageModelOptions"
                        :key="model.value"
                        type="button"
                        class="home-popover__item"
                        :class="{ 'home-popover__item-active': form.imageModel === model.value }"
                        @click="form.imageModel = model.value"
                      >
                        <span class="home-popover__icon">图</span>
                        <span>
                          <strong>{{ model.label }}</strong>
                          <small>{{ modelOptionDescription(model) }}</small>
                        </span>
                        <span v-if="form.imageModel === model.value" class="home-popover__check" aria-hidden="true">
                          <svg viewBox="0 0 20 20" fill="none">
                            <path d="M4.5 10.5 8.2 14.2 15.5 5.8" />
                          </svg>
                        </span>
                      </button>
                    </section>
                  </template>
                </div>
              </transition>
            </div>

            <div class="home-menu">
              <button type="button" class="home-tool" :class="{ 'home-tool-active': activeMenu === 'ratio' }" @click="toggleMenu('ratio')">
                <span class="home-tool__shape"></span>
                {{ ratioToolLabel }}
              </button>
              <transition name="home-popover-float">
                <div v-if="activeMenu === 'ratio'" class="home-popover home-popover-ratio">
                  <section class="home-popover-section">
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
                  </section>
                  <template v-if="selectedMode.kind === 'image'">
                    <section class="home-popover-section">
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
                    </section>
                    <section v-if="selectedImageSizeDimensions" class="home-popover-section">
                      <p class="home-popover__label">尺寸</p>
                      <div class="home-dimension-row">
                        <strong data-label="W">{{ selectedImageSizeDimensions.width }}</strong>
                        <span class="home-dimension-link">⌁</span>
                        <strong data-label="H">{{ selectedImageSizeDimensions.height }}</strong>
                        <span>PX</span>
                      </div>
                    </section>
                  </template>
                  <template v-else>
                    <section class="home-popover-section">
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
                    </section>
                    <section v-if="selectedVideoSizeDimensions" class="home-popover-section">
                      <p class="home-popover__label">尺寸</p>
                      <div class="home-dimension-row">
                        <strong data-label="W">{{ selectedVideoSizeDimensions.width }}</strong>
                        <span class="home-dimension-link">⌁</span>
                        <strong data-label="H">{{ selectedVideoSizeDimensions.height }}</strong>
                        <span>PX</span>
                      </div>
                    </section>
                  </template>
                </div>
              </transition>
            </div>

            <div v-if="selectedMode.kind === 'video'" class="home-menu">
              <button type="button" class="home-tool" :class="{ 'home-tool-active': activeMenu === 'duration' }" @click="toggleMenu('duration')">
                <span class="home-tool__icon">◷</span>
                {{ durationLabel }}
              </button>
              <transition name="home-popover-float">
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
              </transition>
            </div>

            <div v-if="selectedModeValue !== 'character_sheet'" class="home-menu">
              <button type="button" class="home-tool" :class="{ 'home-tool-active': activeMenu === 'count' }" @click="toggleMenu('count')">
                ✦ {{ selectedMode.kind === "image" ? `${imageOutputCount} / 张` : outputCountLabel }}
              </button>
              <transition name="home-popover-float">
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
              </transition>
            </div>

            <div class="home-menu">
              <button type="button" class="home-tool" :class="{ 'home-tool-active': activeMenu === 'mention' }" @click="toggleMenu('mention')">
                <span class="home-tool__icon">@</span>
                引用
              </button>
              <transition name="home-popover-float">
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
              </transition>
            </div>

            <div class="home-menu">
              <button type="button" class="home-tool" :class="{ 'home-tool-active': activeMenu === 'seed' }" @click="toggleMenu('seed')">
                <span class="home-tool__icon">#</span>
                {{ seedMode === "auto" ? "自动种子" : "手动种子" }}
              </button>
              <transition name="home-popover-float">
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
              </transition>
            </div>
          </div>

          <div class="home-composer__meta">
            <span v-if="creditLabel" class="home-credit-pill" :class="{ 'home-credit-pill-exempt': credits?.exempt }">
              {{ creditLabel }}
            </span>
            <RouterLink v-if="createdTaskId" :to="{ name: 'tasks', query: { selected: createdTaskId } }">查看任务</RouterLink>
          </div>
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

    <Transition name="home-toast-slide">
      <div v-if="taskToastTaskId" class="home-task-toast" role="status">
        <span>任务已提交，可在任务管理查看进度</span>
        <RouterLink :to="{ name: 'tasks', query: { selected: taskToastTaskId } }">查看任务</RouterLink>
        <button type="button" aria-label="关闭任务提示" @click="dismissTaskToast">×</button>
      </div>
    </Transition>

    <section v-if="activeTasks.length" class="home-active-tasks" aria-label="进行中的任务">
      <RouterLink
        v-for="task in activeTasks"
        :key="task.id"
        class="home-active-task-card"
        :to="{ name: 'tasks', query: { selected: task.id } }"
      >
        <div class="home-active-task-card__top">
          <span class="home-active-task-card__type">{{ task.aspectRatio || "生成任务" }}</span>
          <span class="home-active-task-card__status">{{ formatTaskStatus(task.status) }}</span>
        </div>
        <h2>{{ task.title }}</h2>
        <p>{{ activeTaskStageLabel(task) }}</p>
        <div class="home-active-task-card__progress" aria-hidden="true">
          <span :style="{ width: `${activeTaskProgress(task)}%` }"></span>
        </div>
        <div class="home-active-task-card__meta">
          <span>{{ activeTaskProgress(task) }}%</span>
          <span>{{ formatActiveTaskTime(task.updatedAt || task.createdAt) }}</span>
        </div>
      </RouterLink>
    </section>

  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { requireAuth } from "@/auth/modal";
import { useAuthSessionState } from "@/auth/session";
import { usePolling } from "@/composables/usePolling";
import { createGenerationTask, fetchCreditSummary, fetchGenerationOptions, fetchTasks, uploadText } from "@/features/home";
import { formatApiErrorMessage } from "@/utils/api-error";
import { formatVideoSizeLabel } from "@/utils/presentation";
import { formatTaskStatus } from "@/utils/task";
import { shouldStopBeforeVideoGeneration } from "@/workbench/developer-settings";
import type {
  CreateGenerationTaskRequest,
  CreditSummary,
  GenerationImageSizeOption,
  GenerationOptionsResponse,
  GenerationTextAnalysisModelInfo,
  GenerationVideoDurationOption,
  GenerationVideoModelInfo,
  GenerationVideoSizeOption,
  TaskListItem,
  TaskStatus,
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

const authState = useAuthSessionState();

const activeMenu = ref<MenuKey>("");
const selectedModeValue = ref<ModeValue>("image");
const loadingOptions = ref(false);
const submitting = ref(false);
const uploadingReference = ref(false);
const credits = ref<CreditSummary | null>(null);
const referenceDevelopingDialogOpen = ref(false);
const statusText = ref("参数加载中...");
const promptText = ref("");
const referenceExpanded = ref(false);
const composingPrompt = ref(false);
const syncingPromptFromEditor = ref(false);
const promptEditorFocused = ref(false);
const promptEditor = ref<HTMLDivElement | null>(null);
const textFileInput = ref<HTMLInputElement | null>(null);
const referenceImages = ref<ReferenceImageItem[]>([]);
const createdTaskId = ref("");
const taskToastTaskId = ref("");
let taskToastTimer: number | null = null;
const seedMode = ref<"auto" | "manual">("auto");
const seedInput = ref("");
const autoSeed = ref(createRandomSeed());
const durationMode = ref<"auto" | "manual">("auto");
const selectedDurationSeconds = ref<number | null>(null);
const imageOutputCount = ref(1);
const options = ref<GenerationOptionsResponse | null>(null);
const activeTasks = ref<TaskListItem[]>([]);
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

const modeIconSvgs = {
  video: `
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="4.5" y="5.5" width="10" height="13" rx="3" />
      <path d="m14.5 10 4.5-2.8v9.6L14.5 14" />
    </svg>
  `,
  image: `
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="4.5" y="5.5" width="15" height="13" rx="3" />
      <path d="M8 14.5 10.8 11.7 13.3 14.2 15.3 12.2 18 14.9" />
      <circle cx="10" cy="9.4" r="1.3" />
    </svg>
  `,
  character_sheet: `
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M7.2 7.6a2.8 2.8 0 1 1 5.6 0a2.8 2.8 0 0 1-5.6 0Z" />
      <path d="M4.8 17.2c.8-2.4 2.7-3.6 5.2-3.6s4.4 1.2 5.2 3.6" />
      <path d="M17.6 6.7v10.6" />
      <path d="M15.8 8.6h3.6" />
      <path d="M15.8 11.9h3.6" />
      <path d="M15.8 15.2h3.6" />
    </svg>
  `,
} as const;

const ACTIVE_TASK_STATUSES = new Set<TaskStatus>(["PENDING", "ANALYZING", "PLANNING", "RENDERING"]);

const modeOptions = [
  {
    value: "video" as const,
    kind: "video" as const,
    label: "视频生成",
    description: "输入文本，自动拆分脚本、关键帧和视频",
    iconSvg: modeIconSvgs.video,
  },
  {
    value: "image" as const,
    kind: "image" as const,
    label: "图片生成",
    description: "素材中心自由模式，支持参考图再创作",
    iconSvg: modeIconSvgs.image,
  },
  {
    value: "character_sheet" as const,
    kind: "image" as const,
    label: "角色三视图",
    description: "生成同一角色正面、侧面、背面设定图",
    iconSvg: modeIconSvgs.character_sheet,
  },
];

const selectedMode = computed(() => modeOptions.find((item) => item.value === selectedModeValue.value) ?? modeOptions[0]);
const promptLabel = computed(() => selectedMode.value.kind === "video" ? "文本 / 小说正文" : "图片提示词");
const showPromptPlaceholder = computed(() => !promptText.value.trim() && !promptEditorFocused.value && !composingPrompt.value);
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
const creditLabel = computed(() => {
  if (!authState.isAuthenticated.value || !credits.value) {
    return "";
  }
  if (credits.value.exempt) {
    return "积分免扣";
  }
  return `积分 ${formatCreditBalance(credits.value.balance ?? 0)}`;
});

const REFERENCE_PREVIEW_WIDTH = 68;
const REFERENCE_PREVIEW_HEIGHT = 98;
const REFERENCE_COLLAPSED_WIDTH = 58;
const REFERENCE_COLLAPSED_HEIGHT = 84;
const REFERENCE_EXPANDED_MAX_TILT_DEG = 30;
const REFERENCE_EXPANDED_GAP = 8;
const REFERENCE_EXPANDED_BOTTOM = 8;
const REFERENCE_ADD_CARD_OFFSET = 86;

function modelOptionDescription(model: { description?: string | null; provider?: string | null; family?: string | null; value: string }) {
  return model.description || model.provider || model.family || model.value;
}

function formatCreditBalance(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(2).replace(/\.?0+$/, "");
}

const ratioDisplayOrder: RatioOptionValue[] = ["智能", "21:9", "16:9", "3:2", "4:3", "1:1", "3:4", "2:3", "9:16"];
const sizeRatioCandidates: RatioOptionValue[] = ["21:9", "16:9", "3:2", "4:3", "1:1", "3:4", "2:3", "9:16"];

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
  const available = unique.length ? unique : availableVideoRatios.value;
  return ["智能", ...available.filter((value) => value !== "智能")];
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
  return filtered;
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
const selectedVideoSizeOption = computed(() => {
  return videoSizeOptions.value.find((item) => item.value === form.value.videoSize) ?? null;
});
const selectedVideoSizeDimensions = computed(() => {
  return selectedVideoSizeOption.value ? parseSize(selectedVideoSizeOption.value) : null;
});
const selectedMaterialAssetType = computed(() => selectedMode.value.value === "character_sheet" ? "character_sheet" : "free");
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

function toggleMenu(menu: Exclude<MenuKey, "">) {
  activeMenu.value = activeMenu.value === menu ? "" : menu;
}

function handleDocumentPointerDown(event: PointerEvent) {
  if (!activeMenu.value) {
    return;
  }
  const target = event.target instanceof Element ? event.target : null;
  if (target?.closest(".home-menu, .hero-mode-button")) {
    return;
  }
  activeMenu.value = "";
}

function handleDocumentKeydown(event: KeyboardEvent) {
  if (event.key === "Escape") {
    activeMenu.value = "";
  }
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

function handleReferenceUploadPointerEnter() {
  if (selectedMode.value.kind === "image" && referenceImages.value.length > 0) {
    referenceExpanded.value = true;
  }
}

function handleReferenceUploadPointerLeave() {
  referenceExpanded.value = false;
}

function referenceUploadSceneStyle() {
  if (referenceImages.value.length <= 1) {
    return referenceExpanded.value
      ? {
          width: `${REFERENCE_PREVIEW_WIDTH + REFERENCE_ADD_CARD_OFFSET}px`,
          height: `${REFERENCE_PREVIEW_HEIGHT}px`,
        }
      : undefined;
  }
  if (!referenceExpanded.value) {
    return undefined;
  }
  const step = referenceExpandedStep();
  const cardWidth = REFERENCE_PREVIEW_WIDTH;
  const addCardLeft = referenceImages.value.length * step;
  return {
    width: `${addCardLeft + cardWidth}px`,
    height: "112px",
  };
}

function referencePreviewRotation(index: number, expanded: boolean) {
  if (expanded) {
    const expandedRotations = [-9, 6, -7, 8, -5, 7, -8, 5, -6, 9, -4, 6];
    return expandedRotations[index % expandedRotations.length];
  }
  const collapsedRotations = [-7, 4, -5, 6, -4, 5];
  return collapsedRotations[index % collapsedRotations.length];
}

function referenceExpandedStep() {
  const radians = REFERENCE_EXPANDED_MAX_TILT_DEG * Math.PI / 180;
  const projectedWidth = REFERENCE_PREVIEW_WIDTH * Math.cos(radians) + REFERENCE_PREVIEW_HEIGHT * Math.sin(radians);
  return Math.ceil(projectedWidth + REFERENCE_EXPANDED_GAP);
}

function referenceRotationBottomDelta(rotateDeg: number) {
  return Math.sin(Math.abs(rotateDeg) * Math.PI / 180) * (REFERENCE_PREVIEW_WIDTH / 2);
}

function referenceExpandedBottom(rotateDeg: number) {
  const firstDelta = referenceRotationBottomDelta(referencePreviewRotation(0, true));
  const currentDelta = referenceRotationBottomDelta(rotateDeg);
  return `${REFERENCE_EXPANDED_BOTTOM - firstDelta + currentDelta}px`;
}

function referencePreviewImageStyle(index: number) {
  const total = referenceImages.value.length;
  if (total <= 1) {
    const rotate = -8;
    return {
      left: "0px",
      top: "0px",
      bottom: "auto",
      width: `${REFERENCE_PREVIEW_WIDTH}px`,
      height: `${REFERENCE_PREVIEW_HEIGHT}px`,
      opacity: "1",
      zIndex: "1",
      "--preview-rotate": `${rotate}deg`,
      "--preview-remove-rotate": `${-rotate}deg`,
      transformOrigin: "center bottom",
      transform: `rotate(${rotate}deg)`,
    };
  }

  if (referenceExpanded.value) {
    const step = referenceExpandedStep();
    const rotate = referencePreviewRotation(index, true);
    return {
      left: `${index * step}px`,
      top: "auto",
      bottom: referenceExpandedBottom(rotate),
      width: `${REFERENCE_PREVIEW_WIDTH}px`,
      height: `${REFERENCE_PREVIEW_HEIGHT}px`,
      opacity: "1",
      zIndex: String(index + 1),
      "--preview-rotate": `${rotate}deg`,
      "--preview-remove-rotate": `${-rotate}deg`,
      transformOrigin: "center bottom",
      transform: `rotate(${rotate}deg)`,
    };
  }

  const visibleIndex = Math.min(index, 4);
  const rotate = referencePreviewRotation(visibleIndex, false);
  return {
    left: `${-6 + visibleIndex * 8}px`,
    top: `${4 - Math.min(visibleIndex, 2) * 2}px`,
    bottom: "auto",
    width: `${REFERENCE_COLLAPSED_WIDTH}px`,
    height: `${REFERENCE_COLLAPSED_HEIGHT}px`,
    opacity: index < 4 ? "0.96" : "0",
    zIndex: String(index + 1),
    "--preview-rotate": `${rotate}deg`,
    "--preview-remove-rotate": `${-rotate}deg`,
    transformOrigin: "center bottom",
    transform: `rotate(${rotate}deg)`,
  };
}

function referenceAddCardStyle() {
  if (referenceImages.value.length <= 1) {
    if (!referenceExpanded.value) {
      return undefined;
    }
    return {
      left: `${REFERENCE_ADD_CARD_OFFSET}px`,
      top: "0px",
      bottom: "auto",
    };
  }
  if (!referenceExpanded.value) {
    return undefined;
  }
  const firstDelta = referenceRotationBottomDelta(referencePreviewRotation(0, true));
  return {
    left: `${referenceImages.value.length * referenceExpandedStep()}px`,
    top: "auto",
    bottom: `${REFERENCE_EXPANDED_BOTTOM - firstDelta}px`,
  };
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
  const previousItems = referenceImages.value;
  const nextItems = referenceImages.value
    .filter((item) => item.id !== id)
    .map((item, index) => ({
      ...item,
      label: `图片${index + 1}`,
    }));
  referenceImages.value = nextItems;
  promptText.value = remapReferenceMentions(promptText.value, previousItems, nextItems);
  if (!nextItems.length) {
    referenceExpanded.value = false;
  }
}

function insertMention(label: string) {
  const mention = `@${label}`;
  if (!promptText.value.includes(mention)) {
    promptText.value = promptText.value.trim() ? `${promptText.value.trim()} ${mention} ` : `${mention} `;
  }
  activeMenu.value = "";
  renderPromptEditor(promptText.value);
  nextTick(() => focusPromptEditorToEnd());
}

function remapReferenceMentions(text: string, previousItems: ReferenceImageItem[], nextItems: ReferenceImageItem[]) {
  const nextLabelById = new Map(nextItems.map((item) => [item.id, item.label]));
  const nextLabelByPreviousLabel = new Map(
    previousItems.map((item) => [item.label, nextLabelById.get(item.id) ?? null]),
  );
  return text.replace(/@图片\d+/g, (token) => {
    const nextLabel = nextLabelByPreviousLabel.get(token.slice(1));
    if (nextLabel === undefined) {
      return token;
    }
    return nextLabel ? `@${nextLabel}` : "";
  });
}

function serializePromptEditorNode(node: Node, parentTag = ""): string {
  if (node.nodeType === Node.TEXT_NODE) {
    return node.textContent ?? "";
  }
  if (!(node instanceof HTMLElement)) {
    return "";
  }
  const mentionLabel = node.dataset.mentionLabel;
  if (mentionLabel) {
    return `@${mentionLabel}`;
  }
  if (node.tagName === "BR") {
    return "\n";
  }
  let text = "";
  node.childNodes.forEach((child) => {
    text += serializePromptEditorNode(child, node.tagName);
  });
  if ((node.tagName === "DIV" || node.tagName === "P") && parentTag !== "DIV" && parentTag !== "P") {
    return `${text}\n`;
  }
  return text;
}

function serializePromptEditorContent() {
  const editor = promptEditor.value;
  if (!editor) {
    return promptText.value;
  }
  return serializePromptEditorNode(editor)
    .replace(/\u00a0/g, " ")
    .replace(/\u200b/g, "")
    .replace(/\n$/, "");
}

function buildMentionChip(item: ReferenceImageItem) {
  const chip = document.createElement("span");
  chip.className = "home-reference-pill home-reference-pill-inline";
  chip.setAttribute("contenteditable", "false");
  chip.dataset.mentionLabel = item.label;
  chip.style.display = "inline-flex";
  chip.style.alignItems = "center";
  chip.style.gap = "6px";
  chip.style.maxWidth = "112px";
  chip.style.height = "24px";
  chip.style.margin = "0 0.2em";
  chip.style.verticalAlign = "middle";
  chip.style.whiteSpace = "nowrap";
  chip.style.pointerEvents = "none";

  const thumb = document.createElement("span");
  thumb.className = "home-reference-pill__thumb";
  thumb.style.display = "inline-block";
  thumb.style.flex = "0 0 auto";
  thumb.style.width = "24px";
  thumb.style.height = "24px";
  thumb.style.overflow = "hidden";
  thumb.style.borderRadius = "6px";
  const image = document.createElement("img");
  image.src = item.fileUrl;
  image.alt = item.label;
  image.style.display = "block";
  image.style.width = "100%";
  image.style.height = "100%";
  image.style.objectFit = "cover";
  thumb.appendChild(image);

  const label = document.createElement("span");
  label.className = "home-reference-pill__label";
  label.textContent = item.label;
  label.style.display = "inline-block";
  label.style.minWidth = "0";
  label.style.overflow = "hidden";
  label.style.textOverflow = "ellipsis";
  label.style.whiteSpace = "nowrap";
  label.style.color = "#657487";
  label.style.fontSize = "0.78rem";
  label.style.fontWeight = "600";
  label.style.lineHeight = "1";
  label.style.alignSelf = "center";

  chip.appendChild(thumb);
  chip.appendChild(label);
  return chip;
}

function renderPromptEditor(value: string) {
  const editor = promptEditor.value;
  if (!editor) {
    return;
  }
  const selectionOffset = getPromptSelectionOffset(editor);
  const referenceImageByLabel = new Map(referenceImages.value.map((item) => [item.label, item]));
  const fragment = document.createDocumentFragment();
  const mentionPattern = /@图片\d+/g;
  let lastIndex = 0;
  let matched = mentionPattern.exec(value);
  while (matched) {
    if (matched.index > lastIndex) {
      fragment.appendChild(document.createTextNode(value.slice(lastIndex, matched.index)));
    }
    const mention = matched[0];
    const label = mention.slice(1);
    const item = referenceImageByLabel.get(label);
    if (item) {
      fragment.appendChild(buildMentionChip(item));
    } else {
      fragment.appendChild(document.createTextNode(mention));
    }
    lastIndex = matched.index + mention.length;
    matched = mentionPattern.exec(value);
  }
  if (lastIndex < value.length) {
    fragment.appendChild(document.createTextNode(value.slice(lastIndex)));
  }
  if (!fragment.childNodes.length) {
    fragment.appendChild(document.createElement("br"));
  }
  editor.replaceChildren(fragment);
  if (selectionOffset !== null) {
    restorePromptSelection(editor, selectionOffset);
  }
}

function getPromptSelectionOffset(editor: HTMLDivElement) {
  const selection = window.getSelection();
  if (!selection?.rangeCount) {
    return null;
  }
  const range = selection.getRangeAt(0);
  if (!editor.contains(range.startContainer)) {
    return null;
  }
  const probe = range.cloneRange();
  probe.selectNodeContents(editor);
  probe.setEnd(range.startContainer, range.startOffset);
  const container = document.createElement("div");
  container.appendChild(probe.cloneContents());
  return serializePromptEditorNode(container).replace(/\u00a0/g, " ").replace(/\u200b/g, "").length;
}

function restorePromptSelection(editor: HTMLDivElement, targetOffset: number) {
  const range = document.createRange();
  const selection = window.getSelection();
  let remaining = targetOffset;
  let placed = false;
  const nodes = Array.from(editor.childNodes);
  for (let index = 0; index < nodes.length; index += 1) {
    const node = nodes[index];
    if (node.nodeType === Node.TEXT_NODE) {
      const content = node.textContent ?? "";
      if (remaining <= content.length) {
        range.setStart(node, remaining);
        placed = true;
        break;
      }
      remaining -= content.length;
      continue;
    }
    if (node instanceof HTMLElement && node.dataset.mentionLabel) {
      const mentionLength = `@${node.dataset.mentionLabel}`.length;
      if (remaining <= mentionLength) {
        if (remaining === 0) {
          range.setStartBefore(node);
        } else {
          range.setStartAfter(node);
        }
        placed = true;
        break;
      }
      remaining -= mentionLength;
      continue;
    }
    if (node instanceof HTMLBRElement) {
      if (remaining <= 1) {
        range.setStartBefore(node);
        placed = true;
        break;
      }
      remaining -= 1;
    }
  }
  if (!placed) {
    range.selectNodeContents(editor);
    range.collapse(false);
  }
  range.collapse(true);
  selection?.removeAllRanges();
  selection?.addRange(range);
}

function focusPromptEditorToEnd() {
  const editor = promptEditor.value;
  if (!editor) {
    return;
  }
  editor.focus();
  restorePromptSelection(editor, serializePromptEditorContent().length);
}

function insertPlainTextAtSelection(text: string) {
  const editor = promptEditor.value;
  const selection = window.getSelection();
  if (!editor || !selection?.rangeCount) {
    return;
  }
  const range = selection.getRangeAt(0);
  if (!editor.contains(range.startContainer)) {
    focusPromptEditorToEnd();
    return insertPlainTextAtSelection(text);
  }
  range.deleteContents();
  const node = document.createTextNode(text);
  range.insertNode(node);
  range.setStart(node, text.length);
  range.collapse(true);
  selection.removeAllRanges();
  selection.addRange(range);
}

function syncPromptTextFromEditor() {
  syncingPromptFromEditor.value = true;
  promptText.value = serializePromptEditorContent();
  nextTick(() => {
    syncingPromptFromEditor.value = false;
  });
}

function handlePromptEditorInput(event: InputEvent) {
  if (event?.isComposing) {
    composingPrompt.value = true;
    return;
  }
  if (event.inputType === "insertCompositionText") {
    return;
  }
  if (composingPrompt.value) {
    return;
  }
  syncPromptTextFromEditor();
}

function handlePromptEditorFocus() {
  promptEditorFocused.value = true;
}

function handlePromptEditorBlur() {
  promptEditorFocused.value = false;
  renderPromptEditor(promptText.value);
}

function handlePromptEditorCompositionStart() {
  composingPrompt.value = true;
}

function handlePromptEditorCompositionEnd() {
  composingPrompt.value = false;
  syncPromptTextFromEditor();
}

function handlePromptEditorBeforeInput(event: InputEvent) {
  if (event.isComposing || event.inputType === "insertCompositionText") {
    composingPrompt.value = true;
  }
}

function handlePromptEditorKeydown(event: KeyboardEvent) {
  if (composingPrompt.value || event.isComposing) {
    return;
  }
  if (event.key !== "Enter") {
    return;
  }
  event.preventDefault();
  insertPlainTextAtSelection("\n");
  syncPromptTextFromEditor();
  nextTick(() => renderPromptEditor(promptText.value));
}

function handlePromptEditorPaste(event: ClipboardEvent) {
  if (composingPrompt.value) {
    return;
  }
  event.preventDefault();
  const text = event.clipboardData?.getData("text/plain") ?? "";
  insertPlainTextAtSelection(text);
  syncPromptTextFromEditor();
  nextTick(() => renderPromptEditor(promptText.value));
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
  if (ratio === "智能") {
    return true;
  }
  const itemRatio = sizeRatioLabel(item);
  return itemRatio === ratio;
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
  const actual = parsed.width / parsed.height;
  const best = sizeRatioCandidates
    .map((value) => {
      const target = ratioValue(value);
      return target ? { value, delta: Math.abs(actual - target) / target } : null;
    })
    .filter((item): item is { value: RatioOptionValue; delta: number } => Boolean(item))
    .sort((a, b) => a.delta - b.delta)[0];
  return best && best.delta <= 0.03 ? best.value : null;
}

function ratioShape(value: RatioOptionValue) {
  if (value === "智能") {
    return "1 / 1";
  }
  return value.replace(":", " / ");
}

function ratioValue(value: RatioOptionValue) {
  if (value === "智能") {
    return null;
  }
  const [width, height] = value.split(":").map(Number);
  if (!Number.isFinite(width) || !Number.isFinite(height) || height <= 0) {
    return null;
  }
  return width / height;
}

function videoAspectRatio(value: RatioOptionValue): AspectRatioValue {
  return value === "9:16" ? "9:16" : "16:9";
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
  const quality = label.includes("4K") ? `${label} ✦` : label;
  const ratio = sizeRatioLabel(item);
  return form.value.aspectRatio === "智能" && ratio ? `${quality} · ${ratio}` : quality;
}

function resolvedImageAspectRatioForSubmit() {
  if (form.value.aspectRatio !== "智能") {
    return form.value.aspectRatio;
  }
  return selectedImageSizeOption.value ? sizeRatioLabel(selectedImageSizeOption.value) ?? "1:1" : "1:1";
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
    statusText.value = "";
  } catch (error) {
    statusText.value = error instanceof Error ? error.message : "加载模型配置失败";
  } finally {
    loadingOptions.value = false;
  }
}

async function loadCredits() {
  if (!authState.isAuthenticated.value) {
    credits.value = null;
    return;
  }
  try {
    credits.value = await fetchCreditSummary();
  } catch {
    credits.value = null;
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

function taskTimestamp(value?: string | null) {
  const timestamp = value ? new Date(value).getTime() : Number.NaN;
  return Number.isFinite(timestamp) ? timestamp : 0;
}

function activeTaskProgress(task: TaskListItem) {
  return Math.max(0, Math.min(100, Math.round(task.progress ?? 0)));
}

function formatActiveTaskTime(value?: string | null) {
  if (!value) {
    return "暂无时间";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function activeTaskStageLabel(task: TaskListItem) {
  if (task.currentStage?.trim()) {
    return task.currentStage.trim();
  }
  if (task.status === "PENDING") {
    return typeof task.queuePosition === "number" && task.queuePosition > 0 ? `队列第 ${task.queuePosition} 位` : "等待开始";
  }
  return formatTaskStatus(task.status);
}

async function loadActiveTasks() {
  if (!authState.isAuthenticated.value) {
    activeTasks.value = [];
    return;
  }
  try {
    const tasks = await fetchTasks({ sort: "updated_desc" });
    activeTasks.value = tasks
      .filter((task) => ACTIVE_TASK_STATUSES.has(task.status))
      .sort((left, right) => taskTimestamp(right.updatedAt || right.createdAt) - taskTimestamp(left.updatedAt || left.createdAt))
      .slice(0, 12);
  } catch {
    activeTasks.value = [];
  }
}

const activeTasksPolling = usePolling(loadActiveTasks, 5000);

watch(
  () => [selectedModeValue.value, imageSizeOptions.value] as const,
  ([, items]) => {
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

watch(promptText, (value) => {
  if (composingPrompt.value || syncingPromptFromEditor.value) {
    return;
  }
  const editor = promptEditor.value;
  if (document.activeElement === editor) {
    return;
  }
  renderPromptEditor(value);
});

watch(referenceImages, () => {
  if (composingPrompt.value) {
    return;
  }
  renderPromptEditor(promptText.value);
}, { deep: true });

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
  try {
    if (selectedMode.value.kind === "image") {
      await submitImageGeneration();
    } else {
      await submitVideoGeneration();
    }
  } catch (error) {
    statusText.value = formatApiErrorMessage(error, "创建失败");
  } finally {
    submitting.value = false;
  }
}

async function submitImageGeneration() {
  const isCharacterSheet = selectedMaterialAssetType.value === "character_sheet";
  const taskType = isCharacterSheet
    ? "character_sheet"
    : referenceImages.value.length
      ? "image_to_image"
      : "image_generation";
  const task = await createGenerationTask({
    title: promptText.value.trim().slice(0, 32) || (isCharacterSheet ? "角色三视图" : "图片生成"),
    taskType,
    assetType: selectedMaterialAssetType.value,
    creativePrompt: promptText.value.trim(),
    aspectRatio: resolvedImageAspectRatioForSubmit(),
    imageSize: form.value.imageSize || null,
    textAnalysisModel: form.value.textAnalysisModel || null,
    imageModel: form.value.imageModel || null,
    videoModel: null,
    videoSize: null,
    outputCount: 1,
    seed: selectedImageModelOption.value?.supportsSeed
      ? (seedMode.value === "manual" ? parsedManualSeed.value : autoSeed.value)
      : null,
    referenceImageUrls: referenceImages.value.map((item) => item.fileUrl),
    referenceAssetIds: [],
    transcriptText: "",
    stopBeforeVideoGeneration: false,
  });
  createdTaskId.value = task.id;
  showTaskToast(task.id);
  statusText.value = "任务已提交，可在任务管理查看进度。";
  void loadActiveTasks();
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
  showTaskToast(task.id);
  statusText.value = "任务已提交，可在任务管理查看进度。";
  void loadActiveTasks();
}

function showTaskToast(taskId: string) {
  taskToastTaskId.value = taskId;
  if (taskToastTimer !== null) {
    window.clearTimeout(taskToastTimer);
  }
  taskToastTimer = window.setTimeout(() => {
    taskToastTaskId.value = "";
    taskToastTimer = null;
  }, 5000);
}

function dismissTaskToast() {
  taskToastTaskId.value = "";
  if (taskToastTimer !== null) {
    window.clearTimeout(taskToastTimer);
    taskToastTimer = null;
  }
}

onMounted(() => {
  loadOptions();
  loadCredits();
  void activeTasksPolling.start();
  document.addEventListener("pointerdown", handleDocumentPointerDown);
  document.addEventListener("keydown", handleDocumentKeydown);
  renderPromptEditor(promptText.value);
});

watch(() => authState.isAuthenticated.value, () => {
  loadCredits();
});

onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", handleDocumentPointerDown);
  document.removeEventListener("keydown", handleDocumentKeydown);
  dismissTaskToast();
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
  width: min(100%, 1280px);
  min-height: 186px;
  padding: 22px 66px 22px 126px;
  border: 1px solid rgba(15, 20, 25, 0.06);
  border-radius: 28px;
  background: #fff;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.96) inset,
    0 1px 2px rgba(15, 20, 25, 0.03),
    0 12px 28px rgba(20, 28, 36, 0.04);
}

.home-hidden-input {
  display: none;
}

.home-composer__upload {
  position: absolute;
  left: 28px;
  top: 24px;
  z-index: 4;
  display: grid;
  place-items: center;
  gap: 6px;
  width: 68px;
  height: 98px;
  border: 1px solid rgba(15, 20, 25, 0.04);
  border-radius: 6px;
  background: linear-gradient(180deg, #fafafa 0%, #f1f1f1 100%);
  color: #6f7c88;
  transform: rotate(-8deg);
  box-shadow: 0 8px 18px rgba(15, 20, 25, 0.06);
  cursor: pointer;
  overflow: visible;
  transition:
    background 220ms ease,
    box-shadow 220ms ease,
    border-color 220ms ease,
    transform 520ms cubic-bezier(0.22, 1, 0.36, 1);
}

.home-composer__upload-has-reference {
  background: transparent;
  border-color: transparent;
  box-shadow: none;
  transform: none;
}

.home-composer__upload-scene {
  position: absolute;
  left: 0;
  top: 0;
  display: block;
  width: 68px;
  height: 98px;
  transition:
    width 520ms cubic-bezier(0.22, 1, 0.36, 1),
    height 520ms cubic-bezier(0.22, 1, 0.36, 1);
}

.home-composer__upload-expanded .home-composer__upload-scene {
  width: 214px;
  height: 122px;
}

.home-composer__upload-preview,
.home-composer__upload-add-card {
  position: absolute;
  width: 68px;
  height: 98px;
  border-radius: 6px;
  transition:
    left 520ms cubic-bezier(0.22, 1, 0.36, 1),
    top 520ms cubic-bezier(0.22, 1, 0.36, 1),
    bottom 520ms cubic-bezier(0.22, 1, 0.36, 1),
    width 520ms cubic-bezier(0.22, 1, 0.36, 1),
    height 520ms cubic-bezier(0.22, 1, 0.36, 1),
    transform 520ms cubic-bezier(0.22, 1, 0.36, 1),
    opacity 320ms ease,
    box-shadow 320ms ease,
    border-color 320ms ease,
    background 320ms ease;
}

.home-composer__upload-preview {
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  overflow: visible;
  border: 0;
  background: transparent;
  box-shadow: none;
  transform: rotate(0deg);
}

.home-composer__upload-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 7px;
  box-shadow: 0 6px 14px rgba(15, 20, 25, 0.08);
}

.home-composer__upload-preview-image {
  position: absolute;
  border-radius: 7px;
  transition:
    left 520ms cubic-bezier(0.22, 1, 0.36, 1),
    top 520ms cubic-bezier(0.22, 1, 0.36, 1),
    bottom 520ms cubic-bezier(0.22, 1, 0.36, 1),
    width 520ms cubic-bezier(0.22, 1, 0.36, 1),
    height 520ms cubic-bezier(0.22, 1, 0.36, 1),
    transform 520ms cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 320ms ease,
    opacity 320ms ease;
}

.home-composer__upload-preview-image-remove {
  position: absolute;
  right: -5px;
  top: -9px;
  z-index: 8;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: rgba(41, 46, 53, 0.94);
  color: #fff;
  font-size: 0.9rem;
  font-weight: 500;
  line-height: 1;
  transform: rotate(var(--preview-remove-rotate, 0deg)) scale(0.84);
  transform-origin: center;
  opacity: 0;
  box-shadow:
    0 10px 24px rgba(15, 20, 25, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.08);
  transition:
    opacity 220ms ease,
    transform 320ms cubic-bezier(0.22, 1, 0.36, 1);
}

.home-composer__upload-add-card {
  left: 60px;
  top: 4px;
  z-index: 0;
  display: grid;
  place-items: center;
  border: 1px dashed rgba(15, 20, 25, 0.12);
  background: linear-gradient(180deg, #fafafa 0%, #f4f4f4 100%);
  color: #7f8b97;
  transform: translateX(-18px) rotate(6deg) scale(0.92);
  opacity: 0;
}

.home-composer__upload-has-multiple .home-composer__upload-preview {
  border-color: transparent;
  background: transparent;
  box-shadow: none;
}

.home-composer__upload-add-card span {
  font-size: 1.2rem;
  line-height: 1;
  font-weight: 700;
}

.home-composer__upload span {
  font-size: 1.72rem;
  line-height: 1;
  font-weight: 500;
}

.home-composer__upload > img {
  width: 46px;
  height: 46px;
  border-radius: 6px;
  object-fit: cover;
  transform: none;
}

.home-task-toast {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 70;
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: min(420px, calc(100vw - 32px));
  min-height: 48px;
  padding: 10px 12px 10px 16px;
  border: 1px solid rgba(15, 20, 25, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  color: var(--text-strong);
  box-shadow: 0 18px 42px rgba(15, 20, 25, 0.14);
  backdrop-filter: blur(12px);
}

.home-task-toast span {
  min-width: 0;
  font-size: 0.88rem;
  font-weight: 700;
}

.home-task-toast a {
  flex: 0 0 auto;
  color: var(--accent-cyan);
  font-size: 0.86rem;
  font-weight: 800;
  text-decoration: none;
}

.home-task-toast button {
  flex: 0 0 auto;
  color: var(--text-muted);
  font-size: 1.05rem;
  line-height: 1;
}

.home-toast-slide-enter-active,
.home-toast-slide-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.home-toast-slide-enter-from,
.home-toast-slide-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

.home-reference-add {
  position: absolute;
  right: -10px;
  bottom: 8px;
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(244, 245, 247, 0.96);
  color: #20262d;
  font-size: 1rem !important;
  font-weight: 500;
  transform: none;
  box-shadow:
    0 8px 18px rgba(15, 20, 25, 0.08),
    0 0 0 1px rgba(15, 20, 25, 0.05);
  transition:
    opacity 220ms ease,
    transform 320ms cubic-bezier(0.22, 1, 0.36, 1);
}

.home-composer__upload:not(.home-composer__upload-has-multiple) .home-reference-add {
  opacity: 0;
  pointer-events: none;
  transform: scale(0.84);
}

.home-composer__upload-expanded .home-composer__upload-add-card {
  opacity: 1;
  transform: translateX(0) rotate(0deg) scale(1);
  box-shadow: 0 8px 18px rgba(15, 20, 25, 0.06);
}

.home-composer__upload-preview-image:hover {
  z-index: 4;
  transform: rotate(var(--preview-rotate, 0deg)) translateY(-4px) !important;
}

.home-composer__upload-preview-image:hover .home-composer__upload-preview-image-remove {
  opacity: 1;
  transform: rotate(var(--preview-remove-rotate, 0deg)) scale(1);
}

.home-composer__upload-expanded .home-reference-add {
  opacity: 0;
  transform: scale(0.84);
}

.home-composer__body {
  display: grid;
  align-content: start;
  gap: 10px;
  min-height: 104px;
}

.home-reference-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 112px;
}

.home-reference-pill-inline {
  margin: 0 0.2em;
  vertical-align: middle;
  white-space: nowrap;
  pointer-events: none;
}

.home-reference-pill__thumb {
  flex: 0 0 auto;
  width: 24px;
  height: 24px;
  overflow: hidden;
  border-radius: 6px;
}

.home-reference-pill__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.home-reference-pill__label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #657487;
  font-size: 0.78rem;
  font-weight: 600;
  line-height: 1;
  align-self: center;
}

.home-composer__prompt {
  position: relative;
  display: block;
  min-height: 104px;
  outline: none;
}

.home-composer__prompt:focus-within,
.home-composer__prompt:focus-visible {
  outline: none;
}

.home-composer__placeholder {
  position: absolute;
  left: 0;
  top: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  max-width: calc(100% - 120px);
  color: #a6b0ba;
  font-size: 0.88rem;
  font-weight: 500;
  line-height: 1.5;
  pointer-events: none;
}

.home-composer__placeholder-tag {
  display: inline-grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border: 1px solid rgba(15, 20, 25, 0.07);
  border-radius: 10px;
  background: #fff;
  color: var(--accent-cyan);
  font-size: 1.04rem;
  font-weight: 800;
  box-shadow: 0 4px 10px rgba(15, 20, 25, 0.04);
}

.home-composer__editor {
  min-height: 104px;
  width: 100%;
  padding: 0 18px 0 0;
  border: 0;
  outline: none;
  background: transparent;
  color: var(--text-strong);
  font-size: 0.98rem;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  caret-color: var(--text-strong);
}

.home-composer__editor:focus,
.home-composer__editor:focus-visible {
  border: 0;
  outline: none;
  box-shadow: none;
}

.home-composer__editor:empty::before {
  content: "";
}

.home-composer__footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  margin-top: 14px;
}

.home-composer__toolbar {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.home-tool {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(15, 20, 25, 0.07);
  border-radius: 9px;
  background: linear-gradient(180deg, #fff 0%, #fcfcfc 100%);
  color: var(--text-strong);
  font-size: 0.84rem;
  font-weight: 700;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.95) inset,
    0 2px 8px rgba(15, 20, 25, 0.03);
  cursor: pointer;
  transition:
    background 180ms ease,
    border-color 180ms ease,
    box-shadow 180ms ease,
    color 180ms ease;
}

.home-tool-accent {
  color: #12202c;
}

.home-tool-active {
  border-color: rgba(15, 20, 25, 0.06);
  background: linear-gradient(180deg, #f4f4f4 0%, #ececec 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 8px 20px rgba(15, 20, 25, 0.04);
}

.home-tool__icon {
  display: inline-grid;
  place-items: center;
  width: 18px;
  height: 18px;
  color: currentColor;
  line-height: 0;
}

.home-tool__icon :deep(svg),
.home-popover__icon :deep(svg),
.home-popover__check svg {
  width: 100%;
  height: 100%;
}

.home-tool__icon :deep(svg),
.home-popover__icon :deep(svg) {
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.home-tool__shape {
  width: 18px;
  height: 18px;
  border: 2px solid currentColor;
  border-radius: 5px;
}

.home-menu {
  position: relative;
}

.home-popover-float-enter-active,
.home-popover-float-leave-active {
  transition:
    opacity 180ms ease,
    transform 220ms cubic-bezier(0.22, 1, 0.36, 1),
    filter 220ms ease;
  transform-origin: left top;
}

.home-popover-float-enter-from,
.home-popover-float-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.98);
  filter: blur(6px);
}

.home-popover {
  position: absolute;
  left: 0;
  top: calc(100% + 10px);
  z-index: 5;
  display: grid;
  gap: 12px;
  width: 360px;
  max-height: min(520px, calc(100vh - 120px));
  overflow-y: auto;
  padding: 14px;
  border: 1px solid rgba(15, 20, 25, 0.06);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow:
    0 16px 44px rgba(20, 28, 36, 0.12),
    0 3px 10px rgba(20, 28, 36, 0.05);
  backdrop-filter: blur(16px);
}

.home-popover-ratio {
  width: min(620px, calc(100vw - 48px));
  gap: 14px;
  padding: 16px;
  border-radius: 22px;
  box-shadow:
    0 24px 72px rgba(20, 28, 36, 0.18),
    0 6px 18px rgba(20, 28, 36, 0.08);
}

.home-popover-model {
  width: min(420px, calc(100vw - 48px));
}

.home-popover-compact,
.home-popover-seed {
  width: 300px;
}

.home-popover-section {
  display: grid;
  gap: 8px;
}

.home-popover__label {
  margin: 0 4px;
  color: #a4b0bd;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.home-popover__item {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) 22px;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 68px;
  border: 0;
  padding: 0 10px;
  border-radius: 16px;
  background: transparent;
  color: var(--text-strong);
  text-align: left;
  cursor: pointer;
  transition: background 180ms ease, color 180ms ease;
}

.home-popover__item-active {
  background: linear-gradient(180deg, #f3f3f3 0%, #ededed 100%);
}

.home-popover__icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 12px;
  background: linear-gradient(180deg, #f0f7fa 0%, #e7f1f5 100%);
  color: #1a8eb0;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
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
  font-size: 0.9rem;
  font-weight: 760;
}

.home-popover__item small,
.home-popover-seed small {
  margin-top: 3px;
  color: #738291;
  font-size: 0.77rem;
  line-height: 1.45;
}

.home-popover__check {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  color: #101820;
}

.home-popover__check svg {
  stroke: currentColor;
  stroke-width: 2.2;
  stroke-linecap: round;
  stroke-linejoin: round;
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
  grid-template-columns: repeat(auto-fit, minmax(68px, 1fr));
  align-items: stretch;
  gap: 4px;
  padding: 4px;
  border-radius: 18px;
  background: #f3f3f4;
}

.home-ratio-list button {
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 8px;
  min-height: 84px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  color: #222c35;
  font-size: 0.7rem;
  font-weight: 500;
  cursor: pointer;
  transition:
    background 180ms ease,
    box-shadow 200ms ease,
    transform 200ms ease,
    color 180ms ease;
}

.home-ratio-active,
.home-segment-active {
  background: #fff !important;
  color: var(--accent-cyan) !important;
  box-shadow: 0 1px 4px rgba(15, 20, 25, 0.08);
}

.home-ratio-active {
  color: #1f2831 !important;
  box-shadow:
    0 10px 28px rgba(20, 28, 36, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.94) !important;
  transform: translateY(-1px);
}

.home-ratio__shape {
  width: 22px;
  max-height: 22px;
  min-height: 10px;
  border: 1.8px solid currentColor;
  border-radius: 4px;
}

.home-resolution-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px;
  overflow: hidden;
  padding: 4px;
  border-radius: 18px;
  background: #f3f3f4;
}

.home-resolution-list button {
  min-height: 56px;
  padding: 0 10px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  color: #1f2831;
  font-size: 0.88rem;
  font-weight: 500;
  cursor: pointer;
  transition:
    background 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.home-resolution-active {
  background: #fff !important;
  color: #1f2831 !important;
  box-shadow:
    0 8px 22px rgba(20, 28, 36, 0.07),
    inset 0 1px 0 rgba(255, 255, 255, 0.94);
  transform: translateY(-1px);
}

.home-dimension-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 38px minmax(0, 1fr) 50px;
  align-items: center;
  gap: 8px;
}

.home-dimension-row span,
.home-dimension-row strong {
  min-height: 56px;
  display: grid;
  align-items: center;
  border-radius: 14px;
  background: #f3f3f4;
}

.home-dimension-row span {
  justify-items: center;
  color: #556473;
  font-size: 0.74rem;
  font-weight: 700;
}

.home-dimension-row strong {
  grid-template-columns: 48px minmax(0, 1fr);
  justify-items: stretch;
  padding: 0 16px;
  color: #1f2831;
  font-size: 0.92rem;
  font-weight: 560;
}

.home-dimension-row strong::before {
  content: attr(data-label);
  display: grid;
  align-items: center;
  color: #556473;
  font-size: 0.74rem;
  font-weight: 700;
}

.home-dimension-row .home-dimension-link {
  background: transparent;
  color: #63717d;
  font-size: 1.18rem;
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
  min-height: 38px;
  max-width: 320px;
  color: var(--text-muted);
  font-size: 0.78rem;
  justify-content: flex-end;
  text-align: right;
}

.home-composer__meta a {
  color: var(--accent-cyan);
  font-weight: 800;
}

.home-credit-pill {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border: 1px solid rgba(15, 20, 25, 0.08);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--text-strong);
  font-size: 0.76rem;
  font-weight: 800;
  white-space: nowrap;
}

.home-credit-pill-exempt {
  border-color: rgba(0, 150, 136, 0.18);
  background: rgba(232, 247, 243, 0.9);
  color: #087767;
}

.home-composer__submit {
  position: absolute;
  right: 20px;
  bottom: 22px;
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 50%;
  background: #111418;
  color: #fff;
  box-shadow: 0 10px 24px rgba(15, 20, 25, 0.12);
  cursor: pointer;
}

.home-composer__submit:not(:disabled) {
  background: #111418;
}

.home-composer__submit:disabled {
  cursor: not-allowed;
}

.home-composer__submit svg {
  width: 18px;
  height: 18px;
}

.home-active-tasks {
  display: flex;
  align-items: stretch;
  gap: 14px;
  width: min(100%, 1280px);
  margin: 14px auto 0;
  overflow-x: auto;
  padding: 2px 2px 10px;
  scroll-snap-type: x proximity;
}

.home-active-tasks::-webkit-scrollbar {
  height: 8px;
}

.home-active-tasks::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(15, 20, 25, 0.12);
}

.home-active-task-card {
  flex: 0 0 304px;
  display: grid;
  grid-template-rows: auto auto auto auto auto;
  gap: 10px;
  min-height: 166px;
  padding: 16px;
  border: 1px solid rgba(15, 20, 25, 0.06);
  border-radius: 18px;
  background: #fff;
  color: var(--text-strong);
  text-decoration: none;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.96) inset,
    0 8px 22px rgba(20, 28, 36, 0.045);
  scroll-snap-align: start;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.home-active-task-card:hover {
  transform: translateY(-2px);
  border-color: rgba(0, 161, 194, 0.22);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.96) inset,
    0 12px 30px rgba(20, 28, 36, 0.07);
}

.home-active-task-card__top,
.home-active-task-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.home-active-task-card__type,
.home-active-task-card__status {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
}

.home-active-task-card__type {
  background: #f5f7f8;
  color: #6a7785;
}

.home-active-task-card__status {
  background: #eef8fb;
  color: var(--accent-cyan);
}

.home-active-task-card h2 {
  display: -webkit-box;
  min-height: 44px;
  margin: 0;
  overflow: hidden;
  color: #15202b;
  font-size: 0.98rem;
  font-weight: 820;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.home-active-task-card p {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: #6a7785;
  font-size: 0.82rem;
  font-weight: 650;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-active-task-card__progress {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(15, 20, 25, 0.07);
}

.home-active-task-card__progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--accent-cyan);
  transition: width 240ms ease;
}

.home-active-task-card__meta {
  color: #7d8a97;
  font-size: 0.76rem;
  font-weight: 760;
}

@media (max-width: 1180px) {
  .home-page {
    padding: 44px 22px 36px;
  }
}

@media (max-width: 720px) {
  .home-page {
    padding: 28px 14px 32px;
  }

  .home-hero {
    gap: 28px;
  }

  .home-composer {
    min-height: 0;
    padding: 18px 62px 18px 18px;
    border-radius: 24px;
  }

  .home-composer__toolbar,
  .home-composer__meta {
    width: 100%;
  }

  .home-composer__upload {
    position: static;
    margin-bottom: 12px;
    width: 64px;
    height: 92px;
    transform: rotate(-6deg);
  }

  .home-composer__upload-has-reference {
    width: 64px;
  }

  .home-composer__upload-add-card,
  .home-composer__upload-preview-image-remove {
    display: none;
  }

  .home-composer__submit {
    right: 16px;
    bottom: 18px;
    width: 40px;
    height: 40px;
  }

  .home-composer__body {
    min-height: 0;
  }

  .home-composer__prompt {
    min-height: 104px;
  }

  .home-composer__editor {
    min-height: 104px;
    padding-right: 0;
  }

  .home-composer__placeholder {
    max-width: 100%;
    font-size: 1rem;
    line-height: 1.6;
  }

  .home-composer__placeholder-tag {
    width: 30px;
    height: 30px;
    border-radius: 10px;
    font-size: 1.05rem;
  }

  .home-composer__footer {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .home-composer__toolbar {
    padding-right: 54px;
  }

  .home-tool {
    min-height: 38px;
    padding: 0 14px;
    border-radius: 9px;
    font-size: 0.84rem;
  }

  .home-tool__shape {
    width: 18px;
    height: 18px;
    border-width: 2px;
  }

  .home-composer__meta {
    max-width: none;
    padding-right: 54px;
    justify-content: flex-start;
    text-align: left;
  }

  .home-popover {
    position: fixed;
    left: 14px;
    right: 14px;
    top: auto;
    bottom: 88px;
    width: auto;
  }

  .home-popover-ratio {
    width: auto;
  }

  .home-dimension-row {
    grid-template-columns: 34px minmax(0, 1fr) 28px 34px minmax(0, 1fr) 36px;
    gap: 6px;
  }

  .home-active-tasks {
    gap: 10px;
    margin-top: 12px;
    padding-bottom: 8px;
  }

  .home-active-task-card {
    flex-basis: min(86vw, 304px);
    min-height: 158px;
    border-radius: 16px;
  }

  .home-task-toast {
    right: 16px;
    bottom: 16px;
    left: 16px;
    max-width: none;
  }

}
</style>
