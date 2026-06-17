import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler'
      }
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 后端未启动时自动重试，减少 ECONNREFUSED
        configure: (proxy) => {
          proxy.on('error', (err) => {
            if ((err as NodeJS.ErrnoException).code === 'ECONNREFUSED') {
              console.warn('[proxy] 后端服务未就绪，请确认 localhost:8080 已启动')
            }
          })
        }
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true
      }
    }
  }
})
