<template>
  <div class="mb-6 flex flex-col gap-3">
    <p class="text-xs font-semibold uppercase tracking-[0.34em] text-rose-300/90">{{ eyebrow }}</p>
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div class="max-w-3xl">
        <h2 class="text-2xl font-semibold tracking-tight text-white sm:text-3xl">{{ title }}</h2>
        <p class="mt-2 max-w-2xl text-sm leading-6 text-slate-300">{{ description }}</p>
      </div>
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow: string;
  title: string;
  description: string;
}>();
</script>
