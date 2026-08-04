import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(currentDir, '..');

function read(relativePath) {
  return readFileSync(resolve(frontendRoot, relativePath), 'utf8');
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

const packageSource = read('package.json');
const httpSource = read('src/api/http.ts');
const httpErrorSource = read('src/api/http-error.ts');
const gatewayBaseSource = read('src/api/gateway-base.ts');
const viteConfigSource = read('vite.config.ts');
const envExampleSource = read('.env.example');

assert(
  packageSource.includes('"verify:gateway-only-api-access": "node scripts/verify-gateway-only-api-access.mjs"'),
  'package.json 应暴露 gateway-only 接口访问校验脚本。',
);

assert(
  httpSource.includes('getGatewayApiBaseUrl'),
  'HTTP 客户端应统一通过 gateway 基线路径发请求。',
);

assert(
  httpErrorSource.includes('getGatewayApiBaseUrl'),
  '401 探测请求也应统一通过 gateway 基线路径发请求。',
);

assert(
  gatewayBaseSource.includes("const GATEWAY_API_BASE_URL = ''"),
  'gateway 基线路径应固定为同源相对地址，不能再被环境变量覆盖成直连服务。',
);

assert(
  gatewayBaseSource.includes('已忽略 VITE_API_BASE_URL='),
  '当本地仍配置 VITE_API_BASE_URL 时，前端应明确提示该配置已被忽略。',
);

assert(
  viteConfigSource.includes("const apiProxyTarget = 'http://127.0.0.1:8100'"),
  'Vite 开发代理应固定转发到 gateway 8100 端口。',
);

assert(
  viteConfigSource.includes('已忽略 VITE_DEV_PROXY_TARGET='),
  '当本地仍配置 VITE_DEV_PROXY_TARGET 时，开发代理应明确提示该配置已被忽略。',
);

assert(
  !envExampleSource.includes('VITE_API_BASE_URL=') && !envExampleSource.includes('VITE_DEV_PROXY_TARGET='),
  '.env.example 不应继续暴露会诱导前端直连业务服务的变量。',
);

console.log('Verified frontend browser requests are forced through gateway.');
