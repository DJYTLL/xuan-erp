import { defineStore } from 'pinia';
import { getUserPreference, saveUserPreference } from '@/api/preferences';
import { appFrameworkConfig } from '@/app/frameworkConfig';
import { normalizeLocaleCode, resolveInitialLocale, type LocaleCode } from '@/i18n/locale';

export type { LocaleCode };
export type ThemeColor = 'blue' | 'green' | 'orange' | 'indigo';
export type FontSizeMode = 'small' | 'default' | 'large';

type ShellLayoutPreference = {
  locale: LocaleCode;
  themeColor: ThemeColor;
  darkMode: boolean;
  compactMode: boolean;
  fontSize: FontSizeMode;
  showTabs: boolean;
  showBreadcrumb: boolean;
  fixedTopbar: boolean;
};

const SHELL_LAYOUT_PREFERENCE_KEY = appFrameworkConfig.preferences.shellLayoutKey;
const settingStorageKey = (name: string) => `${appFrameworkConfig.storage.settingKeyPrefix}-${name}`;

const themeColorMap: Record<ThemeColor, string> = {
  blue: '#1677ff',
  green: '#0f9d76',
  orange: '#f59e0b',
  indigo: '#6366f1',
};

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    locale: resolveInitialLocale(),
    themeColor: (localStorage.getItem(settingStorageKey('theme-color')) as ThemeColor) || 'blue',
    darkMode: localStorage.getItem(settingStorageKey('dark-mode')) === 'true',
    compactMode: localStorage.getItem(settingStorageKey('compact-mode')) === 'true',
    fontSize: (localStorage.getItem(settingStorageKey('font-size')) as FontSizeMode) || 'default',
    showTabs: localStorage.getItem(settingStorageKey('show-tabs')) !== 'false',
    showBreadcrumb: localStorage.getItem(settingStorageKey('show-breadcrumb')) !== 'false',
    fixedTopbar: localStorage.getItem(settingStorageKey('fixed-topbar')) !== 'false',
  }),
  actions: {
    async loadRemotePreferences() {
      try {
        const remotePreference = await getUserPreference<ShellLayoutPreference>(SHELL_LAYOUT_PREFERENCE_KEY);
        if (!remotePreference) {
          return;
        }
        this.locale = normalizeLocale(remotePreference.locale, this.locale);
        this.themeColor = normalizeThemeColor(remotePreference.themeColor, this.themeColor);
        this.darkMode = Boolean(remotePreference.darkMode);
        this.compactMode = Boolean(remotePreference.compactMode);
        this.fontSize = normalizeFontSize(remotePreference.fontSize, this.fontSize);
        this.showTabs = remotePreference.showTabs !== false;
        this.showBreadcrumb = remotePreference.showBreadcrumb !== false;
        this.fixedTopbar = remotePreference.fixedTopbar !== false;
        this.persistLocalPreferences();
        this.applyTheme();
      } catch {
        // 未登录或后端暂不可用时使用本地缓存，不阻断页面进入。
      }
    },
    setLocale(locale: LocaleCode) {
      this.locale = locale;
      localStorage.setItem(appFrameworkConfig.storage.localeKey, locale);
      this.saveRemotePreferences();
    },
    setThemeColor(color: ThemeColor) {
      this.themeColor = color;
      localStorage.setItem(settingStorageKey('theme-color'), color);
      document.documentElement.style.setProperty('--xuan-primary', themeColorMap[color]);
      this.saveRemotePreferences();
    },
    setDarkMode(enabled: boolean) {
      this.darkMode = enabled;
      localStorage.setItem(settingStorageKey('dark-mode'), String(enabled));
      document.documentElement.classList.toggle('xuan-dark', enabled);
      this.saveRemotePreferences();
    },
    setCompactMode(enabled: boolean) {
      this.compactMode = enabled;
      localStorage.setItem(settingStorageKey('compact-mode'), String(enabled));
      document.documentElement.classList.toggle('xuan-compact', enabled);
      this.saveRemotePreferences();
    },
    setFontSize(fontSize: FontSizeMode) {
      this.fontSize = fontSize;
      localStorage.setItem(settingStorageKey('font-size'), fontSize);
      document.documentElement.dataset.xuanFontSize = fontSize;
      this.saveRemotePreferences();
    },
    setShowTabs(enabled: boolean) {
      this.showTabs = enabled;
      localStorage.setItem(settingStorageKey('show-tabs'), String(enabled));
      this.saveRemotePreferences();
    },
    setShowBreadcrumb(enabled: boolean) {
      this.showBreadcrumb = enabled;
      localStorage.setItem(settingStorageKey('show-breadcrumb'), String(enabled));
      this.saveRemotePreferences();
    },
    setFixedTopbar(enabled: boolean) {
      this.fixedTopbar = enabled;
      localStorage.setItem(settingStorageKey('fixed-topbar'), String(enabled));
      this.saveRemotePreferences();
    },
    applyTheme() {
      document.documentElement.style.setProperty('--xuan-primary', themeColorMap[this.themeColor]);
      document.documentElement.classList.toggle('xuan-dark', this.darkMode);
      document.documentElement.classList.toggle('xuan-compact', this.compactMode);
      document.documentElement.dataset.xuanFontSize = this.fontSize;
    },
    persistLocalPreferences() {
      localStorage.setItem(appFrameworkConfig.storage.localeKey, this.locale);
      localStorage.setItem(settingStorageKey('theme-color'), this.themeColor);
      localStorage.setItem(settingStorageKey('dark-mode'), String(this.darkMode));
      localStorage.setItem(settingStorageKey('compact-mode'), String(this.compactMode));
      localStorage.setItem(settingStorageKey('font-size'), this.fontSize);
      localStorage.setItem(settingStorageKey('show-tabs'), String(this.showTabs));
      localStorage.setItem(settingStorageKey('show-breadcrumb'), String(this.showBreadcrumb));
      localStorage.setItem(settingStorageKey('fixed-topbar'), String(this.fixedTopbar));
    },
    saveRemotePreferences() {
      const preference: ShellLayoutPreference = {
        locale: this.locale,
        themeColor: this.themeColor,
        darkMode: this.darkMode,
        compactMode: this.compactMode,
        fontSize: this.fontSize,
        showTabs: this.showTabs,
        showBreadcrumb: this.showBreadcrumb,
        fixedTopbar: this.fixedTopbar,
      };
      saveUserPreference(SHELL_LAYOUT_PREFERENCE_KEY, preference).catch(() => {
        // 后端偏好同步失败时保留本地缓存，避免设置面板出现阻塞。
      });
    },
  },
});

function normalizeLocale(value: unknown, fallback: LocaleCode): LocaleCode {
  return normalizeLocaleCode(value, fallback);
}

function normalizeThemeColor(value: unknown, fallback: ThemeColor): ThemeColor {
  return value === 'blue' || value === 'green' || value === 'orange' || value === 'indigo' ? value : fallback;
}

function normalizeFontSize(value: unknown, fallback: FontSizeMode): FontSizeMode {
  return value === 'small' || value === 'default' || value === 'large' ? value : fallback;
}
