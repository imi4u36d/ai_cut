import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? "http://127.0.0.1:8000";

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url))
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
      "/api/v2": {
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
