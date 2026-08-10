package com.xuan.erp.iam.application.command;

/**
 * 修改 IAM 资源状态命令。
 */
public record UpdateIamResourceStateCommand(
        String stateName,
        String description,
        Integer sortNo,
        Boolean enabled,
        String metadataJson,
        String operator
) {
}
