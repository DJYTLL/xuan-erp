import { appFrameworkConfig } from '@/app/frameworkConfig';

export type LocaleCode = 'zh-CN' | 'en-US';

const SUPPORTED_LOCALES: LocaleCode[] = ['zh-CN', 'en-US'];

export function normalizeLocaleCode(value: unknown): LocaleCode | undefined;
export function normalizeLocaleCode(value: unknown, fallback: LocaleCode): LocaleCode;
export function normalizeLocaleCode(value: unknown, fallback?: LocaleCode): LocaleCode | undefined {
  return SUPPORTED_LOCALES.includes(value as LocaleCode) ? (value as LocaleCode) : fallback;
}

export function resolveInitialLocale(): LocaleCode {
  const localLocale = normalizeLocaleCode(localStorage.getItem(appFrameworkConfig.storage.localeKey));
  if (localLocale) {
    return localLocale;
  }

  const systemLanguage = navigator.language.toLowerCase();
  return systemLanguage.startsWith('zh') ? 'zh-CN' : 'en-US';
}
