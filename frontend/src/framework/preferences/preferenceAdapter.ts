import { inject, type InjectionKey } from 'vue';

export type FrameworkPreferenceAdapter = {
  getPreference<TPreference extends object>(preferenceKey: string): Promise<TPreference | null>;
  savePreference<TPreference extends object>(preferenceKey: string, value: TPreference): Promise<TPreference>;
};

const noopPreferenceAdapter: FrameworkPreferenceAdapter = {
  async getPreference() {
    return null;
  },
  async savePreference(_preferenceKey, value) {
    return value;
  },
};

export const frameworkPreferenceAdapterKey: InjectionKey<FrameworkPreferenceAdapter> = Symbol('frameworkPreferenceAdapter');

export function useFrameworkPreferenceAdapter() {
  return inject(frameworkPreferenceAdapterKey, noopPreferenceAdapter);
}
