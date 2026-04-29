<template>
  <div class="workspace-shell">
    <aside
      class="workspace-sidebar"
      :class="{
        'workspace-sidebar-open': sidebarOpen,
      }"
    >
      <div class="workspace-sidebar__top">
        <div class="workspace-sidebar__topbar">
          <div class="sidebar-brand">
            <img alt="煎豆 Logo" class="sidebar-brand__logo" src="/brand/jiandou-mark.svg" />
          </div>
        </div>

        <nav class="sidebar-nav">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            class="sidebar-nav__item"
            :class="{ 'sidebar-nav__item-active': isActive(item.to) }"
            :to="item.to"
          >
            <span class="sidebar-nav__icon" v-html="item.icon"></span>
            <span class="sidebar-nav__label">{{ item.label }}</span>
          </RouterLink>
        </nav>
      </div>

      <section class="sidebar-dev">
        <label class="sidebar-dev__toggle">
          <span>Dev</span>
          <input v-model="developerSettings.enabled" type="checkbox" />
          <span class="sidebar-dev__switch" :class="{ 'sidebar-dev__switch-on': developerSettings.enabled }"></span>
        </label>
        <label v-if="developerSettings.enabled" class="sidebar-dev__option">
          <input v-model="developerSettings.stopBeforeVideoGeneration" type="checkbox" />
          <div>
            <strong>视频生成前停止</strong>
          </div>
        </label>

        <div class="sidebar-user-card">
          <div class="sidebar-user-card__header">
            <div class="sidebar-user-card__avatar">{{ avatarInitials }}</div>
            <div>
              <p class="sidebar-user-card__name">{{ currentUser?.displayName || "未登录" }}</p>
              <p class="sidebar-user-card__meta">{{ currentUser?.username || "-" }} · {{ currentUser?.role || "-" }}</p>
            </div>
          </div>
          <div class="sidebar-user-card__actions">
            <a
              v-if="isAdmin"
              class="sidebar-user-card__link"
              :href="adminPortalUrl"
            >
              管理端
            </a>
            <button class="sidebar-user-card__logout" type="button" @click="handleLogout">
              退出登录
            </button>
          </div>
        </div>
      </section>
    </aside>

    <div v-if="sidebarOpen" class="workspace-sidebar-mask" @click="sidebarOpen = false"></div>

    <div class="workspace-main">
      <header class="workspace-mobile-bar">
        <button class="shell-ghost-btn" type="button" @click="sidebarOpen = true">菜单</button>
        <p class="workspace-mobile-bar__title">{{ currentTitle }}</p>
        <span class="workspace-mobile-bar__placeholder"></span>
      </header>

      <main class="workspace-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 工作区组件。
 */
import { computed, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getRuntimeConfig } from "@/api/runtime-config";
import { logoutAndClearSession, useAuthSessionState } from "@/auth/session";
import { loadDeveloperSettings, saveDeveloperSettings } from "@/workbench/developer-settings";

const route = useRoute();
const router = useRouter();
const authState = useAuthSessionState();
const adminPortalUrl = getRuntimeConfig().adminBaseUrl;

