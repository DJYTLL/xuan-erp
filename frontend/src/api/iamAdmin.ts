import { http } from './http';
import type { ApiResponse } from '@/types/auth';
import type {
  IamMenu,
  IamMenuPayload,
  IamPermission,
  IamPermissionPayload,
  IamRole,
  IamRolePayload,
  IamRolePermissionGrant,
  IamTenantInitTemplate,
  IamTenantInitTemplatePayload,
  IamUser,
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
