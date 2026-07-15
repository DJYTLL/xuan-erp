import { http } from './http';
import type { ApiResponse } from '@/types/auth';
import type {
  InterfaceTraceDetail,
  InterfaceTraceQueryParams,
  InterfaceTraceSummary,
  SqlRankingEntry,
  SqlRankingQueryParams,
} from '@/types/observability';

function unwrap<T>(body: ApiResponse<T> | T): T {
  if (body && typeof body === 'object' && 'data' in body) {
    return (body as ApiResponse<T>).data as T;
  }
  return body as T;
}

export async function listInterfaceTraces(params: InterfaceTraceQueryParams): Promise<InterfaceTraceSummary[]> {
  const response = await http.get<ApiResponse<InterfaceTraceSummary[]> | InterfaceTraceSummary[]>(
    '/api/audit/observability/interface-traces',
    { params },
  );
  return unwrap<InterfaceTraceSummary[]>(response.data);
}

export async function getInterfaceTrace(traceId: string): Promise<InterfaceTraceDetail> {
  const response = await http.get<ApiResponse<InterfaceTraceDetail> | InterfaceTraceDetail>(
    `/api/audit/observability/interface-traces/${encodeURIComponent(traceId)}`,
  );
  return unwrap<InterfaceTraceDetail>(response.data);
}

export async function listSqlRankings(params: SqlRankingQueryParams): Promise<SqlRankingEntry[]> {
  const response = await http.get<ApiResponse<SqlRankingEntry[]> | SqlRankingEntry[]>(
    '/api/audit/observability/sql-rankings',
    {
      params: {
        ...params,
        databaseNames: params.databaseNames?.join(','),
      },
    },
  );
  return unwrap<SqlRankingEntry[]>(response.data);
}
