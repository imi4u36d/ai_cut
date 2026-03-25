<template>
  <article class="grid min-w-0 gap-4 rounded-[24px] border border-white/10 bg-[linear-gradient(180deg,rgba(15,15,35,0.88),rgba(8,11,24,0.74))] px-4 py-4 shadow-[0_14px_40px_rgba(0,0,0,0.2)] lg:grid-cols-[minmax(0,2.2fr)_minmax(0,1.15fr)_auto]">
    <div class="min-w-0">
      <div class="flex flex-wrap items-center gap-2">
        <StatusBadge :status="task.status" />
        <span class="text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-500">{{ task.platform }} / {{ task.aspectRatio ?? "9:16" }}</span>
        <span v-if="task.hasTimedTranscript" class="rounded-full border border-sky-400/20 bg-sky-500/10 px-2.5 py-1 text-[11px] text-sky-100">时间轴字幕</span>
        <span v-else-if="task.hasTranscript" class="rounded-full border border-fuchsia-400/20 bg-fuchsia-500/10 px-2.5 py-1 text-[11px] text-fuchsia-100">文本语义</span>
      </div>
      <h3 class="mt-3 break-words text-base font-semibold text-white">{{ task.title }}</h3>
      <p class="mt-1 break-all text-sm text-slate-300">{{ task.sourceFileName || "源文件信息待同步" }}</p>
      <div class="mt-3 h-1.5 overflow-hidden rounded-full bg-white/10">
        <div class="h-full rounded-full bg-gradient-to-r from-rose-500 via-orange-400 to-amber-300 transition-all duration-300" :style="{ width: `${task.progress}%` }"></div>
      </div>
      <div class="mt-3 flex flex-wrap gap-3 text-xs text-slate-400">
        <span>进度 {{ task.progress }}%</span>
        <span>输出 {{ completedOutputCount }} / {{ task.outputCount }}</span>
        <span>时长 {{ durationLabel }}</span>
        <span>重试 {{ retryCount }}</span>
      </div>
    </div>

    <div class="grid gap-2 text-sm text-slate-300 sm:grid-cols-2 lg:grid-cols-1">
      <div class="rounded-2xl border border-white/8 bg-white/[0.04] p-3">
        <p class="text-[11px] uppercase tracking-[0.24em] text-slate-500">更新时间</p>
        <p class="mt-2 text-sm font-medium text-white">{{ updatedAtLabel }}</p>
      </div>
      <div class="rounded-2xl border border-white/8 bg-white/[0.04] p-3">
        <p class="text-[11px] uppercase tracking-[0.24em] text-slate-500">状态说明</p>
        <p class="mt-2 text-sm font-medium text-white">{{ lifecycleLabel }}</p>
      </div>
    </div>

    <div class="flex flex-wrap items-start justify-start gap-2 lg:w-[230px] lg:flex-col lg:items-stretch">
      <RouterLink :to="`/tasks/${task.id}`" class="inline-flex min-h-[44px] items-center justify-center rounded-full border border-white/10 bg-white/[0.04] px-4 py-2 text-sm text-slate-100 transition duration-200 hover:border-rose-300/40 hover:bg-white/10">
        查看详情
      </RouterLink>
      <button class="min-h-[44px] rounded-full bg-rose-500 px-4 py-2 text-sm font-medium text-white transition duration-200 hover:bg-rose-400 disabled:cursor-not-allowed disabled:opacity-50" :disabled="busy" type="button" @click="$emit('clone', task)">
        复制参数
      </button>
      <button
        v-if="task.status === 'FAILED'"
        class="min-h-[44px] rounded-full border border-amber-300/25 bg-amber-500/10 px-4 py-2 text-sm font-medium text-amber-100 transition duration-200 hover:bg-amber-500/20 disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="busy"
        type="button"
        @click="$emit('retry', task)"
      >
        失败重试
      </button>
      <button
        class="min-h-[44px] rounded-full border border-white/10 bg-slate-950/50 px-4 py-2 text-sm text-slate-200 transition duration-200 hover:border-rose-300/35 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="busy || running"
        type="button"
        @click="$emit('delete', task)"
      >
        删除
      </button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { TaskListItem } from "@/types";
import StatusBadge from "./StatusBadge.vue";
import { formatTaskRange, getTaskLifecycleGroup } from "@/utils/task";

const props = defineProps<{
  task: TaskListItem;
  busy?: boolean;
}>();

defineEmits<{
  (event: "clone", task: TaskListItem): void;
  (event: "retry", task: TaskListItem): void;
  (event: "delete", task: TaskListItem): void;
}>();

const completedOutputCount = computed(() => props.task.completedOutputCount ?? 0);
const retryCount = computed(() => props.task.retryCount ?? 0);
const durationLabel = computed(() => {
  if (typeof props.task.minDurationSeconds === "number" && typeof props.task.maxDurationSeconds === "number") {
    return formatTaskRange(props.task.minDurationSeconds, props.task.maxDurationSeconds);
  }
  return "待配置";
});
const updatedAtLabel = computed(() => new Date(props.task.updatedAt).toLocaleString());
const running = computed(() => getTaskLifecycleGroup(props.task.status) === "running");
const lifecycleLabel = computed(() => {
  const lifecycle = getTaskLifecycleGroup(props.task.status);
  switch (lifecycle) {
    case "completed":
      return "已完成，可直接复盘";
    case "failed":
      return "失败，建议重试或删除";
    case "running":
      return "正在处理，请稍后";
    default:
      return "等待开始";
  }
});
</script>
