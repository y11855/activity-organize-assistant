// Vite 构建配置：开发代理、CDN base、Element Plus 自动导入、分包
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // CDN 地址：配置后打包产物中的 js/css/图片会引用 CDN 域名，留空则走同域
  const cdnBase = process.env.VITE_CDN_BASE || env.VITE_CDN_BASE || '/'
  return {
    base: cdnBase,
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    plugins: [
      vue(),
      AutoImport({
        imports: ['vue', 'vue-router', 'pinia'],
        resolvers: [ElementPlusResolver()],
        dts: 'src/types/auto-imports.d.ts'
      }),
      Components({
        resolvers: [ElementPlusResolver()],
        dts: 'src/types/components.d.ts'
      })
    ],
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy: {
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
          // 聊天 WebSocket 走 /api/ws/chat，开发环境需要代理 Upgrade
          ws: true
        }
      }
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      // 关闭 modulepreload：预热用的 link 与真实 import 的 CORS 模式不一致时，
      // 浏览器不复用预加载结果，同一个文件会被下载两次（实测每个懒加载依赖都重复）
      modulePreload: false,
      // 合并 CSS：Element Plus 按组件拆分会生成十几个 2~3KB 的小 css，
      // 3G 下每个都要一次独立往返（实测每个耗时 2.5 秒左右），合成一个文件更快
      cssCodeSplit: false,
      rollupOptions: {
        output: {
          manualChunks: {
            vue: ['vue', 'vue-router', 'pinia'],
            element: ['element-plus', '@element-plus/icons-vue']
          }
        }
      }
    }
  }
})
