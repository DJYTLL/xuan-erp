import { http } from './http';
import type { ApiResponse } from '@/types/auth';
import type {
  IamColumnPermissionTemplate,
  IamColumnPermissionTemplateItem,
  IamColumnPermissionTemplateItemPayload,
  IamColumnPermissionTemplatePayload,
  IamMenu,
  IamMenuPayload,
  IamPermission,
  IamPermissionPayload,
  IamResourceColumn,
  IamRole,
  IamRoleColumnPermissionRule,
  IamRoleColumnPermissionRulePayload,
  IamRoleColumnPermissionTemplateBinding,
  IamRoleColumnPermissionTemplatePayload,
  IamTenantColumnPermissionTemplateAssignment,
  IamTenantColumnPermissionTemplateAssignmentPayload,
  IamRolePayload,
  IamRolePermissionGrant,
  IamTenantInitTemplate,
  IamTenantInitTemplatePayload,
  IamUser,
  IamTenantAdminPasswordResetPayload,
  IamUserPasswordResetPayload,
  IamUserPayload,
  IamUserRoleGrant,
} from '@/types/iamAdmin';

function unwrap<T>(body: ApiResponse<T> | T): T {
  if (body && typeof body === 'object' && 'data' in body) {
    return (body as ApiResponse<T>).data as T;
  }
  return body as T;
}

export async function listIamMenus(): Promise<IamMenu[]> {
  const response = await http.get<ApiResponse<IamMenu[]> | IamMenu[]>('/api/iam/menus');
  return unwrap<IamMenu[]>(response.data);
}

export async function listIamMenuOptions(): Promise<IamMenu[]> {
  const response = await http.get<ApiResponse<IamMenu[]> | IamMenu[]>('/api/iam/menus/options');
  return unwrap<IamMenu[]>(response.data);
}

export async function createIamMenu(payload: IamMenuPayload): Promise<IamMenu> {
  const response = await http.post<ApiResponse<IamMenu> | IamMenu>('/api/iam/menus', payload);
  return unwrap<IamMenu>(response.data);
}

export async function updateIamMenu(menuId: number, payload: IamMenuPayload): Promise<IamMenu> {
  const response = await http.put<ApiResponse<IamMenu> | IamMenu>(`/api/iam/menus/${menuId}`, payload);
  return unwrap<IamMenu>(response.data);
}

export async function setIamMenuEnabled(menuId: number, enabled: boolean): Promise<IamMenu> {
  const action = enabled ? 'enable' : 'disable';
  const response = await http.post<ApiResponse<IamMenu> | IamMenu>(`/api/iam/menus/${menuId}/${action}`);
  return unwrap<IamMenu>(response.data);
}

export async function listIamPermissions(): Promise<IamPermission[]> {
  const response = await http.get<ApiResponse<IamPermission[]> | IamPermission[]>('/api/iam/permissions');
  return unwrap<IamPermission[]>(response.data);
}

export async function createIamPermission(payload: IamPermissionPayload): Promise<IamPermission> {
  const response = await http.post<ApiResponse<IamPermission> | IamPermission>('/api/iam/permissions', payload);
  return unwrap<IamPermission>(response.data);
}

export async function updateIamPermission(permissionId: number, payload: IamPermissionPayload): Promise<IamPermission> {
  const response = await http.put<ApiResponse<IamPermission> | IamPermission>(`/api/iam/permissions/${permissionId}`, payload);
  return unwrap<IamPermission>(response.data);
}

export async function setIamPermissionEnabled(permissionId: number, enabled: boolean): Promise<IamPermission> {
  const action = enabled ? 'enable' : 'disable';
  const response = await http.post<ApiResponse<IamPermission> | IamPermission>(`/api/iam/permissions/${permissionId}/${action}`);
  return unwrap<IamPermission>(response.data);
}

export async function listIamRoles(tenantId: number): Promise<IamRole[]> {
  const response = await http.get<ApiResponse<IamRole[]> | IamRole[]>('/api/iam/roles', { params: { tenantId } });
  return unwrap<IamRole[]>(response.data);
}

