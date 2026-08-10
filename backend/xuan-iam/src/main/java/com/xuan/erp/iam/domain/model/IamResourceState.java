package com.xuan.erp.iam.domain.model;

/**
 * IAM 资源状态定义，表示业务资源可参与状态动作授权的状态。
 */
public record IamResourceState(
        Long id,
        Long tenantId,
        String resourceKey,
        String stateCode,
        String stateName,
        String description,
        Integer sortNo,
        boolean enabled,
        String metadataJson
) {
}
