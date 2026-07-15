export type TenantStatus = 'PROVISIONING' | 'PROVISIONED' | 'ENABLED' | 'SUSPENDED' | 'DISABLED';

export type TenantProvisionTaskStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELED';

export type TenantProvisionTaskStepStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED';

export type TenantPlanStatus = 'ENABLED' | 'DISABLED';

export type BillingCycle = 'MONTHLY' | 'YEARLY' | 'PERMANENT';

export interface PageResult<TRecord> {
  records: TRecord[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export interface Tenant {
  id: number;
  code: string;
  name: string;
  status: TenantStatus;
  contactName: string | null;
  contactPhone: string | null;
  provisionedAt: string | null;
  enabledAt: string | null;
  remark: string | null;
  currentPlanAssignmentId: number | null;
  currentPlanId: number | null;
  currentPlanCode: string | null;
  currentPlanName: string | null;
  currentPlanExpiresAt: string | null;
  primaryDomainId: number | null;
  primaryDomain: string | null;
  statusHistoryCount: number;
  latestStatusChangeType: string | null;
  latestStatusChangedAt: string | null;
}

export interface CreateTenantPayload {
  code: string;
  name: string;
  contactName?: string;
  contactPhone?: string;
  remark?: string;
  idempotencyKey?: string;
  adminUsername?: string;
  adminPassword?: string;
  adminDisplayName?: string;
  adminEmail?: string;
  adminPhone?: string;
  planId?: number | null;
  planExpiresAt?: string | null;
}

export interface UpdateTenantPayload {
  name: string;
  contactName?: string;
  contactPhone?: string;
  remark?: string;
  idempotencyKey?: string;
}

export interface TenantPlanPayload {
  code?: string;
  name: string;
  billingCycle: BillingCycle;
  priceAmount: number | string;
  currency: string;
  maxUserCount?: number | null;
  maxWarehouseCount?: number | null;
  maxStorageGb?: number | string | null;
  featureFlagsJson: string;
  iamInitTemplateCode?: string;
  sortNo?: number | null;
  remark?: string;
}

export interface TenantPlanAssignmentPayload {
  tenantId: number;
  previousPlanId?: number | null;
  planId: number;
  status: 'ACTIVE' | 'PENDING' | 'EXPIRED' | 'CANCELED';
  effectiveAt?: string | null;
  expiresAt?: string | null;
  assignedAt?: string | null;
  assignedBy?: string;
  changeReason?: string;
  source?: string;
  remark?: string;
}

export interface TenantPlan {
  id: number;
  code: string;
  name: string;
  status: TenantPlanStatus;
  billingCycle: BillingCycle;
  priceAmount: number | string;
  currency: string;
  maxUserCount: number | null;
  maxWarehouseCount: number | null;
  maxStorageGb: number | string | null;
  featureFlagsJson: string | null;
  sortNo: number;
  remark: string | null;
}

export interface TenantProvisionTask {
  id: number;
  tenantId: number;
  taskKey: string;
  taskType: string;
  status: TenantProvisionTaskStatus;
  lastErrorCode?: string | null;
  lastErrorMessage?: string | null;
  steps: TenantProvisionTaskStep[];
}

export interface TenantProvisionTaskStep {
  id: number;
  provisionTaskId: number;
  stepKey: string;
  stepName: string;
  status: TenantProvisionTaskStepStatus;
  sequenceNo: number;
  lastErrorCode?: string | null;
  lastErrorMessage?: string | null;
}

export interface RetryTenantProvisionTaskPayload {
  stepKey: string;
  operator?: string;
  reason: string;
}

export interface RetryTenantOutboxEventPayload {
  operator?: string;
  reason: string;
}
