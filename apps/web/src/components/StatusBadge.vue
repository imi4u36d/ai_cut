<template>
  <span :class="badgeClass" class="inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-semibold tracking-[0.18em]">
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
      return "border border-emerald-200 bg-emerald-50/95 text-emerald-700 shadow-[0_8px_18px_rgba(60,159,139,0.08)]";
    case "FAILED":
      return "border border-rose-200 bg-rose-50/95 text-rose-700 shadow-[0_8px_18px_rgba(217,95,119,0.08)]";
    case "RENDERING":
      return "border border-amber-200 bg-amber-50/95 text-amber-700 shadow-[0_8px_18px_rgba(237,177,78,0.08)]";
    case "PLANNING":
      return "border border-sky-200 bg-sky-50/95 text-sky-700 shadow-[0_8px_18px_rgba(107,146,255,0.08)]";
    case "ANALYZING":
      return "border border-cyan-200 bg-cyan-50/95 text-cyan-700 shadow-[0_8px_18px_rgba(88,190,214,0.08)]";
    default:
      return "border border-slate-200 bg-white/85 text-slate-600 shadow-[0_8px_18px_rgba(122,144,177,0.06)]";
  }
});
</script>
