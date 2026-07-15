import { fileURLToPath, URL } from 'node:url';
import vue from '@vitejs/plugin-vue';
import { defineConfig, loadEnv } from 'vite';

const chunkGroups: Array<[string, string[]]> = [
  ['vue-core', ['/node_modules/vue/', '/node_modules/@vue/', '/node_modules/vue-router/', '/node_modules/pinia/', '/node_modules/vue-i18n/']],
  ['http-client', ['/node_modules/axios/']],
  ['icons', ['/node_modules/lucide-vue-next/', '/node_modules/@element-plus/icons-vue/']],
  ['vueuse', ['/node_modules/@vueuse/']],
];

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const apiProxyTarget = env.VITE_DEV_PROXY_TARGET || 'http://localhost:8100';

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      host: '127.0.0.1',
      port: 5173,
      proxy: {
        '/api': {
          target: apiProxyTarget,
          changeOrigin: true,
        },
      },
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            const normalizedId = normalizeModuleId(id);
            const elementPlusChunk = resolveElementPlusChunk(normalizedId);
            if (elementPlusChunk) {
              return elementPlusChunk;
            }
            for (const [chunkName, patterns] of chunkGroups) {
              if (patterns.some((pattern) => normalizedId.includes(pattern))) {
                return chunkName;
              }
            }
            if (normalizedId.includes('/node_modules/')) {
              return 'vendor';
            }
          },
        },
        onwarn(warning, defaultHandler) {
          if (
            warning.code === 'INVALID_ANNOTATION'
            && warning.id?.replaceAll('\\', '/').includes('/node_modules/@vueuse/core/')
          ) {
            return;
          }
          defaultHandler(warning);
        },
      },
    },
  };
});

function normalizeModuleId(id: string) {
  return id.replaceAll('\\', '/');
}

function resolveElementPlusChunk(id: string) {
  if (!id.includes('/node_modules/element-plus/')) {
    return '';
  }
  return 'element-plus';
}
