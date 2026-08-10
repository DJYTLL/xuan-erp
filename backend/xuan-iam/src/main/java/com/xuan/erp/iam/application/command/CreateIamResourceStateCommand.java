package com.xuan.erp.iam.application.command;

/**
 * 创建 IAM 资源状态命令。
 */
public record CreateIamResourceStateCommand(
        Long tenantId,
        String resourceKey,
        String stateCode,
        String stateName,
        String description,
        Integer sortNo,
        Boolean enabled,
        String metadataJson,
        String operator
) {
}
