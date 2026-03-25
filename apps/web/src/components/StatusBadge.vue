<template>
  <span :class="badgeClass" class="inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-semibold tracking-wide">
    <span class="h-1.5 w-1.5 rounded-full bg-current opacity-90"></span>
    <span>{{ label }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { TaskStatus } from "@/types";
import { formatTaskStatus } from "@/utils/task";

const props = defineProps<{
  status: TaskStatus;
}>();

const label = computed(() => formatTaskStatus(props.status));

const badgeClass = computed(() => {
  switch (props.status) {
    case "COMPLETED":
      return "border border-emerald-400/20 bg-emerald-500/15 text-emerald-100 shadow-[0_0_0_1px_rgba(16,185,129,0.08)]";
    case "FAILED":
      return "border border-rose-400/20 bg-rose-500/15 text-rose-100 shadow-[0_0_0_1px_rgba(244,63,94,0.08)]";
    case "RENDERING":
      return "border border-amber-400/20 bg-amber-500/15 text-amber-100 shadow-[0_0_0_1px_rgba(245,158,11,0.08)]";
    case "PLANNING":
      return "border border-sky-400/20 bg-sky-500/15 text-sky-100 shadow-[0_0_0_1px_rgba(56,189,248,0.08)]";
    case "ANALYZING":
      return "border border-cyan-400/20 bg-cyan-500/15 text-cyan-100 shadow-[0_0_0_1px_rgba(34,211,238,0.08)]";
    default:
      return "border border-slate-400/20 bg-slate-500/15 text-slate-100 shadow-[0_0_0_1px_rgba(148,163,184,0.08)]";
  }
});
</script>
