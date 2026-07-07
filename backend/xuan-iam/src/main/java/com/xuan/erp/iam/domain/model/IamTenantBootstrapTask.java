package com.xuan.erp.iam.domain.model;

import com.xuan.erp.iam.domain.model.type.IamBootstrapTaskStatus;
import java.time.OffsetDateTime;

/**
 * IAM 租户初始化任务领域模型，记录租户菜单授权初始化的幂等执行结果。
 */
public record IamTenantBootstrapTask(
        Long id,
        Long tenantId,
        String taskKey,
        IamBootstrapTaskStatus status,
        String idempotencyKey,
        String requestedBy,
        int menuGrantCount,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String lastErrorMessage,
        String createdBy,
        OffsetDateTime createdAt,
        String updatedBy,
        OffsetDateTime updatedAt
) {
}
