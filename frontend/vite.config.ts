import { fileURLToPath, URL } from 'node:url';
import vue from '@vitejs/plugin-vue';
import { defineConfig, loadEnv } from 'vite';

const chunkGroups: Array<[string, string[]]> = [
  ['vue-core', ['/node_modules/vue/', '/node_modules/@vue/', '/node_modules/vue-router/', '/node_modules/pinia/', '/node_modules/vue-i18n/']],
  ['http-client', ['/node_modules/axios/']],
  ['icons', ['/node_modules/lucide-vue-next/', '/node_modules/@element-plus/icons-vue/']],
  ['vueuse', ['/node_modules/@vueuse/']],
];
const canonicalDevOrigin = 'http://127.0.0.1:5173';
const localhostDevOrigin = 'http://localhost:5173';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const apiProxyTarget = 'http://127.0.0.1:8100';
  warnDeprecatedGatewayRoutingEnv(env);

  return {
    plugins: [createCanonicalDevHostPlugin(canonicalDevOrigin), vue(), createApiProxyPlugin(apiProxyTarget)],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
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

function warnDeprecatedGatewayRoutingEnv(env: Record<string, string>) {
  const directApiBaseUrl = env.VITE_API_BASE_URL?.trim();
  if (directApiBaseUrl) {
    console.warn(
      `[xuan-api-proxy] 已忽略 VITE_API_BASE_URL=${directApiBaseUrl}，浏览器请求统一通过 gateway 的相对 /api 路径发送。`,
    );
  }
  const proxyTarget = env.VITE_DEV_PROXY_TARGET?.trim();
  if (proxyTarget && proxyTarget !== 'http://127.0.0.1:8100') {
    console.warn(
      `[xuan-api-proxy] 已忽略 VITE_DEV_PROXY_TARGET=${proxyTarget}，开发代理固定转发到 gateway http://127.0.0.1:8100。`,
    );
  }
}

function createCanonicalDevHostPlugin(canonicalOrigin: string) {
  const localhostDevHost = new URL(localhostDevOrigin).host;
  const redirectLocalhostToCanonicalOrigin = (request, response, next) => {
    const host = String(request.headers.host || '').toLowerCase();
    if (host !== localhostDevHost || request.url?.startsWith('/api')) {
      next();
      return;
    }

    response.statusCode = 307;
    response.setHeader('Location', new URL(request.url || '/', canonicalOrigin).toString());
    response.end();
  };
  return {
    name: 'xuan-canonical-dev-host',
    configureServer(server) {
      server.middlewares.use(redirectLocalhostToCanonicalOrigin);
    },
    configurePreviewServer(server) {
      server.middlewares.use(redirectLocalhostToCanonicalOrigin);
    },
  };
}

function normalizeModuleId(id: string) {
  return id.replaceAll('\\', '/');
}

function resolveElementPlusChunk(id: string) {
  if (!id.includes('/node_modules/element-plus/')) {
    return '';
  }
  return 'element-plus';
}

function createApiProxyPlugin(apiProxyTarget: string) {
  return {
    name: 'xuan-api-proxy',
    configureServer(server) {
      server.middlewares.use(async (request, response, next) => {
        if (!request.url?.startsWith('/api')) {
          next();
          return;
        }

        try {
          const upstreamResponse = await fetch(new URL(request.url, apiProxyTarget), {
            method: request.method,
            headers: createProxyHeaders(request.headers),
            body: await readRequestBody(request),
          });
          response.statusCode = upstreamResponse.status;
          response.statusMessage = upstreamResponse.statusText;
          upstreamResponse.headers.forEach((value, key) => {
            if (!shouldSkipProxyResponseHeader(key)) {
              response.setHeader(key, value);
            }
          });
          response.end(Buffer.from(await upstreamResponse.arrayBuffer()));
        } catch (error) {
          response.statusCode = 502;
          response.setHeader('content-type', 'text/plain; charset=utf-8');
          response.end(formatProxyError(error));
        }
      });
    },
  };
}

function formatProxyError(error: unknown) {
  if (!(error instanceof Error)) {
    return 'API proxy failed';
  }
  const cause = error.cause instanceof Error ? `: ${error.cause.message}` : '';
  return `${error.message}${cause}`;
}

function createProxyHeaders(headers: Record<string, string | string[] | undefined>) {
  const proxyHeaders = new Headers();
  for (const [key, value] of Object.entries(headers)) {
    if (value === undefined || shouldSkipProxyRequestHeader(key)) {
      continue;
    }
    proxyHeaders.set(key, Array.isArray(value) ? value.join(',') : value);
  }
  proxyHeaders.set('origin', canonicalDevOrigin);
  return proxyHeaders;
}

function shouldSkipProxyRequestHeader(headerName: string) {
  return ['host', 'connection', 'content-length', 'expect', 'transfer-encoding'].includes(headerName.toLowerCase());
}

function shouldSkipProxyResponseHeader(headerName: string) {
  return ['connection', 'content-length', 'content-encoding', 'transfer-encoding', 'keep-alive'].includes(
    headerName.toLowerCase(),
  );
}

async function readRequestBody(request: NodeJS.ReadableStream & { method?: string }) {
  if (request.method === 'GET' || request.method === 'HEAD') {
    return undefined;
  }
  const chunks: Buffer[] = [];
  for await (const chunk of request) {
    chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
  }
  return Buffer.concat(chunks);
}
