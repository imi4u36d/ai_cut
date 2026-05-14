import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? "http://127.0.0.1:8000";

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
      "@jiandou/api-client": fileURLToPath(new URL("../../packages/api-client/src/index.ts", import.meta.url)),
      "@jiandou/api-client/generated": fileURLToPath(new URL("../../packages/api-client/src/generated/index.ts", import.meta.url)),
      "@jiandou/frontend-domain": fileURLToPath(new URL("../../packages/frontend-domain/src/index.ts", import.meta.url)),
      "@jiandou/frontend-ui": fileURLToPath(new URL("../../packages/frontend-ui/src/index.ts", import.meta.url))
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          "vue-vendor": ["vue", "vue-router"],
          "element-plus": ["element-plus", "@element-plus/icons-vue"]
        }
      }
    }
  },
  server: {
    host: "0.0.0.0",
    port: 5174,
    proxy: {
      "/api/v3": {
        target: apiProxyTarget,
        changeOrigin: true
      },
      "/storage": {
        target: apiProxyTarget,
        changeOrigin: true
      }
    }
  }
});
