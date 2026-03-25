<template>
  <article class="group min-w-0 rounded-[28px] border border-white/10 bg-[linear-gradient(180deg,rgba(15,15,35,0.84),rgba(8,11,24,0.7))] p-5 shadow-[0_20px_60px_rgba(0,0,0,0.32)] transition duration-200 hover:-translate-y-0.5 hover:border-rose-300/30 hover:shadow-[0_24px_80px_rgba(225,29,72,0.12)]">
    <div class="flex items-start justify-between gap-4">
      <div class="min-w-0">
        <p class="text-xs font-semibold uppercase tracking-[0.3em] text-slate-400">{{ task.platform }} / {{ task.aspectRatio ?? "9:16" }}</p>
        <h3 class="mt-2 truncate text-lg font-semibold text-white">{{ task.title }}</h3>
        <p class="mt-2 line-clamp-2 text-sm leading-6 text-slate-300">
          {{ task.sourceFileName || "源文件信息待同步" }}
        </p>
      </div>
      <div class="shrink-0">
        <StatusBadge :status="task.status" />
      </div>
    </div>

    <div class="mt-4 flex flex-wrap gap-2">
      <span v-if="task.hasTimedTranscript" class="rounded-full border border-sky-400/20 bg-sky-500/10 px-3 py-1 text-[11px] font-medium text-sky-100">时间轴字幕</span>
      <span v-else-if="task.hasTranscript" class="rounded-full border border-fuchsia-400/20 bg-fuchsia-500/10 px-3 py-1 text-[11px] font-medium text-fuchsia-100">文本语义</span>
      <span v-if="task.status === 'FAILED'" class="rounded-full border border-rose-400/20 bg-rose-500/10 px-3 py-1 text-[11px] font-medium text-rose-100">需要处理</span>
      <span v-if="task.status === 'COMPLETED'" class="rounded-full border border-emerald-400/20 bg-emerald-500/10 px-3 py-1 text-[11px] font-medium text-emerald-100">可复盘</span>
    </div>

    <div class="mt-4 grid grid-cols-2 gap-3 text-sm text-slate-300">
      <div class="rounded-2xl border border-white/8 bg-white/[0.04] p-3">
        <p class="text-xs uppercase tracking-[0.24em] text-slate-400">进度</p>
        <p class="mt-2 text-base font-semibold text-white">{{ task.progress }}%</p>
      </div>
      <div class="rounded-2xl border border-white/8 bg-white/[0.04] p-3">
        <p class="text-xs uppercase tracking-[0.24em] text-slate-400">输出</p>
        <p class="mt-2 text-base font-semibold text-white">{{ completedOutputCount }} / {{ task.outputCount }}</p>
      </div>
      <div class="rounded-2xl border border-white/8 bg-white/[0.04] p-3">
        <p class="text-xs uppercase tracking-[0.24em] text-slate-400">时长</p>
        <p class="mt-2 text-base font-semibold text-white">{{ durationLabel }}</p>
      </div>
      <div class="rounded-2xl border border-white/8 bg-white/[0.04] p-3">
        <p class="text-xs uppercase tracking-[0.24em] text-slate-400">重试</p>
        <p class="mt-2 text-base font-semibold text-white">{{ retryCount }}</p>
      </div>
    </div>

    <div class="mt-4 h-2 overflow-hidden rounded-full bg-white/10">
      <div class="h-full rounded-full bg-gradient-to-r from-rose-500 via-orange-400 to-amber-300 transition-all duration-300" :style="{ width: `${task.progress}%` }"></div>
    </div>

    <div class="mt-4 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-400">
      <span>更新时间 {{ updatedAtLabel }}</span>
      <span>{{ lifecycleLabel }}</span>
    </div>

    <div class="mt-4 flex flex-wrap gap-2">
      <RouterLink :to="`/tasks/${task.id}`" class="rounded-full border border-white/10 bg-white/[0.04] px-4 py-2 text-sm text-slate-100 transition duration-200 hover:border-rose-300/40 hover:bg-white/10">
        查看详情
      </RouterLink>
      <button class="rounded-full bg-rose-500 px-4 py-2 text-sm font-medium text-white transition duration-200 hover:bg-rose-400 disabled:cursor-not-allowed disabled:opacity-50" :disabled="busy" type="button" @click="$emit('clone', task)">
        复制参数
      </button>
      <button
        v-if="task.status === 'FAILED'"
        class="rounded-full border border-amber-300/25 bg-amber-500/10 px-4 py-2 text-sm font-medium text-amber-100 transition duration-200 hover:bg-amber-500/20 disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="busy"
        type="button"
        @click="$emit('retry', task)"
      >
        失败重试
      </button>
      <button
        class="rounded-full border border-white/10 bg-slate-950/50 px-4 py-2 text-sm text-slate-200 transition duration-200 hover:border-rose-300/35 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
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
      return "归档完成";
    case "failed":
      return "处理失败";
    case "running":
      return "正在处理";
    default:
      return "等待开始";
  }
});
</script>
