<template>
  <div class="mb-6 flex flex-col gap-3">
    <p class="text-xs font-semibold uppercase tracking-[0.32em] text-slate-500">{{ eyebrow }}</p>
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div class="max-w-3xl">
        <h2 class="text-2xl font-semibold tracking-[-0.03em] text-slate-900 sm:text-[2.1rem]">{{ title }}</h2>
        <p class="mt-2 max-w-2xl text-sm leading-7 text-slate-600">{{ description }}</p>
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
