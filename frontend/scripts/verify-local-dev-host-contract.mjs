import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(currentDir, '..', '..');
const frontendRoot = resolve(repoRoot, 'frontend');

function readFromFrontend(relativePath) {
  return readFileSync(resolve(frontendRoot, relativePath), 'utf8');
}

function readFromRepo(relativePath) {
  return readFileSync(resolve(repoRoot, relativePath), 'utf8');
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

const packageSource = readFromFrontend('package.json');
const viteConfigSource = readFromFrontend('vite.config.ts');
const envExampleSource = readFromFrontend('.env.example');
const gatewayApplicationSource = readFromRepo('backend/xuan-gateway/src/main/resources/application.yml');
const gatewaySecurityPropertiesSource = readFromRepo('backend/xuan-gateway/src/main/java/com/xuan/erp/gateway/infrastructure/config/GatewaySecurityProperties.java');
const readmeSource = readFromRepo('README.md');

assert(
  packageSource.includes('verify:local-dev-host'),
  'package.json 应暴露本地开发 host 契约校验脚本。',
);
assert(
  packageSource.includes('"dev": "vite --host 0.0.0.0 --port 5173"')
    && packageSource.includes('"preview": "vite preview --host 0.0.0.0 --port 5173"'),
  '前端 dev/preview 应监听 0.0.0.0，让 127.0.0.1 和 localhost 都能进入开发服务。',
);
assert(
  viteConfigSource.includes("const canonicalDevOrigin = 'http://127.0.0.1:5173'")
    && viteConfigSource.includes('createCanonicalDevHostPlugin')
    && viteConfigSource.includes('http://localhost:5173'),
  'Vite 应把 localhost:5173 重定向到规范开发入口 http://127.0.0.1:5173。',
);
assert(
  viteConfigSource.includes("proxyHeaders.set('origin', canonicalDevOrigin)"),
  'Vite 代理转发到 Gateway 时应使用规范 Origin，避免前端 host 混用造成差异。',
);
assert(
  gatewayApplicationSource.includes('http://127.0.0.1:5173')
    && gatewayApplicationSource.includes('http://localhost:5173'),
  'Gateway CORS 配置应同时允许 127.0.0.1:5173 和 localhost:5173。',
);
assert(
  gatewaySecurityPropertiesSource.includes('"http://127.0.0.1:5173", "http://localhost:5173"'),
  'Gateway CORS Java 默认值应同时允许 127.0.0.1:5173 和 localhost:5173。',
);
assert(
  envExampleSource.includes('推荐固定访问 http://127.0.0.1:5173')
    && envExampleSource.includes('localStorage 按 origin 隔离'),
  '.env.example 应说明本地推荐入口和 storage origin 隔离行为。',
);
assert(
  readmeSource.includes('推荐固定访问')
    && readmeSource.includes('http://127.0.0.1:5173')
    && readmeSource.includes('localhost:5173')
    && readmeSource.includes('重定向到')
    && readmeSource.includes('127.0.0.1:5173'),
  'README 应写清本地开发固定入口和 localhost 重定向策略。',
);

console.log('Verified local development host is canonicalized and CORS allows both dev origins.');