const navItems = [
  {
    to: "/workspace",
    label: "工作台",
    icon: `
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M3 11.5 12 4l9 7.5" />
        <path d="M5.5 10.5V20h13V10.5" />
      </svg>
    `,
  },
  {
    to: "/workflows",
    label: "阶段工作流",
    icon: `
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M5 6h6v4H5z" />
        <path d="M13 14h6v4h-6z" />
        <path d="M11 8h2a3 3 0 0 1 3 3v3" />
        <path d="M8 10v8" />
        <path d="M5 18h6" />
      </svg>
    `,
  },
  {
    to: "/tasks",
    label: "任务管理",
    icon: `
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <rect x="4" y="4" width="6" height="6" rx="1.5" />
        <rect x="14" y="4" width="6" height="6" rx="1.5" />
        <rect x="4" y="14" width="6" height="6" rx="1.5" />
        <rect x="14" y="14" width="6" height="6" rx="1.5" />
      </svg>
    `,
  },
  {
    to: "/material-center",
    label: "素材中心",
    icon: `
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M4 6.5h16" />
        <path d="M6.5 4v5" />
        <path d="M17.5 4v5" />
        <rect x="4" y="11" width="7" height="8" rx="1.6" />
        <rect x="13" y="11" width="7" height="8" rx="1.6" />
        <path d="m7 16 1.4-1.6 2.1 2.6" />
        <path d="m16 16 1.1-1.2 1.9 2.2" />
      </svg>
    `,
  },
  {
    to: "/materials",
    label: "素材库",
    icon: `
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M4 7.5 12 4l8 3.5-8 3.5-8-3.5Z" />
        <path d="M4 12l8 3.5 8-3.5" />
        <path d="M4 16.5 12 20l8-3.5" />
      </svg>
    `,
  },
  {
    to: "/settings",
    label: "设置",
    icon: `
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <circle cx="12" cy="12" r="3.2" />
        <path d="M19.4 15a1 1 0 0 0 .2 1.1l.1.1a1.2 1.2 0 0 1 0 1.7l-1.2 1.2a1.2 1.2 0 0 1-1.7 0l-.1-.1a1 1 0 0 0-1.1-.2 1 1 0 0 0-.6.9V20a1.2 1.2 0 0 1-1.2 1.2h-1.7A1.2 1.2 0 0 1 11 20v-.2a1 1 0 0 0-.6-.9 1 1 0 0 0-1.1.2l-.1.1a1.2 1.2 0 0 1-1.7 0l-1.2-1.2a1.2 1.2 0 0 1 0-1.7l.1-.1a1 1 0 0 0 .2-1.1 1 1 0 0 0-.9-.6H4A1.2 1.2 0 0 1 2.8 13v-2A1.2 1.2 0 0 1 4 9.8h.2a1 1 0 0 0 .9-.6 1 1 0 0 0-.2-1.1L4.8 8A1.2 1.2 0 0 1 4.8 6.3l1.2-1.2a1.2 1.2 0 0 1 1.7 0l.1.1a1 1 0 0 0 1.1.2 1 1 0 0 0 .6-.9V4A1.2 1.2 0 0 1 10.7 2.8h1.7A1.2 1.2 0 0 1 13.6 4v.2a1 1 0 0 0 .6.9 1 1 0 0 0 1.1-.2l.1-.1a1.2 1.2 0 0 1 1.7 0l1.2 1.2a1.2 1.2 0 0 1 0 1.7l-.1.1a1 1 0 0 0-.2 1.1 1 1 0 0 0 .9.6h.2A1.2 1.2 0 0 1 21.2 11v2a1.2 1.2 0 0 1-1.2 1.2h-.2a1 1 0 0 0-.9.8Z" />
      </svg>
    `,
  },
];

const sidebarOpen = ref(false);
const developerSettings = reactive(loadDeveloperSettings());

const currentUser = computed(() => authState.user.value);
const isAdmin = computed(() => authState.isAdmin.value);
const avatarInitials = computed(() => {
  const source = currentUser.value?.displayName || currentUser.value?.username || "JD";
  return source.slice(0, 2).toUpperCase();
});
function isActive(target: string) {
  return route.path === target || route.path.startsWith(`${target}/`);
}

const currentTitle = computed(() => {
  const metaTitle = route.meta?.title;
  return typeof metaTitle === "string" && metaTitle.trim() ? metaTitle : "煎豆工作台";
});

async function handleLogout() {
  await logoutAndClearSession();
  await router.replace("/login");
}

watch(
  () => route.fullPath,
  () => {
    sidebarOpen.value = false;
  },
);

watch(
  developerSettings,
  (value) => {
    saveDeveloperSettings({ ...value });
  },
  { deep: true },
);
</script>

<style scoped>
.workspace-shell {
  position: relative;
  display: flex;
  height: 100vh;
  min-height: 100vh;
  background: var(--bg-base);
  color: var(--text-strong);
  overflow: hidden;
}

.workspace-sidebar {
  position: relative;
  z-index: 30;
  height: 100%;
  width: 86px;
  flex: 0 0 86px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 18px 10px;
  border-right: 1px solid rgba(15, 20, 25, 0.06);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(18px);
}

.workspace-sidebar__top {
  display: grid;
  gap: 210px;
  justify-items: center;
}

.workspace-sidebar__topbar {
  display: grid;
  place-items: center;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-brand__logo {
  display: block;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  object-fit: contain;
}

.sidebar-nav {
  display: grid;
  gap: 16px;
  justify-items: center;
}

.sidebar-nav__item {
  position: relative;
  display: grid;
  justify-items: center;
  align-content: center;
  align-items: center;
  gap: 5px;
  width: 64px;
  min-height: 48px;
  padding: 4px 0;
  border-radius: 12px;
  color: #0f1419;
  border: 0;
  transition:
    color 180ms ease,
    background 180ms ease,
    transform 180ms ease;
}

.sidebar-nav__item:hover {
  transform: translateY(-1px);
  color: var(--accent-cyan);
}

.sidebar-nav__item-active {
  color: var(--accent-cyan);
  font-weight: 800;
}

.sidebar-nav__item-active::before {
  content: "";
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 999px;
  background: var(--accent-cyan);
}

.sidebar-nav__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  color: currentColor;
}

