<template>
  <div class="workspace-shell">
    <div class="workspace-shell__aurora workspace-shell__aurora-left" aria-hidden="true"></div>
    <div class="workspace-shell__aurora workspace-shell__aurora-right" aria-hidden="true"></div>

    <aside
      class="workspace-sidebar"
      :class="{
        'workspace-sidebar-open': sidebarOpen,
        'workspace-sidebar-collapsed': sidebarCollapsed,
      }"
    >
      <div class="workspace-sidebar__top">
        <div class="workspace-sidebar__topbar">
          <div class="sidebar-brand">
            <img alt="煎豆 Logo" class="sidebar-brand__logo" src="/brand/jiandou-mark.svg" />
            <span class="sidebar-brand__time">{{ currentTimeLabel }}</span>
          </div>
          <button
            class="sidebar-collapse-btn"
            type="button"
            :aria-label="sidebarCollapsed ? '展开系统菜单' : '收起系统菜单'"
            :title="sidebarCollapsed ? '展开系统菜单' : '收起系统菜单'"
            @click="sidebarCollapsed = !sidebarCollapsed"
          >
            <span v-if="sidebarCollapsed">»</span>
            <span v-else>«</span>
          </button>
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
        <div class="sidebar-dev__line"></div>
        <label class="sidebar-dev__toggle">
          <span>开发者模式</span>
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

      <header class="workspace-header">
        <h1 class="workspace-header__title">{{ currentTitle }}</h1>
        <div class="workspace-header__meta">
          <div class="workspace-header__identity">
            <div class="workspace-header__avatar">{{ avatarInitials }}</div>
            <div class="workspace-header__user">
              <span class="workspace-header__user-name">{{ currentUser?.displayName || "未登录" }}</span>
              <span class="workspace-header__user-role">{{ currentUser?.username || "-" }} · {{ currentUser?.role || "-" }}</span>
            </div>
          </div>
          <div class="workspace-header__actions">
            <a
              v-if="isAdmin"
              class="workspace-header__link"
              :href="adminPortalUrl"
            >
              管理端
            </a>
            <button class="workspace-header__logout" type="button" @click="handleLogout">
              退出登录
            </button>
          </div>
        </div>
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
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getRuntimeConfig } from "@/api/runtime-config";
import { logoutAndClearSession, useAuthSessionState } from "@/auth/session";
import { loadDeveloperSettings, saveDeveloperSettings } from "@/workbench/developer-settings";

