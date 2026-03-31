<template>
  <div :class="containerClass" class="rounded-[24px] border p-4 shadow-[0_10px_24px_rgba(121,144,177,0.08)] transition duration-200">
    <div class="flex items-start justify-between gap-3">
      <div>
        <span class="text-sm font-semibold text-slate-900">{{ label }}</span>
        <p class="mt-1 text-xs leading-5 text-slate-500">{{ description }}</p>
      </div>
      <span :class="stateClass" class="rounded-full px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.28em]">
        {{ stateLabel }}
      </span>
    </div>
    <div class="mt-4 h-2 overflow-hidden rounded-full bg-slate-200/80">
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
      return "border-emerald-200 bg-white/75";
    case "current":
      return "border-sky-200 bg-white/82";
    default:
      return "border-white/70 bg-white/62";
  }
});

const stateClass = computed(() => {
  switch (props.state) {
    case "done":
      return "bg-emerald-50 text-emerald-700";
    case "current":
      return "bg-sky-50 text-sky-700";
    default:
      return "bg-slate-100 text-slate-600";
  }
});

const barClass = computed(() => {
  switch (props.state) {
    case "done":
      return "bg-gradient-to-r from-emerald-500 to-emerald-300";
    case "current":
      return "bg-gradient-to-r from-sky-500 to-indigo-400";
    default:
      return "bg-slate-300";
  }
});
</script>
