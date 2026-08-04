import { inject, type InjectionKey } from 'vue';

export type FrameworkPermission = string | string[] | undefined;
export type FrameworkPermissionChecker = (permission: FrameworkPermission) => boolean;

export const frameworkPermissionCheckerKey: InjectionKey<FrameworkPermissionChecker> = Symbol('frameworkPermissionChecker');

export function useFrameworkPermissionChecker() {
  return inject(frameworkPermissionCheckerKey, (permission) => !permission);
}