const SIDEBAR_COLLAPSED_STORAGE_KEY = "jiandou-workspace-sidebar-collapsed-v1";

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
    to: "/generate",
    label: "一键生成",
    icon: `
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M7 3.5h7l4 4V20a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 6 20V5A1.5 1.5 0 0 1 7.5 3.5Z" />
        <path d="M14 3.5V8h4" />
        <path d="M9 12h6" />
        <path d="M9 16h6" />
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

function loadSidebarCollapsed() {
  if (typeof window === "undefined") {
    return false;
  }
  try {
    return window.localStorage.getItem(SIDEBAR_COLLAPSED_STORAGE_KEY) === "true";
  } catch {
    return false;
  }
}

function saveSidebarCollapsed(value: boolean) {
  if (typeof window === "undefined") {
    return;
  }
  try {
    window.localStorage.setItem(SIDEBAR_COLLAPSED_STORAGE_KEY, String(value));
  } catch {
    // 忽略持久化失败，避免影响主流程。
  }
}

const sidebarOpen = ref(false);
const sidebarCollapsed = ref(loadSidebarCollapsed());
const developerSettings = reactive(loadDeveloperSettings());
const currentTime = ref(Date.now());
let clockTimer: number | null = null;

const currentUser = computed(() => authState.user.value);
const isAdmin = computed(() => authState.isAdmin.value);
const avatarInitials = computed(() => {
  const source = currentUser.value?.displayName || currentUser.value?.username || "JD";
  return source.slice(0, 2).toUpperCase();
});
const currentTimeLabel = computed(() =>
  new Intl.DateTimeFormat("zh-CN", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(currentTime.value)),
);

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

watch(sidebarCollapsed, (value) => {
  saveSidebarCollapsed(value);
});

onMounted(() => {
  clockTimer = window.setInterval(() => {
    currentTime.value = Date.now();
  }, 1000);
});

onBeforeUnmount(() => {
  if (clockTimer !== null) {
    window.clearInterval(clockTimer);
    clockTimer = null;
  }
});
</script>

<style scoped>
.workspace-shell {
  position: relative;
  display: flex;
  height: 100vh;
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(78, 208, 255, 0.12), transparent 24%),
    radial-gradient(circle at bottom left, rgba(164, 83, 255, 0.18), transparent 28%),
    radial-gradient(circle at 84% 84%, rgba(72, 198, 255, 0.12), transparent 24%),
    #06070b;
  color: var(--text-strong);
  overflow: hidden;
}

.workspace-shell__aurora {
  position: absolute;
  border-radius: 999px;
  filter: blur(84px);
  opacity: 0.45;
  pointer-events: none;
}

.workspace-shell__aurora-left {
  left: -140px;
  top: -60px;
  width: 320px;
  height: 320px;
  background: rgba(78, 208, 255, 0.2);
}

.workspace-shell__aurora-right {
  right: -120px;
  bottom: 60px;
  width: 300px;
  height: 300px;
  background: rgba(164, 83, 255, 0.18);
}

.workspace-sidebar {
  position: relative;
  z-index: 30;
  height: 100%;
  width: 228px;
  flex: 0 0 228px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 20px 14px 22px;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(180deg, rgba(9, 11, 17, 0.92), rgba(5, 7, 11, 0.84));
  backdrop-filter: blur(24px);
  transition:
    width 220ms linear,
    flex-basis 220ms linear,
    padding 220ms linear,
    box-shadow 220ms linear;
}

.workspace-sidebar__top {
  display: grid;
  gap: 22px;
  justify-items: stretch;
}

.workspace-sidebar__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  transition: gap 220ms linear;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  transition: gap 220ms linear, width 220ms linear;
}

.sidebar-brand__logo {
  display: block;
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  filter: drop-shadow(0 0 18px rgba(104, 128, 255, 0.22));
  object-fit: contain;
  transition: filter 320ms ease;
}

.sidebar-brand__time {
  display: inline-flex;
  color: rgba(255, 255, 255, 0.92);
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  white-space: nowrap;
  max-width: 72px;
  overflow: hidden;
  opacity: 1;
  transition:
    max-width 180ms linear,
    opacity 160ms linear;
}

.sidebar-collapse-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.82);
  font-size: 1rem;
  line-height: 1;
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease;
}

.sidebar-collapse-btn:hover {
  transform: translateY(-1px);
  border-color: rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.08);
}

.sidebar-nav {
  display: grid;
  gap: 8px;
  justify-items: stretch;
}

.sidebar-nav__item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 46px;
  padding: 0 12px;
  border-radius: 13px;
  color: var(--text-muted);
  border: 1px solid transparent;
  transition:
    color 180ms ease,
    border-color 180ms ease,
    background 180ms ease,
    box-shadow 180ms ease,
    padding 220ms linear,
    gap 220ms linear,
    width 220ms linear,
    min-height 220ms linear;
}

.sidebar-nav__item:hover {
  transform: translateY(-1px);
  color: rgba(255, 255, 255, 0.92);
}

.sidebar-nav__item-active {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.04));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 10px 24px rgba(0, 0, 0, 0.28);
}

.sidebar-nav__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: currentColor;
  flex: 0 0 20px;
}

.sidebar-nav__icon :deep(svg) {
  width: 100%;
  height: 100%;
  aspect-ratio: 1 / 1;
}

.sidebar-nav__label {
  display: inline-block;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  max-width: 120px;
  opacity: 1;
  transition:
    max-width 180ms linear,
    opacity 160ms linear;
}

.sidebar-nav__label {
  min-width: 0;
}

.sidebar-dev {
  display: grid;
  gap: 12px;
  max-height: 320px;
  margin-top: 0;
  overflow: hidden;
  opacity: 1;
  transform: none;
  transform-origin: left bottom;
  transition:
    max-height 220ms linear,
    opacity 160ms linear,
    transform 220ms linear,
    margin-top 220ms linear;
}

.sidebar-dev__line {
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0));
}

.sidebar-dev__toggle {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 0.82rem;
}

.sidebar-dev__toggle input,
.sidebar-dev__option input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.sidebar-dev__switch {
  position: relative;
  width: 42px;
  height: 24px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
  transition: background 180ms ease;
}

.sidebar-dev__switch::after {
  content: "";
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  transition: transform 180ms ease;
}

.sidebar-dev__switch-on {
  background: linear-gradient(90deg, rgba(164, 83, 255, 0.9), rgba(69, 215, 255, 0.9));
}

.sidebar-dev__switch-on::after {
  transform: translateX(18px);
}

.sidebar-dev__option {
  display: grid;
  gap: 4px;
  padding: 12px 12px 12px 16px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
}

.sidebar-dev__option strong {
  font-size: 0.82rem;
  color: rgba(255, 255, 255, 0.88);
}

.sidebar-dev__option small {
  color: rgba(255, 255, 255, 0.58);
  line-height: 1.55;
}

.sidebar-user-card {
  display: none;
  gap: 12px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
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
  background: linear-gradient(135deg, rgba(255, 169, 77, 0.9), rgba(71, 193, 255, 0.88));
  color: #06111a;
  font-size: 0.76rem;
  font-weight: 800;
}

.sidebar-user-card__name {
  margin: 0;
  color: #fff;
  font-size: 0.9rem;
  font-weight: 700;
}

.sidebar-user-card__meta {
  margin: 4px 0 0;
  color: rgba(255, 255, 255, 0.56);
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
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.84);
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

.workspace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 60px;
  padding: 0 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.workspace-header__title {
  margin: 0;
  font-size: clamp(1.18rem, 1.45vw, 1.72rem);
  font-weight: 700;
  letter-spacing: -0.04em;
  color: #f5f7fb;
}

.workspace-header__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(255, 255, 255, 0.03);
}

.workspace-header__identity {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.workspace-header__avatar {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 169, 77, 0.9), rgba(71, 193, 255, 0.88));
  color: #06111a;
  font-size: 0.72rem;
  font-weight: 800;
}

.workspace-header__user {
  display: grid;
  justify-items: start;
  gap: 2px;
  min-width: 0;
}

.workspace-header__user-name {
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.8rem;
  font-weight: 700;
}

.workspace-header__user-role {
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.7rem;
}

.workspace-header__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.workspace-header__link,
.workspace-header__logout {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.84);
  font-size: 0.76rem;
  font-weight: 700;
  white-space: nowrap;
}

.workspace-header__logout {
  cursor: pointer;
}

.workspace-content {
  flex: 1;
  min-width: 0;
  min-height: 0;
  padding: 18px 22px 22px;
  overflow: hidden;
}

.workspace-sidebar-collapsed {
  width: 76px;
  flex-basis: 76px;
  padding: 18px 10px;
}

.workspace-sidebar-collapsed .workspace-sidebar__top {
  gap: 20px;
  justify-items: center;
}

.workspace-sidebar-collapsed .workspace-sidebar__topbar {
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
}

.workspace-sidebar-collapsed .sidebar-brand {
  justify-content: center;
  width: 48px;
  gap: 0;
}

.workspace-sidebar-collapsed .sidebar-brand__time {
  max-width: 0;
  opacity: 0;
}

.workspace-sidebar-collapsed .sidebar-collapse-btn {
  width: 48px;
  height: 48px;
  align-self: center;
}

.workspace-sidebar-collapsed .sidebar-nav {
  justify-items: center;
}

.workspace-sidebar-collapsed .sidebar-nav__item {
  justify-content: center;
  justify-self: center;
  width: 48px;
  min-height: 48px;
  padding: 0;
  gap: 0;
}

.workspace-sidebar-collapsed .sidebar-nav__label {
  max-width: 0;
  opacity: 0;
}

.workspace-sidebar-collapsed .sidebar-dev {
  max-height: 0;
  opacity: 0;
  margin-top: 0;
  pointer-events: none;
  transform: none;
}

.workspace-mobile-bar {
  display: none;
}

.workspace-sidebar-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(6px);
}

.shell-ghost-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.84);
}

.workspace-mobile-bar__title {
  margin: 0;
  color: rgba(255, 255, 255, 0.92);
  font-size: 0.92rem;
  font-weight: 700;
}

.workspace-mobile-bar__placeholder {
  width: 56px;
}

@media (max-width: 1024px) {
  .workspace-sidebar-collapsed {
    width: 228px;
    flex-basis: 228px;
    padding: 20px 14px 22px;
  }

  .workspace-sidebar-collapsed .workspace-sidebar__topbar {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .workspace-sidebar-collapsed .workspace-sidebar__top,
  .workspace-sidebar-collapsed .sidebar-nav {
    justify-items: stretch;
  }

  .workspace-sidebar-collapsed .sidebar-nav__item {
    justify-content: flex-start;
    justify-self: stretch;
    width: auto;
    min-height: 46px;
    padding: 0 12px;
  }

  .workspace-sidebar-collapsed .sidebar-nav__label {
    max-width: 120px;
    opacity: 1;
  }

  .workspace-sidebar-collapsed .sidebar-dev {
    display: grid;
    max-height: 320px;
    opacity: 1;
    margin-top: 0;
    pointer-events: auto;
    transform: none;
  }

  .sidebar-user-card {
    display: grid;
  }

  .sidebar-collapse-btn {
    display: none;
  }

  .workspace-sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    transform: translateX(-100%);
    transition: transform 180ms ease;
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
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    background: rgba(6, 8, 13, 0.92);
    backdrop-filter: blur(20px);
  }

  .workspace-header {
    display: none;
  }

  .workspace-content {
    padding: 16px;
  }
}
</style>
