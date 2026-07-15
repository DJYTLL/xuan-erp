import type { FrameworkAppConfig } from '@/framework/config/types';

export type StoredAuthSession = {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
};

export function getStoredAuthToken(config: FrameworkAppConfig) {
  return localStorage.getItem(config.storage.tokenKey);
}

export function getStoredAuthTokenExpiresAt(config: FrameworkAppConfig) {
  return localStorage.getItem(config.storage.tokenExpiresAtKey);
}

export function setStoredAuthToken(config: FrameworkAppConfig, token: string) {
  localStorage.setItem(config.storage.tokenKey, token);
}

export function getStoredRefreshToken(config: FrameworkAppConfig) {
  return localStorage.getItem(config.storage.refreshTokenKey);
}

export function getStoredRefreshTokenExpiresAt(config: FrameworkAppConfig) {
  return localStorage.getItem(config.storage.refreshTokenExpiresAtKey);
}

export function setStoredRefreshToken(config: FrameworkAppConfig, token: string) {
  localStorage.setItem(config.storage.refreshTokenKey, token);
}

export function setStoredAuthSession(config: FrameworkAppConfig, session: StoredAuthSession) {
  localStorage.setItem(config.storage.tokenKey, session.accessToken);
  localStorage.setItem(config.storage.tokenExpiresAtKey, session.accessTokenExpiresAt);
  localStorage.setItem(config.storage.refreshTokenKey, session.refreshToken);
  localStorage.setItem(config.storage.refreshTokenExpiresAtKey, session.refreshTokenExpiresAt);
}

export function clearStoredAuthToken(config: FrameworkAppConfig) {
  localStorage.removeItem(config.storage.tokenKey);
  localStorage.removeItem(config.storage.tokenExpiresAtKey);
}

export function clearStoredRefreshToken(config: FrameworkAppConfig) {
  localStorage.removeItem(config.storage.refreshTokenKey);
  localStorage.removeItem(config.storage.refreshTokenExpiresAtKey);
}

export function clearStoredAuthSession(config: FrameworkAppConfig) {
  clearStoredAuthToken(config);
  clearStoredRefreshToken(config);
}

export function getStoredLocale(config: FrameworkAppConfig) {
  return localStorage.getItem(config.storage.localeKey) || 'zh-CN';
}
