package com.xuan.erp.iam.application.command;

/**
 * IAM 列权限模板明细保存命令项。
 */
public record IamColumnPermissionTemplateItemCommand(
        Long resourceColumnId,
        String accessMode
) {
}
