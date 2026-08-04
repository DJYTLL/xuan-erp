const GATEWAY_API_BASE_URL = '';
let warnedIgnoredGatewayOverride = false;

export function getGatewayApiBaseUrl() {
  warnIgnoredGatewayOverride();
  return GATEWAY_API_BASE_URL;
}

function warnIgnoredGatewayOverride() {
  const directApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();
  if (!directApiBaseUrl || warnedIgnoredGatewayOverride) {
    return;
  }
  warnedIgnoredGatewayOverride = true;
  console.warn(
    `[xuan-frontend] 已忽略 VITE_API_BASE_URL=${directApiBaseUrl}，浏览器请求必须通过 gateway 的同源 /api 路径转发。`,
  );
}
