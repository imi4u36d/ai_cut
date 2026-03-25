<template>
  <div :class="containerClass" class="rounded-[22px] border p-4 transition duration-200">
    <div class="flex items-start justify-between gap-3">
      <div>
        <span class="text-sm font-semibold text-white">{{ label }}</span>
        <p class="mt-1 text-xs leading-5 text-slate-400">{{ description }}</p>
      </div>
      <span :class="stateClass" class="rounded-full px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.28em]">
        {{ stateLabel }}
      </span>
    </div>
    <div class="mt-4 h-2 overflow-hidden rounded-full bg-white/10">
      <div :class="barClass" class="h-full rounded-full transition-all duration-300" :style="{ width: barWidth }"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  label: string;
  description?: string;
  state: "done" | "current" | "pending";
}>();

const stateLabel = computed(() => {
  switch (props.state) {
    case "done":
      return "完成";
    case "current":
      return "进行中";
    default:
      return "待机";
  }
});

const barWidth = computed(() => {
  switch (props.state) {
    case "done":
      return "100%";
    case "current":
      return "66%";
    default:
      return "0%";
  }
});

const containerClass = computed(() => {
  switch (props.state) {
    case "done":
      return "border-emerald-400/15 bg-emerald-500/[0.06]";
    case "current":
      return "border-rose-400/15 bg-rose-500/[0.06]";
    default:
      return "border-white/10 bg-slate-950/50";
  }
});

const stateClass = computed(() => {
  switch (props.state) {
    case "done":
      return "bg-emerald-500/15 text-emerald-100";
    case "current":
      return "bg-rose-500/15 text-rose-100";
    default:
      return "bg-white/5 text-slate-300";
  }
});

const barClass = computed(() => {
  switch (props.state) {
    case "done":
      return "bg-gradient-to-r from-emerald-400 to-emerald-300";
    case "current":
      return "bg-gradient-to-r from-rose-500 to-amber-400";
    default:
      return "bg-slate-600";
  }
});
</script>
