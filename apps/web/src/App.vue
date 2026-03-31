<template>
  <RouterView />
</template>

<script setup lang="ts">
import { onBeforeUnmount, watch } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();

function syncDocumentTheme(path: string) {
  const isAdminRoute = path === "/admin" || path.startsWith("/admin/");
  document.documentElement.classList.toggle("admin-mode", isAdminRoute);
  document.body.classList.toggle("admin-mode", isAdminRoute);
}

watch(
  () => route.path,
  (path) => {
    syncDocumentTheme(path);
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  document.documentElement.classList.remove("admin-mode");
  document.body.classList.remove("admin-mode");
});
</script>
