<template>
  <div class="relative min-h-screen overflow-x-hidden text-white">
    <div class="pointer-events-none absolute inset-0 z-0 bg-[radial-gradient(circle_at_top_left,_rgba(225,29,72,0.18),_transparent_28%),radial-gradient(circle_at_top_right,_rgba(59,130,246,0.12),_transparent_24%),linear-gradient(180deg,_rgba(15,15,35,0.92)_0%,_rgba(3,7,18,0.96)_52%,_#000_100%)]"></div>
    <div class="pointer-events-none absolute inset-x-0 top-0 z-0 h-40 bg-[linear-gradient(90deg,transparent,rgba(255,255,255,0.04),transparent)] opacity-60"></div>
    <AppShell />
  </div>
</template>

<script setup lang="ts">
import AppShell from "@/components/AppShell.vue";
</script>
