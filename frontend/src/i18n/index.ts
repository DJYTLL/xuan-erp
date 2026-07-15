import { createI18n } from 'vue-i18n';
import { messages } from './messages';
import { resolveInitialLocale } from './locale';

export const i18n = createI18n({
  legacy: false,
  locale: resolveInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages,
});