export async function createIamRole(payload: IamRolePayload): Promise<IamRole> {
  const response = await http.post<ApiResponse<IamRole> | IamRole>('/api/iam/roles', payload);
  return unwrap<IamRole>(response.data);
}

export async function updateIamRole(roleId: number, payload: IamRolePayload): Promise<IamRole> {
  const response = await http.put<ApiResponse<IamRole> | IamRole>(`/api/iam/roles/${roleId}`, payload);
  return unwrap<IamRole>(response.data);
}

export async function setIamRoleEnabled(roleId: number, enabled: boolean): Promise<IamRole> {
  const action = enabled ? 'enable' : 'disable';
  const response = await http.post<ApiResponse<IamRole> | IamRole>(`/api/iam/roles/${roleId}/${action}`);
  return unwrap<IamRole>(response.data);
}

export async function getIamRolePermissions(tenantId: number, roleId: number): Promise<IamRolePermissionGrant> {
  const response = await http.get<ApiResponse<IamRolePermissionGrant> | IamRolePermissionGrant>(
    `/api/iam/roles/${roleId}/permissions`,
    { params: { tenantId } },
  );
  return unwrap<IamRolePermissionGrant>(response.data);
}

export async function setIamRolePermissions(
  tenantId: number,
  roleId: number,
  permissionCodes: string[],
  operator: string,
): Promise<IamRolePermissionGrant> {
  const response = await http.put<ApiResponse<IamRolePermissionGrant> | IamRolePermissionGrant>(
    `/api/iam/roles/${roleId}/permissions`,
    { tenantId, permissionCodes, operator },
  );
  return unwrap<IamRolePermissionGrant>(response.data);
}

export async function listIamUsers(tenantId: number): Promise<IamUser[]> {
  const response = await http.get<ApiResponse<IamUser[]> | IamUser[]>('/api/iam/users', { params: { tenantId } });
  return unwrap<IamUser[]>(response.data);
}

export async function createIamUser(payload: IamUserPayload): Promise<IamUser> {
  const response = await http.post<ApiResponse<IamUser> | IamUser>('/api/iam/users', payload);
  return unwrap<IamUser>(response.data);
}

export async function updateIamUser(userId: number, payload: IamUserPayload): Promise<IamUser> {
  const response = await http.put<ApiResponse<IamUser> | IamUser>(`/api/iam/users/${userId}`, payload);
  return unwrap<IamUser>(response.data);
}

export async function resetIamUserPassword(
  userId: number,
  payload: IamUserPasswordResetPayload,
): Promise<IamUser> {
  const response = await http.post<ApiResponse<IamUser> | IamUser>(
    `/api/iam/users/${userId}/reset-password`,
    payload,
  );
  return unwrap<IamUser>(response.data);
}

export async function resetIamTenantAdminPassword(
  tenantId: number,
  payload: IamTenantAdminPasswordResetPayload,
): Promise<IamUser> {
  const response = await http.post<ApiResponse<IamUser> | IamUser>(
    `/api/iam/users/tenants/${tenantId}/admin/reset-password`,
    payload,
  );
  return unwrap<IamUser>(response.data);
}

export async function getIamUserRoles(tenantId: number, userId: number): Promise<IamUserRoleGrant> {
  const response = await http.get<ApiResponse<IamUserRoleGrant> | IamUserRoleGrant>(
    `/api/iam/users/${userId}/roles`,
    { params: { tenantId } },
  );
  return unwrap<IamUserRoleGrant>(response.data);
}

export async function setIamUserRoles(
  tenantId: number,
  userId: number,
  roleIds: number[],
  operator: string,
): Promise<IamUserRoleGrant> {
  const response = await http.put<ApiResponse<IamUserRoleGrant> | IamUserRoleGrant>(
    `/api/iam/users/${userId}/roles`,
    { tenantId, roleIds, operator },
  );
  return unwrap<IamUserRoleGrant>(response.data);
}

