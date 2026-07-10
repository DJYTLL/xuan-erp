import { defineStore } from 'pinia';

export type LocaleCode = 'zh-CN' | 'en-US';
export type ThemeColor = 'blue' | 'green' | 'orange' | 'indigo';
export type FontSizeMode = 'small' | 'default' | 'large';

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
    darkMode: localStorage.getItem('xuan-dark-mode') === 'true',
    compactMode: localStorage.getItem('xuan-compact-mode') === 'true',
    fontSize: (localStorage.getItem('xuan-font-size') as FontSizeMode) || 'default',
    showTabs: localStorage.getItem('xuan-show-tabs') !== 'false',
    showBreadcrumb: localStorage.getItem('xuan-show-breadcrumb') !== 'false',
    fixedTopbar: localStorage.getItem('xuan-fixed-topbar') !== 'false',
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
    setDarkMode(enabled: boolean) {
      this.darkMode = enabled;
      localStorage.setItem('xuan-dark-mode', String(enabled));
      document.documentElement.classList.toggle('xuan-dark', enabled);
    },
    setCompactMode(enabled: boolean) {
      this.compactMode = enabled;
      localStorage.setItem('xuan-compact-mode', String(enabled));
      document.documentElement.classList.toggle('xuan-compact', enabled);
    },
    setFontSize(fontSize: FontSizeMode) {
      this.fontSize = fontSize;
      localStorage.setItem('xuan-font-size', fontSize);
      document.documentElement.dataset.xuanFontSize = fontSize;
    },
    setShowTabs(enabled: boolean) {
      this.showTabs = enabled;
      localStorage.setItem('xuan-show-tabs', String(enabled));
    },
    setShowBreadcrumb(enabled: boolean) {
      this.showBreadcrumb = enabled;
      localStorage.setItem('xuan-show-breadcrumb', String(enabled));
    },
    setFixedTopbar(enabled: boolean) {
      this.fixedTopbar = enabled;
      localStorage.setItem('xuan-fixed-topbar', String(enabled));
    },
    applyTheme() {
      this.setThemeColor(this.themeColor);
      this.setDarkMode(this.darkMode);
      this.setCompactMode(this.compactMode);
      this.setFontSize(this.fontSize);
    },
  },
});
