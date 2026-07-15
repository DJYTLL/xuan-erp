export type AuditLogStatus = 'SUCCESS' | 'FAILED';

export interface AuditLogQueryParams {
  tenantId?: number;
  actorUsername?: string;
  action?: string;
  entityType?: string;
  status?: AuditLogStatus | '';
  startTime?: string;
  endTime?: string;
  limit?: number;
}

export interface AuditLogEntry {
  id: number;
  tenantId: number;
  actorUsername?: string;
  actorUserId?: number;
  action: string;
  entityType: string;
  entityId?: string;
  detail?: string;
  status: AuditLogStatus;
  requestId?: string;
  clientIp?: string;
  userAgent?: string;
  durationMs?: number;
  method?: string;
  path?: string;
  httpStatus?: number;
  errorCode?: string;
  errorMessage?: string;
  authTenantId?: number;
  authTenantCode?: string;
  crossTenant: boolean;
  createdAt: string;
}
