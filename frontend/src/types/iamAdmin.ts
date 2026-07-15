export interface IamMenu {
  id: number;
  code: string;
  parentId: number | null;
  title: string;
  i18nKey: string | null;
  path: string | null;
  icon: string | null;
  permissionCode: string | null;
  sortNo: number;
  enabled: boolean;
}

export interface IamPermission {
  id: number;
  code: string;
  name: string;
  serviceName: string;
  menuCode: string | null;
  description: string | null;
  enabled: boolean;
}

export interface IamRole {
  id: number;
  tenantId: number;
  code: string;
  name: string;
  description: string | null;
  enabled: boolean;
}

export interface IamUser {
  id: number;
  tenantId: number;
  username: string;
  displayName: string | null;
  email: string | null;
  phone: string | null;
  enabled: boolean;
  accountNonLocked: boolean;
  authVersion: number;
  remark: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface IamRolePermissionGrant {
  tenantId: number;
  roleId: number;
  permissionCodes: string[];
}

export interface IamUserRoleGrant {
  tenantId: number;
  userId: number;
  roleIds: number[];
}

export interface IamTenantInitTemplate {
  id: number;
  code: string;
  name: string;
  description: string | null;
  permissionCodes: string[];
  defaultTemplate: boolean;
  enabled: boolean;
}

export type IamMenuPayload = Omit<IamMenu, 'id' | 'enabled'> & { enabled?: boolean };
export type IamPermissionPayload = Omit<IamPermission, 'id' | 'enabled'> & { enabled?: boolean };
export type IamRolePayload = Omit<IamRole, 'id' | 'enabled'> & { enabled?: boolean };
export type IamTenantInitTemplatePayload = Omit<IamTenantInitTemplate, 'id' | 'permissionCodes'> & {
  permissionCodes?: string[];
};
