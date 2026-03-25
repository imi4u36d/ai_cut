<template>
  <section class="grid min-w-0 gap-6 xl:grid-cols-[minmax(0,0.98fr)_minmax(0,1.02fr)]">
    <div class="min-w-0 rounded-[30px] border border-white/10 bg-[linear-gradient(180deg,rgba(15,15,35,0.88),rgba(8,11,24,0.78))] p-6 shadow-[0_24px_80px_rgba(0,0,0,0.35)]">
      <PageHeader
        eyebrow="Progress"
        :title="task?.title || '任务详情'"
        description="查看任务状态、阶段进度、规划方案和渲染输出，快速判断当前素材的生产质量。"
      >
        <div class="flex flex-wrap gap-2">
          <button class="rounded-full border border-white/10 bg-white/[0.04] px-4 py-2 text-sm text-slate-100 transition duration-200 hover:border-rose-300/40 hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-50" :disabled="actionLoading" type="button" @click="openCloneFlow">
            复制参数
          </button>
          <button
            v-if="task?.status === 'FAILED'"
            class="rounded-full bg-rose-500 px-4 py-2 text-sm font-medium text-white transition duration-200 hover:bg-rose-400 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="actionLoading"
            type="button"
            @click="handleRetry"
          >
            失败重试
          </button>
          <button
            class="rounded-full border border-white/10 bg-slate-950/55 px-4 py-2 text-sm text-slate-200 transition duration-200 hover:border-rose-300/35 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="actionLoading || runningTask"
            type="button"
            @click="handleDelete"
          >
            删除任务
          </button>
        </div>
      </PageHeader>

      <div v-if="errorMessage" class="mb-4 rounded-[24px] border border-rose-500/20 bg-rose-500/10 p-4 text-sm text-rose-100">
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <p>{{ errorMessage }}</p>
          <button class="rounded-full border border-rose-300/30 px-4 py-2 text-xs font-medium text-rose-50 transition hover:bg-rose-500/20" type="button" @click="loadTask">
            重新加载
          </button>
        </div>
      </div>

      <div v-if="loading" class="rounded-[24px] border border-white/10 bg-white/[0.04] p-6 text-sm text-slate-300">
        正在加载任务详情...
      </div>

      <template v-else-if="task">
        <div class="grid gap-4 rounded-[28px] border border-white/10 bg-white/[0.04] p-5">
          <div class="flex flex-wrap items-center gap-3">
            <StatusBadge :status="task.status" />
            <span class="text-sm text-slate-300">{{ task.progress }}%</span>
            <span class="text-sm text-slate-400">{{ statusHint }}</span>
          </div>

          <div :class="focusCardClass(planningModeSummary.tone)" class="rounded-[24px] border p-4">
            <div class="flex flex-wrap items-center gap-2">
              <span :class="toneBadgeClass(planningModeSummary.tone)" class="rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.24em]">
                规划方式
              </span>
              <span class="text-xs text-slate-500">{{ planningModeSummary.label }}</span>
            </div>
            <p class="mt-3 text-base font-semibold text-white">{{ planningModeSummary.title }}</p>
            <p class="mt-1 break-words text-sm leading-6 text-slate-300">{{ planningModeSummary.detail }}</p>
          </div>

          <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <div class="rounded-2xl border border-white/8 bg-slate-950/50 p-4">
              <p class="text-xs uppercase tracking-[0.24em] text-slate-400">输出数量</p>
              <p class="mt-2 text-lg font-semibold text-white">{{ completedOutputCount }} / {{ task.outputCount }}</p>
            </div>
            <div class="rounded-2xl border border-white/8 bg-slate-950/50 p-4">
              <p class="text-xs uppercase tracking-[0.24em] text-slate-400">重试次数</p>
              <p class="mt-2 text-lg font-semibold text-white">{{ task.retryCount ?? 0 }}</p>
            </div>
            <div class="rounded-2xl border border-white/8 bg-slate-950/50 p-4">
              <p class="text-xs uppercase tracking-[0.24em] text-slate-400">开始时间</p>
              <p class="mt-2 text-sm font-semibold text-white">{{ formatTime(task.startedAt) }}</p>
            </div>
            <div class="rounded-2xl border border-white/8 bg-slate-950/50 p-4">
              <p class="text-xs uppercase tracking-[0.24em] text-slate-400">结束时间</p>
              <p class="mt-2 text-sm font-semibold text-white">{{ formatTime(task.finishedAt) }}</p>
            </div>
          </div>

          <div class="grid gap-3">
            <TimelineStage label="素材分析" description="读取源文件、探测时长和基础轨道信息。" :state="getStageState('analysis')" />
            <TimelineStage label="剪辑规划" description="生成切条方案，选择更适合投放的片段。" :state="getStageState('planning')" />
            <TimelineStage label="视频渲染" description="输出预览版和下载版素材。" :state="getStageState('rendering')" />
          </div>
        </div>

        <div class="mt-6 grid gap-4 rounded-[28px] border border-white/10 bg-slate-950/45 p-5">
          <div class="grid gap-1 min-w-0">
            <span class="text-xs uppercase tracking-[0.24em] text-slate-400">任务配置</span>
            <span class="break-words text-sm text-slate-200">{{ task.platform }} · {{ task.aspectRatio }} · {{ task.minDurationSeconds }}-{{ task.maxDurationSeconds }} 秒 · {{ task.outputCount }} 条</span>
          </div>
          <div class="grid gap-4 sm:grid-cols-2">
            <div class="min-w-0">
              <p class="text-xs uppercase tracking-[0.24em] text-slate-400">源文件</p>
              <p class="mt-2 break-all text-sm text-white">{{ task.sourceFileName }}</p>
              <p v-if="task.source?.originalFileName" class="mt-1 break-all text-xs text-slate-400">原始资产：{{ task.source.originalFileName }}</p>
            </div>
            <div v-if="task.creativePrompt" class="min-w-0">
              <p class="text-xs uppercase tracking-[0.24em] text-slate-400">创意补充</p>
              <p class="mt-2 break-words text-sm leading-6 text-slate-300">{{ task.creativePrompt }}</p>
            </div>
          </div>

          <div v-if="task.hasTranscript" class="min-w-0 rounded-[22px] border border-white/8 bg-white/[0.04] p-4">
            <p class="text-xs uppercase tracking-[0.24em] text-slate-400">语义规划输入</p>
            <p class="mt-2 text-sm font-medium text-white">{{ task.hasTimedTranscript ? "已提供带时间戳字幕/台词" : "已提供纯文本台词/字幕" }}</p>
            <p class="mt-1 text-xs text-slate-400">时间轴片段：{{ task.transcriptCueCount ?? 0 }} 条</p>
            <p v-if="task.transcriptPreview" class="mt-2 break-words text-sm leading-6 text-slate-300">{{ task.transcriptPreview }}</p>
          </div>

          <div v-if="task.plan?.length" class="min-w-0 rounded-[22px] border border-white/8 bg-white/[0.04] p-4">
            <p class="text-xs uppercase tracking-[0.24em] text-slate-400">规划方案</p>
            <div v-if="task.plan?.length" class="mt-4 grid gap-3">
              <article v-for="clip in task.plan" :key="clip.clipIndex" class="min-w-0 rounded-2xl border border-white/8 bg-slate-950/50 p-4">
                <div class="flex items-start justify-between gap-4">
                  <div class="min-w-0">
                    <p class="break-words text-sm font-semibold text-white">#{{ clip.clipIndex }} {{ clip.title }}</p>
                    <p class="mt-2 break-words text-sm leading-6 text-slate-300">{{ clip.reason }}</p>
                  </div>
                  <span class="shrink-0 rounded-full bg-white/5 px-3 py-1 text-xs font-semibold text-slate-200">{{ clip.durationSeconds.toFixed(1) }}s</span>
                </div>
                <div class="mt-4 grid gap-3 text-xs text-slate-400 sm:grid-cols-3">
                  <div>起点 {{ clip.startSeconds.toFixed(1) }}s</div>
                  <div>终点 {{ clip.endSeconds.toFixed(1) }}s</div>
                  <div>时长 {{ clip.durationSeconds.toFixed(1) }}s</div>
                </div>
              </article>
            </div>
          </div>

          <div v-if="task.source" class="grid gap-3 sm:grid-cols-2">
            <div class="rounded-2xl border border-white/8 bg-white/[0.04] p-4">
              <p class="text-xs uppercase tracking-[0.24em] text-slate-400">素材信息</p>
              <p class="mt-2 text-sm text-white">{{ formatDuration(task.source.durationSeconds) }}</p>
              <p class="mt-1 text-xs text-slate-400">{{ formatResolution(task.source.width, task.source.height) }}</p>
            </div>
            <div class="rounded-2xl border border-white/8 bg-white/[0.04] p-4">
              <p class="text-xs uppercase tracking-[0.24em] text-slate-400">音轨 / 体积</p>
              <p class="mt-2 text-sm text-white">{{ task.source.hasAudio === false ? "无音轨" : "含音轨" }}</p>
              <p class="mt-1 text-xs text-slate-400">{{ formatBytes(task.source.sizeBytes) }}</p>
            </div>
          </div>

          <div v-if="task.errorMessage" class="rounded-2xl border border-rose-500/20 bg-rose-500/10 p-4 text-rose-200">
            {{ task.errorMessage }}
          </div>
        </div>

        <div class="mt-6 rounded-[28px] border border-white/10 bg-slate-950/45 p-5">
          <div class="flex flex-wrap items-end justify-between gap-3">
            <div>
              <p class="text-xs uppercase tracking-[0.24em] text-slate-400">Live Trace</p>
              <h3 class="mt-2 text-lg font-semibold text-white">全链路进度追踪</h3>
              <p class="mt-1 text-sm text-slate-400">默认只展示摘要，展开后再查看实时事件流，避免长日志压住主要内容。</p>
            </div>
            <div class="flex flex-wrap gap-2">
              <button class="rounded-full border border-white/10 bg-white/[0.04] px-4 py-2 text-sm text-slate-100 transition duration-200 hover:border-rose-300/40 hover:bg-white/10" type="button" @click="loadTrace">
                刷新日志
              </button>
              <button class="rounded-full border border-white/10 bg-slate-950/55 px-4 py-2 text-sm text-slate-100 transition duration-200 hover:border-rose-300/40 hover:bg-white/10" type="button" @click="traceExpanded = !traceExpanded">
                {{ traceExpanded ? "收起实时日志" : "展开实时日志" }}
              </button>
            </div>
          </div>

          <div v-if="traceErrorMessage" class="mt-4 rounded-2xl border border-rose-500/20 bg-rose-500/10 p-4 text-sm text-rose-100">
            {{ traceErrorMessage }}
          </div>
          <div v-else-if="traceLoading && traceEvents.length === 0" class="mt-4 text-sm text-slate-400">正在加载任务日志...</div>
          <div v-else-if="traceEvents.length === 0" class="mt-4 rounded-2xl border border-dashed border-white/10 bg-white/[0.03] p-4 text-sm text-slate-400">
            当前任务还没有可展示的 trace 事件。
          </div>
          <div v-else-if="!traceExpanded" class="mt-4 grid gap-4">
            <div :class="focusCardClass(currentFocus.tone)" class="rounded-[24px] border p-4">
              <div class="flex flex-wrap items-center gap-2">
                <span :class="toneBadgeClass(currentFocus.tone)" class="rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.24em]">
                  当前重点
                </span>
                <span class="text-xs text-slate-500">{{ currentFocus.timestamp ? formatTime(currentFocus.timestamp) : "实时追踪中" }}</span>
                <span v-if="traceLoading" class="rounded-full border border-white/10 bg-white/[0.04] px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-300">
                  同步中
                </span>
              </div>
              <p class="mt-3 text-lg font-semibold text-white">{{ currentFocus.title }}</p>
              <p class="mt-1 break-words text-sm leading-6 text-slate-300">{{ currentFocus.detail }}</p>
            </div>

            <div class="grid gap-3 lg:grid-cols-3">
              <article v-for="entry in collapsedTracePreview" :key="entry.key" :class="focusCardClass(entry.tone)" class="min-w-0 rounded-2xl border p-4">
                <div class="flex flex-wrap items-center gap-2">
                  <span :class="toneBadgeClass(entry.tone)" class="rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.24em]">
                    {{ entry.stageLabel }}
                  </span>
                  <span class="text-xs text-slate-500">{{ formatTime(entry.timestamp) }}</span>
                </div>
                <p class="mt-3 break-words text-sm font-semibold text-white">{{ entry.title }}</p>
                <p v-if="entry.detail" class="mt-1 break-words text-sm leading-6 text-slate-300">{{ entry.detail }}</p>
              </article>
            </div>
          </div>
          <div v-else class="mt-4 grid gap-4">
            <div :class="focusCardClass(currentFocus.tone)" class="rounded-[24px] border p-4">
              <div class="flex flex-wrap items-center gap-2">
                <span :class="toneBadgeClass(currentFocus.tone)" class="rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.24em]">
                  当前重点
                </span>
                <span class="text-xs text-slate-500">{{ currentFocus.timestamp ? formatTime(currentFocus.timestamp) : "实时追踪中" }}</span>
                <span v-if="traceLoading" class="rounded-full border border-white/10 bg-white/[0.04] px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-300">
                  同步中
                </span>
              </div>
              <p class="mt-3 text-lg font-semibold text-white">{{ currentFocus.title }}</p>
              <p class="mt-1 break-words text-sm leading-6 text-slate-300">{{ currentFocus.detail }}</p>
            </div>

            <div class="grid gap-3 lg:grid-cols-2">
              <article
                v-for="item in stageProgressItems"
                :key="item.key"
                :class="focusCardClass(item.tone)"
                class="rounded-[24px] border p-4"
              >
                <div class="flex flex-wrap items-start justify-between gap-3">
                  <div class="min-w-0">
                    <div class="flex flex-wrap items-center gap-2">
                      <span :class="toneBadgeClass(item.tone)" class="rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.24em]">
                        {{ item.label }}
                      </span>
                      <span class="text-xs text-slate-500">{{ stageStatusLabel(item.status) }}</span>
                    </div>
                    <p class="mt-2 text-sm text-slate-400">{{ item.description }}</p>
                  </div>
                  <span class="text-sm font-semibold text-white">{{ item.progress }}%</span>
                </div>
                <p class="mt-4 break-words text-sm leading-6 text-white">{{ item.hint }}</p>
                <div class="mt-4 h-2 overflow-hidden rounded-full bg-white/10">
                  <div class="h-full rounded-full transition-all duration-300" :class="progressBarClass(item.tone)" :style="{ width: `${item.progress}%` }"></div>
                </div>
              </article>
            </div>

            <div class="grid gap-3">
              <article
                v-for="entry in readableTraceEvents"
                :key="entry.key"
                :class="focusCardClass(entry.tone)"
                class="min-w-0 overflow-hidden rounded-2xl border p-4"
              >
                <div class="flex flex-wrap items-center gap-2">
                  <span :class="toneBadgeClass(entry.tone)" class="rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.24em]">
                    {{ entry.stageLabel }}
                  </span>
                  <span v-if="entry.important" class="rounded-full border border-white/10 bg-white/[0.04] px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-200">
                    重点
                  </span>
                  <span class="text-xs text-slate-500">{{ formatTime(entry.timestamp) }}</span>
                </div>
                <p class="mt-3 break-words text-sm font-semibold text-white">{{ entry.title }}</p>
                <p v-if="entry.detail" class="mt-1 break-words text-sm leading-6 text-slate-300">{{ entry.detail }}</p>
                <div v-if="entry.tags.length" class="mt-3 flex flex-wrap gap-2">
                  <span v-for="tag in entry.tags" :key="tag" class="rounded-full border border-white/10 bg-white/[0.04] px-3 py-1 text-xs text-slate-200">
                    {{ tag }}
                  </span>
                </div>
                <p class="mt-3 break-all font-mono text-[11px] text-slate-500">{{ entry.event }}</p>
              </article>
            </div>
          </div>
        </div>
      </template>
    </div>

    <div class="min-w-0 rounded-[30px] border border-white/10 bg-[linear-gradient(180deg,rgba(15,15,35,0.86),rgba(8,11,24,0.72))] p-6 shadow-[0_24px_80px_rgba(0,0,0,0.32)]">
      <PageHeader
        eyebrow="Outputs"
        title="生成结果"
        description="预览和下载生成素材，并按片段查看每条内容的规划理由。"
      />

      <div v-if="loading && !task" class="rounded-[24px] border border-dashed border-white/15 bg-slate-950/40 p-8 text-center text-sm text-slate-300">
        任务完成后会在这里展示剪辑结果。
      </div>

      <div v-else-if="!task || task.outputs.length === 0" class="rounded-[24px] border border-dashed border-white/15 bg-slate-950/40 p-8 text-center text-sm text-slate-300">
        任务完成后会在这里展示剪辑结果。
      </div>

      <div v-else class="grid gap-4">
        <div class="grid grid-cols-2 gap-3 rounded-[24px] border border-white/10 bg-white/[0.04] p-4 text-sm text-slate-300">
          <div>
            <p class="text-xs uppercase tracking-[0.24em] text-slate-400">完成输出</p>
            <p class="mt-2 text-lg font-semibold text-white">{{ completedOutputCount }}</p>
          </div>
          <div>
            <p class="text-xs uppercase tracking-[0.24em] text-slate-400">渲染目标</p>
            <p class="mt-2 text-lg font-semibold text-white">{{ task.outputCount }}</p>
          </div>
        </div>

        <article v-for="output in task.outputs" :key="output.id" class="min-w-0 rounded-[24px] border border-white/10 bg-slate-950/45 p-4">
          <div class="flex flex-col gap-4 xl:flex-row">
            <video :src="resolveStorageUrl(output.previewUrl)" controls class="aspect-[9/16] w-full max-w-[240px] shrink-0 rounded-2xl border border-white/10 bg-black object-cover"></video>
            <div class="min-w-0 flex-1">
              <div class="flex items-start justify-between gap-4">
                <div class="min-w-0">
                  <h3 class="break-words text-lg font-semibold text-white">{{ output.title }}</h3>
                  <p class="mt-2 break-words text-sm leading-6 text-slate-300">{{ output.reason }}</p>
                </div>
                <a :href="resolveStorageUrl(output.downloadUrl)" class="shrink-0 rounded-full bg-rose-500 px-4 py-2 text-sm font-medium text-white transition duration-200 hover:bg-rose-400" download>
                  下载
                </a>
              </div>
              <div class="mt-4 grid gap-3 text-sm text-slate-300 sm:grid-cols-2 xl:grid-cols-4">
                <div>序号：{{ output.clipIndex }}</div>
                <div>起点：{{ output.startSeconds.toFixed(1) }}s</div>
                <div>终点：{{ output.endSeconds.toFixed(1) }}s</div>
                <div>时长：{{ output.durationSeconds.toFixed(1) }}s</div>
              </div>
            </div>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getRuntimeConfig } from "@/api/runtime-config";
