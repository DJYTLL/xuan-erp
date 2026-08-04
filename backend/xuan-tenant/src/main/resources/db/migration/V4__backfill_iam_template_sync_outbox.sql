-- Backfill IAM template sync outbox events for historical active tenant plan assignments.
-- xuan-iam and xuan-tenant use separate databases, so IAM cannot infer the current plan template directly.

WITH active_assignment AS (
    SELECT DISTINCT ON (assignment.tenant_id)
           assignment.id AS assignment_id,
           assignment.tenant_id,
           assignment.plan_id,
           COALESCE(NULLIF(btrim(assignment.assigned_by), ''), 'system') AS operator
    FROM tenant_plan_assignment assignment
    WHERE assignment.deleted_at IS NULL
      AND assignment.status = 'ACTIVE'
      AND assignment.tenant_id > 0
    ORDER BY assignment.tenant_id,
             assignment.effective_at DESC,
             assignment.assigned_at DESC,
             assignment.id DESC
),
template_assignment AS (
    SELECT active_assignment.assignment_id,
           active_assignment.tenant_id,
           active_assignment.plan_id,
           active_assignment.operator,
           NULLIF(btrim(plan.feature_flags ->> 'iamInitTemplateCode'), '') AS iam_init_template_code
    FROM active_assignment
    JOIN tenant_plan plan
      ON plan.id = active_assignment.plan_id
     AND plan.deleted_at IS NULL
    WHERE jsonb_exists(plan.feature_flags, 'iamInitTemplateCode')
),
event_seed AS (
    SELECT template_assignment.assignment_id,
           template_assignment.tenant_id,
           template_assignment.plan_id,
           template_assignment.operator,
           template_assignment.iam_init_template_code,
           'tenant-plan-assignment:' || template_assignment.assignment_id || ':iam-template' AS task_key,
           'tenant-plan-assignment:' || template_assignment.assignment_id || ':iam-template:' || template_assignment.iam_init_template_code AS idempotency_key,
           'tenant-plan-assignment:' || template_assignment.assignment_id || ':iam-template:' || template_assignment.iam_init_template_code || ':v4-backfill' AS event_id
    FROM template_assignment
    WHERE template_assignment.iam_init_template_code IS NOT NULL
)
INSERT INTO tenant_outbox_event (
    tenant_id,
    event_id,
    aggregate_type,
    aggregate_id,
    event_type,
    topic,
    payload,
    headers,
    status,
    retry_count,
    max_retry_count,
    next_retry_at,
    created_by,
    updated_by
)
SELECT event_seed.tenant_id,
       event_seed.event_id,
       'TENANT_PLAN_ASSIGNMENT',
       event_seed.assignment_id,
       'TenantIamBootstrapRequested',
       'xuan-tenant-event',
       jsonb_build_object(
           'tenantId', event_seed.tenant_id,
           'assignmentId', event_seed.assignment_id,
           'planId', event_seed.plan_id,
           'eventType', 'TenantIamBootstrapRequested',
           'taskKey', event_seed.task_key,
           'provisionStep', 'IAM_BOOTSTRAP',
           'idempotencyKey', event_seed.idempotency_key,
           'iamInitTemplateCode', event_seed.iam_init_template_code,
           'callbackRequired', false,
           'occurredAt', now()::text,
           'sourceService', 'xuan-tenant',
           'backfillReason', 'missing-iam-template-binding-v4'
       ),
       jsonb_build_object('sourceService', 'xuan-tenant'),
       'PENDING',
       0,
       5,
       now(),
       event_seed.operator,
       event_seed.operator
FROM event_seed
WHERE NOT EXISTS (
    SELECT 1
    FROM tenant_outbox_event existing
    WHERE existing.tenant_id = event_seed.tenant_id
      AND existing.event_type = 'TenantIamBootstrapRequested'
      AND existing.payload ->> 'idempotencyKey' = event_seed.idempotency_key
);
