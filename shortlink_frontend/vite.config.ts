import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const adminTarget = env.VITE_ADMIN_PROXY_TARGET || 'http://localhost:8080';
  const projectTarget = env.VITE_PROJECT_PROXY_TARGET || 'http://localhost:8081';

  return {
    plugins: [vue()],
    cacheDir: '.vite-cache',
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy: {
        '/admin-api': {
          target: adminTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/admin-api/, '')
        },
        '/project-api': {
          target: projectTarget,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/project-api/, '')
        }
      }
    },
    preview: {
      host: '0.0.0.0',
      port: 4173
    }
  };
});
