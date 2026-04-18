import { createRouter, createWebHistory } from "vue-router";
import { ensureAuthSession, useAuthSessionState } from "@/auth/session";
import AdminLayout from "@/layouts/AdminLayout.vue";
import ForbiddenView from "@/views/ForbiddenView.vue";
import LoginView from "@/views/LoginView.vue";
import UserManagementView from "@/views/UserManagementView.vue";

function normalizeRedirectTarget(value: unknown) {
  if (typeof value !== "string" || !value.startsWith("/") || value.startsWith("//")) {
    return "/users";
  }
  return value;
}

const authState = useAuthSessionState();

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/login",
      name: "login",
      component: LoginView,
      meta: {
        title: "管理员登录",
        guestOnly: true
      }
    },
    {
      path: "/403",
      name: "forbidden",
      component: ForbiddenView,
      meta: {
        title: "无权限访问"
      }
    },
    {
      path: "/",
      component: AdminLayout,
      meta: {
        requiresAdmin: true
      },
      children: [
        {
          path: "",
          redirect: "/users"
        },
        {
          path: "users",
          name: "users",
          component: UserManagementView,
          meta: {
            title: "用户管理"
          }
        }
      ]
    },
    {
      path: "/:pathMatch(.*)*",
      redirect: "/users"
    }
  ]
});

router.beforeEach(async (to) => {
  const requiresSession = to.matched.some((record) => record.meta.requiresAdmin || record.meta.guestOnly);
  if (requiresSession) {
    await ensureAuthSession();
  }
  const isAuthenticated = authState.isAuthenticated.value;
  const isAdmin = authState.isAdmin.value;
  const guestOnly = to.matched.some((record) => record.meta.guestOnly);
  const requiresAdmin = to.matched.some((record) => record.meta.requiresAdmin);

  if (guestOnly && isAuthenticated && isAdmin) {
    return normalizeRedirectTarget(to.query.redirect);
  }
  if (guestOnly && isAuthenticated && !isAdmin) {
    return {
      path: "/403"
    };
  }
  if (requiresAdmin && !isAuthenticated) {
    return {
      path: "/login",
      query: {
        redirect: to.fullPath
      }
    };
  }
  if (requiresAdmin && !isAdmin) {
    return {
      path: "/403"
    };
  }
  return true;
});

router.afterEach((to) => {
  const title = typeof to.meta.title === "string" && to.meta.title.trim() ? to.meta.title : "JianDou 管理系统";
  document.title = `${title} · JianDou Admin`;
});

export default router;
