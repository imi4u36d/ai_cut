<template>
  <div class="day-shell relative min-h-screen overflow-x-hidden text-slate-900">
    <div class="pointer-events-none absolute inset-0 z-0 bg-[radial-gradient(circle_at_14%_10%,rgba(255,255,255,0.94),transparent_24%),radial-gradient(circle_at_78%_14%,rgba(171,213,255,0.56),transparent_23%),radial-gradient(circle_at_52%_92%,rgba(191,236,226,0.72),transparent_34%)]"></div>
    <div class="pointer-events-none absolute inset-x-0 top-0 z-0 h-48 bg-[linear-gradient(180deg,rgba(255,255,255,0.72),transparent)]"></div>
    <div class="pointer-events-none absolute inset-0 z-0 opacity-[0.34]" style="background-image: linear-gradient(rgba(255,255,255,0.5) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.4) 1px, transparent 1px); background-size: 72px 72px;"></div>

    <div class="relative z-10 mx-auto flex min-h-screen max-w-[1360px] flex-col px-4 py-4 sm:px-6 lg:px-8 lg:py-6">
      <header class="surface-panel sticky top-4 z-20 mb-6 px-4 py-4 sm:px-6">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-3">
              <span class="surface-chip text-[11px] font-semibold uppercase tracking-[0.24em]">AI Cut</span>
              <span class="surface-chip text-[11px]">{{ executionModeLabel }}</span>
            </div>
            <h1 class="mt-4 text-3xl font-semibold tracking-[-0.04em] text-slate-900 sm:text-[3.2rem] sm:leading-[0.98]">
              极简任务工作流
            </h1>
            <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-600 sm:text-[15px]">
              用更轻的层级查看任务、启动新剪辑、跟踪进度和回收结果。界面只保留当前决策真正需要的信息。
            </p>
          </div>

          <div class="grid min-w-0 gap-3 lg:w-[460px]">
            <div class="surface-tile p-4">
              <div class="flex items-start justify-between gap-4">
                <div class="min-w-0">
                  <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-500">模型状态</p>
                  <p class="mt-2 text-base font-semibold text-slate-900">{{ modelTitle }}</p>
                  <p class="mt-1 text-sm leading-6 text-slate-600">{{ modelDescription }}</p>
                </div>
                <span class="shrink-0 rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.2em]" :class="modelBadgeClass">
                  {{ modelStatusLabel }}
                </span>
              </div>
            </div>
            <div class="grid gap-3 sm:grid-cols-2">
              <div class="surface-tile p-4">
                <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-500">主模型</p>
                <p class="mt-2 truncate text-sm font-semibold text-slate-900">{{ health?.runtime.model.primary_model || "读取中" }}</p>
                <p class="mt-1 text-xs text-slate-500">{{ health?.runtime.model.fallback_model || "无回退模型" }}</p>
              </div>
              <div class="surface-tile p-4">
                <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-500">流程</p>
                <p class="mt-2 text-sm font-semibold text-slate-900">Upload → Plan → Render</p>
                <p class="mt-1 text-xs text-slate-500">任务从创建到复盘保持单路径。</p>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-5 flex flex-wrap items-center gap-2 border-t border-white/50 pt-4">
          <RouterLink to="/tasks" class="btn-nav" :class="route.path === '/tasks' ? 'btn-nav-active' : ''">
            任务
          </RouterLink>
          <RouterLink to="/tasks/new" class="btn-nav" :class="route.path === '/tasks/new' ? 'btn-nav-active' : ''">
            新建
          </RouterLink>
          <RouterLink to="/admin" class="btn-ghost ml-auto">
            管理后台
          </RouterLink>
        </div>
      </header>

      <main class="flex-1 pb-10">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { fetchHealth } from "@/api/health";
import type { HealthResponse } from "@/types";

const health = ref<HealthResponse | null>(null);
const route = useRoute();

const modelStatusLabel = computed(() => {
  if (!health.value) {
    return "加载中";
  }
  return health.value.runtime.model.ready ? "就绪" : "待配置";
});

const modelBadgeClass = computed(() => {
  if (!health.value) {
    return "bg-slate-200/90 text-slate-600";
  }
  return health.value.runtime.model.ready
    ? "bg-emerald-100 text-emerald-700"
    : "bg-amber-100 text-amber-700";
});

const modelTitle = computed(() => {
  if (!health.value) {
    return "读取运行状态中";
  }
  return health.value.runtime.model.ready ? "大模型规划已接通" : "模型配置未完成";
});

const modelDescription = computed(() => {
  if (!health.value) {
    return "正在读取 API、模型和规划能力配置。";
  }
  if (health.value.runtime.model.ready) {
    if (
      health.value.runtime.planning_capabilities.audio_peak_signal &&
      health.value.runtime.planning_capabilities.scene_boundary_signal &&
      health.value.runtime.planning_capabilities.fusion_timeline_planning
    ) {
      return "已启用四信号两阶段规划：视觉事件、字幕、音频卡点和镜头切换会一起决定最终切点。";
    }
    if (health.value.runtime.planning_capabilities.visual_event_reasoning) {
      return "已启用视觉事件识别，会先分析关键帧里的冲突、反转和高燃点，再交给规划模型决定切点。";
    }
    if (health.value.runtime.planning_capabilities.subtitle_visual_fusion) {
      return "已启用视频内容理解 + 字幕时间轴融合，会把关键帧高点和对白冲突点一起拿来决定切点。";
    }
    if (health.value.runtime.planning_capabilities.visual_content_analysis) {
      return "已启用视频内容理解规划，会先分析关键帧里的冲突、反转和高燃点，再决定切点。";
    }
    return health.value.runtime.planning_capabilities.timed_transcript_supported
      ? "支持带时间戳字幕驱动的语义规划，不需要消耗 token 做在线自检。"
      : "已配置模型，但语义规划能力未完全打开。";
  }
  const errors = health.value.runtime.model.config_errors.join(", ");
  return errors ? `缺失项：${errors}` : "当前仅能依赖本地启发式切条。";
});

const executionModeLabel = computed(() => {
  if (!health.value) {
    return "Light workbench";
  }
  return health.value.runtime.execution_mode === "queue" ? "Queue workbench" : "Inline workbench";
});

onMounted(async () => {
  try {
    health.value = await fetchHealth();
  } catch {
    health.value = null;
  }
});
</script>
