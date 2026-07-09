import { defineStore } from 'pinia';

export type LocaleCode = 'zh-CN' | 'en-US';
export type ThemeColor = 'blue' | 'green' | 'orange' | 'indigo';

const themeColorMap: Record<ThemeColor, string> = {
  blue: '#1677ff',
  green: '#0f9d76',
  orange: '#f59e0b',
  indigo: '#6366f1',
};

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    locale: (localStorage.getItem('xuan-locale') as LocaleCode) || 'zh-CN',
    themeColor: (localStorage.getItem('xuan-theme-color') as ThemeColor) || 'blue',
  }),
  actions: {
    setLocale(locale: LocaleCode) {
      this.locale = locale;
      localStorage.setItem('xuan-locale', locale);
    },
    setThemeColor(color: ThemeColor) {
      this.themeColor = color;
      localStorage.setItem('xuan-theme-color', color);
      document.documentElement.style.setProperty('--xuan-primary', themeColorMap[color]);
    },
    applyTheme() {
      this.setThemeColor(this.themeColor);
    },
  },
});