import { deleteTask, fetchTask, fetchTaskTrace, retryTask } from "@/api/tasks";
import type { TaskDetail, TaskStatus, TaskTraceEvent } from "@/types";
import PageHeader from "@/components/PageHeader.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import TimelineStage from "@/components/TimelineStage.vue";
import { usePolling } from "@/composables/usePolling";
import { formatTaskStatus, getTaskLifecycleGroup, isTerminalTaskStatus } from "@/utils/task";
import { resolveRuntimeUrl } from "@/utils/url";

type TraceTone = "slate" | "emerald" | "sky" | "fuchsia" | "amber" | "rose";
type ProgressStatus = "pending" | "active" | "done" | "error";

interface TraceFocusItem {
  title: string;
  detail: string;
  tone: TraceTone;
  timestamp?: string;
}

interface TraceProgressItem {
  key: string;
  label: string;
  description: string;
  hint: string;
  progress: number;
  status: ProgressStatus;
  tone: TraceTone;
}

interface ReadableTraceItem {
  key: string;
  stageLabel: string;
  title: string;
  detail: string;
  tags: string[];
  tone: TraceTone;
  important: boolean;
  timestamp: string;
  event: string;
}

interface PlanningModeSummary {
  label: string;
  title: string;
  detail: string;
  tone: TraceTone;
}