export async function listIamTenantInitTemplates(): Promise<IamTenantInitTemplate[]> {
  const response = await http.get<ApiResponse<IamTenantInitTemplate[]> | IamTenantInitTemplate[]>(
    '/api/iam/tenant-init-templates',
  );
  return unwrap<IamTenantInitTemplate[]>(response.data);
}

export async function createIamTenantInitTemplate(
  payload: IamTenantInitTemplatePayload,
): Promise<IamTenantInitTemplate> {
  const response = await http.post<ApiResponse<IamTenantInitTemplate> | IamTenantInitTemplate>(
    '/api/iam/tenant-init-templates',
    payload,
  );
  return unwrap<IamTenantInitTemplate>(response.data);
}

export async function updateIamTenantInitTemplate(
  templateId: number,
  payload: IamTenantInitTemplatePayload,
): Promise<IamTenantInitTemplate> {
  const response = await http.put<ApiResponse<IamTenantInitTemplate> | IamTenantInitTemplate>(
    `/api/iam/tenant-init-templates/${templateId}`,
    payload,
  );
  return unwrap<IamTenantInitTemplate>(response.data);
}

export async function setIamTenantInitTemplatePermissions(
  templateId: number,
  permissionCodes: string[],
  operator: string,
): Promise<IamTenantInitTemplate> {
  const response = await http.put<ApiResponse<IamTenantInitTemplate> | IamTenantInitTemplate>(
    `/api/iam/tenant-init-templates/${templateId}/permissions`,
    { permissionCodes, operator },
  );
  return unwrap<IamTenantInitTemplate>(response.data);
}

export async function listIamResourceColumns(): Promise<IamResourceColumn[]> {
  const response = await http.get<ApiResponse<IamResourceColumn[]> | IamResourceColumn[]>(
    '/api/iam/column-permissions/resources',
  );
  return unwrap<IamResourceColumn[]>(response.data);
}

export async function listIamColumnPermissionTemplates(params: {
  tenantId?: number;
  keyword?: string;
  enabled?: boolean;
} = {}): Promise<IamColumnPermissionTemplate[]> {
  const response = await http.get<ApiResponse<IamColumnPermissionTemplate[]> | IamColumnPermissionTemplate[]>(
    '/api/iam/column-permissions/templates',
    { params },
  );
  return unwrap<IamColumnPermissionTemplate[]>(response.data);
}

export async function createIamColumnPermissionTemplate(
  payload: IamColumnPermissionTemplatePayload,
): Promise<IamColumnPermissionTemplate> {
  const response = await http.post<ApiResponse<IamColumnPermissionTemplate> | IamColumnPermissionTemplate>(
    '/api/iam/column-permissions/templates',
    payload,
  );
  return unwrap<IamColumnPermissionTemplate>(response.data);
}

export async function updateIamColumnPermissionTemplate(
  templateId: number,
  payload: IamColumnPermissionTemplatePayload,
): Promise<IamColumnPermissionTemplate> {
  const response = await http.put<ApiResponse<IamColumnPermissionTemplate> | IamColumnPermissionTemplate>(
    `/api/iam/column-permissions/templates/${templateId}`,
    payload,
  );
  return unwrap<IamColumnPermissionTemplate>(response.data);
}

export async function setIamColumnPermissionTemplateEnabled(
  templateId: number,
  enabled: boolean,
  operator?: string,
): Promise<IamColumnPermissionTemplate> {
  const action = enabled ? 'enable' : 'disable';
  const response = await http.post<ApiResponse<IamColumnPermissionTemplate> | IamColumnPermissionTemplate>(
    `/api/iam/column-permissions/templates/${templateId}/${action}`,
    null,
    { params: { operator } },
  );
  return unwrap<IamColumnPermissionTemplate>(response.data);
}

