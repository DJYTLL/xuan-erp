export interface InterfaceTraceQueryParams {
  serviceName?: string;
  endpointName?: string;
  startTime?: string;
  endTime?: string;
  limit?: number;
}

export interface InterfaceTraceSummary {
  traceId: string;
  serviceName: string;
  endpointName: string;
  durationMs: number;
  startTime: string;
  error: boolean;
}

export interface InterfaceSpanDetail {
  spanId: string;
  parentSpanId: string;
  serviceName: string;
  endpointName: string;
  type: string;
  durationMs: number;
  error: boolean;
}

export interface InterfaceTraceDetail {
  traceId: string;
  spans: InterfaceSpanDetail[];
}

export interface SqlRankingQueryParams {
  databaseNames?: string[];
  sortBy?: string;
  limit?: number;
}

export interface SqlRankingEntry {
  databaseName: string;
  query: string;
  calls: number;
  totalExecTime: number;
  meanExecTime: number;
  maxExecTime: number;
  rows: number;
}
