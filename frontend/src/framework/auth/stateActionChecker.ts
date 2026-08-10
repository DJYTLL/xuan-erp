import { inject, type InjectionKey } from 'vue';

export type FrameworkStateActionChecker = (
  resourceKey: string,
  stateCode: string | number | null | undefined,
  actionCode: string | number | null | undefined,
) => boolean;

export const frameworkStateActionCheckerKey: InjectionKey<FrameworkStateActionChecker> = Symbol('frameworkStateActionChecker');

export function useFrameworkStateActionChecker() {
  return inject(frameworkStateActionCheckerKey, () => true);
}
