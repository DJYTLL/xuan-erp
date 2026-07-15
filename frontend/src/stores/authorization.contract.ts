import type { CurrentPermissionSnapshot } from '@/types/auth';
import { getCurrentMenus, getCurrentPermissionSnapshot } from '@/api/auth';
import { useAuthorizationStore } from '@/stores/authorization';

type Assert<T extends true> = T;
type IsSame<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false;

type SnapshotApiReturnsSnapshot = Assert<
  IsSame<Awaited<ReturnType<typeof getCurrentPermissionSnapshot>>, CurrentPermissionSnapshot>
>;

type MenusApiReturnsMenus = Assert<
  IsSame<Awaited<ReturnType<typeof getCurrentMenus>>, CurrentPermissionSnapshot['menus']>
>;

type AuthorizationStoreContract = ReturnType<typeof useAuthorizationStore>;

type StoreExposesRoutePermission = Assert<
  IsSame<ReturnType<AuthorizationStoreContract['hasRoutePermission']>, boolean>
>;

type StoreExposesButtonPermission = Assert<
  IsSame<ReturnType<AuthorizationStoreContract['hasButtonPermission']>, boolean>
>;
