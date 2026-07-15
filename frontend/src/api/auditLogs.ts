import { http } from './http';
import type { ApiResponse } from '@/types/auth';
import type { AuditLogEntry, AuditLogQueryParams } from '@/types/auditLog';

function unwrap<T>(body: ApiResponse<T> | T): T {
  if (body && typeof body === 'object' && 'data' in body) {
    return (body as ApiResponse<T>).data as T;
  }
  return body as T;
}

export async function listAuditLogs(params: AuditLogQueryParams): Promise<AuditLogEntry[]> {
  const response = await http.get<ApiResponse<AuditLogEntry[]> | AuditLogEntry[]>(
    '/api/audit/logs',
    {
      params: {
        tenantId: params.tenantId,
        actorUsername: params.actorUsername || undefined,
        action: params.action || undefined,
        entityType: params.entityType || undefined,
        status: params.status || undefined,
        startTime: params.startTime,
        endTime: params.endTime,
        limit: params.limit,
      },
    },
  );
  return unwrap<AuditLogEntry[]>(response.data);
}
