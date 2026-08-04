import { http } from './http';
import type { ApiResponse } from '@/types/auth';
import type {
  ChangeTenantStatusPayload,
  CreateTenantPayload,
  DeleteTenantPayload,
  PageResult,
  RetryTenantOutboxEventPayload,
  RetryTenantProvisionTaskPayload,
  Tenant,
  TenantPlan,
  TenantPlanAssignmentPayload,
  TenantPlanPayload,
  TenantProvisionTask,
  UpdateTenantPayload,
} from '@/types/tenant';

function unwrap<T>(body: ApiResponse<T> | T): T {
  if (body && typeof body === 'object' && 'data' in body) {
    return (body as ApiResponse<T>).data as T;
  }
  return body as T;
}

export async function listTenants(pageNum = 1, pageSize = 20): Promise<PageResult<Tenant>> {
  const response = await http.get<ApiResponse<PageResult<Tenant>> | PageResult<Tenant>>('/api/tenants', {
    params: { pageNum, pageSize },
  });
  return unwrap<PageResult<Tenant>>(response.data);
}

export async function listColumnPermissionTenants(pageNum = 1, pageSize = 20): Promise<PageResult<Tenant>> {
  const response = await http.get<ApiResponse<PageResult<Tenant>> | PageResult<Tenant>>(
    '/api/tenants/column-permission-options',
    {
      params: { pageNum, pageSize },
    },
  );
  return unwrap<PageResult<Tenant>>(response.data);
}

export async function createTenant(payload: CreateTenantPayload): Promise<Tenant> {
  const response = await http.post<ApiResponse<Tenant> | Tenant>('/api/tenants', payload);
  return unwrap<Tenant>(response.data);
}

export async function updateTenant(tenantId: number, payload: UpdateTenantPayload): Promise<Tenant> {
  const response = await http.put<ApiResponse<Tenant> | Tenant>(`/api/tenants/${tenantId}`, payload);
  return unwrap<Tenant>(response.data);
}

export async function enableTenant(tenantId: number, payload: ChangeTenantStatusPayload): Promise<Tenant> {
  const response = await http.post<ApiResponse<Tenant> | Tenant>(`/api/tenants/${tenantId}/enable`, payload);
  return unwrap<Tenant>(response.data);
}

export async function disableTenant(tenantId: number, payload: ChangeTenantStatusPayload): Promise<Tenant> {
  const response = await http.post<ApiResponse<Tenant> | Tenant>(`/api/tenants/${tenantId}/disable`, payload);
  return unwrap<Tenant>(response.data);
}

export async function deleteTenant(tenantId: number, payload: DeleteTenantPayload): Promise<void> {
  await http.delete<ApiResponse<null> | null>(`/api/tenants/${tenantId}`, {
    data: payload,
  });
}

export async function repairTenantPermissionSync(tenantId: number): Promise<Tenant> {
  const response = await http.post<ApiResponse<Tenant> | Tenant>(`/api/tenants/${tenantId}/permission-sync/repair`);
  return unwrap<Tenant>(response.data);
}

export async function listTenantPlans(): Promise<TenantPlan[]> {
  const response = await http.get<ApiResponse<TenantPlan[]> | TenantPlan[]>('/api/tenant-plans');
  return unwrap<TenantPlan[]>(response.data);
}

export async function createTenantPlan(payload: TenantPlanPayload): Promise<TenantPlan> {
  const response = await http.post<ApiResponse<TenantPlan> | TenantPlan>('/api/tenant-plans', payload);
  return unwrap<TenantPlan>(response.data);
}

export async function updateTenantPlan(planId: number, payload: TenantPlanPayload): Promise<TenantPlan> {
  const response = await http.put<ApiResponse<TenantPlan> | TenantPlan>(`/api/tenant-plans/${planId}`, payload);
  return unwrap<TenantPlan>(response.data);
}

export async function enableTenantPlan(planId: number, reason: string, operator?: string): Promise<TenantPlan> {
  const response = await http.post<ApiResponse<TenantPlan> | TenantPlan>(`/api/tenant-plans/${planId}/enable`, {
    reason,
    operator,
  });
  return unwrap<TenantPlan>(response.data);
}

export async function disableTenantPlan(planId: number, reason: string, operator?: string): Promise<TenantPlan> {
  const response = await http.post<ApiResponse<TenantPlan> | TenantPlan>(`/api/tenant-plans/${planId}/disable`, {
    reason,
    operator,
  });
  return unwrap<TenantPlan>(response.data);
}

export async function deleteTenantPlan(planId: number, reason: string, operator?: string): Promise<void> {
  await http.delete<ApiResponse<null> | null>(`/api/tenant-plans/${planId}`, {
    data: { reason, operator },
  });
}

export async function createTenantPlanAssignment(payload: TenantPlanAssignmentPayload): Promise<Record<string, unknown>> {
  const response = await http.post<ApiResponse<Record<string, unknown>> | Record<string, unknown>>(
    '/api/tenant-plan-assignments',
    payload,
  );
  return unwrap<Record<string, unknown>>(response.data);
}

export async function updateTenantPlanAssignment(
  assignmentId: number,
  payload: TenantPlanAssignmentPayload,
): Promise<Record<string, unknown>> {
  const response = await http.put<ApiResponse<Record<string, unknown>> | Record<string, unknown>>(
    `/api/tenant-plan-assignments/${assignmentId}`,
    payload,
  );
  return unwrap<Record<string, unknown>>(response.data);
}

export async function listTenantProvisionTasks(tenantId: number): Promise<TenantProvisionTask[]> {
  const response = await http.get<ApiResponse<TenantProvisionTask[]> | TenantProvisionTask[]>(
    `/api/tenants/${tenantId}/provision-tasks`,
  );
  return unwrap<TenantProvisionTask[]>(response.data);
}

export async function retryTenantProvisionTask(
  taskId: number,
  payload: RetryTenantProvisionTaskPayload,
): Promise<void> {
  await http.post<ApiResponse<null> | null>(`/api/tenant-provision-tasks/${taskId}/retry`, payload);
}

export async function retryTenantOutboxEvent(
  eventId: number,
  payload: RetryTenantOutboxEventPayload,
): Promise<void> {
  await http.post<ApiResponse<null> | null>(`/api/tenant-outbox-events/${eventId}/retry`, payload);
}
