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
const httpErrorSource = read('src/api/http-error.ts');

assert(
  packageSource.includes('"verify:http-error-real-message": "node scripts/verify-http-error-real-message.mjs"'),
  'package.json 应暴露真实后端错误原因校验脚本。',
);

assert(
  httpErrorSource.includes('getServerErrorMessage'),
  'HTTP 错误处理应从后端响应体提取真实 message/msg/detail/reason。',
);

assert(
  httpErrorSource.includes('isGenericHttpMessage'),
  'HTTP 错误处理应识别 Unauthorized/Internal Server Error 等通用占位文案。',
);

assert(
  httpErrorSource.includes("'detail'") && httpErrorSource.includes("'reason'"),
  'HTTP 错误处理应兼容 Spring / 网关常见 detail 与 reason 字段。',
);

assert(
  httpErrorSource.indexOf('const serverMessage = getServerErrorMessage') <
    httpErrorSource.indexOf('const translatedMessage = errorCode ?'),
  '后端真实错误消息应优先于前端错误码翻译，避免被通用文案盖掉。',
);

console.log('Verified HTTP error handler prefers real backend error messages.');