const route = useRoute();
const router = useRouter();
const task = ref<TaskDetail | null>(null);
const loading = ref(true);
const errorMessage = ref("");
const traceEvents = ref<TaskTraceEvent[]>([]);
const traceLoading = ref(false);
const traceErrorMessage = ref("");
const traceExpanded = ref(false);
const actionLoading = ref(false);

const taskId = computed(() => {
  const value = route.params.id;
  if (Array.isArray(value)) {
    return value[0] ?? "";
  }
  return typeof value === "string" ? value : "";
});

const statusHint = computed(() => {
  if (!task.value) {
    return "";
  }
  return formatTaskStatus(task.value.status);
});

const completedOutputCount = computed(() => task.value?.completedOutputCount ?? task.value?.outputs.length ?? 0);
const runningTask = computed(() => Boolean(task.value && !isTerminalTaskStatus(task.value.status)));
const traceEventsDesc = computed(() => [...traceEvents.value].reverse());

function lastTraceEvent(...events: string[]) {
  for (const entry of traceEventsDesc.value) {
    if (events.includes(entry.event)) {
      return entry;
    }
  }
  return null;
}

function hasTraceEvent(...events: string[]) {
  return Boolean(lastTraceEvent(...events));
}

function resolveStorageUrl(value: string) {
  return resolveRuntimeUrl(value, getRuntimeConfig().storageBaseUrl);
}

function formatTime(value?: string | null) {
  if (!value) {
    return "等待生成";
  }
  return new Date(value).toLocaleString();
}

function formatDuration(value?: number | null) {
  if (typeof value !== "number") {
    return "时长未知";
  }
  return `${value.toFixed(1)} 秒`;
}

function formatResolution(width?: number | null, height?: number | null) {
  if (!width || !height) {
    return "分辨率待探测";
  }
  return `${width} × ${height}`;
}

