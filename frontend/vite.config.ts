import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  // 🟢 核心配置：跨域代理
  server: {
    port: 5173, // 前端端口
    proxy: {
      // 只要请求以 /api 开头，就转发给后端
      '/api': {
        target: 'http://localhost:8080', // 后端地址
        changeOrigin: true, // 允许跨域
        rewrite: (path) => path.replace(/^\/api/, '') // 🟢 关键：把 /api 去掉
        // 比如：前端请求 /api/competition/list -> 转发给后端 /competition/list
      }
    }
  }
})
