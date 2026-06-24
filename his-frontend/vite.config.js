import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      // 拦截所有以 /api 开头的请求，转发到你的后端服务器
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