.sidebar-nav__icon :deep(svg) {
  width: 100%;
  height: 100%;
  aspect-ratio: 1 / 1;
}

.sidebar-nav__label {
  display: inline-block;
  max-width: 60px;
  overflow: visible;
  white-space: nowrap;
  color: currentColor;
  font-size: 0.72rem;
  line-height: 1;
}

.sidebar-dev {
  display: grid;
  justify-items: center;
  gap: 14px;
}

.sidebar-dev__toggle {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 6px;
  color: var(--text-muted);
  font-size: 0.68rem;
  font-weight: 700;
}

.sidebar-dev__toggle input,
.sidebar-dev__option input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.sidebar-dev__switch {
  position: relative;
  width: 38px;
  height: 22px;
  border-radius: 999px;
  background: #e1e5e8;
  transition: background 180ms ease;
}

.sidebar-dev__switch::after {
  content: "";
  position: absolute;
  top: 3px;
  left: 3px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(15, 20, 25, 0.14);
  transition: transform 180ms ease;
}

.sidebar-dev__switch-on {
  background: var(--accent-cyan);
}

.sidebar-dev__switch-on::after {
  transform: translateX(16px);
}

.sidebar-dev__option {
  position: absolute;
  left: 74px;
  bottom: 18px;
  display: grid;
  gap: 4px;
  width: 180px;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid var(--surface-border);
  background: #fff;
  box-shadow: var(--shadow-panel);
}

.sidebar-dev__option strong {
  font-size: 0.82rem;
  color: var(--text-strong);
}

.sidebar-user-card {
  display: none;
  gap: 12px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--surface-border);
  background: #fff;
}

.sidebar-user-card__header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sidebar-user-card__avatar {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--bg-accent);
  color: #fff;
  font-size: 0.76rem;
  font-weight: 800;
}

.sidebar-user-card__name {
  margin: 0;
  color: var(--text-strong);
  font-size: 0.9rem;
  font-weight: 700;
}

.sidebar-user-card__meta {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 0.76rem;
}

.sidebar-user-card__actions {
  display: flex;
  gap: 8px;
}

.sidebar-user-card__link,
.sidebar-user-card__logout {
  flex: 1;
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  border: 1px solid var(--surface-border);
  background: #f6f8f9;
  color: var(--text-strong);
  font-size: 0.8rem;
  font-weight: 700;
}

.sidebar-user-card__logout {
  cursor: pointer;
}

.workspace-main {
  position: relative;
  z-index: 1;
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.workspace-content {
  flex: 1;
  min-width: 0;
  min-height: 0;
  padding: 0;
  overflow: auto;
}

.workspace-mobile-bar {
  display: none;
}

.workspace-sidebar-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  background: rgba(15, 20, 25, 0.24);
  backdrop-filter: blur(4px);
}

.shell-ghost-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--surface-border);
  background: #fff;
  color: var(--text-strong);
}

.workspace-mobile-bar__title {
  margin: 0;
  color: var(--text-strong);
  font-size: 0.92rem;
  font-weight: 700;
}

.workspace-mobile-bar__placeholder {
  width: 56px;
}

@media (max-width: 1024px) {
  .sidebar-user-card {
    display: grid;
  }

  .workspace-sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: 228px;
    padding: 20px 14px 22px;
    transform: translateX(-100%);
    transition: transform 180ms ease;
  }

  .workspace-sidebar__top {
    gap: 24px;
    justify-items: stretch;
  }

  .sidebar-nav {
    justify-items: stretch;
  }

  .sidebar-nav__item {
    grid-template-columns: 22px minmax(0, 1fr);
    justify-items: start;
    width: auto;
    min-height: 46px;
    padding: 0 12px;
    gap: 12px;
  }

  .sidebar-nav__label {
    max-width: none;
    font-size: 0.86rem;
  }

  .sidebar-dev {
    justify-items: stretch;
  }

  .sidebar-dev__toggle {
    display: flex;
    justify-content: space-between;
    font-size: 0.82rem;
  }

  .sidebar-dev__option {
    position: static;
    width: auto;
  }

  .workspace-sidebar-open {
    transform: translateX(0);
  }

  .workspace-mobile-bar {
    position: sticky;
    top: 0;
    z-index: 10;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    min-height: 52px;
    padding: 8px 16px;
    border-bottom: 1px solid rgba(15, 20, 25, 0.06);
    background: rgba(246, 247, 248, 0.9);
    backdrop-filter: blur(20px);
  }
}
</style>
