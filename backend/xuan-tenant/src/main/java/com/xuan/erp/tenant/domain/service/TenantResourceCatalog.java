package com.xuan.erp.tenant.domain.service;

import com.xuan.erp.tenant.domain.model.resource.TenantResourceDefinition;
import java.util.List;
import java.util.Optional;

public record TenantResourceCatalog(List<TenantResourceDefinition> resources) {

    public static TenantResourceCatalog defaultCatalog() {
        return new TenantResourceCatalog(List.of(
                new TenantResourceDefinition("tenants", "tenant", List.of(
                        "code", "normalized_code", "name", "status", "contact_name", "contact_phone",
                        "provisioned_at", "enabled_at", "disabled_at", "disabled_reason", "remark"
                ), true),
                new TenantResourceDefinition("tenant-plans", "tenant_plan", List.of(
                        "code", "name", "status", "billing_cycle", "price_amount", "currency",
                        "max_user_count", "max_warehouse_count", "max_storage_gb", "feature_flags", "sort_no", "remark"
                ), true),
                new TenantResourceDefinition("tenant-plan-assignments", "tenant_plan_assignment", List.of(
                        "tenant_id", "previous_plan_id", "plan_id", "status", "effective_at", "expires_at",
                        "assigned_at", "assigned_by", "change_reason", "source", "remark"
                ), true),
                new TenantResourceDefinition("tenant-domains", "tenant_domain", List.of(
                        "tenant_id", "domain", "normalized_domain", "status", "is_primary",
                        "verification_token", "verified_at", "last_checked_at", "remark"
                ), true),
                new TenantResourceDefinition("tenant-contacts", "tenant_contact", List.of(
                        "tenant_id", "contact_type", "name", "phone", "email", "is_primary", "remark"
                ), true),
                new TenantResourceDefinition("tenant-status-histories", "tenant_status_history", List.of(
                        "tenant_id", "from_status", "to_status", "change_type", "change_reason",
                        "changed_at", "changed_by", "trace_id", "request_id", "source"
                ), false),
                new TenantResourceDefinition("tenant-configs", "tenant_config", List.of(
                        "tenant_id", "config_key", "config_value", "value_type", "description",
                        "is_public", "is_sensitive", "is_encrypted"
                ), true),
                new TenantResourceDefinition("tenant-provision-tasks", "tenant_provision_task", List.of(
                        "tenant_id", "task_key", "task_type", "status", "idempotency_key", "step_name",
                        "request_payload", "result_payload", "retry_count", "max_retry_count",
                        "last_error_code", "last_error_message", "started_at", "finished_at"
                ), true),
                new TenantResourceDefinition("tenant-provision-task-steps", "tenant_provision_task_step", List.of(
                        "tenant_id", "provision_task_id", "step_key", "step_name", "status", "sequence_no",
                        "idempotency_key", "request_payload", "result_payload", "retry_count", "max_retry_count",
                        "last_error_code", "last_error_message", "started_at", "finished_at"
                ), true),
                new TenantResourceDefinition("tenant-outbox-events", "tenant_outbox_event", List.of(
                        "tenant_id", "event_id", "aggregate_type", "aggregate_id", "event_type", "topic",
                        "payload", "headers", "status", "retry_count", "max_retry_count", "locked_by",
                        "locked_at", "lock_expires_at", "next_retry_at", "published_at",
                        "last_error_code", "last_error_message", "first_failed_at", "dead_letter_at"
                ), false)
        ));
    }

    public Optional<TenantResourceDefinition> find(String resourceName) {
        return resources.stream()
                .filter(resource -> resource.resourceName().equals(resourceName))
                .findFirst();
    }
}
