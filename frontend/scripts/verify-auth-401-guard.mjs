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
const frameworkConfigSource = read('src/app/frameworkConfig.ts');

assert(
  packageSource.includes('verify:auth-401-guard'),
  'package.json 应暴露 401 会话保护校验脚本。',
);

assert(
  httpErrorSource.includes('pendingUnauthorizedProbe'),
  '401 处理应复用单次会话探测，避免并发请求重复判定登录失效。',
);

assert(
  httpErrorSource.includes('probeCurrentSession'),
  '401 处理应先探测当前会话是否仍然有效。',
);

assert(
  httpErrorSource.includes('appFrameworkConfig.auth.currentUserProbePath'),
  '401 会话探测应从框架配置读取当前用户探测路径。',
);

assert(
  frameworkConfigSource.includes("currentUserProbePath: '/api/iam/auth/current-user'"),
  'Xuan ERP 项目配置应把 401 会话探测指向 /api/iam/auth/current-user。',
);

assert(
  httpErrorSource.includes('shouldForceLogout'),
  '401 处理应在探测后再决定是否真正登出。',
);

assert(
  httpErrorSource.indexOf('await shouldForceLogout(error)') < httpErrorSource.indexOf('notifyHttpError(message)'),
  '401 处理应先完成会话探测，再弹出登录过期提示。',
);

assert(
  httpErrorSource.includes('getBusinessUnauthorizedMessage'),
  '非终止 401 应使用业务未授权提示，不能复用登录过期默认文案。',
);

assert(
  httpErrorSource.includes('handlerOptions.onUnauthorized?.()'),
  '确认会话失效后仍应保留统一登出跳转。',
);

assert(
  httpSource.includes('await handleHttpError(error)'),
  'Axios 响应拦截器应等待 401 会话探测完成。',
);

console.log('Verified 401 guard probes current session before forcing logout.');
