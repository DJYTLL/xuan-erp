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
  enabled: boolean | null;
  accountNonLocked: boolean | null;
  authVersion: number | null;
  remark: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface IamRolePermissionGrant {
  tenantId: number;
  roleId: number;
  permissionCodes: string[];
  availablePermissionCodes: string[];
  availablePermissions: IamPermission[];
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

export interface IamResourceColumn {
  id: number;
  resourceKey: string;
  columnKey: string;
  columnName: string;
  dataType: string | null;
  maskType: string | null;
  enabled: boolean;
  sortNo: number;
}

export interface IamColumnPermissionTemplate {
  id: number;
  tenantId: number;
  code: string;
  name: string;
  description: string | null;
  enabled: boolean;
}

export interface IamColumnPermissionTemplateItem {
  id: number | null;
  templateId: number;
  resourceColumnId: number;
  resourceKey: string;
  columnKey: string;
  columnName: string;
  accessMode: 'VISIBLE' | 'MASKED' | 'HIDDEN';
}

export interface IamRoleColumnPermissionTemplateBinding {
  tenantId: number;
  roleId: number;
  templateId: number;
  templateCode: string;
  templateName: string;
}

export interface IamRoleColumnPermissionRule {
  id: number | null;
  tenantId: number;
  roleId: number;
  resourceColumnId: number;
  resourceKey: string;
  columnKey: string;
  columnName: string;
  accessMode: 'VISIBLE' | 'MASKED' | 'HIDDEN';
}

export interface IamTenantColumnPermissionTemplateAssignment {
  tenantId: number;
  templateId: number;
  templateCode: string;
  templateName: string;
  templateDescription: string | null;
  defaultTemplate: boolean;
  templateEnabled: boolean;
}

export interface IamResourceState {
  id: number;
  tenantId: number;
  resourceKey: string;
  stateCode: string;
  stateName: string;
  description: string | null;
  sortNo: number;
  enabled: boolean;
  metadataJson: string;
}

export interface IamResourceAction {
  id: number;
  tenantId: number;
  resourceKey: string;
  actionCode: string;
  actionName: string;
  permissionCode: string | null;
  description: string | null;
  sortNo: number;
  enabled: boolean;
  metadataJson: string;
}

export interface IamRoleStateActionRule {
  id: number | null;
  tenantId: number;
  roleId: number;
  resourceKey: string;
  stateCode: string;
  actionCode: string;
  enabled: boolean;
}

export type IamMenuPayload = Omit<IamMenu, 'id' | 'enabled'> & { enabled?: boolean };
export type IamPermissionPayload = Omit<IamPermission, 'id' | 'enabled'> & { enabled?: boolean };
export type IamRolePayload = Omit<IamRole, 'id' | 'enabled'> & { enabled?: boolean };
export type IamUserPayload = {
  tenantId: number;
  username?: string;
  initialPassword?: string;
  displayName?: string;
  email?: string;
  phone?: string;
  enabled?: boolean;
  remark?: string;
  operator?: string;
};
export type IamUserPasswordResetPayload = {
  tenantId: number;
  newPassword: string;
  operator?: string;
};
export type IamTenantAdminPasswordResetPayload = {
  newPassword: string;
  operator?: string;
};
export type IamTenantInitTemplatePayload = Omit<IamTenantInitTemplate, 'id' | 'permissionCodes'> & {
  permissionCodes?: string[];
};
export type IamColumnPermissionTemplatePayload = Omit<IamColumnPermissionTemplate, 'id'> & {
  operator?: string;
};
export type IamColumnPermissionTemplateItemPayload = {
  resourceColumnId: number;
  accessMode: IamColumnPermissionTemplateItem['accessMode'];
};
export type IamRoleColumnPermissionTemplatePayload = {
  tenantId: number;
  templateId: number;
  operator?: string;
};
export type IamRoleColumnPermissionRulePayload = {
  tenantId: number;
  rules: Array<{
    resourceColumnId: number;
    accessMode: IamRoleColumnPermissionRule['accessMode'];
  }>;
  operator?: string;
};
export type IamTenantColumnPermissionTemplateAssignmentPayload = {
  templateIds: number[];
  defaultTemplateId?: number | null;
  operator?: string;
};
export type IamResourceStatePayload = Omit<IamResourceState, 'id' | 'enabled' | 'metadataJson'> & {
  enabled?: boolean;
  metadataJson?: string;
  operator?: string;
};
export type IamResourceActionPayload = Omit<IamResourceAction, 'id' | 'enabled' | 'metadataJson'> & {
  enabled?: boolean;
  metadataJson?: string;
  operator?: string;
};
export type IamRoleStateActionRulePayload = {
  tenantId: number;
  rules: Array<{
    resourceKey: string;
    stateCode: string;
    actionCode: string;
  }>;
  operator?: string;
};