function formatBytes(value?: number | null) {
  if (typeof value !== "number" || Number.isNaN(value)) {
    return "体积未知";
  }
  if (value < 1024) {
    return `${value} B`;
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`;
  }
  return `${(value / (1024 * 1024)).toFixed(1)} MB`;
}

function payloadString(payload: Record<string, unknown>, key: string) {
  const value = payload[key];
  return typeof value === "string" ? value : "";
}

function payloadNumber(payload: Record<string, unknown>, key: string) {
  const value = payload[key];
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string" && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

function payloadBoolean(payload: Record<string, unknown>, key: string) {
  const value = payload[key];
  return typeof value === "boolean" ? value : null;
}

function payloadStringArray(payload: Record<string, unknown>, key: string) {
  const value = payload[key];
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => (typeof item === "string" ? item.trim() : String(item ?? "").trim()))
    .filter(Boolean);
}

function payloadNumberRange(payload: Record<string, unknown>, key: string) {
  const value = payload[key];
  if (!Array.isArray(value) || value.length < 2) {
    return [];
  }
  const start = Number(value[0]);
  const end = Number(value[1]);
  return Number.isFinite(start) && Number.isFinite(end) ? [start, end] : [];
}

function clipTimeLabel(start?: number | null, end?: number | null) {
  if (typeof start !== "number" || typeof end !== "number") {
    return "";
  }
  return `${start.toFixed(1)}s - ${end.toFixed(1)}s`;
}

function shortText(value: string, max = 80) {
  if (!value) {
    return "";
  }
  return value.length > max ? `${value.slice(0, max)}...` : value;
}

function joinParts(parts: Array<string | null | undefined>, separator = "，") {
  return parts.filter(Boolean).join(separator);
}

function stageLabel(stage: string) {
  switch (stage) {
    case "api":
      return "任务";
    case "dispatch":
      return "调度";
    case "pipeline":
      return "流程";
    case "analysis":
      return "素材分析";
    case "audio":
      return "音频节奏";
    case "planning":
      return "方案规划";
    case "vision":
      return "视频理解";
    case "fusion":
      return "融合规划";
    case "llm":
      return "大模型";
    case "render":
      return "FFmpeg";
    default:
      return stage || "日志";
  }
}

function toneBadgeClass(tone: TraceTone) {
  switch (tone) {
    case "emerald":
      return "bg-emerald-500/15 text-emerald-100";
    case "sky":
      return "bg-sky-500/15 text-sky-100";
    case "fuchsia":
      return "bg-fuchsia-500/15 text-fuchsia-100";
    case "amber":
      return "bg-amber-500/15 text-amber-100";
    case "rose":
      return "bg-rose-500/15 text-rose-100";
    default:
      return "bg-white/10 text-slate-200";
  }
}

function focusCardClass(tone: TraceTone) {
  switch (tone) {
    case "emerald":
      return "border-emerald-400/20 bg-emerald-500/8";
    case "sky":
      return "border-sky-400/20 bg-sky-500/8";
    case "fuchsia":
      return "border-fuchsia-400/20 bg-fuchsia-500/8";
    case "amber":
      return "border-amber-400/20 bg-amber-500/8";
    case "rose":
      return "border-rose-400/25 bg-rose-500/8";
    default:
      return "border-white/8 bg-white/[0.04]";
  }
}

function progressBarClass(tone: TraceTone) {
  switch (tone) {
    case "emerald":
      return "bg-gradient-to-r from-emerald-500 to-teal-300";
    case "sky":
      return "bg-gradient-to-r from-sky-500 to-cyan-300";
    case "fuchsia":
      return "bg-gradient-to-r from-fuchsia-500 to-pink-300";
    case "amber":
      return "bg-gradient-to-r from-amber-500 to-orange-300";
    case "rose":
      return "bg-gradient-to-r from-rose-500 to-red-300";
    default:
      return "bg-gradient-to-r from-slate-500 to-slate-300";
  }
}

function stageStatusLabel(status: ProgressStatus) {
  switch (status) {
    case "done":
      return "已完成";
    case "active":
      return "进行中";
    case "error":
      return "异常";
    default:
      return "待开始";
  }
}

function describeTraceEvent(entry: TaskTraceEvent) {
  const payload = entry.payload || {};
  const model = payloadString(payload, "model");
  const clipTitles = payloadStringArray(payload, "clip_titles");
  const clipIndex = payloadNumber(payload, "clip_index");
  const clipCount = payloadNumber(payload, "clip_count");
  const startSeconds = payloadNumber(payload, "start_seconds");
  const endSeconds = payloadNumber(payload, "end_seconds");
  const progress = payloadNumber(payload, "progress");
  const contentExcerpt = payloadString(payload, "content_excerpt");
  const promptLength = payloadNumber(payload, "prompt_length");
  const transcriptCueCount = payloadNumber(payload, "transcript_cue_count");
  const audioPeakCount = payloadNumber(payload, "audio_peak_count");
  const frameCount = payloadNumber(payload, "frame_count");
  const error = payloadString(payload, "error");

  switch (entry.event) {
    case "task.created": {
      const durationRange = payloadNumberRange(payload, "duration_range");
      const outputCount = payloadNumber(payload, "output_count");
      return {
        title: "任务已创建",
        detail: joinParts([
          payloadString(payload, "platform") ? `平台 ${payloadString(payload, "platform")}` : "",
          durationRange.length === 2 ? `时长 ${durationRange[0]}-${durationRange[1]} 秒` : "",
          outputCount ? `目标输出 ${outputCount} 条` : "",
          payloadBoolean(payload, "has_transcript") ? "已附带字幕/台词输入" : "未附带字幕/台词输入",
        ]),
        tags: [payloadString(payload, "aspect_ratio"), payloadString(payload, "title")].filter(Boolean),
        tone: "emerald" as const,
        important: true,
      };
    }
    case "task.enqueued":
      return {
        title: "任务已进入队列",
        detail: "等待 worker 拉取后继续执行。",
        tags: [],
        tone: "sky" as const,
        important: false,
      };
    case "task.dispatched_inline":
      return {
        title: "任务已直接启动",
        detail: "当前环境使用本地线程执行，不经过外部队列。",
        tags: [],
        tone: "sky" as const,
        important: false,
      };
    case "task.enqueue_failed":
      return {
        title: "入队失败，已切回本地执行",
        detail: "队列不可用时任务不会卡住，会直接改走本地线程。",
        tags: [],
        tone: "amber" as const,
        important: true,
      };
    case "task.claimed":
    case "task.processing_started":
      return {
        title: "任务开始处理",
        detail: "执行器已接管任务，流程正式开始。",
        tags: [],
        tone: "sky" as const,
        important: true,
      };
    case "analysis.started":
      return {
        title: "开始分析源视频",
        detail: "正在识别时长、分辨率、音轨等素材信息。",
        tags: [],
        tone: "sky" as const,
        important: true,
      };
    case "analysis.completed":
      return {
        title: "素材分析完成",
        detail: joinParts([
          payloadNumber(payload, "durationSeconds") ? `时长 ${payloadNumber(payload, "durationSeconds")?.toFixed(1)} 秒` : "",
          payloadNumber(payload, "width") && payloadNumber(payload, "height")
            ? `分辨率 ${payloadNumber(payload, "width")} × ${payloadNumber(payload, "height")}`
            : "",
          payloadBoolean(payload, "hasAudio") === false ? "无音轨" : "含音轨",
        ]),
        tags: [],
        tone: "emerald" as const,
        important: true,
      };
    case "planning.started":
      return {
        title: "开始生成剪辑方案",
        detail: joinParts([
          transcriptCueCount ? `带 ${transcriptCueCount} 条时间轴字幕` : payloadBoolean(payload, "has_transcript") ? "带文本字幕输入" : "无字幕输入",
          payloadBoolean(payload, "creative_prompt_present") ? "带创意补充" : "无创意补充",
          payloadString(payload, "primary_model") ? `主模型 ${payloadString(payload, "primary_model")}` : "",
        ]),
        tags: [
          payloadString(payload, "fallback_model") ? `回退 ${payloadString(payload, "fallback_model")}` : "",
          payloadString(payload, "vision_model") ? `视觉 ${payloadString(payload, "vision_model")}` : "",
        ].filter(Boolean),
        tone: "sky" as const,
        important: true,
      };
    case "planning.subtitle_signals":
      return {
        title: "已锁定字幕冲突点",
        detail: transcriptCueCount ? `本次从 ${transcriptCueCount} 条字幕时间轴里提取了强信号切点。` : "已提取字幕强信号时间轴。",
        tags: [],
        tone: "sky" as const,
        important: true,
      };
    case "audio.peaks_detected":
      return {
        title: audioPeakCount ? `已检测到 ${audioPeakCount} 个音频卡点` : "已检测到音频峰值卡点",
        detail: "这些音频峰值会和字幕时间轴、画面高点一起参与切点判断。",
        tags: [],
        tone: "amber" as const,
        important: true,
      };
    case "heuristic.start":
      return {
        title: "未使用大模型，已切到本地规则规划",
        detail: transcriptCueCount ? `当前按本地规则规划，参考 ${transcriptCueCount} 条字幕时间轴。` : "当前没有拿到大模型结果，系统改用本地规则规划候选片段。",
        tags: [],
        tone: "amber" as const,
        important: true,
      };
    case "heuristic.completed":
      return {
        title: "本地规则规划已完成",
        detail: clipCount ? `没有使用大模型，本地规则生成了 ${clipCount} 条候选片段。` : "没有使用大模型，本地规则已生成候选片段。",
        tags: clipTitles.slice(0, 3),
        tone: "amber" as const,
        important: true,
      };
    case "vision.attempt":
      return {
        title: "开始识别视频剧情事件",
        detail: joinParts([model ? `视觉模型 ${model}` : "", frameCount ? `准备分析 ${frameCount} 张关键帧` : "准备分析视频关键帧"]),
        tags: [],
        tone: "sky" as const,
        important: true,
      };
    case "vision.request":
      return {
        title: "视觉模型已收到关键帧和时间轴信号",
        detail: joinParts([
          model ? `模型 ${model}` : "",
          frameCount ? `${frameCount} 张关键帧` : "",
          transcriptCueCount ? `融合 ${transcriptCueCount} 条字幕时间轴` : payloadBoolean(payload, "has_transcript") ? "带字幕上下文" : "无字幕上下文",
          audioPeakCount ? `融合 ${audioPeakCount} 个音频卡点` : "",
        ]),
        tags: [],
        tone: "sky" as const,
        important: true,
      };
    case "vision.response":
      {
        const parsedEventCount = payloadNumber(payload, "parsed_event_count");
        const eventTitles = payloadStringArray(payload, "event_titles");
        const eventTypes = payloadStringArray(payload, "event_types");
        const parsedClipCount = payloadNumber(payload, "parsed_clip_count");
        return {
          title: parsedEventCount ? `视觉理解返回了 ${parsedEventCount} 个剧情事件` : parsedClipCount ? `视频内容理解返回了 ${parsedClipCount} 条高点方案` : "视频内容理解已返回结果",
          detail: contentExcerpt
            ? `返回摘录：${shortText(contentExcerpt, 160)}`
            : parsedEventCount
              ? "视觉模型已经把高燃、冲突、反转这类事件定位到了具体时间点，后续会再做最终切点规划。"
              : "视觉模型已基于关键帧识别出可剪辑的高燃或卡点时刻。",
          tags: [model ? `模型 ${model}` : "", ...eventTypes.slice(0, 3), ...eventTitles.slice(0, 2), ...clipTitles.slice(0, 2)].filter(Boolean),
          tone: "sky" as const,
          important: true,
        };
      }
    case "fusion.vision_fallback":
      return {
        title: "视觉事件识别失败，已降级继续规划",
        detail: "视觉模型这轮没成功返回，系统会改用字幕时间轴、音频卡点和候选片段继续做最终剪辑规划。",
        tags: [],
        tone: "amber" as const,
        important: true,
      };
    case "fusion.attempt":
      return {
        title: "开始生成最终剪辑时间点",
        detail: joinParts([
          model ? `融合模型 ${model}` : "",
          payloadBoolean(payload, "used_visual_events") ? "已接入视觉事件结果" : "当前不带视觉事件结果",
        ]),
        tags: [payloadString(payload, "visual_model") ? `视觉 ${payloadString(payload, "visual_model")}` : ""].filter(Boolean),
        tone: "fuchsia" as const,
        important: true,
      };
    case "fusion.request":
      return {
        title: "融合规划请求已发出",
        detail: joinParts([
          model ? `模型 ${model}` : "",
          promptLength ? `Prompt ${promptLength} 字符` : "",
          transcriptCueCount ? `字幕时间轴 ${transcriptCueCount} 条` : "",
          audioPeakCount ? `音频卡点 ${audioPeakCount} 个` : "",
          payloadNumber(payload, "visual_event_count") ? `视觉事件 ${payloadNumber(payload, "visual_event_count")} 个` : "",
        ]),
        tags: [payloadBoolean(payload, "used_visual_events") ? "视觉事件已接入" : "未接入视觉事件"].filter(Boolean),
        tone: "fuchsia" as const,
        important: true,
      };
    case "fusion.response":
      return {
        title: clipCount ? `融合规划返回了 ${clipCount} 条最终方案` : "融合规划已返回结果",
        detail: contentExcerpt ? `返回摘录：${shortText(contentExcerpt, 160)}` : "最终剪辑时间点已经确定，后续会按这些切点执行 FFmpeg 渲染。",
        tags: [
          model ? `模型 ${model}` : "",
          payloadBoolean(payload, "used_visual_events") ? "已结合视觉事件" : "未结合视觉事件",
          ...clipTitles.slice(0, 3),
        ].filter(Boolean),
        tone: "fuchsia" as const,
        important: true,
      };
    case "fusion.http_error":
      return {
        title: "融合规划请求返回错误状态",
        detail: joinParts([
          model ? `模型 ${model}` : "",
          payloadNumber(payload, "status_code") ? `HTTP ${payloadNumber(payload, "status_code")}` : "",
          payloadString(payload, "response_excerpt") ? shortText(payloadString(payload, "response_excerpt"), 160) : "",
        ]),
        tags: [],
        tone: "rose" as const,
        important: true,
      };
    case "fusion.timeout":
      return {
        title: "融合规划响应超时",
        detail: joinParts([
          model ? `模型 ${model}` : "",
          payloadNumber(payload, "timeout_seconds") ? `等待 ${payloadNumber(payload, "timeout_seconds")} 秒后仍未返回` : "",
          promptLength ? `Prompt ${promptLength} 字符` : "",
        ]),
        tags: [],
        tone: "rose" as const,
        important: true,
      };
    case "fusion.network_error":
      return {
        title: "融合规划网络失败",
        detail: joinParts([model ? `模型 ${model}` : "", error || entry.message]),
        tags: [],
        tone: "rose" as const,
        important: true,
      };
    case "fusion.attempt_failed":
      return {
        title: "当前融合模型失败，准备回退",
        detail: joinParts([model ? `模型 ${model}` : "", error ? shortText(error, 160) : "准备尝试下一个可用模型。"]),
        tags: [],
        tone: "amber" as const,
        important: true,
      };
    case "vision.timeout":
      return {
        title: "视频内容理解超时",
        detail: joinParts([
          model ? `模型 ${model}` : "",
          payloadNumber(payload, "timeout_seconds") ? `等待 ${payloadNumber(payload, "timeout_seconds")} 秒后仍未返回` : "",
          frameCount ? `${frameCount} 张关键帧` : "",
        ]),
        tags: [],
        tone: "rose" as const,
        important: true,
      };
    case "vision.http_error":
      return {
        title: "视频内容理解请求返回错误状态",
        detail: joinParts([
          model ? `模型 ${model}` : "",
          payloadNumber(payload, "status_code") ? `HTTP ${payloadNumber(payload, "status_code")}` : "",
          payloadString(payload, "response_excerpt") ? shortText(payloadString(payload, "response_excerpt"), 160) : "",
        ]),
        tags: [],
        tone: "rose" as const,
        important: true,
      };
    case "vision.network_error":
      return {
        title: "视频内容理解网络失败",
        detail: joinParts([model ? `模型 ${model}` : "", error || entry.message]),
        tags: [],
        tone: "rose" as const,
        important: true,
      };
    case "vision.attempt_failed":
      return {
        title: "当前视觉模型失败，准备回退",
        detail: joinParts([model ? `模型 ${model}` : "", error ? shortText(error, 160) : "准备尝试下一个可用视觉模型。"]),
        tags: [],
        tone: "amber" as const,
        important: true,
      };
    case "qwen.attempt":
      return {
        title: "开始调用大模型",
        detail: model ? `当前尝试模型 ${model}。` : "开始向大模型发起规划请求。",
        tags: model ? [`模型 ${model}`] : [],
        tone: "fuchsia" as const,
        important: true,
      };
    case "qwen.request":
      return {
        title: "大模型请求已发出",
        detail: joinParts([
          model ? `模型 ${model}` : "",
          promptLength ? `Prompt ${promptLength} 字符` : "",
          transcriptCueCount ? `字幕时间轴 ${transcriptCueCount} 条` : "",
        ]),
        tags: contentExcerpt ? [`返回前上下文已准备`] : [],
        tone: "fuchsia" as const,
        important: false,
      };
    case "qwen.response":
      return {
        title: clipCount ? `大模型返回了 ${clipCount} 条剪辑方案` : "大模型已返回规划结果",
        detail: contentExcerpt ? `返回摘录：${shortText(contentExcerpt, 160)}` : "已收到模型响应并完成结构化解析。",
        tags: [model ? `模型 ${model}` : "", ...clipTitles.slice(0, 3)].filter(Boolean),
        tone: "fuchsia" as const,
        important: true,
      };
    case "qwen.http_error":
      return {
        title: "大模型请求返回错误状态",
        detail: joinParts([
          model ? `模型 ${model}` : "",
          payloadNumber(payload, "status_code") ? `HTTP ${payloadNumber(payload, "status_code")}` : "",
          payloadString(payload, "response_excerpt") ? shortText(payloadString(payload, "response_excerpt"), 160) : "",
        ]),
        tags: [],
        tone: "rose" as const,
        important: true,
      };
    case "qwen.timeout":
      return {
        title: "大模型响应超时",
        detail: joinParts([
          model ? `模型 ${model}` : "",
          payloadNumber(payload, "timeout_seconds") ? `等待 ${payloadNumber(payload, "timeout_seconds")} 秒后仍未返回` : "",
          promptLength ? `Prompt ${promptLength} 字符` : "",
        ]),
        tags: [],
        tone: "rose" as const,
        important: true,
      };
    case "qwen.network_error":
      return {
        title: "大模型请求网络失败",
        detail: joinParts([model ? `模型 ${model}` : "", error || entry.message]),
        tags: [],
        tone: "rose" as const,
        important: true,
      };
    case "qwen.attempt_failed":
      return {
        title: "当前模型失败，准备回退",
        detail: joinParts([model ? `模型 ${model}` : "", error ? shortText(error, 160) : "准备尝试下一个可用模型。"]),
        tags: [],
        tone: "amber" as const,
        important: true,
      };
    case "planning.completed":
      return {
        title: clipCount ? `剪辑方案已生成，共 ${clipCount} 条` : "剪辑方案已生成",
        detail: clipTitles.length ? `方案摘要：${clipTitles.slice(0, 3).join(" / ")}` : "可以进入 FFmpeg 渲染阶段。",
        tags: [],
        tone: "emerald" as const,
        important: true,
      };
    case "render.started":
      return {
        title: "开始执行 FFmpeg 剪辑",
        detail: clipCount ? `本轮计划输出 ${clipCount} 条成片。` : "开始按规划方案输出成片。",
        tags: [],
        tone: "amber" as const,
        important: true,
      };
    case "render.clip_started":
      return {
        title: clipIndex ? `开始剪第 ${clipIndex} 条素材` : "开始执行单条剪辑",
        detail: joinParts([
          payloadString(payload, "title"),
          clipTimeLabel(startSeconds, endSeconds),
        ]),
        tags: [],
        tone: "amber" as const,
        important: false,
      };
    case "render.clip_completed":
      return {
        title: clipIndex ? `第 ${clipIndex} 条剪辑成功` : "单条剪辑完成",
        detail: joinParts([
          payloadString(payload, "title"),
          progress ? `任务进度 ${progress}%` : "",
        ]),
        tags: [],
        tone: "emerald" as const,
        important: true,
      };
    case "render.clip_failed":
      return {
        title: clipIndex ? `第 ${clipIndex} 条 FFmpeg 剪辑失败` : "FFmpeg 剪辑失败",
        detail: joinParts([
          payloadString(payload, "title"),
          clipTimeLabel(startSeconds, endSeconds),
          error ? shortText(error, 160) : "",
        ]),
        tags: [],
        tone: "rose" as const,
        important: true,
      };
    case "task.completed":
      return {
        title: "任务已完成",
        detail: clipCount ? `共生成 ${clipCount} 条输出素材，可以直接预览和下载。` : "全部输出已完成。",
        tags: [],
        tone: "emerald" as const,
        important: true,
      };
    case "task.failed":
      return {
        title: "任务失败",
        detail: error ? shortText(error, 180) : entry.message,
        tags: [],
        tone: "rose" as const,
        important: true,
      };
    case "task.source_missing":
      return {
        title: "任务源素材丢失",
        detail: "源文件不存在，任务无法继续。",
        tags: [],
        tone: "rose" as const,
        important: true,
      };
    default:
      return {
        title: entry.message || entry.event,
        detail: "",
        tags: [],
        tone: entry.level.toUpperCase() === "ERROR" ? ("rose" as const) : entry.level.toUpperCase() === "WARN" ? ("amber" as const) : ("slate" as const),
        important: false,
      };
  }
}

const readableTraceEvents = computed<ReadableTraceItem[]>(() =>
  traceEventsDesc.value.map((entry, index) => {
    const description = describeTraceEvent(entry);
    return {
      key: `${entry.timestamp}-${entry.event}-${index}`,
      stageLabel: stageLabel(entry.stage),
      title: description.title,
      detail: description.detail,
      tags: description.tags,
      tone: description.tone,
      important: description.important,
      timestamp: entry.timestamp,
      event: entry.event,
    };
  })
);

const collapsedTracePreview = computed(() => readableTraceEvents.value.slice(0, 3));

const renderStageProgress = computed(() => {
  const total = task.value?.outputCount ?? 0;
  const done = completedOutputCount.value;
  if (total <= 0) {
    return 0;
  }
  return Math.max(0, Math.min(100, Math.round((done / total) * 100)));
});

const planningModeSummary = computed<PlanningModeSummary>(() => {
  const visionResponse = lastTraceEvent("vision.response");
  const visionAttempt = lastTraceEvent("vision.request", "vision.attempt");
  const visionFailure = lastTraceEvent("vision.timeout", "vision.http_error", "vision.network_error", "vision.attempt_failed");
  const fusionResponse = lastTraceEvent("fusion.response");
  const fusionAttempt = lastTraceEvent("fusion.request", "fusion.attempt");
  const fusionFailure = lastTraceEvent("fusion.timeout", "fusion.http_error", "fusion.network_error", "fusion.attempt_failed");
  const fusionVisionFallback = lastTraceEvent("fusion.vision_fallback");
  const llmResponse = lastTraceEvent("qwen.response");
  const llmAttempt = lastTraceEvent("qwen.request", "qwen.attempt");
  const llmFailure = lastTraceEvent("qwen.timeout", "qwen.http_error", "qwen.network_error", "qwen.attempt_failed");
  const heuristicCompleted = lastTraceEvent("heuristic.completed");
  const planningStarted = lastTraceEvent("planning.started", "heuristic.start");

  if (fusionResponse) {
    const payload = fusionResponse.payload || {};
    const model = payloadString(payload, "model");
    const visualModel = payloadString(payload, "visual_model");
    const parsedClipCount = payloadNumber(payload, "parsed_clip_count");
    const visualEventCount = payloadNumber(payload, "visual_event_count");
    const usedVisualEvents = payloadBoolean(payload, "used_visual_events");
    return {
      label: usedVisualEvents ? "已使用视觉理解 + 融合规划" : "已使用融合规划",
      title: model ? `本次最终切点由 ${model} 规划` : "本次最终切点已由融合模型规划完成",
      detail: usedVisualEvents
        ? joinParts([
            visualModel ? `先由 ${visualModel} 识别剧情事件` : "先完成了视觉事件识别",
            visualEventCount ? `共提取 ${visualEventCount} 个剧情事件` : "",
            parsedClipCount ? `再生成 ${parsedClipCount} 条最终剪辑方案` : "再输出最终剪辑方案",
          ])
        : parsedClipCount
          ? `当前直接根据字幕时间轴、音频卡点和候选片段生成了 ${parsedClipCount} 条最终剪辑方案。`
          : "当前由融合规划模型输出最终剪辑方案。",
      tone: usedVisualEvents ? "fuchsia" : "sky",
    };
  }

  if (visionResponse) {
    const payload = visionResponse.payload || {};
    const model = payloadString(payload, "model");
    const parsedEventCount = payloadNumber(payload, "parsed_event_count");
    const parsedClipCount = payloadNumber(payload, "parsed_clip_count");
    return {
      label: "视觉事件已识别",
      title: model ? `上游已由 ${model} 完成剧情事件识别` : "上游已完成剧情事件识别",
      detail: parsedEventCount
        ? task.value?.hasTimedTranscript
          ? `视觉模型已经结合字幕时间轴和音频卡点识别出 ${parsedEventCount} 个关键事件，接下来会再做最终时间点规划。`
          : `视觉模型已经识别出 ${parsedEventCount} 个关键事件，接下来会再做最终时间点规划。`
        : parsedClipCount
          ? `旧版任务直接从视觉模型拿到了 ${parsedClipCount} 条方案。`
          : task.value?.hasTimedTranscript
            ? "视觉模型已经结合字幕时间轴和音频卡点返回内容理解结果，接下来会按剧情强点规划最终切点。"
            : "视觉模型已经返回视频内容理解结果，接下来会按识别到的剧情强点规划最终切点。",
      tone: "sky",
    };
  }

  if ((fusionFailure || visionFailure || fusionVisionFallback) && heuristicCompleted) {
    return {
      label: "模型失败后回退",
      title: "模型规划没有成功返回，已回退到本地规则规划",
      detail: "这说明系统尝试过视觉理解或融合规划，但中途失败、超时或解析异常，最终退回本地规则来保证任务继续执行。",
      tone: "amber",
    };
  }

  if (llmResponse) {
    const payload = llmResponse.payload || {};
    const model = payloadString(payload, "model");
    const parsedClipCount = payloadNumber(payload, "parsed_clip_count");
    return {
      label: "已使用大模型",
      title: model ? `本次规划由 ${model} 完成` : "本次规划已使用大模型完成",
      detail: parsedClipCount
        ? `大模型已经返回 ${parsedClipCount} 条剪辑方案，后续 FFmpeg 会按这些切点执行。`
        : "大模型已经返回规划结果，这次不是本地启发式规划。",
      tone: "fuchsia",
    };
  }

  if (heuristicCompleted && llmFailure) {
    return {
      label: "大模型失败后回退",
      title: "大模型没有成功返回，已改用本地规则规划",
      detail: "这表示系统尝试过大模型，但请求失败、超时或解析失败，最终回退到启发式规划来保证任务继续执行。",
      tone: "amber",
    };
  }

  if (heuristicCompleted) {
    return {
      label: "未使用大模型",
      title: "当前使用的是本地启发式规划",
      detail: "“启发式规划”就是没有真正拿到大模型方案，而是按时长、字幕时间轴和基础规则自动切片。",
      tone: "amber",
    };
  }

  if (fusionAttempt || (task.value?.status === "PLANNING" && visionResponse)) {
    return {
      label: "等待融合规划返回",
      title: "系统正在结合视觉事件、字幕和音频输出最终切点",
      detail: "只有出现“融合规划已返回结果”或“本次最终切点由某个模型规划”，才代表这次真正拿到了最终剪辑时间点。",
      tone: "fuchsia",
    };
  }

  if (llmAttempt || (task.value?.status === "PLANNING" && planningStarted && !heuristicCompleted)) {
    return {
      label: "等待大模型返回",
      title: "系统正在调用大模型生成剪辑方案",
      detail: "只有出现“大模型已返回规划结果”或“本次规划由某个模型完成”，才代表这次真的用了大模型。",
      tone: "fuchsia",
    };
  }

  if (visionAttempt || (task.value?.status === "PLANNING" && planningStarted)) {
    return {
      label: "等待视频理解返回",
      title: "系统正在分析视频关键帧里的剧情事件",
      detail: "只有出现“视觉理解已返回结果”或“视觉事件已识别”，才代表这次真的分析了视频内容。",
      tone: "sky",
    };
  }

  return {
    label: "等待规划开始",
    title: "还没进入最终规划结果阶段",
    detail: "任务进入规划阶段后，这里会明确告诉你是大模型规划，还是回退为本地启发式规划。",
    tone: "slate",
  };
});

const currentFocus = computed<TraceFocusItem>(() => {
  const failure = lastTraceEvent("render.clip_failed", "task.failed", "vision.timeout", "vision.http_error", "vision.network_error", "fusion.timeout", "fusion.http_error", "fusion.network_error", "qwen.timeout", "qwen.http_error", "qwen.network_error");
  if (task.value?.status === "FAILED") {
    return {
      title: "任务当前处于失败状态",
      detail: task.value.errorMessage || (failure ? describeTraceEvent(failure).detail : "请查看最近一条错误日志定位失败点。"),
      tone: "rose",
      timestamp: failure?.timestamp ?? task.value.finishedAt ?? undefined,
    };
  }

  if (task.value?.status === "COMPLETED") {
    return {
      title: "全部剪辑已经完成",
      detail: `当前共生成 ${completedOutputCount.value} / ${task.value.outputCount} 条输出，可直接预览和下载。`,
      tone: "emerald",
      timestamp: task.value.finishedAt ?? lastTraceEvent("task.completed")?.timestamp ?? undefined,
    };
  }

  const activeRender = lastTraceEvent("render.clip_started", "render.started");
  if (task.value?.status === "RENDERING" && activeRender) {
    return {
      title: "正在执行 FFmpeg 剪辑",
      detail: `当前已完成 ${completedOutputCount.value} / ${task.value.outputCount} 条输出，进度会随着每条成片生成实时刷新。`,
      tone: "amber",
      timestamp: activeRender.timestamp,
    };
  }

  const activeAudio = lastTraceEvent("audio.peaks_detected");
  if (task.value?.status === "PLANNING" && activeAudio && !hasTraceEvent("vision.response", "heuristic.completed", "planning.completed")) {
    return {
      title: "正在融合音频节奏卡点",
      detail: "系统已经提取音频峰值，正在把它和字幕时间轴、关键帧高点一起用于切点判断。",
      tone: "amber",
      timestamp: activeAudio.timestamp,
    };
  }

  const activeVision = lastTraceEvent("vision.request", "vision.attempt");
  if (task.value?.status === "PLANNING" && activeVision && !hasTraceEvent("vision.response", "fusion.response", "heuristic.completed", "planning.completed")) {
    return {
      title: "正在识别视频里的剧情事件",
      detail: describeTraceEvent(activeVision).detail || "系统正在把关键帧送给视觉模型，寻找高燃、反转和冲突事件。",
      tone: "sky",
      timestamp: activeVision.timestamp,
    };
  }

  const activeFusion = lastTraceEvent("fusion.request", "fusion.attempt");
  if (task.value?.status === "PLANNING" && activeFusion && !hasTraceEvent("fusion.response", "heuristic.completed", "planning.completed")) {
    return {
      title: "正在输出最终剪辑时间点",
      detail: describeTraceEvent(activeFusion).detail || "系统正在把视觉事件、字幕和音频卡点融合成最终剪辑方案。",
      tone: "fuchsia",
      timestamp: activeFusion.timestamp,
    };
  }

  const activeLLM = lastTraceEvent("qwen.request", "qwen.attempt");
  if (activeLLM && !hasTraceEvent("qwen.response", "heuristic.completed", "planning.completed")) {
    return {
      title: "正在等待大模型返回方案",
      detail: describeTraceEvent(activeLLM).detail || "模型请求已经发出，返回后会自动进入下一阶段。",
      tone: "fuchsia",
      timestamp: activeLLM.timestamp,
    };
  }

  const activePlanning = lastTraceEvent("planning.started", "heuristic.start");
  if (task.value?.status === "PLANNING" && activePlanning) {
    return {
      title: "正在生成剪辑规划",
      detail: describeTraceEvent(activePlanning).detail || "正在整理切点、标题和理由。",
      tone: activePlanning.event.startsWith("heuristic") ? "amber" : "sky",
      timestamp: activePlanning.timestamp,
    };
  }

  const activeAnalysis = lastTraceEvent("analysis.started");
  if (task.value?.status === "ANALYZING" && activeAnalysis) {
    return {
      title: "正在分析源视频素材",
      detail: "系统正在识别时长、分辨率和音轨，为后续规划做准备。",
      tone: "sky",
      timestamp: activeAnalysis.timestamp,
    };
  }

  const created = lastTraceEvent("task.created", "task.enqueued", "task.dispatched_inline");
  return {
    title: "任务已创建，等待更多进度",
    detail: "详情页会每 3 秒自动刷新一次，新的大模型调用和剪辑结果会直接出现在这里。",
    tone: "slate",
    timestamp: created?.timestamp,
  };
});

const stageProgressItems = computed<TraceProgressItem[]>(() => {
  const queueEvent = lastTraceEvent("task.created", "task.enqueued", "task.dispatched_inline", "task.claimed", "task.processing_started");
  const analysisCompleted = lastTraceEvent("analysis.completed");
  const analysisStarted = lastTraceEvent("analysis.started");
  const audioDetected = lastTraceEvent("audio.peaks_detected");
  const visionResponse = lastTraceEvent("vision.response");
  const visionFailed = lastTraceEvent("vision.timeout", "vision.http_error", "vision.network_error");
  const visionAttempt = lastTraceEvent("vision.request", "vision.attempt");
  const fusionResponse = lastTraceEvent("fusion.response");
  const fusionFailed = lastTraceEvent("fusion.timeout", "fusion.http_error", "fusion.network_error", "fusion.attempt_failed");
  const fusionAttempt = lastTraceEvent("fusion.request", "fusion.attempt", "fusion.vision_fallback");
  const llmResponse = lastTraceEvent("qwen.response");
  const heuristicCompleted = lastTraceEvent("heuristic.completed");
  const llmFailed = lastTraceEvent("qwen.timeout", "qwen.http_error", "qwen.network_error");
  const llmAttempt = lastTraceEvent("qwen.request", "qwen.attempt", "planning.started", "heuristic.start");
  const renderStarted = lastTraceEvent("render.started", "render.clip_started");
  const renderFailed = lastTraceEvent("render.clip_failed");
  const renderCompleted = lastTraceEvent("task.completed", "render.clip_completed");

  const queueStatus: ProgressStatus = queueEvent ? "done" : "pending";
  const analysisStatus: ProgressStatus =
    analysisCompleted ? "done" : task.value?.status === "FAILED" && analysisStarted ? "error" : analysisStarted || task.value?.status === "ANALYZING" ? "active" : "pending";
  const llmStatus: ProgressStatus =
    fusionResponse || llmResponse || heuristicCompleted || lastTraceEvent("planning.completed")
      ? "done"
      : task.value?.status === "FAILED" && (visionFailed || fusionFailed || llmFailed)
        ? "error"
        : fusionAttempt || visionAttempt || llmAttempt || task.value?.status === "PLANNING"
          ? "active"
          : "pending";
  const renderStatus: ProgressStatus =
    task.value?.status === "COMPLETED" || (task.value && completedOutputCount.value >= task.value.outputCount && task.value.outputCount > 0)
      ? "done"
      : task.value?.status === "FAILED" && (renderFailed || renderStarted)
        ? "error"
        : renderStarted || task.value?.status === "RENDERING"
          ? "active"
          : "pending";

  return [
    {
      key: "queue",
      label: "任务接入",
      description: "创建任务并进入执行器。",
      hint: queueEvent ? describeTraceEvent(queueEvent).title : "等待任务正式创建。",
      progress: queueEvent ? 100 : 0,
      status: queueStatus,
      tone: queueStatus === "done" ? "emerald" : "slate",
    },
    {
      key: "analysis",
      label: "素材分析",
      description: "识别时长、分辨率和音轨。",
      hint: analysisCompleted ? describeTraceEvent(analysisCompleted).detail : analysisStarted ? "正在读取素材基础信息。" : "等待开始分析。",
      progress: analysisCompleted ? 100 : analysisStarted || task.value?.status === "ANALYZING" ? 55 : 0,
      status: analysisStatus,
      tone: analysisStatus === "error" ? "rose" : analysisStatus === "done" ? "emerald" : analysisStatus === "active" ? "sky" : "slate",
    },
    {
      key: "planning",
      label: "视频理解 / 规划",
      description: "先识别剧情事件，再融合字幕、音频节奏和关键帧内容生成最终切点。",
      hint: fusionResponse
        ? describeTraceEvent(fusionResponse).detail
        : visionResponse
        ? describeTraceEvent(visionResponse).detail
        : audioDetected
          ? describeTraceEvent(audioDetected).detail
        : llmResponse
          ? describeTraceEvent(llmResponse).detail
          : heuristicCompleted
            ? describeTraceEvent(heuristicCompleted).detail
            : fusionFailed
              ? describeTraceEvent(fusionFailed).detail
          : visionFailed
            ? describeTraceEvent(visionFailed).detail
            : fusionAttempt
              ? describeTraceEvent(fusionAttempt).detail || "正在准备最终剪辑规划请求。"
            : llmFailed
              ? describeTraceEvent(llmFailed).detail
              : visionAttempt
                ? describeTraceEvent(visionAttempt).detail || "正在准备视频内容理解请求。"
              : llmAttempt
                ? describeTraceEvent(llmAttempt).detail || "正在准备规划请求。"
              : "等待进入规划阶段。",
      progress: fusionResponse || llmResponse || heuristicCompleted || lastTraceEvent("planning.completed") ? 100 : fusionAttempt ? 82 : visionResponse ? 62 : audioDetected || visionAttempt || llmAttempt || task.value?.status === "PLANNING" ? 42 : 0,
      status: llmStatus,
      tone: llmStatus === "error" ? "rose" : heuristicCompleted ? "amber" : fusionResponse ? "fuchsia" : fusionAttempt ? "fuchsia" : visionResponse ? "sky" : audioDetected ? "amber" : llmResponse ? "fuchsia" : visionAttempt ? "sky" : llmStatus === "active" ? "fuchsia" : "slate",
    },
    {
      key: "render",
      label: "FFmpeg 剪辑",
      description: "按规划切片并输出成片。",
      hint: renderFailed
        ? describeTraceEvent(renderFailed).detail
        : task.value?.status === "COMPLETED"
          ? `已完成 ${completedOutputCount.value} / ${task.value.outputCount} 条输出。`
          : renderStarted || task.value?.status === "RENDERING"
            ? `已完成 ${completedOutputCount.value} / ${task.value?.outputCount ?? 0} 条输出。`
            : "等待进入渲染阶段。",
      progress: renderStatus === "done" ? 100 : renderStageProgress.value,
      status: renderStatus,
      tone: renderStatus === "error" ? "rose" : renderStatus === "done" ? "emerald" : renderStatus === "active" ? "amber" : "slate",
    },
  ];
});

function getStageState(stage: "analysis" | "planning" | "rendering") {
  if (!task.value) {
    return "pending" as const;
  }

  const progress = task.value.progress ?? 0;
  if (task.value.status === "COMPLETED") {
    return "done" as const;
  }

  if (task.value.status === "FAILED") {
    if (stage === "analysis") {
      return progress >= 10 ? "done" as const : "current" as const;
    }
    if (stage === "planning") {
      return progress >= 25 ? "done" as const : progress >= 10 ? "current" as const : "pending" as const;
    }
    return progress >= 40 ? "current" as const : progress >= 25 ? "current" as const : "pending" as const;
  }

  if (task.value.status === "PENDING") {
    return stage === "analysis" ? "current" as const : "pending" as const;
  }

  if (task.value.status === "ANALYZING") {
    if (stage === "analysis") {
      return "current" as const;
    }
    return "pending" as const;
  }

  if (task.value.status === "PLANNING") {
    if (stage === "analysis") {
      return "done" as const;
    }
    if (stage === "planning") {
      return "current" as const;
    }
    return "pending" as const;
  }

  if (task.value.status === "RENDERING") {
    if (stage === "rendering") {
      return "current" as const;
    }
    return "done" as const;
  }

  return getTaskLifecycleGroup(task.value.status as TaskStatus) === "completed" ? "done" : "pending";
}

async function loadTask() {
  errorMessage.value = "";
  if (!taskId.value) {
    errorMessage.value = "缺少任务 ID";
    loading.value = false;
    stop();
    return;
  }
  loading.value = task.value === null;
  try {
    task.value = await fetchTask(taskId.value);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载任务详情失败";
  } finally {
    loading.value = false;
  }
}

async function loadTrace() {
  traceErrorMessage.value = "";
  if (!taskId.value) {
    traceEvents.value = [];
    return;
  }
  traceLoading.value = true;
  try {
    traceEvents.value = await fetchTaskTrace(taskId.value, 800);
  } catch (error) {
    traceErrorMessage.value = error instanceof Error ? error.message : "加载任务日志失败";
  } finally {
    traceLoading.value = false;
  }
}

async function handleRetry() {
  errorMessage.value = "";
  if (!taskId.value) {
    errorMessage.value = "缺少任务 ID";
    return;
  }
  actionLoading.value = true;
  try {
    task.value = await retryTask(taskId.value);
    await loadTrace();
    if (isTerminalTaskStatus(task.value.status)) {
      stop();
    } else {
      await start(false);
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "重试失败";
  } finally {
    actionLoading.value = false;
  }
}

async function handleDelete() {
  errorMessage.value = "";
  if (!taskId.value) {
    errorMessage.value = "缺少任务 ID";
    return;
  }
  if (!window.confirm(`确认删除任务“${task.value?.title || taskId.value}”吗？已生成的输出和日志也会一并清理。`)) {
    return;
  }
  actionLoading.value = true;
  try {
    await deleteTask(taskId.value);
    router.push("/tasks");
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "删除任务失败";
  } finally {
    actionLoading.value = false;
  }
}

function openCloneFlow() {
  if (!taskId.value) {
    return;
  }
  router.push({ path: "/tasks/new", query: { cloneFrom: taskId.value } });
}

const { start, stop } = usePolling(async () => {
  await loadTask();
  await loadTrace();
  if (task.value && isTerminalTaskStatus(task.value.status)) {
    stop();
  }
}, 3000);

watch(
  taskId,
  async (_, __, onCleanup) => {
    onCleanup(stop);
    task.value = null;
    errorMessage.value = "";
    traceEvents.value = [];
    traceErrorMessage.value = "";
    traceExpanded.value = false;
    loading.value = true;
    await start();
  },
  { immediate: true }
);
</script>