export async function getIamColumnPermissionTemplateItems(
  templateId: number,
): Promise<IamColumnPermissionTemplateItem[]> {
  const response = await http.get<ApiResponse<IamColumnPermissionTemplateItem[]> | IamColumnPermissionTemplateItem[]>(
    `/api/iam/column-permissions/templates/${templateId}/items`,
  );
  return unwrap<IamColumnPermissionTemplateItem[]>(response.data);
}

export async function setIamColumnPermissionTemplateItems(
  templateId: number,
  items: IamColumnPermissionTemplateItemPayload[],
  operator?: string,
): Promise<IamColumnPermissionTemplateItem[]> {
  const response = await http.put<ApiResponse<IamColumnPermissionTemplateItem[]> | IamColumnPermissionTemplateItem[]>(
    `/api/iam/column-permissions/templates/${templateId}/items`,
    { items, operator },
  );
  return unwrap<IamColumnPermissionTemplateItem[]>(response.data);
}

export async function listIamTenantColumnPermissionTemplates(
  tenantId: number,
): Promise<IamTenantColumnPermissionTemplateAssignment[]> {
  const response = await http.get<ApiResponse<IamTenantColumnPermissionTemplateAssignment[]> | IamTenantColumnPermissionTemplateAssignment[]>(
    `/api/iam/column-permissions/tenants/${tenantId}/templates`,
  );
  return unwrap<IamTenantColumnPermissionTemplateAssignment[]>(response.data);
}

export async function setIamTenantColumnPermissionTemplates(
  tenantId: number,
  payload: IamTenantColumnPermissionTemplateAssignmentPayload,
): Promise<IamTenantColumnPermissionTemplateAssignment[]> {
  const response = await http.put<ApiResponse<IamTenantColumnPermissionTemplateAssignment[]> | IamTenantColumnPermissionTemplateAssignment[]>(
    `/api/iam/column-permissions/tenants/${tenantId}/templates`,
    payload,
  );
  return unwrap<IamTenantColumnPermissionTemplateAssignment[]>(response.data);
}

export async function getIamRoleColumnPermissions(
  tenantId: number,
  roleId: number,
): Promise<IamRoleColumnPermissionRule[]> {
  const response = await http.get<ApiResponse<IamRoleColumnPermissionRule[]> | IamRoleColumnPermissionRule[]>(
    `/api/iam/column-permissions/roles/${roleId}/column-permissions`,
    { params: { tenantId } },
  );
  return unwrap<IamRoleColumnPermissionRule[]>(response.data);
}

export async function setIamRoleColumnPermissions(
  roleId: number,
  payload: IamRoleColumnPermissionRulePayload,
): Promise<IamRoleColumnPermissionRule[]> {
  const response = await http.put<ApiResponse<IamRoleColumnPermissionRule[]> | IamRoleColumnPermissionRule[]>(
    `/api/iam/column-permissions/roles/${roleId}/column-permissions`,
    payload,
  );
  return unwrap<IamRoleColumnPermissionRule[]>(response.data);
}

export async function getIamRoleColumnPermissionTemplate(
  tenantId: number,
  roleId: number,
): Promise<IamRoleColumnPermissionTemplateBinding | null> {
  const response = await http.get<ApiResponse<IamRoleColumnPermissionTemplateBinding | null> | IamRoleColumnPermissionTemplateBinding | null>(
    `/api/iam/column-permissions/roles/${roleId}/column-permission-template`,
    { params: { tenantId } },
  );
  return unwrap<IamRoleColumnPermissionTemplateBinding | null>(response.data);
}

export async function setIamRoleColumnPermissionTemplate(
  roleId: number,
  payload: IamRoleColumnPermissionTemplatePayload,
): Promise<IamRoleColumnPermissionTemplateBinding> {
  const response = await http.put<ApiResponse<IamRoleColumnPermissionTemplateBinding> | IamRoleColumnPermissionTemplateBinding>(
    `/api/iam/column-permissions/roles/${roleId}/column-permission-template`,
    payload,
  );
  return unwrap<IamRoleColumnPermissionTemplateBinding>(response.data);
}
